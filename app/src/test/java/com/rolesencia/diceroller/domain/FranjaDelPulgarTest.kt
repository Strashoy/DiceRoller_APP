package com.rolesencia.diceroller.domain

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.dados.ordenParaElegir
import com.rolesencia.diceroller.domain.historial.Atajos
import com.rolesencia.diceroller.domain.historial.EntradaHistorial
import com.rolesencia.diceroller.domain.historial.Notacion
import com.rolesencia.diceroller.domain.historial.TiradaGuardada
import com.rolesencia.diceroller.domain.reglas.CatalogoSistemas
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.domain.reglas.NivelResultado
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import com.rolesencia.diceroller.domain.reglas.sistemas.D20ContraCd
import com.rolesencia.diceroller.domain.reglas.totalFueraDelTitulo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lo que sostiene la pantalla principal en un teléfono angosto (auditoría del capítulo 19). */
class FranjaDelPulgarTest {

    private fun pool(vararg tipos: TipoDado) = tipos.mapIndexed { i, t -> DadoEnMesa(i, t) }

    private fun entrada(instante: Long, sistema: String, pool: List<DadoEnMesa>, parametros: ParametrosTirada = ParametrosTirada()) =
        EntradaHistorial(
            instante, sistema, sistema, Notacion.de(pool, parametros.modificador), "1", NivelResultado.NEUTRO,
            titulo = "1", detalle = "", pool = pool, parametros = parametros
        )

    // --- El carrusel ---

    @Test fun los_dados_de_la_mesa_van_primero() {
        // D&D: un d20 en la mesa. Con el orden fijo quedaba sexto, fuera de una pantalla de 320 dp.
        assertEquals(TipoDado.D20, ordenParaElegir(pool(TipoDado.D20)).first())
        assertEquals(
            listOf(TipoDado.D6, TipoDado.D20, TipoDado.D4, TipoDado.D8),
            ordenParaElegir(pool(TipoDado.D20, TipoDado.D6, TipoDado.D6)).take(4)
        )
    }

    @Test fun el_orden_siempre_trae_todos_los_tipos_una_vez() {
        listOf(pool(), pool(TipoDado.D20), pool(*TipoDado.entries.toTypedArray())).forEach { p ->
            val orden = ordenParaElegir(p)
            assertEquals(TipoDado.entries.size, orden.size)
            assertEquals(TipoDado.entries.toSet(), orden.toSet())
        }
        assertEquals(TipoDado.entries.toList(), ordenParaElegir(pool()))
    }

    // --- El marcador ---

    @Test fun el_total_va_aparte_solo_si_el_titulo_no_lo_dice() {
        assertEquals(19, totalFueraDelTitulo("Éxito", 19))
        assertEquals(25, totalFueraDelTitulo("¡20 natural!", 25))
        assertNull(totalFueraDelTitulo("54", 54))
        assertNull(totalFueraDelTitulo("3 éxitos", 3))
        assertNull(totalFueraDelTitulo("Fallo", null))
        assertEquals(-2, totalFueraDelTitulo("Fallo", -2))
    }

    @Test fun sin_cd_el_d20_no_juzga_y_muestra_el_total() {
        val campo = D20ContraCd.campoObjetivo
        assertTrue(campo.opcional)
        assertNull("al elegir D&D la mesa arranca sin CD", campo.inicial)
        val tirada = listOf(ResultadoDado(DadoEnMesa(0, TipoDado.D20), 14))
        val sinCd = D20ContraCd.evaluar(tirada, ParametrosTirada(modificador = 5, objetivo = null))
        assertEquals(NivelResultado.NEUTRO, sinCd.nivel)
        assertEquals("19", sinCd.titulo)
        assertEquals(19, sinCd.total)
        // Con CD sigue juzgando, y el natural 20 es crítico aunque no haya CD.
        assertEquals(NivelResultado.EXITO, D20ContraCd.evaluar(tirada, ParametrosTirada(5, 15)).nivel)
        val veinte = listOf(ResultadoDado(DadoEnMesa(0, TipoDado.D20), 20))
        assertEquals(NivelResultado.EXITO_CRITICO, D20ContraCd.evaluar(veinte, ParametrosTirada()).nivel)
    }

