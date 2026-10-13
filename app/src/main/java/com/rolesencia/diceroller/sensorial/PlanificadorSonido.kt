package com.rolesencia.diceroller.sensorial

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.fisica.EventoColision
import com.rolesencia.diceroller.fisica.Superficie
import com.rolesencia.diceroller.fisica.TipoEvento
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

enum class PesoDado { LIGERO, PESADO }

/** Material del piso de la bandeja. Las paredes siempre son de madera. */
enum class MaterialMesa(val titulo: String) { LONA("Tapete"), MADERA("Madera") }

/** Los 13 bancos grabados (5 tomas cada uno = 65 archivos en res/raw). */
enum class Categoria {
    LIGERO_LONA_SUAVE, LIGERO_LONA_FUERTE, PESADO_LONA_SUAVE, PESADO_LONA_FUERTE,
    LIGERO_MADERA_SUAVE, LIGERO_MADERA_FUERTE, PESADO_MADERA_SUAVE, PESADO_MADERA_FUERTE,
    LIGERO_LIGERO_ROCE, LIGERO_LIGERO_IMPACTO, PESADO_PESADO_IMPACTO,
    LIGERO_ASENTAMIENTO, PESADO_ASENTAMIENTO
}

/** Una reproducción concreta: qué toma de qué banco, con qué volumen por canal y qué tono. */
data class Disparo(
    val categoria: Categoria,
    val muestra: Int,
    val volumenIzq: Float,
    val volumenDer: Float,
    val tono: Float
) {
    val prioridad: Float get() = maxOf(volumenIzq, volumenDer)
}

/**
 * "Bolsa" de tomas: se reparten las 5 en orden aleatorio y recién al vaciarse se rellena.
 * Nunca suena la misma toma dos veces seguidas (ni siquiera al rellenar la bolsa).
 */
class BolsaAleatoria(private val tamano: Int, private val random: Random) {
    private val pendientes = ArrayDeque<Int>()
    private var ultima = -1

    fun siguiente(): Int {
        if (pendientes.isEmpty()) {
            val nueva = (0 until tamano).shuffled(random).toMutableList()
            if (tamano > 1 && nueva.first() == ultima) nueva.add(nueva.removeAt(0))
            pendientes.addAll(nueva)
        }
        return pendientes.removeFirst().also { ultima = it }
    }
}

fun pesoDe(tipo: TipoDado): PesoDado = when (tipo) {
    TipoDado.D4, TipoDado.D6, TipoDado.D8, TipoDado.FATE -> PesoDado.LIGERO
    else -> PesoDado.PESADO
}

/**
 * Traduce un evento físico en uno o dos [Disparo]s. Fuentes de variedad (además de las 65 tomas):
 *  - bolsa aleatoria por banco (sin repeticiones inmediatas);
 *  - capa suave/fuerte elegida por probabilidad según la intensidad (zona de transición, no un corte);
 *  - golpes muy fuertes = dos tomas superpuestas (fuerte + cuerpo suave), combinación casi única;
 *  - tono según el tamaño del dado (d4 agudo, d20 grave) ± 2,5 % aleatorio y un leve brillo con la fuerza;
 *  - paneo estéreo según dónde ocurrió el choque en la mesa;
 *  - curva de volumen perceptual y ±8 % de variación;
 *  - choques de dado contra dado mezclan bancos ligero/pesado y usan el roce para los rozones;
 *  - material de la mesa elegible: tapete (lona) o madera, cada uno con sus 20 tomas.
 * Es Kotlin puro: se prueba en la JVM sin SoundPool.
 */
class PlanificadorSonido(private val random: Random = Random.Default) {

    private val bolsas = Categoria.entries.associateWith { BolsaAleatoria(TOMAS_POR_BANCO, random) }
    private val ultimoPorFuente = HashMap<Long, Pair<Float, Float>>()

    companion object {
        const val TOMAS_POR_BANCO = 5
        const val VELOCIDAD_MAXIMA = 250f   // cm/s de impacto que suena "a tope"
        const val ESPERA_MISMA_FUENTE = 0.035f
    }

    fun planificar(evento: EventoColision, mesa: MaterialMesa, mitadAnchoMesa: Float, ahora: Float): List<Disparo> {
        val intensidad = when (evento.tipo) {
            TipoEvento.IMPACTO -> (evento.velocidad / VELOCIDAD_MAXIMA).coerceIn(0f, 1f)
            TipoEvento.ROCE -> (evento.velocidad / 80f).coerceIn(0f, 1f) * 0.6f
            TipoEvento.ASENTAMIENTO -> 0.30f + random.nextFloat() * 0.15f
        }
        if (intensidad < 0.02f) return emptyList()
        if (!pasaEspera(evento, intensidad, ahora)) return emptyList()

        val peso = pesoDe(evento.dado.tipo)
        val paneo = (evento.posicion.x / mitadAnchoMesa).coerceIn(-1f, 1f) * 0.8f
        val disparos = ArrayList<Disparo>(2)

        when {
            evento.tipo == TipoEvento.ASENTAMIENTO -> disparos += disparo(
                if (peso == PesoDado.LIGERO) Categoria.LIGERO_ASENTAMIENTO else Categoria.PESADO_ASENTAMIENTO,
                intensidad, 0.75f, evento.dado, paneo
            )
            evento.superficie == Superficie.DADO -> disparos += disparoEntreDados(evento, intensidad, paneo)
            else -> {
                val material = if (evento.superficie == Superficie.PARED) MaterialMesa.MADERA else mesa
                val fuerte = random.nextFloat() < suavizado(0.25f, 0.55f, intensidad)
                val eq = if (material == MaterialMesa.LONA) 0.9f else 1f
                disparos += disparo(banco(material, peso, fuerte), intensidad, eq, evento.dado, paneo)
                // Golpe muy fuerte: se suma una toma suave como "cuerpo" del sonido.
                if (intensidad > 0.75f && fuerte) {
                    disparos += disparo(banco(material, peso, false), intensidad * 0.4f, eq, evento.dado, paneo)
                }
            }
        }
        return disparos
    }

