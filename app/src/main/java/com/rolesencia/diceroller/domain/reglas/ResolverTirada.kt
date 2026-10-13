package com.rolesencia.diceroller.domain.reglas

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.FuenteResultados
import com.rolesencia.diceroller.domain.dados.ResultadoDado

data class TiradaResuelta(
    val resultados: List<ResultadoDado>,
    val veredicto: Veredicto,
    /** Cuántas rondas de dados explosivos hubo (0 = ninguna). */
    val rondasExtra: Int
)

/**
 * Lanza el pool, encadena las explosiones que pida el sistema y evalúa.
 * Es independiente de la UI: la mesa 3D usará el mismo flujo, sólo que su [FuenteResultados]
 * esperará a que la física termine y leerá las caras.
 */
fun resolverTirada(
    sistema: SistemaReglas,
    pool: List<DadoEnMesa>,
    parametros: ParametrosTirada,
    fuente: FuenteResultados,
    maxRondasExtra: Int = 20
): TiradaResuelta {
    val acumulados = fuente.lanzar(pool).toMutableList()
    var ronda: List<ResultadoDado> = acumulados
    var rondas = 0
    while (rondas < maxRondasExtra) {
        val siguienteId = (acumulados.maxOfOrNull { it.dado.id } ?: -1) + 1
        val extra = sistema.dadosExplosivos(ronda, siguienteId)
        if (extra.isEmpty()) break
        ronda = fuente.lanzar(extra)
        acumulados += ronda
        rondas++
    }
    return TiradaResuelta(acumulados, sistema.evaluar(acumulados, parametros), rondas)
}