    @Test fun solo_la_cd_es_opcional() {
        // Los demás sistemas necesitan su objetivo para decir algo (habilidad, dificultad, umbral).
        val opcionales = CatalogoSistemas.todos.filter { it.campoObjetivo?.opcional == true }.map { it.id }
        assertEquals(listOf(D20ContraCd.id), opcionales)
        CatalogoSistemas.todos.mapNotNull { it.campoObjetivo }.filterNot { it.opcional }.forEach {
            assertEquals(it.porDefecto, it.inicial)
        }
    }

    // --- La barra superior y el botón "Tirada" ---

    @Test fun los_nombres_cortos_entran_en_un_telefono_angosto() {
        // 148 dp para el nombre en una pantalla de 320 dp: unas 18 letras en negrita de 14 sp.
        CatalogoSistemas.todos.forEach {
            assertTrue("${it.nombreCorto} (${it.nombreCorto.length})", it.nombreCorto.length <= 18)
        }
        assertEquals("los nombres cortos no se repiten", CatalogoSistemas.todos.size, CatalogoSistemas.todos.map { it.nombreCorto }.toSet().size)
    }

    @Test fun las_etiquetas_cortas_entran_en_el_boton_tirada() {
        // 50 dp de texto a 12 sp: "Éxitos 10" es lo más largo que entra.
        CatalogoSistemas.todos.mapNotNull { it.campoObjetivo }.forEach {
            assertTrue("${it.etiquetaCorta} ${it.rango.last}", "${it.etiquetaCorta} ${it.rango.last}".length <= 9)
        }
    }

    // --- Los atajos ---

    private val ataque = pool(TipoDado.D20)
    private val danio = pool(TipoDado.D8)
    private val mas5 = ParametrosTirada(modificador = 5)
    private val mas3 = ParametrosTirada(modificador = 3)

    @Test fun despues_del_danio_el_atajo_es_el_ataque() {
        // Se tiró ataque y después daño; el daño quedó en la mesa.
        val historial = listOf(entrada(2, "d20", danio, mas3), entrada(1, "d20", ataque, mas5))
        val atajos = Atajos.recientes(historial, emptyList(), "d20", danio, mas3)
        assertEquals(listOf(1L), atajos.map { it.instante })
        // Se toca el atajo: ahora el ataque está en la mesa y el atajo que queda es el daño.
        val despues = listOf(entrada(3, "d20", ataque, mas5)) + historial
        assertEquals(listOf(2L), Atajos.recientes(despues, emptyList(), "d20", ataque, mas5).map { it.instante })
    }

    @Test fun no_repite_tiradas_iguales_ni_las_que_ya_estan_guardadas() {
        val fuego = pool(TipoDado.D6, TipoDado.D6, TipoDado.D6)
        val historial = listOf(
            entrada(5, "d20", ataque, mas5),
            entrada(4, "d20", ataque, mas5),                    // igual a la anterior
            entrada(3, "suma", fuego),                          // está guardada como "Bola de fuego"
            entrada(2, "d20", danio, mas3),
            entrada(1, "d20", ataque, ParametrosTirada(5, null, Conservar.MAYOR))   // con ventaja: es otra
        )
        val guardadas = listOf(TiradaGuardada("Bola de fuego", "suma", fuego.reversed(), ParametrosTirada()))
        val atajos = Atajos.recientes(historial, guardadas, "suma", pool(TipoDado.D4), ParametrosTirada(), maximo = 5)
        assertEquals(listOf(5L, 2L, 1L), atajos.map { it.instante })
        assertEquals(2, Atajos.recientes(historial, guardadas, "suma", pool(TipoDado.D4), ParametrosTirada()).size)
    }

    @Test fun un_dado_especial_no_es_lo_mismo_que_uno_normal() {
        val hambre = listOf(DadoEnMesa(0, TipoDado.D10), DadoEnMesa(1, TipoDado.D10, RolDado.ESPECIAL))
        val normales = pool(TipoDado.D10, TipoDado.D10)
        val atajos = Atajos.recientes(listOf(entrada(1, "v5", hambre)), emptyList(), "v5", normales, ParametrosTirada())
        assertEquals(1, atajos.size)
    }

    @Test fun sin_historial_no_hay_atajos() {
        assertTrue(Atajos.recientes(emptyList(), emptyList(), "suma", ataque, ParametrosTirada()).isEmpty())
    }
}
