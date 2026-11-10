package com.rolesencia.diceroller.ui.mesa3d

import com.rolesencia.diceroller.fisica.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La cámara tiene que seguir siendo usable aunque no le quede lugar en la pantalla. Una escala cero
 * o negativa termina en un degradé radial de radio 0, que Android rechaza con una excepción: así se
 * cerraba la app al abrirse el teclado (capítulo 21 de la bitácora).
 */
class CamaraTest {

    private val bandeja = listOf(Vec3(-9f, 0f, -9f), Vec3(9f, 0f, -9f), Vec3(9f, 0f, 9f), Vec3(-9f, 0f, 9f))
    private val antorcha = Vec3(4f, 6f, -2f)

    private fun camara() = Camara(Vec3(0f, 30f, 35f), Vec3(0f, 0f, 0f))

    @Test fun con_lugar_la_bandeja_entra_en_el_area_util() {
        val c = camara().also { it.ajustar(bandeja, 1000f, 1500f, margen = 6f, x0 = 0f, y0 = 300f) }
        assertTrue(c.escala > 0f)
        bandeja.map { c.proyectar(it) }.forEach {
            assertTrue("x=${it.x}", it.x in 0f..1000f)
            assertTrue("y=${it.y}", it.y in 300f..1800f)
        }
    }

    @Test fun sin_lugar_la_escala_sigue_siendo_positiva() {
        // Lo que recibía la cámara con el teclado abierto: 1 px de alto útil y 6 px de margen.
        for ((ancho, alto) in listOf(1240f to 1f, 1f to 1f, 1240f to 0f, 0f to 0f, 1240f to -500f, -3f to -3f)) {
            val c = camara().also { it.ajustar(bandeja, ancho, alto, margen = 6f) }
            assertTrue("$ancho × $alto → escala ${c.escala}", c.escala > 0f && c.escala.isFinite())
            // El radio del halo de la antorcha, que es lo que rompía.
            assertTrue(c.tamanoEnPantalla(antorcha, 2.6f) > 0f)
        }
    }

    @Test fun al_pasar_a_la_cenital_sin_lugar_tampoco_se_rompe() {
        val c = camara()
        for (t in listOf(0f, 0.5f, 1f)) {
            c.ajustarEntre(bandeja, 6f, bandeja.take(2), 24f, ancho = 1240f, alto = 1f, t = t)
            assertTrue("t=$t → escala ${c.escala}", c.escala > 0f && c.escala.isFinite())
        }
    }

    @Test fun un_solo_punto_no_divide_por_cero() {
        val c = camara().also { it.ajustar(listOf(antorcha), 1000f, 1500f, margen = 6f) }
        assertTrue(c.escala > 0f && c.escala.isFinite())
        val p = c.proyectar(antorcha)
        assertEquals(500f, p.x, 0.5f)
        assertEquals(750f, p.y, 0.5f)
    }
}
