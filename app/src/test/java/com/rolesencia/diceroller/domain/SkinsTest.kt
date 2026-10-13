package com.rolesencia.diceroller.domain

import com.rolesencia.diceroller.data.codificarPool
import com.rolesencia.diceroller.data.codificarSkins
import com.rolesencia.diceroller.data.decodificarPool
import com.rolesencia.diceroller.data.decodificarSkins
import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.CategoriaSkin
import com.rolesencia.diceroller.domain.apariencia.ColorArgb
import com.rolesencia.diceroller.domain.apariencia.EstiloSkin
import com.rolesencia.diceroller.domain.apariencia.IdeaSkin
import com.rolesencia.diceroller.domain.apariencia.Tipografia
import com.rolesencia.diceroller.domain.apariencia.aRomano
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SkinsTest {

    @Test fun todas_las_skins_del_catalogo_se_leen_bien() {
        CatalogoSkins.predefinidas.forEach { skin ->
            TipoDado.entries.filter { it != TipoDado.LEGENDARIO }.forEach { tipo ->
                // La tinta declarada se lee sobre el cuerpo en los estilos de un solo color…
                if (skin.estilo != EstiloSkin.BICOLOR && skin.estilo != EstiloSkin.DEGRADADO) {
                    val c = ColorArgb.contraste(skin.cuerpoPara(tipo), skin.tinta)
                    assertTrue("${skin.nombre} en ${tipo.etiqueta}: contraste ${"%.2f".format(c)}", c >= 4.5)
                }
                // …y en TODAS las caras la tinta efectiva se lee: contraste 4,5 o, en tonos medios,
                // al menos 3 y con contorno.
                for (i in 0 until tipo.caras) for (altura in listOf(-1f, 0f, 1f)) {
                    val fondo = skin.colorCara(tipo, i, altura)
                    val c = ColorArgb.contraste(fondo, skin.tintaEfectiva(fondo))
                    assertTrue("${skin.nombre} ${tipo.etiqueta} cara $i: ${"%.2f".format(c)}", c >= 4.5 || (c >= 3.0 && skin.necesitaContorno(fondo)))
                }
            }
        }
    }

    @Test fun numeros_romanos() {
        assertEquals("XX", aRomano(20))
        assertEquals("XIV", aRomano(14))
        assertEquals("XC", aRomano(90))
        assertEquals("N", aRomano(0))
    }

    @Test fun cada_estilo_tiene_al_menos_un_ejemplo_en_el_catalogo() {
        EstiloSkin.entries.filter { it != EstiloSkin.IMAGEN }.forEach { e ->
            assertTrue(e.titulo, CatalogoSkins.predefinidas.any { it.estilo == e })
        }
        assertTrue(CatalogoSkins.sugeridaPara("v5", especial = true)!!.id == "v5_hambre")
        assertTrue(CatalogoSkins.sugeridaPara("daggerheart", especial = false)!!.id == "dh_esperanza")
    }

    @Test fun los_ids_del_catalogo_no_se_repiten() {
        val ids = CatalogoSkins.predefinidas.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun todas_las_ideas_del_creador_dan_numeros_legibles() {
        val random = Random(3)
        IdeaSkin.entries.forEach { idea ->
            repeat(200) {
                val base = ColorArgb.desdeHsl(random.nextFloat() * 360f, random.nextFloat(), random.nextFloat())
                val colores = idea.aplicar(base, random)
                val c = ColorArgb.contraste(colores.cuerpo, colores.tinta)
                assertTrue("${idea.titulo} desde ${ColorArgb.hex(base)}: $c", c >= 4.5)
            }
        }
    }

    @Test fun hsl_ida_y_vuelta() {
        listOf(0xFFC62828L, 0xFF1565C0L, 0xFFF9D71CL, 0xFF616161L).forEach { c ->
            val hsl = ColorArgb.aHsl(c)
            val vuelta = ColorArgb.desdeHsl(hsl.h, hsl.s, hsl.l)
            assertTrue(kotlin.math.abs(ColorArgb.rojo(c) - ColorArgb.rojo(vuelta)) <= 1)
            assertTrue(kotlin.math.abs(ColorArgb.azul(c) - ColorArgb.azul(vuelta)) <= 1)
        }
    }

    @Test fun las_skins_y_el_pool_se_guardan_y_recuperan() {
        val mia = SkinDado("mia_1", "Mi | skin", CategoriaSkin.PERSONALIZADAS, 0xFF123456L, 0xFFFFFFFFL, 0xFF000000L,
            EstiloSkin.MARMOL, 0xFFAABBCCL, Tipografia.ROMANA, listOf("/data/a.jpg", "/data/b.jpg"))
        val sinBorde = mia.copy(id = "mia_2", borde = null)
        val skins = decodificarSkins(codificarSkins(listOf(mia, sinBorde)))
        assertEquals(listOf(mia.copy(nombre = "Mi   skin"), sinBorde.copy(nombre = "Mi   skin")), skins)

        val pool = listOf(
            DadoEnMesa(0, TipoDado.D6, RolDado.NORMAL, CatalogoSkins.porId("rojo")),
            DadoEnMesa(1, TipoDado.D20, RolDado.ESPECIAL, skins[0])
        )
        val recuperado = decodificarPool(codificarPool(pool), skins)
        assertEquals(pool.map { Triple(it.tipo, it.rol, it.skin.id) }, recuperado.map { Triple(it.tipo, it.rol, it.skin.id) })
    }
}
