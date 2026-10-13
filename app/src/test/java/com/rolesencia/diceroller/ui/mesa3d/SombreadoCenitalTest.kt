package com.rolesencia.diceroller.ui.mesa3d

import androidx.compose.ui.graphics.Color
import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.EstiloSkin
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.fisica.Vec3
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * La cara superior de un dado, ¿cambia de color al pasar de la vista general a la cenital?
 * Se prueba cada skin del catálogo en varios puntos de la bandeja, con y sin apagar los reflejos.
 */
class SombreadoCenitalTest {

    private val mz = 9f
    private val ojosGenerales = listOf(Vec3(0f, 40f, 2 * mz * 0.6f), Vec3(0f, 30f, mz + 26f))   // bandeja, torre
    private val posiciones = listOf(-6f, 0f, 6f).flatMap { x -> listOf(-6f, 0f, 6f).map { z -> Vec3(x, 0.8f, z) } }

    private fun Color.rgb() = Triple((red * 255).roundToInt(), (green * 255).roundToInt(), (blue * 255).roundToInt())
    private fun diferencia(a: Color, b: Color): Int {
        val (r1, g1, b1) = a.rgb(); val (r2, g2, b2) = b.rgb()
        return maxOf(abs(r1 - r2), abs(g1 - g2), abs(b1 - b2))
    }

    /** Peor diferencia (0..255) de la cara de arriba entre la vista general y la cenital. */
    private fun peorDiferencia(estilo: EstiloSkin, base: Color, plana: Float): Int = posiciones.maxOf { p ->
        val cenital = Sombreado.tonoCara(estilo, base, Vec3.ARRIBA, (Vec3(p.x, 40f, p.z + 0.4f) - p).normalizado(), plana)
        ojosGenerales.maxOf { ojo -> diferencia(Sombreado.tonoCara(estilo, base, Vec3.ARRIBA, (ojo - p).normalizado(), 0f), cenital) }
    }

    /** Con el ajuste: cuánto varía la cara de arriba en la cenital según dónde cayó el dado. */
    private fun variacionCenital(estilo: EstiloSkin, base: Color): Int {
        val tonos = posiciones.map { p -> Sombreado.tonoCara(estilo, base, Vec3.ARRIBA, (Vec3(p.x, 40f, p.z + 0.4f) - p).normalizado(), 1f) }
        return tonos.maxOf { a -> tonos.maxOf { b -> diferencia(a, b) } }
    }

    @Test fun solo_metal_y_gema_cambian_de_color_en_la_toma_cenital() {
        val filas = CatalogoSkins.predefinidas.map { skin ->
            val base = Color(skin.colorCara(TipoDado.D6, 0, 1f))
            Triple(skin, peorDiferencia(skin.estilo, base, plana = 0f), variacionCenital(skin.estilo, base))
        }
        filas.forEach { (skin, sin, con) -> println("%-28s %-16s general vs cenital sin ajuste %3d   variación cenital con ajuste %3d".format(skin.nombre, skin.estilo, sin, con)) }
        // Sin ajuste: sólo los estilos con reflejo cambian; el resto es idéntico.
        filas.filter { it.first.estilo != EstiloSkin.METALICO && it.first.estilo != EstiloSkin.GEMA }
            .forEach { assertTrue(it.first.nombre, it.second == 0) }
        // Con el ajuste, la cara de arriba en la cenital tiene un único tono (el de la luz difusa).
        filas.forEach { assertTrue("${it.first.nombre} con ajuste", it.third <= 1) }
    }
}