    private fun disparoEntreDados(evento: EventoColision, intensidad: Float, paneo: Float): Disparo {
        val pesoA = pesoDe(evento.dado.tipo)
        val pesoB = evento.otro?.let { pesoDe(it.tipo) } ?: pesoA
        val esRoce = evento.tipo == TipoEvento.ROCE || intensidad < 0.12f
        val pesados = listOf(pesoA, pesoB).count { it == PesoDado.PESADO }
        return if (esRoce) {
            // El banco de roce es de dados ligeros: para los pesados se baja el tono.
            disparo(Categoria.LIGERO_LIGERO_ROCE, intensidad, 0.6f, evento.dado, paneo, tonoExtra = if (pesados > 0) 0.9f else 1f)
        } else {
            val categoria = when (pesados) {
                0 -> Categoria.LIGERO_LIGERO_IMPACTO
                2 -> Categoria.PESADO_PESADO_IMPACTO
                else -> if (random.nextBoolean()) Categoria.LIGERO_LIGERO_IMPACTO else Categoria.PESADO_PESADO_IMPACTO
            }
            disparo(categoria, intensidad, 0.85f, evento.dado, paneo)
        }
    }

    private fun banco(material: MaterialMesa, peso: PesoDado, fuerte: Boolean): Categoria = when (material) {
        MaterialMesa.LONA -> when (peso) {
            PesoDado.LIGERO -> if (fuerte) Categoria.LIGERO_LONA_FUERTE else Categoria.LIGERO_LONA_SUAVE
            PesoDado.PESADO -> if (fuerte) Categoria.PESADO_LONA_FUERTE else Categoria.PESADO_LONA_SUAVE
        }
        MaterialMesa.MADERA -> when (peso) {
            PesoDado.LIGERO -> if (fuerte) Categoria.LIGERO_MADERA_FUERTE else Categoria.LIGERO_MADERA_SUAVE
            PesoDado.PESADO -> if (fuerte) Categoria.PESADO_MADERA_FUERTE else Categoria.PESADO_MADERA_SUAVE
        }
    }

    private fun disparo(
        categoria: Categoria, intensidad: Float, ecualizacion: Float, dado: DadoEnMesa, paneo: Float, tonoExtra: Float = 1f
    ): Disparo {
        val volumen = ((0.10f + 0.90f * intensidad.pow(0.75f)) * ecualizacion * (0.92f + random.nextFloat() * 0.16f))
            .coerceIn(0.05f, 1f)
        val tono = (tonoPorTamano(dado.tipo) * tonoExtra * (1f + (random.nextFloat() - 0.5f) * 0.05f) * (1f + 0.03f * intensidad))
            .coerceIn(0.5f, 2f)
        // Paneo de potencia constante, compensado para que en el centro no baje el volumen.
        val angulo = (paneo + 1f) * (PI.toFloat() / 4f)
        val izq = (volumen * cos(angulo) * 1.414f).coerceAtMost(1f)
        val der = (volumen * sin(angulo) * 1.414f).coerceAtMost(1f)
        return Disparo(categoria, bolsas.getValue(categoria).siguiente(), izq, der, tono)
    }

    private fun tonoPorTamano(tipo: TipoDado): Float = when (tipo) {
        TipoDado.D4 -> 1.07f
        TipoDado.D6, TipoDado.FATE -> 1.03f
        TipoDado.D8 -> 1.00f
        TipoDado.D10, TipoDado.D100_DECENAS -> 0.98f
        TipoDado.D12 -> 0.96f
        TipoDado.LEGENDARIO -> 0.93f   // el Legendario es más grande y pesado
        TipoDado.D20 -> 0.94f
    }

    /** Evita "ametralladoras": la misma fuente no suena de nuevo antes de 35 ms, salvo un golpe claramente mayor. */
    private fun pasaEspera(evento: EventoColision, intensidad: Float, ahora: Float): Boolean {
        val otro = evento.otro?.id ?: (1000 + evento.superficie.ordinal)
        val clave = (evento.dado.id.toLong() shl 32) or (otro.toLong() shl 4) or evento.tipo.ordinal.toLong()
        val previo = ultimoPorFuente[clave]
        if (previo != null && ahora - previo.first < ESPERA_MISMA_FUENTE && intensidad < previo.second * 1.5f) return false
        ultimoPorFuente[clave] = ahora to intensidad
        return true
    }

    private fun suavizado(desde: Float, hasta: Float, x: Float): Float {
        val t = ((x - desde) / (hasta - desde)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
