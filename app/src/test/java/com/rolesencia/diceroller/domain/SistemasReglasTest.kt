package com.rolesencia.diceroller.domain

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.CatalogoSistemas
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.domain.reglas.NivelResultado.EXITO
import com.rolesencia.diceroller.domain.reglas.NivelResultado.EXITO_CRITICO
import com.rolesencia.diceroller.domain.reglas.NivelResultado.EXITO_PARCIAL
import com.rolesencia.diceroller.domain.reglas.NivelResultado.FALLO
import com.rolesencia.diceroller.domain.reglas.NivelResultado.FALLO_CRITICO
import com.rolesencia.diceroller.domain.reglas.NivelResultado.NEUTRO
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import com.rolesencia.diceroller.domain.reglas.sistemas.CypherSystem
import com.rolesencia.diceroller.domain.reglas.sistemas.D20ContraCd
import com.rolesencia.diceroller.domain.reglas.sistemas.Daggerheart
import com.rolesencia.diceroller.domain.reglas.sistemas.FateCore
import com.rolesencia.diceroller.domain.reglas.sistemas.ForgedInTheDark
import com.rolesencia.diceroller.domain.reglas.sistemas.Ironsworn
import com.rolesencia.diceroller.domain.reglas.sistemas.LlamadaDeCthulhu
import com.rolesencia.diceroller.domain.reglas.sistemas.PoweredByTheApocalypse
import com.rolesencia.diceroller.domain.reglas.sistemas.Rolesencia
import com.rolesencia.diceroller.domain.reglas.sistemas.Shadowrun
import com.rolesencia.diceroller.domain.reglas.sistemas.VampiroV5
import com.rolesencia.diceroller.domain.reglas.sistemas.YearZero
import org.junit.Assert.assertEquals
import org.junit.Test

/** Construye resultados: `r(TipoDado.D6, 3, 5)`; los especiales con `e(...)`. */
private class Mesa {
    private var id = 0
    val lista = mutableListOf<ResultadoDado>()
    fun r(tipo: TipoDado, vararg valores: Int) = apply { valores.forEach { lista += ResultadoDado(DadoEnMesa(id++, tipo), it) } }
    fun e(tipo: TipoDado, vararg valores: Int) = apply {
        valores.forEach { lista += ResultadoDado(DadoEnMesa(id++, tipo, RolDado.ESPECIAL), it) }
    }
}

private fun mesa() = Mesa()

class SistemasReglasTest {

    @Test fun pbta_bandas() {
        fun nivel(a: Int, b: Int, mod: Int = 0) =
            PoweredByTheApocalypse.evaluar(mesa().r(TipoDado.D6, a, b).lista, ParametrosTirada(mod)).nivel
        assertEquals(FALLO, nivel(3, 3))
        assertEquals(EXITO_PARCIAL, nivel(3, 4))
        assertEquals(EXITO_PARCIAL, nivel(4, 5))
        assertEquals(EXITO, nivel(4, 6))
        assertEquals(EXITO, nivel(4, 4, mod = 2))
    }

    @Test fun solo_dieces_cuenta_unicamente_los_10() {
        val v = CatalogoSistemas.SOLO_DIECES.evaluar(mesa().r(TipoDado.D10, 10, 9, 10, 1).lista, ParametrosTirada(objetivo = 2))
        assertEquals(2, v.total)
        assertEquals(EXITO, v.nivel)
    }

    @Test fun mundo_de_tinieblas_pifia_y_unos_restan() {
        val wod = CatalogoSistemas.MUNDO_DE_TINIEBLAS
        assertEquals(FALLO_CRITICO, wod.evaluar(mesa().r(TipoDado.D10, 1, 3, 5).lista, ParametrosTirada(objetivo = 1)).nivel)
        assertEquals(1, wod.evaluar(mesa().r(TipoDado.D10, 7, 8, 1).lista, ParametrosTirada(objetivo = 1)).total)
    }

    @Test fun exalted_diez_cuenta_doble() {
        assertEquals(3, CatalogoSistemas.EXALTED.evaluar(mesa().r(TipoDado.D10, 10, 7, 3).lista, ParametrosTirada()).total)
    }

