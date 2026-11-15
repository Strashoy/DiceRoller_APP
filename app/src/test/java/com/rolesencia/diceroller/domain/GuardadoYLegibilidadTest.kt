package com.rolesencia.diceroller.domain

import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.historial.IndicioHistorial
import com.rolesencia.diceroller.domain.historial.MAX_HISTORIAL
import com.rolesencia.diceroller.domain.historial.NombreGuardada
import com.rolesencia.diceroller.domain.historial.TiradaGuardada
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fase 2: el nombre propuesto al guardar y el indicio del chip Historial. */
class GuardadoYLegibilidadTest {
    private fun dado(id: Int) = DadoEnMesa(id, TipoDado.D6, RolDado.NORMAL, CatalogoSkins.POR_TIPO)
    private fun guardada(nombre: String) = TiradaGuardada(nombre, "d20", listOf(dado(0)), ParametrosTirada())

    @Test fun el_nombre_propuesto_es_la_notacion() {
        assertEquals("2d6 + 1d4", NombreGuardada.sugerido(listOf(dado(0), dado(1)), "2d6 + 1d4", emptyList()))
    }

    @Test fun con_la_mesa_vacia_no_se_propone_nada() {
        assertEquals("", NombreGuardada.sugerido(emptyList(), "Sin dados", emptyList()))
    }

    @Test fun si_el_nombre_ya_existe_se_numera_sin_importar_mayusculas() {
        val pool = listOf(dado(0))
        assertEquals("1d6 (2)", NombreGuardada.sugerido(pool, "1d6", listOf(guardada("1D6"))))
        assertEquals("1d6 (3)", NombreGuardada.sugerido(pool, "1d6", listOf(guardada("1d6"), guardada("1d6 (2)"))))
    }

    @Test fun el_nombre_propuesto_nunca_pasa_del_maximo() {
        val largo = "1d20 + 1d12 + 1d10 + 1d8 + 1d6 + 1d4 + 1d% + 1dF + 99"
        val pool = listOf(dado(0))
        val solo = NombreGuardada.sugerido(pool, largo, emptyList())
        assertTrue(solo.length <= NombreGuardada.MAX)
        val repetido = NombreGuardada.sugerido(pool, largo, listOf(guardada(solo)))
        assertTrue(repetido.length <= NombreGuardada.MAX)
        assertTrue(repetido.endsWith(" (2)"))
    }

    @Test fun el_chip_se_resalta_solo_hasta_abrir_el_historial() {
        assertFalse(IndicioHistorial.resaltar(descubierto = false, tiradas = 0))
        assertTrue(IndicioHistorial.resaltar(descubierto = false, tiradas = 1))
        assertFalse(IndicioHistorial.resaltar(descubierto = true, tiradas = 9))
    }

    @Test fun el_contador_no_pasa_del_limite_del_historial() {
        assertNull(IndicioHistorial.contador(0))
        assertEquals("7", IndicioHistorial.contador(7))
        assertEquals("$MAX_HISTORIAL", IndicioHistorial.contador(MAX_HISTORIAL + 10))
    }
}
