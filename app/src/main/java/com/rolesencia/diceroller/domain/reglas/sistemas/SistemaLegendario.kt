package com.rolesencia.diceroller.domain.reglas.sistemas

import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.CampoParametro
import com.rolesencia.diceroller.domain.reglas.NivelResultado
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import com.rolesencia.diceroller.domain.reglas.SistemaReglas
import com.rolesencia.diceroller.domain.reglas.Veredicto
import com.rolesencia.diceroller.domain.reglas.pool

/** Símbolo del d12 legendario (portado de `DadoLogica.kt` del Lanzador Legendario). */
enum class SimboloLegendario(val rango: IntRange) {
    HEPTAGRAMA(1..7),
    YING_YANG(8..9),
    MEGA(10..12);

    companion object {
        fun de(valor: Int): SimboloLegendario = entries.first { valor in it.rango }
    }
}

/**
 * Reglas de Rolesencia (port directo de `ReglasJuego.calcularJuicio`).
 * Dado Legendario = [TipoDado.LEGENDARIO] (por compatibilidad, también un d12 ESPECIAL).
 * El primer otro dado de la mesa es el de Atributo (d4/d6/d8/d10).
 * [ParametrosTirada.objetivo] = dificultad; 3 = tirada enfrentada.
 */
object Rolesencia : SistemaReglas {
    const val DIFICULTAD_ENFRENTADA = 3

    override val id = "rolesencia"
    override val nombre = "Rolesencia (Dado Legendario)"
    override val descripcion = "d12 Legendario: 10–12 Éxito Legendario, 8–9 Éxito Parcial, 1–7 decide la " +
        "capacidad (caras del dado de Atributo ≥ dificultad). Atributo ≥ dificultad: éxito. Dificultad 3: enfrentada."
    override val campoObjetivo = CampoParametro("Dificultad", 3..10, 4)

    override fun tiradaSugerida() = pool {
        agregar(TipoDado.LEGENDARIO)
        agregar(TipoDado.D6)
    }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val legendario = resultados.firstOrNull { it.tipo == TipoDado.LEGENDARIO }
            ?: resultados.firstOrNull { it.esEspecial && it.tipo == TipoDado.D12 }
        val atributo = resultados.firstOrNull { it !== legendario && it.tipo != TipoDado.LEGENDARIO }
        val dificultad = parametros.objetivo ?: 4
        val enfrentada = dificultad == DIFICULTAD_ENFRENTADA

        val textoAtributo = atributo?.let {
            when {
                enfrentada -> "Atributo: ${it.valor}"
                it.valor >= dificultad -> "Atributo: ${it.valor} (éxito)"
                else -> "Atributo: ${it.valor} (fallo)"
            }
        }

        if (legendario == null) {
            if (atributo == null) return Veredicto(NivelResultado.NEUTRO, "Sin dados")
            val nivel = when {
                enfrentada -> NivelResultado.NEUTRO
                atributo.valor >= dificultad -> NivelResultado.EXITO
                else -> NivelResultado.FALLO
            }
            val titulo = when (nivel) {
                NivelResultado.EXITO -> "Éxito"
                NivelResultado.FALLO -> "Fallo"
                else -> "${atributo.valor}"
            }
            return Veredicto(nivel, titulo, textoAtributo.orEmpty(), atributo.valor, setOf(atributo.dado.id))
        }

        val simbolo = SimboloLegendario.de(legendario.valor)
        val carasAtributo = atributo?.tipo?.maximo
        val (nivel, titulo) = when (simbolo) {
            SimboloLegendario.MEGA -> NivelResultado.EXITO_CRITICO to "¡Éxito legendario!"
            SimboloLegendario.YING_YANG -> NivelResultado.EXITO_PARCIAL to "¡Éxito parcial!"
            SimboloLegendario.HEPTAGRAMA -> when {
                // Sin dado de Atributo en la mesa, la app se calla (como en el original).
                carasAtributo == null -> NivelResultado.NEUTRO to ""
                enfrentada -> NivelResultado.NEUTRO to "$carasAtributo"
                carasAtributo >= dificultad -> NivelResultado.EXITO to "Éxito por capacidad"
                else -> NivelResultado.FALLO_CRITICO to "¡Fallo legendario!"
            }
        }
        val detalle = listOfNotNull("Legendario: ${legendario.valor} (${simbolo.name})", textoAtributo).joinToString(" · ")
        return Veredicto(nivel, titulo, detalle, legendario.valor, setOf(legendario.dado.id))
    }
}
