package com.rolesencia.diceroller.ui

import com.rolesencia.diceroller.ui.viewmodel.DiceRollerUiState
import com.rolesencia.diceroller.ui.viewmodel.interaccionHabilitada
import com.rolesencia.diceroller.ui.viewmodel.mesaOcupada
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La regla de la Fase 1 (N1): con la mesa ocupada la interfaz entera se deshabilita salvo tocar la
 * mesa, así no hay toques que se pierdan en silencio. Se prueba sobre el estado puro, sin Android.
 */
class MesaOcupadaTest {
    private val libre = DiceRollerUiState()

    @Test fun `en reposo la interfaz responde`() {
        assertFalse(libre.mesaOcupada)
        assertTrue(libre.interaccionHabilitada)
    }

    @Test fun `rodando bloquea la interfaz`() {
        val s = libre.copy(estaRodando = true)
        assertTrue(s.mesaOcupada)
        assertFalse(s.interaccionHabilitada)
    }

    @Test fun `pre-simulando bloquea la interfaz`() {
        assertFalse(libre.copy(preparando = true).interaccionHabilitada)
    }

    @Test fun `esperar la foto de Discord bloquea la interfaz`() {
        val s = libre.copy(fotoPedida = 1234L, fotoAnimada = false)
        assertTrue(s.mesaOcupada)
        assertFalse(s.interaccionHabilitada)
    }

    @Test fun `esperar el GIF no bloquea aunque dure hasta 25 segundos`() {
        val s = libre.copy(fotoPedida = 1234L, fotoAnimada = true)
        assertFalse(s.mesaOcupada)
        assertTrue(s.interaccionHabilitada)
    }

    @Test fun `rodando con GIF pedido sigue bloqueado`() {
        assertTrue(libre.copy(estaRodando = true, fotoPedida = 1L, fotoAnimada = true).mesaOcupada)
    }

    @Test fun `al terminar todo la interfaz vuelve`() {
        val ocupada = libre.copy(estaRodando = true, preparando = true, fotoPedida = 9L)
        val terminada = ocupada.copy(estaRodando = false, preparando = false, fotoPedida = null)
        assertTrue(ocupada.mesaOcupada)
        assertTrue(terminada.interaccionHabilitada)
    }
}
