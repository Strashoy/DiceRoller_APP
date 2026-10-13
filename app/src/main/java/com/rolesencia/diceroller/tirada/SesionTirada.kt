package com.rolesencia.diceroller.tirada

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.fisica.EventoColision
import com.rolesencia.diceroller.fisica.Quat
import com.rolesencia.diceroller.fisica.Vec3

/** Las dos maneras de decidir el número de cada dado. Cada una vive en su propio paquete. */
enum class ModoDecision(val titulo: String) {
    /** La simulación corre en vivo y el número es la cara que quedó arriba ([fisicareal]). */
    FISICA_REAL("Física real"),
    /** El número se elige antes; la física se pre-simula y se reproduce mostrando ese número ([predeterminada]). */
    PREDETERMINADO("Resultado predeterminado")
}

/** Medidas internas de la bandeja, en cm. */
data class ConfigMesa(val ancho: Float = ANCHO_BANDEJA, val profundidad: Float = 24f) {
    companion object {
        /** Bandeja de dados típica: 18 cm de ancho. La profundidad sigue la proporción de la pantalla. */
        const val ANCHO_BANDEJA = 18f

        /** Con muchos dados la bandeja crece para que no se formen pilas (y los dados se ven más chicos). */
        fun para(cantidadDados: Int, altoSobreAncho: Float): ConfigMesa {
            val ancho = when {
                cantidadDados <= 10 -> ANCHO_BANDEJA
                cantidadDados <= 20 -> 24f
                else -> 30f
            }
            return ConfigMesa(ancho, (ancho * altoSobreAncho).coerceIn(ancho * 0.8f, ancho * 2f))
        }
    }
}

/** Dirección del deslizamiento en la mesa (x: derecha, z: hacia el fondo = negativo) y fuerza 0..1. */
data class Gesto(val dirX: Float, val dirZ: Float, val fuerza: Float)

/** Lo que el render necesita de cada dado en un cuadro. [orientacion] es la VISUAL (puede diferir de la física). */
data class DadoVisual(
    val dado: DadoEnMesa,
    val posicion: Vec3,
    val orientacion: Quat,
    val enReposo: Boolean
)

class FrameMesa(val dados: List<DadoVisual>, val eventos: List<EventoColision>)

data class MetricasSesion(
    val modo: ModoDecision,
    /** Sólo en el modo predeterminado: cuánto tardó la simulación completa antes de mostrar nada. */
    val preSimulacionMs: Long? = null,
    val segundosSimulados: Float = 0f,
    val pasos: Int = 0,
    /** Dados montados que se "empujaron" para que caigan bien. */
    val empujones: Int = 0,
    /** Dados que hubo que apoyar a la fuerza (montados persistentes o límite de tiempo). */
    val forzados: Int = 0,
    /** Golpecitos a la torre para destrabar dados (sólo con torre). */
    val golpecitos: Int = 0,
    /** Explicación por dado de lo que hizo el modo. */
    val detalle: List<String> = emptyList()
)

/**
 * Una tirada en curso, vista desde la mesa 3D. La mesa llama a [avanzar] en cada cuadro con el
 * tiempo real transcurrido y dibuja lo que recibe; cuando [terminada], pide [resultados].
 */
interface SesionTirada {
    val modo: ModoDecision
    val dados: List<DadoEnMesa>
    fun avanzar(dtReal: Float): FrameMesa
    val terminada: Boolean
    fun resultados(): List<ResultadoDado>
    fun metricas(): MetricasSesion
    /** Posición final de cada dado, para dejarlo en la mesa (y como obstáculo en explosiones). */
    fun estadoFinal(): List<DadoVisual>
}
