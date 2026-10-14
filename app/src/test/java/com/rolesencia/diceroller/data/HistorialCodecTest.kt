package com.rolesencia.diceroller.data

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.historial.EntradaHistorial
import com.rolesencia.diceroller.domain.historial.TiradaGuardada
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.domain.reglas.NivelResultado
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import org.junit.Assert.assertEquals
import org.junit.Test

class HistorialCodecTest {

    private val pool = listOf(
        DadoEnMesa(0, TipoDado.D20),
        DadoEnMesa(1, TipoDado.D10, RolDado.ESPECIAL)
    )

    @Test fun el_historial_ida_y_vuelta_conserva_todo() {
        val entrada = EntradaHistorial(
            instante = 1_760_000_000_000,
            sistemaId = "d20", sistemaNombre = "d20 contra CD (D&D 5e)",
            notacion = "1d20 + 1d10 especial + 5", valores = "18 · 3",
            nivel = NivelResultado.EXITO, titulo = "Éxito", detalle = "18 + 5 = 23 vs CD 15",
            pool = pool, parametros = ParametrosTirada(5, 15, Conservar.MAYOR)
        )
        val vuelta = decodificarHistorial(codificarHistorial(listOf(entrada, entrada.copy(instante = 1))))
        assertEquals(2, vuelta.size)
        assertEquals(entrada, vuelta.first())
    }

    @Test fun los_separadores_en_los_textos_no_rompen_la_linea() {
        val entrada = EntradaHistorial(
            1, "suma", "Suma", "1d20", "7", NivelResultado.NEUTRO,
            titulo = "Total | 7", detalle = "una\nlínea", pool = pool, parametros = ParametrosTirada()
        )
        val vuelta = decodificarHistorial(codificarHistorial(listOf(entrada))).single()
        assertEquals("Total / 7", vuelta.titulo)
        assertEquals("una línea", vuelta.detalle)
    }

    @Test fun las_tiradas_guardadas_ida_y_vuelta() {
        val t = TiradaGuardada("Espada larga", "d20", pool, ParametrosTirada(modificador = -1, objetivo = null))
        assertEquals(listOf(t), decodificarTiradas(codificarTiradas(listOf(t))))
    }
}
