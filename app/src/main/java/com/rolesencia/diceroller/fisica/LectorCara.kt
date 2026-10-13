package com.rolesencia.diceroller.fisica

import com.rolesencia.diceroller.domain.dados.TipoDado

data class LecturaCara(
    val indice: Int,
    val valor: Int,
    /** Coseno entre la normal elegida y la vertical. ~1 = bien apoyado; bajo = dado "montado". */
    val alineacion: Float
) {
    /** Dado apoyado contra otro dado o contra la pared: conviene relanzarlo. */
    fun estaMontado(umbral: Float = 0.9f): Boolean = alineacion < umbral
}

/**
 * Traduce la orientación final de un cuerpo rígido en el número del dado.
 * Es el puente entre la simulación física y [com.rolesencia.diceroller.domain.reglas.SistemaReglas].
 */
object LectorCara {

    fun leer(tipo: TipoDado, orientacion: Quat): LecturaCara {
        val referencia = if (GeometriaDados.seLeeHaciaAbajo(tipo)) Vec3.ABAJO else Vec3.ARRIBA
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