    @Test fun blades_critico_y_cero_dados() {
        assertEquals(EXITO_CRITICO, ForgedInTheDark.evaluar(mesa().r(TipoDado.D6, 6, 6, 2).lista, ParametrosTirada()).nivel)
        assertEquals(EXITO_PARCIAL, ForgedInTheDark.evaluar(mesa().r(TipoDado.D6, 5, 2).lista, ParametrosTirada()).nivel)
        val ceroDados = ForgedInTheDark.evaluar(mesa().r(TipoDado.D6, 6, 6).lista, ParametrosTirada(conservar = Conservar.MENOR))
        assertEquals(EXITO, ceroDados.nivel)
        assertEquals(FALLO, ForgedInTheDark.evaluar(mesa().r(TipoDado.D6, 6, 2).lista, ParametrosTirada(conservar = Conservar.MENOR)).nivel)
    }

    @Test fun d20_ventaja_y_naturales() {
        val adv = D20ContraCd.evaluar(mesa().r(TipoDado.D20, 4, 17).lista, ParametrosTirada(2, 15, Conservar.MAYOR))
        assertEquals(19, adv.total)
        assertEquals(EXITO, adv.nivel)
        assertEquals(FALLO, D20ContraCd.evaluar(mesa().r(TipoDado.D20, 4, 17).lista, ParametrosTirada(2, 15, Conservar.MENOR)).nivel)
        assertEquals(EXITO_CRITICO, D20ContraCd.evaluar(mesa().r(TipoDado.D20, 20).lista, ParametrosTirada(objetivo = 30)).nivel)
        assertEquals(FALLO_CRITICO, D20ContraCd.evaluar(mesa().r(TipoDado.D20, 1).lista, ParametrosTirada(10, 5)).nivel)
    }

    @Test fun shadowrun_glitches() {
        val sr = Shadowrun()
        assertEquals(FALLO_CRITICO, sr.evaluar(mesa().r(TipoDado.D6, 1, 1, 1, 2).lista, ParametrosTirada(objetivo = 1)).nivel)
        assertEquals(EXITO_PARCIAL, sr.evaluar(mesa().r(TipoDado.D6, 1, 1, 6).lista, ParametrosTirada(objetivo = 1)).nivel)
        assertEquals(EXITO, sr.evaluar(mesa().r(TipoDado.D6, 5, 6, 2, 1).lista, ParametrosTirada(objetivo = 2)).nivel)
    }

    @Test fun v5_criticos_y_hambre() {
        val critico = VampiroV5.evaluar(mesa().r(TipoDado.D10, 10, 10, 3).lista, ParametrosTirada(objetivo = 3))
        assertEquals(4, critico.total)
        assertEquals("¡Crítico!", critico.titulo)
        val caotico = VampiroV5.evaluar(mesa().r(TipoDado.D10, 10).e(TipoDado.D10, 10).lista, ParametrosTirada(objetivo = 2))
        assertEquals("Crítico caótico", caotico.titulo)
        val bestial = VampiroV5.evaluar(mesa().r(TipoDado.D10, 6, 2).e(TipoDado.D10, 1).lista, ParametrosTirada(objetivo = 3))
        assertEquals(FALLO_CRITICO, bestial.nivel)
    }

    @Test fun year_zero_panico() {
        assertEquals(EXITO, YearZero.evaluar(mesa().r(TipoDado.D6, 6, 3).lista, ParametrosTirada()).nivel)
        assertEquals(FALLO_CRITICO, YearZero.evaluar(mesa().r(TipoDado.D6, 3).e(TipoDado.D6, 1).lista, ParametrosTirada()).nivel)
    }

    @Test fun cthulhu_niveles_y_percentil() {
        assertEquals(100, LlamadaDeCthulhu.valorPercentil(0, 10))
        assertEquals(5, LlamadaDeCthulhu.valorPercentil(0, 5))
        fun nivel(dec: Int, uni: Int, hab: Int) =
            LlamadaDeCthulhu.evaluar(mesa().r(TipoDado.D100_DECENAS, dec).r(TipoDado.D10, uni).lista, ParametrosTirada(objetivo = hab))
        assertEquals("¡Crítico!", nivel(0, 1, 50).titulo)
        assertEquals("Éxito extremo", nivel(10, 10, 50).titulo)   // 10 ≤ 50/5
        assertEquals("Éxito difícil", nivel(20, 5, 50).titulo)    // 25 ≤ 50/2
        assertEquals("Éxito normal", nivel(40, 2, 50).titulo)
        assertEquals(FALLO_CRITICO, nivel(90, 7, 40).nivel)        // 97 con habilidad < 50
        assertEquals(FALLO, nivel(90, 7, 60).nivel)
    }

