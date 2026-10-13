package com.rolesencia.diceroller.ui.mesa3d

import androidx.compose.ui.geometry.Offset
import com.rolesencia.diceroller.fisica.Vec3

/**
 * Cámara con perspectiva, mirando la bandeja desde arriba y un poco desde el jugador.
 * [ajustar] calcula escala y centro para que la bandeja entre completa en el lienzo.
 */
class Camara(val ojo: Vec3, objetivo: Vec3) {
    val adelante: Vec3 = (objetivo - ojo).normalizado()
    private val derecha: Vec3 = (adelante cross Vec3.ARRIBA).normalizado()
    private val arriba: Vec3 = derecha cross adelante

    var escala = 1f
        private set
    private var cx = 0f
    private var cy = 0f

    fun profundidad(p: Vec3): Float = (p - ojo) dot adelante

    private fun cruda(p: Vec3): Offset {
        val d = p - ojo
        val z = (d dot adelante).coerceAtLeast(0.1f)
        return Offset((d dot derecha) / z, -(d dot arriba) / z)
    }

    fun proyectar(p: Vec3): Offset {
        val c = cruda(p)
        return Offset(cx + escala * c.x, cy + escala * c.y)
    }

    /** Tamaño en píxeles de un objeto de [radio] cm a la distancia de [p]. */
    fun tamanoEnPantalla(p: Vec3, radio: Float): Float = escala * radio / profundidad(p).coerceAtLeast(0.1f)

    fun ajustar(puntos: List<Vec3>, ancho: Float, alto: Float, margen: Float) {
        val crudos = puntos.map { cruda(it) }
        val minX = crudos.minOf { it.x }
        val maxX = crudos.maxOf { it.x }
        val minY = crudos.minOf { it.y }
        val maxY = crudos.maxOf { it.y }
        escala = minOf((ancho - 2 * margen) / (maxX - minX), (alto - 2 * margen) / (maxY - minY))
        cx = ancho / 2f - escala * (minX + maxX) / 2f
        cy = alto / 2f - escala * (minY + maxY) / 2f
    }
}
