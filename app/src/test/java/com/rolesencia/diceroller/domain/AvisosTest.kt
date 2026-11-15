package com.rolesencia.diceroller.domain

import com.rolesencia.diceroller.domain.historial.Avisos
import com.rolesencia.diceroller.domain.historial.TiradaGuardada
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvisosTest {
    private fun guardada(nombre: String) = TiradaGuardada(nombre, "d20", emptyList(), ParametrosTirada())

    @Test
    fun elConsejoParaQuitarEntraCompletoEnUnaLinea() {
        assertTrue(Avisos.CONSEJO_QUITAR.length <= Avisos.MAX_SIN_ACCION)
        // Era "Para quitar un dado, mantenelo apretado (acá o en la mesa) o tocá su −": se cortaba.
        assertTrue(Avisos.CONSEJO_QUITAR.contains("apretado"))
    }

    @Test
    fun elAvisoDeQuitadoEntraConDeshacerAlLado() {
        for (etiqueta in listOf("d4", "d6", "d20", "d100")) {
            assertTrue(Avisos.quitado(etiqueta).length <= Avisos.MAX_CON_ACCION)
        }
        assertEquals("Quitaste un d6", Avisos.quitado("d6"))
    }

    @Test
    fun laConfirmacionNombraLaTiradaGuardada() {
        assertEquals("Guardada: Espada larga", Avisos.guardada("  Espada larga "))
    }

    @Test
    fun laConfirmacionCortaLosNombresLargosConPuntosSuspensivos() {
        val texto = Avisos.guardada("Prueba para ustedes con un nombre larguísimo")
        assertTrue(texto.endsWith("…"))
        assertTrue(texto.length <= "Guardada: ".length + 24)
    }

    @Test
    fun laPosicionSeBuscaSinDistinguirMayusculas() {
        val lista = listOf(guardada("Daño"), guardada("1D6"), guardada("Iniciativa"))
        assertEquals(1, Avisos.posicionDe(lista, " 1d6 "))
        assertEquals(-1, Avisos.posicionDe(lista, "otra"))
    }
}
