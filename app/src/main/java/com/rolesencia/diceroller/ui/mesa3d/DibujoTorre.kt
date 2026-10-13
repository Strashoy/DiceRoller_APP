package com.rolesencia.diceroller.ui.mesa3d

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.rolesencia.diceroller.fisica.Poliedros
import com.rolesencia.diceroller.fisica.Vec3
import com.rolesencia.diceroller.tirada.torre.Acabado
import com.rolesencia.diceroller.tirada.torre.ModeloTorre
import kotlin.math.ceil
import kotlin.math.max

/** Algo para dibujar en orden de profundidad (lo más lejano primero). */
class Dibujable(val profundidad: Float, val dibujar: DrawScope.() -> Unit)

private val LUZ_TORRE = Vec3(-0.35f, 1f, 0.45f).normalizado()

/** Una baldosa de una cara de la torre (las caras grandes se parten para ordenar bien contra los dados). */
private class Tesela(
    val esquinas: List<Vec3>,
    val centro: Vec3,
    val normal: Vec3,
    val color: Color,
    val juntas: Color?,
    val alfa: Float
)

/** Contorno de una cara: borde oscuro, o costura punteada en el cuero. */
private class Contorno(val esquinas: List<Vec3>, val centro: Vec3, val normal: Vec3, val color: Color, val punteado: Boolean)

/**
 * Geometría visible de una torre, precalculada una vez. Cada cuadro sólo se proyecta,
 * se descartan las caras de espaldas y se arma la lista de [Dibujable]s.
 */
class EscenaTorre(val modelo: ModeloTorre) {

    private val teselas = ArrayList<Tesela>()
    private val contornos = ArrayList<Contorno>()

    /** Vértices de todas las piezas visibles (para encuadrar la cámara). */
    val vertices = ArrayList<Vec3>()

    init {
        for (pv in modelo.piezas) {
            if (pv.acabado == Acabado.INVISIBLE) continue
            val p = pv.pieza
            val poli = Poliedros.caja(p.mitad)
            val mundo = poli.vertices.map { p.centro + p.rotacion.rotar(it) }
            vertices += mundo
            val detalle = Color(pv.colorDetalle)
            for (cara in poli.caras) {
                val esq = cara.vertices.map { mundo[it] }
                val normal = p.rotacion.rotar(cara.normal)
                val centro = esq.reduce { a, b -> a + b } * 0.25f
                // Forro: las caras que miran al interior de la torre llevan otro color y van lisas.
                val forrada = pv.colorInterior != null && pv.puntoInterior != null &&
                    ((pv.puntoInterior - centro) dot normal) > 0f
                val base = Color(if (forrada) pv.colorInterior!! else pv.color)
                val (tamU, tamV) = when (pv.acabado) {
                    Acabado.LADRILLO -> 2.2f to 1.1f
                    Acabado.PIEDRA -> 2.0f to 1.6f
                    else -> 2.5f to 2.5f
                }
                val eu = esq[1] - esq[0]
                val ev = esq[3] - esq[0]
                val nu = max(1, ceil(eu.longitud / tamU).toInt())
                val nv = max(1, ceil(ev.longitud / tamV).toInt())
                for (i in 0 until nu) for (j in 0 until nv) {
                    fun punto(a: Float, b: Float) = esq[0] + eu * a + ev * b
                    val a0 = i / nu.toFloat(); val a1 = (i + 1) / nu.toFloat()
                    val b0 = j / nv.toFloat(); val b1 = (j + 1) / nv.toFloat()
                    val esqT = listOf(punto(a0, b0), punto(a1, b0), punto(a1, b1), punto(a0, b1))
                    // Variación de tono por baldosa: piedras y ladrillos no son todos iguales.
                    val variacion = when (pv.acabado) {
                        Acabado.PIEDRA -> ((i * 73 + j * 151 + nu * 7) % 9) / 9f * 0.22f - 0.11f
                        Acabado.LADRILLO -> ((i * 31 + j * 17) % 5) / 5f * 0.08f - 0.04f
                        else -> 0f
                    }
                    val color = if (variacion >= 0f) lerp(base, Color.White, variacion) else lerp(base, Color.Black, -variacion)
                    val juntas = when (pv.acabado) {
                        Acabado.LADRILLO, Acabado.PIEDRA -> detalle
                        else -> null
                    }
                    teselas += Tesela(esqT, esqT.reduce { a, b -> a + b } * 0.25f, normal, color, juntas,
                        if (pv.acabado == Acabado.VIDRIO) 0.16f else 1f,
                    )
                }
                if (pv.acabado == Acabado.COSTURA && !forrada) {
                    // Costura: rectángulo un poco hacia adentro del borde de la cara.
                    val inset = esq.map { it + (centro - it).normalizado() * 0.45f }
                    contornos += Contorno(inset, centro, normal, detalle, punteado = true)
                }
                contornos += Contorno(esq, centro, normal, lerp(base, Color.Black, 0.55f), punteado = false)
            }
        }
    }

    fun dibujables(camara: Camara): List<Dibujable> {
        val lista = ArrayList<Dibujable>(teselas.size + contornos.size)
        for (t in teselas) {
            if (((camara.ojo - t.centro) dot t.normal) <= 0f) continue
            val luz = 0.45f + 0.55f * maxOf(0f, t.normal dot LUZ_TORRE)
            val color = lerp(Color.Black, t.color, luz)
            val pts = t.esquinas.map { camara.proyectar(it) }
            lista += Dibujable(camara.profundidad(t.centro)) {
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y); lineTo(pts[1].x, pts[1].y); lineTo(pts[2].x, pts[2].y); lineTo(pts[3].x, pts[3].y); close()
                }
                drawPath(path, color.copy(alpha = t.alfa))
                if (t.juntas != null) drawPath(path, t.juntas.copy(alpha = 0.55f * t.alfa), style = Stroke(width = 1f))
                // Superficies lisas: un trazo del mismo color tapa la junta entre baldosas vecinas.
                else if (t.alfa >= 1f) drawPath(path, color, style = Stroke(width = 1.5f))
            }
        }
        for (c in contornos) {
            if (((camara.ojo - c.centro) dot c.normal) <= 0f) continue
            val pts = c.esquinas.map { camara.proyectar(it) }
            // Un poco por delante de su cara para que el borde no quede tapado por las baldosas.
            lista += Dibujable(camara.profundidad(c.centro) - 0.05f) {
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y); lineTo(pts[1].x, pts[1].y); lineTo(pts[2].x, pts[2].y); lineTo(pts[3].x, pts[3].y); close()
                }
                drawPath(
                    path, c.color,
                    style = Stroke(width = if (c.punteado) 1.6f else 1.2f, pathEffect = if (c.punteado) PathEffect.dashPathEffect(floatArrayOf(6f, 5f)) else null)
                )
            }
        }
        return lista
    }
}
