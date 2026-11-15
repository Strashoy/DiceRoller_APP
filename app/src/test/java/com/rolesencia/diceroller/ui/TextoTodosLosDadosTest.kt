package com.rolesencia.diceroller.ui

import com.rolesencia.diceroller.ui.skins.textoTodosLosDados
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextoTodosLosDadosTest {
    @Test fun con_la_mesa_vacia_explica_por_que_no_se_puede() {
        assertTrue(textoTodosLosDados(0, "Rubí").contains("Agregá dados"))
    }

    @Test fun nombra_la_cantidad_y_el_color_que_se_aplica() {
        assertEquals("Pintar el dado de la mesa con «Rubí»", textoTodosLosDados(1, "Rubí"))
        assertEquals("Pintar los 5 dados de la mesa con «Rubí»", textoTodosLosDados(5, "Rubí"))
    }
}
