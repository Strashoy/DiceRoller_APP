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

/** Material del piso de la bandeja (sus paredes son de madera). Las torres usan [PerfilAcustico]. */
enum class MaterialMesa(val titulo: String) { LONA("Tapete"), MADERA("Madera") }

/**
 * Cómo suena cada lugar donde se lanza, con las mismas 65 grabaciones. Lo que se puede variar sin
 * grabar nada nuevo: el banco del piso y de las paredes (madera o tapete), la capa (toma suave o
 * fuerte), el tono (velocidad de reproducción) y el volumen.
 *
 * - **Torre de madera (taberna)**: aguda y seca. Madera en todo, tono +18 %, más tomas fuertes.
 * - **Caja de cuero**: apagada. Tapete también en las paredes (cuero acolchado), tono −20 %, casi
 *   todo tomas suaves, sin capa de refuerzo y a ~70 % de volumen; los dados entre sí también suenan
 *   más graves y bajos, amortiguados por el fieltro.
 * - **Castillo**: piedra, apenas más grave que la madera.
 * - **Bandeja**: el piso según su superficie y las paredes de madera (como siempre).
 */
data class PerfilAcustico(
    val piso: MaterialMesa,
    val pared: MaterialMesa,
    /** Multiplica el tono de todo (velocidad de reproducción): > 1 agudo, < 1 grave. */
    val tono: Float,
    /** Se suma a la intensidad sólo para elegir la capa: > 0 más tomas fuertes, < 0 más suaves. */
    val sesgoFuerza: Float,
    val volumen: Float,
    val tonoEntreDados: Float,
    val volumenEntreDados: Float,
    /** En golpes muy fuertes, sumar una toma suave como "cuerpo" del sonido. */
    val capaCuerpo: Boolean
) {
    companion object {
        fun bandeja(piso: MaterialMesa) =
            PerfilAcustico(piso, MaterialMesa.MADERA, 1f, 0f, 1f, 1f, 1f, capaCuerpo = true)

        val TABERNA = PerfilAcustico(MaterialMesa.MADERA, MaterialMesa.MADERA, tono = 1.18f, sesgoFuerza = 0.18f,
            volumen = 1f, tonoEntreDados = 1.10f, volumenEntreDados = 1f, capaCuerpo = true)
        val CASTILLO = PerfilAcustico(MaterialMesa.MADERA, MaterialMesa.MADERA, tono = 0.95f, sesgoFuerza = 0.05f,
            volumen = 0.95f, tonoEntreDados = 1f, volumenEntreDados = 0.95f, capaCuerpo = true)
        val CUERO = PerfilAcustico(MaterialMesa.LONA, MaterialMesa.LONA, tono = 0.80f, sesgoFuerza = -0.30f,
            volumen = 0.70f, tonoEntreDados = 0.85f, volumenEntreDados = 0.65f, capaCuerpo = false)

        /** Perfil del lugar donde se lanza: la torre, o la bandeja según su superficie. */
        fun para(torre: String?, mesa: MaterialMesa): PerfilAcustico = when (torre) {
            "madera" -> TABERNA
            "castillo" -> CASTILLO
            "cuero" -> CUERO
            else -> bandeja(mesa)
        }
    }
}

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

    /** Bandeja con esa superficie (paredes de madera). */
    fun planificar(evento: EventoColision, mesa: MaterialMesa, mitadAnchoMesa: Float, ahora: Float): List<Disparo> =
        planificar(evento, PerfilAcustico.bandeja(mesa), mitadAnchoMesa, ahora)

    fun planificar(evento: EventoColision, perfil: PerfilAcustico, mitadAnchoMesa: Float, ahora: Float): List<Disparo> {
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
                intensidad, 0.75f * perfil.volumen, evento.dado, paneo, tonoExtra = perfil.tono
            )
            evento.superficie == Superficie.DADO -> disparos += disparoEntreDados(evento, intensidad, paneo, perfil)
            else -> {
                val material = if (evento.superficie == Superficie.PARED) perfil.pared else perfil.piso
                val fuerte = random.nextFloat() < suavizado(0.25f, 0.55f, intensidad + perfil.sesgoFuerza)
                val eq = (if (material == MaterialMesa.LONA) 0.9f else 1f) * perfil.volumen
                disparos += disparo(banco(material, peso, fuerte), intensidad, eq, evento.dado, paneo, tonoExtra = perfil.tono)
                // Golpe muy fuerte: se suma una toma suave como "cuerpo" del sonido.
                if (perfil.capaCuerpo && intensidad > 0.75f && fuerte) {
                    disparos += disparo(banco(material, peso, false), intensidad * 0.4f, eq, evento.dado, paneo, tonoExtra = perfil.tono)
                }
            }
        }
        return disparos
    }

    private fun disparoEntreDados(evento: EventoColision, intensidad: Float, paneo: Float, perfil: PerfilAcustico): Disparo {
        val pesoA = pesoDe(evento.dado.tipo)
        val pesoB = evento.otro?.let { pesoDe(it.tipo) } ?: pesoA
        val esRoce = evento.tipo == TipoEvento.ROCE || intensidad < 0.12f
        val pesados = listOf(pesoA, pesoB).count { it == PesoDado.PESADO }
        return if (esRoce) {
            // El banco de roce es de dados ligeros: para los pesados se baja el tono.
            disparo(
                Categoria.LIGERO_LIGERO_ROCE, intensidad, 0.6f * perfil.volumenEntreDados, evento.dado, paneo,
                tonoExtra = (if (pesados > 0) 0.9f else 1f) * perfil.tonoEntreDados
            )
        } else {
            val categoria = when (pesados) {
                0 -> Categoria.LIGERO_LIGERO_IMPACTO
                2 -> Categoria.PESADO_PESADO_IMPACTO
                else -> if (random.nextBoolean()) Categoria.LIGERO_LIGERO_IMPACTO else Categoria.PESADO_PESADO_IMPACTO
            }
            disparo(categoria, intensidad, 0.85f * perfil.volumenEntreDados, evento.dado, paneo, tonoExtra = perfil.tonoEntreDados)
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