    @Test fun cthulhu_dado_de_bonificacion() {
        val v = LlamadaDeCthulhu.evaluar(
            mesa().r(TipoDado.D100_DECENAS, 70, 20).r(TipoDado.D10, 3).lista,
            ParametrosTirada(objetivo = 30, conservar = Conservar.MENOR)
        )
        assertEquals(23, v.total)
    }

    @Test fun fate_shifts() {
        assertEquals(EXITO_CRITICO, FateCore.evaluar(mesa().r(TipoDado.FATE, 1, 1, 0, 0).lista, ParametrosTirada(2, 1)).nivel)
        assertEquals(EXITO_PARCIAL, FateCore.evaluar(mesa().r(TipoDado.FATE, -1, 1, 0, 0).lista, ParametrosTirada(2, 2)).nivel)
        assertEquals("Grande", FateCore.nombreEscalera(4))
    }

    @Test fun cypher_objetivo_triple() {
        assertEquals(EXITO, CypherSystem.evaluar(mesa().r(TipoDado.D20, 12).lista, ParametrosTirada(objetivo = 4)).nivel)
        assertEquals(FALLO, CypherSystem.evaluar(mesa().r(TipoDado.D20, 11).lista, ParametrosTirada(objetivo = 4)).nivel)
        assertEquals(FALLO_CRITICO, CypherSystem.evaluar(mesa().r(TipoDado.D20, 1).lista, ParametrosTirada(objetivo = 0)).nivel)
    }

    @Test fun daggerheart_esperanza_miedo_y_dobles() {
        fun v(h: Int, f: Int) = Daggerheart.evaluar(mesa().r(TipoDado.D12, h).e(TipoDado.D12, f).lista, ParametrosTirada(objetivo = 12))
        assertEquals(EXITO, v(9, 5).nivel)
        assertEquals(EXITO_PARCIAL, v(5, 9).nivel)
        assertEquals(EXITO_CRITICO, v(3, 3).nivel)
        assertEquals(FALLO_CRITICO, v(2, 4).nivel)
    }

    @Test fun ironsworn_golpes() {
        fun v(d6: Int, a: Int, b: Int, mod: Int = 2) =
            Ironsworn.evaluar(mesa().r(TipoDado.D6, d6).r(TipoDado.D10, a, b).lista, ParametrosTirada(mod)).nivel
        assertEquals(EXITO, v(5, 3, 6))
        assertEquals(EXITO_PARCIAL, v(5, 3, 7))
        assertEquals(FALLO, v(1, 3, 9))
        assertEquals(FALLO_CRITICO, v(1, 8, 8))
    }

    @Test fun rolesencia_respeta_el_lanzador_original() {
        fun v(leg: Int, atr: Int, tipoAtr: TipoDado = TipoDado.D6, dif: Int = 4) =
            Rolesencia.evaluar(mesa().r(TipoDado.LEGENDARIO, leg).r(tipoAtr, atr).lista, ParametrosTirada(objetivo = dif))
        assertEquals(EXITO_CRITICO, v(11, 1).nivel)
        assertEquals(EXITO_PARCIAL, v(8, 1).nivel)
        assertEquals("Éxito por capacidad", v(3, 1).titulo)               // d6 ≥ dificultad 4
        assertEquals(FALLO_CRITICO, v(3, 1, TipoDado.D4, dif = 6).nivel)  // d4 < 6
        assertEquals(NEUTRO, v(3, 2, dif = Rolesencia.DIFICULTAD_ENFRENTADA).nivel)
        // Compatibilidad: un d12 marcado como especial sigue funcionando como Legendario.
        val viejo = Rolesencia.evaluar(mesa().e(TipoDado.D12, 11).r(TipoDado.D6, 1).lista, ParametrosTirada(objetivo = 4))
        assertEquals(EXITO_CRITICO, viejo.nivel)
    }

    @Test fun todos_los_sistemas_evaluan_su_tirada_sugerida() {
        CatalogoSistemas.todos.forEach { sistema ->
            val resultados = sistema.tiradaSugerida().map { ResultadoDado(it, it.tipo.maximo) }
            val params = ParametrosTirada(objetivo = sistema.campoObjetivo?.porDefecto)
            val v = sistema.evaluar(resultados, params)
            assert(v.titulo.isNotEmpty() || v.nivel == NEUTRO) { "${sistema.id} devolvió veredicto vacío" }
        }
    }
}
