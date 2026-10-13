package com.rolesencia.diceroller.fisica

import com.rolesencia.diceroller.domain.dados.TipoDado

/**
 * Normales de cara en espacio LOCAL de cada dado, derivadas de la malla real de [Poliedros].
 * El índice i corresponde a `TipoDado.valoresCaras[i]`. Las caras opuestas suman lo mismo que
 * en un dado real (d6: 7, d8: 9, d12: 13, d20: 21, d10: 9 contando el 0).
 */
object GeometriaDados {

    fun normales(tipo: TipoDado): List<Vec3> = Poliedros.de(tipo).caras.map { it.normal }

    /** El d4 se lee por la cara de abajo (el número aparece en el vértice de arriba); el resto por arriba. */
    fun seLeeHaciaAbajo(tipo: TipoDado): Boolean = tipo == TipoDado.D4
}
