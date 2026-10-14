package com.rolesencia.diceroller.domain

import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.historial.Notacion
import com.rolesencia.diceroller.domain.historial.Ventaja
import com.rolesencia.diceroller.domain.reglas.Conservar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HistorialTest {

    private fun dado(id: Int, tipo: TipoDado, rol: RolDado = RolDado.NORMAL) = DadoEnMesa(id, tipo, rol)

    @Test fun la_notacion_agrupa_por_tipo_con_los_dados_grandes_primero() {
        val pool = listOf(dado(0, TipoDado.D8), dado(1, TipoDado.D20), dado(2, TipoDado.D8))
        assertEquals("1d20 + 2d8 + 5", Notacion.de(pool, 5))
        assertEquals("1d20 + 2d8 − 2", Notacion.de(pool, -2))
        assertEquals("1d20 + 2d8", Notacion.de(pool))
    }

    @Test fun los_dados_especiales_se_cuentan_aparte() {
        val pool = List(4) { dado(it, TipoDado.D10) } + dado(4, TipoDado.D10, RolDado.ESPECIAL)
        assertEquals("4d10 + 1d10 especial", Notacion.de(pool))
    }

    @Test fun sin_dados_la_notacion_lo_dice() {
        assertEquals("Sin dados", Notacion.de(emptyList()))
    }

    @Test fun los_valores_respetan_las_caras_especiales() {
        val r = listOf(
            ResultadoDado(dado(0, TipoDado.FATE), 1),
            ResultadoDado(dado(1, TipoDado.FATE), -1),
            ResultadoDado(dado(2, TipoDado.D100_DECENAS), 0),
            ResultadoDado(dado(3, TipoDado.D20), 18)
        )
        assertEquals("+ · − · 00 · 18", Notacion.valores(r))
    }

    @Test fun la_ventaja_agrega_el_segundo_d20_y_la_tirada_normal_lo_quita() {
        val uno = listOf(dado(0, TipoDado.D20))
        val conVentaja = Ventaja.ajustarPool(Ventaja.SISTEMA_D20, uno, Conservar.MAYOR, CatalogoSkins.POR_TIPO)
        assertEquals(2, conVentaja.count { it.tipo == TipoDado.D20 })
        val normal = Ventaja.ajustarPool(Ventaja.SISTEMA_D20, conVentaja, Conservar.TODOS, CatalogoSkins.POR_TIPO)
        assertEquals(uno, normal)
    }

    @Test fun la_ventaja_no_toca_el_pool_de_otros_sistemas() {
        val pool = listOf(dado(0, TipoDado.D6), dado(1, TipoDado.D6))
        assertSame(pool, Ventaja.ajustarPool("fitd", pool, Conservar.MAYOR, CatalogoSkins.POR_TIPO))
    }
}
