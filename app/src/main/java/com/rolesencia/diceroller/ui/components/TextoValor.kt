package com.rolesencia.diceroller.ui.components

import com.rolesencia.diceroller.domain.dados.TipoDado

/** Cómo se escribe el valor de un dado: dF con signos, d% con dos cifras. */
internal fun textoValor(tipo: TipoDado, valor: Int): String = when (tipo) {
    TipoDado.FATE -> when { valor > 0 -> "+"; valor < 0 -> "−"; else -> "0" }
    TipoDado.D100_DECENAS -> if (valor == 0) "00" else "$valor"
    else -> "$valor"
}
