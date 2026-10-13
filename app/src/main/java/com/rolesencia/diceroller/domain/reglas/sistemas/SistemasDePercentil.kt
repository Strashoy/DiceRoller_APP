package com.rolesencia.diceroller.domain.reglas.sistemas

import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.CampoParametro
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.domain.reglas.NivelResultado
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import com.rolesencia.diceroller.domain.reglas.SistemaReglas
import com.rolesencia.diceroller.domain.reglas.Veredicto
import com.rolesencia.diceroller.domain.reglas.pool

/**
 * La Llamada de Cthulhu 7ª ed.: d100 "bajo" la habilidad.
 * 01 crítico · ≤ 1/5 extremo · ≤ 1/2 difícil · ≤ habilidad normal · pifia en 100 (o 96–100 si habilidad < 50).
 *
 * Se lanza un dado de decenas (d%) + un d10 de unidades. Dados de bonificación/penalización:
 * agregar más d% y elegir Menor (bonificación) o Mayor (penalización).
 */
object LlamadaDeCthulhu : SistemaReglas {
    override val id = "coc"
    override val nombre = "La Llamada de Cthulhu (d100)"
    override val descripcion = "d100 ≤ habilidad es éxito. ≤ mitad: difícil. ≤ quinto: extremo. 01: crítico. " +
        "100 (o 96+ con habilidad < 50): pifia. Dados extra de decenas = bonificación/penalización."
    override val campoObjetivo = CampoParametro("Habilidad", 1..99, 50)

    override fun tiradaSugerida() = pool {
        agregar(TipoDado.D100_DECENAS)
        agregar(TipoDado.D10)
    }

    /** Combina decenas + unidades. "00" + "0" = 100. */
    fun valorPercentil(decenas: Int, unidadD10: Int): Int {
        val unidades = unidadD10 % 10
        val total = decenas + unidades
        return if (total == 0) 100 else total
    }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val unidad = resultados.firstOrNull { it.tipo == TipoDado.D10 }
        val decenas = resultados.filter { it.tipo == TipoDado.D100_DECENAS }
        if (unidad == null || decenas.isEmpty()) return Veredicto(NivelResultado.NEUTRO, "Necesita d% y d10")

        val candidatos = decenas.map { it to valorPercentil(it.valor, unidad.valor) }
        val (decenaElegida, tirada) = when (parametros.conservar) {
            Conservar.MAYOR -> candidatos.maxBy { it.second }
            Conservar.MENOR -> candidatos.minBy { it.second }
            Conservar.TODOS -> candidatos.first()
        }
        val habilidad = parametros.objetivo ?: 50
        val pifia = tirada == 100 || (habilidad < 50 && tirada >= 96)
        val (nivel, titulo) = when {
            tirada == 1 -> NivelResultado.EXITO_CRITICO to "¡Crítico!"
            pifia -> NivelResultado.FALLO_CRITICO to "¡Pifia!"
            tirada <= habilidad / 5 -> NivelResultado.EXITO_CRITICO to "Éxito extremo"
            tirada <= habilidad / 2 -> NivelResultado.EXITO to "Éxito difícil"
            tirada <= habilidad -> NivelResultado.EXITO to "Éxito normal"
            else -> NivelResultado.FALLO to "Fallo"
        }
        return Veredicto(nivel, titulo, "$tirada vs $habilidad", tirada, setOf(decenaElegida.dado.id, unidad.dado.id))
    }
}
