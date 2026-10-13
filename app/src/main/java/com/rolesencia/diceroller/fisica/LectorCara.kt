package com.rolesencia.diceroller.fisica

import com.rolesencia.diceroller.domain.dados.TipoDado

data class LecturaCara(
    val indice: Int,
    val valor: Int,
    /** Coseno entre la normal elegida y la vertical. ~1 = bien apoyado; bajo = dado "montado". */
    val alineacion: Float
) {
    /** Dado apoyado sobre una arista o un vértice (no sobre una cara): conviene empujarlo. */
    fun estaMontado(umbral: Float = 0.9f): Boolean = alineacion < umbral
}

/**
 * Traduce la orientación final de un cuerpo rígido en el número del dado.
 * Es el puente entre la simulación física y [com.rolesencia.diceroller.domain.reglas.SistemaReglas].
 */
object LectorCara {

    /**
     * Cara de arriba respecto de [arriba]: por defecto la vertical, pero puede ser la normal de la
     * superficie que sostiene al dado (una rampa, la cara de otro dado). Un dado apoyado plano sobre
     * una rampa muestra "arriba" la cara paralela a la rampa, como en la mesa real.
     */
    fun leer(tipo: TipoDado, orientacion: Quat, arriba: Vec3 = Vec3.ARRIBA): LecturaCara {
        val referencia = if (GeometriaDados.seLeeHaciaAbajo(tipo)) -arriba else arriba
        var mejor = 0
        var mejorDot = -2f
        GeometriaDados.normales(tipo).forEachIndexed { i, normalLocal ->
            val d = orientacion.rotar(normalLocal) dot referencia
            if (d > mejorDot) {
                mejorDot = d
                mejor = i
            }
        }
        return LecturaCara(mejor, tipo.valoresCaras[mejor], mejorDot)
    }

    /**
     * Rotación local que pone la cara [indice] mirando hacia arriba. Sirve para la animación
     * de "asentamiento" y para el modo opcional de resultado predeterminado (ver plan).
     */
    fun orientacionParaCara(tipo: TipoDado, indice: Int): Quat {
        val normal = GeometriaDados.normales(tipo)[indice]
        val destino = if (GeometriaDados.seLeeHaciaAbajo(tipo)) Vec3.ABAJO else Vec3.ARRIBA
        return Quat.entre(normal, destino)
    }
}
