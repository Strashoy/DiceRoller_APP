package com.rolesencia.diceroller.domain

import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.ColorArgb
import com.rolesencia.diceroller.domain.apariencia.PaletasAmbiente
import com.rolesencia.diceroller.domain.dados.TipoDado
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/** La skin "Por tipo" cambia con el ambiente: colores distintos entre sí y números siempre legibles. */
class PaletasAmbienteTest {

    private val tipos = TipoDado.entries.filter { it != TipoDado.LEGENDARIO }

    @After fun restaurar() {
        CatalogoSkins.ambienteActivo = PaletasAmbiente.BANDEJA_LONA
    }

    private fun distancia(a: Long, b: Long): Double {
        fun c(x: Long, s: Int) = ((x shr s) and 0xFF).toDouble()
        val dr = c(a, 16) - c(b, 16); val dg = c(a, 8) - c(b, 8); val db = c(a, 0) - c(b, 0)
        return sqrt(dr * dr + dg * dg + db * db)
    }

    @Test fun en_cada_ambiente_los_tipos_tienen_colores_bien_distintos() {
        for (clave in PaletasAmbiente.claves) {
            val colores = tipos.map { PaletasAmbiente.porTipo(clave, it) }
            for (i in colores.indices) for (j in i + 1 until colores.size) {
                val d = distancia(colores[i], colores[j])
                assertTrue("$clave: ${tipos[i].etiqueta} y ${tipos[j].etiqueta} se parecen (distancia ${"%.0f".format(d)})", d > 60.0)
            }
        }
    }

    @Test fun en_cada_ambiente_los_numeros_por_tipo_se_leen() {
        val skin = CatalogoSkins.POR_TIPO
        for (clave in PaletasAmbiente.claves) {
            CatalogoSkins.ambienteActivo = clave
            for (tipo in tipos) {
                val fondo = skin.cuerpoPara(tipo)
                val c = ColorArgb.contraste(fondo, skin.tintaEfectiva(fondo))
                assertTrue("$clave ${tipo.etiqueta}: contraste ${"%.2f".format(c)}", c >= 4.5 || (c >= 3.0 && skin.necesitaContorno(fondo)))
            }
        }
    }

    @Test fun el_legendario_siempre_es_dorado() {
        val dorados = PaletasAmbiente.claves.map { PaletasAmbiente.porTipo(it, TipoDado.LEGENDARIO) }.toSet()
        assertTrue(dorados.size == 1)
    }
}
