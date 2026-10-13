package com.rolesencia.diceroller.sensorial

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import com.rolesencia.diceroller.R
import com.rolesencia.diceroller.fisica.EventoColision

/** Cuánta variedad sonó en la última tirada (se muestra en el panel de métricas). */
data class EstadisticasSonido(
    val eventos: Int = 0,
    val disparos: Int = 0,
    val descartados: Int = 0,
    val tomasDistintas: Int = 0,
    val tomasTotales: Int = Categoria.entries.size * PlanificadorSonido.TOMAS_POR_BANCO
)

/**
 * Reproduce los [Disparo]s que decide [PlanificadorSonido] con SoundPool (latencia mínima, muchas
 * voces cortas a la vez). Los 65 archivos son los del Lanzador Legendario.
 */
class GestorSonido(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_VOCES)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val planificador = PlanificadorSonido()
    private val bancos: Map<Categoria, List<Int>>
    private var cargados = 0
    private val totalArchivos = Categoria.entries.size * PlanificadorSonido.TOMAS_POR_BANCO

    private var estadisticas = EstadisticasSonido()
    private val tomasUsadas = HashSet<Pair<Categoria, Int>>()

    companion object {
        const val MAX_VOCES = 16
        /** Como mucho estas reproducciones nuevas por cuadro; se priorizan las más fuertes. */
        const val MAX_POR_CUADRO = 6
    }

    init {
        soundPool.setOnLoadCompleteListener { _, _, estado -> if (estado == 0) cargados++ }
        fun cargar(vararg recursos: Int) = recursos.map { soundPool.load(context, it, 1) }
        bancos = mapOf(
            Categoria.LIGERO_LONA_SUAVE to cargar(R.raw.ligero_lona_suave_1, R.raw.ligero_lona_suave_2, R.raw.ligero_lona_suave_3, R.raw.ligero_lona_suave_4, R.raw.ligero_lona_suave_5),
            Categoria.LIGERO_LONA_FUERTE to cargar(R.raw.ligero_lona_fuerte_1, R.raw.ligero_lona_fuerte_2, R.raw.ligero_lona_fuerte_3, R.raw.ligero_lona_fuerte_4, R.raw.ligero_lona_fuerte_5),
            Categoria.PESADO_LONA_SUAVE to cargar(R.raw.pesado_lona_suave_1, R.raw.pesado_lona_suave_2, R.raw.pesado_lona_suave_3, R.raw.pesado_lona_suave_4, R.raw.pesado_lona_suave_5),
            Categoria.PESADO_LONA_FUERTE to cargar(R.raw.pesado_lona_fuerte_1, R.raw.pesado_lona_fuerte_2, R.raw.pesado_lona_fuerte_3, R.raw.pesado_lona_fuerte_4, R.raw.pesado_lona_fuerte_5),
            Categoria.LIGERO_MADERA_SUAVE to cargar(R.raw.ligero_madera_suave_1, R.raw.ligero_madera_suave_2, R.raw.ligero_madera_suave_3, R.raw.ligero_madera_suave_4, R.raw.ligero_madera_suave_5),
            Categoria.LIGERO_MADERA_FUERTE to cargar(R.raw.ligero_madera_fuerte_1, R.raw.ligero_madera_fuerte_2, R.raw.ligero_madera_fuerte_3, R.raw.ligero_madera_fuerte_4, R.raw.ligero_madera_fuerte_5),
            Categoria.PESADO_MADERA_SUAVE to cargar(R.raw.pesado_madera_suave_1, R.raw.pesado_madera_suave_2, R.raw.pesado_madera_suave_3, R.raw.pesado_madera_suave_4, R.raw.pesado_madera_suave_5),
            Categoria.PESADO_MADERA_FUERTE to cargar(R.raw.pesado_madera_fuerte_1, R.raw.pesado_madera_fuerte_2, R.raw.pesado_madera_fuerte_3, R.raw.pesado_madera_fuerte_4, R.raw.pesado_madera_fuerte_5),
            Categoria.LIGERO_LIGERO_ROCE to cargar(R.raw.ligero_ligero_roce_1, R.raw.ligero_ligero_roce_2, R.raw.ligero_ligero_roce_3, R.raw.ligero_ligero_roce_4, R.raw.ligero_ligero_roce_5),
            Categoria.LIGERO_LIGERO_IMPACTO to cargar(R.raw.ligero_ligero_impacto_1, R.raw.ligero_ligero_impacto_2, R.raw.ligero_ligero_impacto_3, R.raw.ligero_ligero_impacto_4, R.raw.ligero_ligero_impacto_5),
            Categoria.PESADO_PESADO_IMPACTO to cargar(R.raw.pesado_pesado_impacto_1, R.raw.pesado_pesado_impacto_2, R.raw.pesado_pesado_impacto_3, R.raw.pesado_pesado_impacto_4, R.raw.pesado_pesado_impacto_5),
            Categoria.LIGERO_ASENTAMIENTO to cargar(R.raw.ligero_asentamiento_1, R.raw.ligero_asentamiento_2, R.raw.ligero_asentamiento_3, R.raw.ligero_asentamiento_4, R.raw.ligero_asentamiento_5),
            Categoria.PESADO_ASENTAMIENTO to cargar(R.raw.pesado_asentamiento_1, R.raw.pesado_asentamiento_2, R.raw.pesado_asentamiento_3, R.raw.pesado_asentamiento_4, R.raw.pesado_asentamiento_5)
        )
    }

    /** Se llama una vez por cuadro con los eventos físicos nuevos. */
    fun reproducir(eventos: List<EventoColision>, mesa: MaterialMesa, mitadAnchoMesa: Float) {
        if (eventos.isEmpty() || cargados < totalArchivos) return
        val ahora = SystemClock.uptimeMillis() / 1000f
        val candidatos = eventos.flatMap { planificador.planificar(it, mesa, mitadAnchoMesa, ahora) }
        val elegidos = candidatos.sortedByDescending { it.prioridad }.take(MAX_POR_CUADRO)
        for (d in elegidos) {
            soundPool.play(bancos.getValue(d.categoria)[d.muestra], d.volumenIzq, d.volumenDer, 1, 0, d.tono)
            tomasUsadas += d.categoria to d.muestra
        }
        estadisticas = estadisticas.copy(
            eventos = estadisticas.eventos + eventos.size,
            disparos = estadisticas.disparos + elegidos.size,
            descartados = estadisticas.descartados + (candidatos.size - elegidos.size),
            tomasDistintas = tomasUsadas.size
        )
    }

    fun reiniciarEstadisticas() {
        estadisticas = EstadisticasSonido()
        tomasUsadas.clear()
    }

    fun estadisticas(): EstadisticasSonido = estadisticas

    fun liberarMemoria() {
        soundPool.release()
    }
}
