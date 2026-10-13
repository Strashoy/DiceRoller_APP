package com.rolesencia.diceroller.ui.mesa3d

import androidx.compose.ui.geometry.Offset
import com.rolesencia.diceroller.fisica.Vec3
import kotlin.math.abs

/**
 * Cámara con perspectiva, mirando la bandeja desde arriba y un poco desde el jugador.
 * [ajustar] calcula escala y centro para que la bandeja entre completa en el lienzo.
 */
class Camara(val ojo: Vec3, objetivo: Vec3) {
    val adelante: Vec3 = (objetivo - ojo).normalizado()
    private val derecha: Vec3 = (adelante cross Vec3.ARRIBA).normalizado()
    private val arriba: Vec3 = derecha cross adelante

    /**
     * 0 en la vista general, 1 mirando a plomo (vista del resultado). Sirve para que en la toma
     * cenital las caras de arriba muestren el color de la skin tal cual, sin reflejos.
     */
    val cenital: Float = ((abs(adelante.y) - 0.97f) / 0.03f).coerceIn(0f, 1f)

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
        fijar(encuadre(puntos, ancho, alto, margen))
    }

    /**
     * Pasa de encuadrar [desde] a encuadrar [hasta] (fracción [t]): escala y centro se mezclan, así
     * el zoom acompaña al movimiento de la cámara sin saltos.
     */
    fun ajustarEntre(desde: List<Vec3>, margenDesde: Float, hasta: List<Vec3>, margenHasta: Float, ancho: Float, alto: Float, t: Float) {
        val a = encuadre(desde, ancho, alto, margenDesde)
        if (t <= 0f) return fijar(a)
        val b = encuadre(hasta, ancho, alto, margenHasta)
        fijar(Encuadre(a.escala + (b.escala - a.escala) * t, a.cx + (b.cx - a.cx) * t, a.cy + (b.cy - a.cy) * t))
    }

    private class Encuadre(val escala: Float, val cx: Float, val cy: Float)

    private fun fijar(e: Encuadre) {
        escala = e.escala
        cx = e.cx
        cy = e.cy
    }

    private fun encuadre(puntos: List<Vec3>, ancho: Float, alto: Float, margen: Float): Encuadre {
        val crudos = puntos.map { cruda(it) }
        val minX = crudos.minOf { it.x }
        val maxX = crudos.maxOf { it.x }
        val minY = crudos.minOf { it.y }
        val maxY = crudos.maxOf { it.y }
        val e = minOf((ancho - 2 * margen) / (maxX - minX), (alto - 2 * margen) / (maxY - minY))
        return Encuadre(e, ancho / 2f - e * (minX + maxX) / 2f, alto / 2f - e * (minY + maxY) / 2f)
    }
}
