package com.rolesencia.diceroller.tirada.fisicareal

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.fisica.EventoColision
import com.rolesencia.diceroller.tirada.DadoVisual
import com.rolesencia.diceroller.tirada.FrameMesa
import com.rolesencia.diceroller.tirada.MetricasSesion
import com.rolesencia.diceroller.tirada.ModoDecision
import com.rolesencia.diceroller.tirada.SesionTirada
import com.rolesencia.diceroller.tirada.SimulacionTirada
import kotlin.math.min

/**
 * MODO 1 — FÍSICA REAL.
 *
 * Proceso:
 *  1. Al tocar "Lanzar" se crean los cuerpos con su impulso inicial (no se calcula nada más).
 *  2. En cada cuadro de pantalla (~16 ms) se avanza la simulación los sub-pasos fijos de 1/240 s
 *     que correspondan al tiempo real transcurrido (acumulador de tiempo, "fixed timestep").
 *  3. Se dibuja exactamente lo que la física calculó: la orientación visual ES la física.
 *  4. Cuando [SimulacionTirada] garantiza que todos están apoyados, el número de cada dado se LEE
 *     de su cara superior con `LectorCara`. Nadie lo eligió antes: lo decidió la física.
 *
 * Latencia: casi nula, el primer cuadro ya muestra movimiento.
 */
class SesionFisicaReal(private val simulacion: SimulacionTirada) : SesionTirada {

    override val modo = ModoDecision.FISICA_REAL
    override val dados: List<DadoEnMesa> get() = simulacion.dados

    private var acumulado = 0f

    override fun avanzar(dtReal: Float): FrameMesa {
        // Si el teléfono se traba, no intentamos "alcanzar" más de 1/10 s de golpe.
        acumulado += min(dtReal, 0.1f)
        val eventos = ArrayList<EventoColision>()
        while (acumulado >= SimulacionTirada.SUBPASO && !simulacion.terminada) {
            eventos += simulacion.paso()
            acumulado -= SimulacionTirada.SUBPASO
        }
        return FrameMesa(estadoFinal(), eventos)
    }

    override val terminada: Boolean get() = simulacion.terminada

    override fun resultados(): List<ResultadoDado> {
        val lecturas = simulacion.lecturas()
        return dados.map { ResultadoDado(it, lecturas.getValue(it.id).valor) }
    }

    override fun estadoFinal(): List<DadoVisual> =
        simulacion.estados().map { DadoVisual(it.dado, it.posicion, it.orientacion, it.enReposo, it.apoyo) }

    override fun metricas(): MetricasSesion {
        val lecturas = simulacion.lecturas()
        return MetricasSesion(
            modo = modo,
            segundosSimulados = simulacion.tiempo,
            pasos = simulacion.pasos,
            empujones = simulacion.empujones,
            forzados = simulacion.forzados,
            golpecitos = simulacion.golpecitos,
            detalle = dados.map { d ->
                val l = lecturas.getValue(d.id)
                "${d.tipo.etiqueta} #${d.id}: la física dejó arriba la cara ${l.indice} → ${l.valor} " +
                    "(alineación ${"%.3f".format(l.alineacion)})"
            }
        )
    }
}
