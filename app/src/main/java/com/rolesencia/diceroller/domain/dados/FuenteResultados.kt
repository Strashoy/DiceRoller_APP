package com.rolesencia.diceroller.domain.dados

import kotlin.random.Random

/**
 * De dónde sale el número de cada dado.
 *
 * - [LanzadorAleatorio]: RNG puro (equivalente al `(1..n).random()` del Lanzador Legendario).
 *   Sirve para tests, para el modo "sin animaciones" y mientras la mesa 3D no exista.
 * - Mesa 3D (Fase 2): la simulación física se ejecuta y el valor se LEE de la cara que quedó
 *   hacia arriba con [com.rolesencia.diceroller.fisica.LectorCara]. Ver DICE_ROLLER_PLAN.md.
 */
fun interface FuenteResultados {
    fun lanzar(dados: List<DadoEnMesa>): List<ResultadoDado>
}

class LanzadorAleatorio(private val random: Random = Random.Default) : FuenteResultados {
    override fun lanzar(dados: List<DadoEnMesa>): List<ResultadoDado> =
        dados.map { ResultadoDado(it, it.tipo.valoresCaras.random(random)) }
}
