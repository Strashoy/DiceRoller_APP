package com.rolesencia.diceroller.tirada.predeterminada

import com.rolesencia.diceroller.fisica.EstadoCuerpo
import com.rolesencia.diceroller.fisica.EventoColision
import com.rolesencia.diceroller.fisica.LecturaCara
import com.rolesencia.diceroller.tirada.SimulacionTirada

/** Una tirada completa ya calculada: cuadros a 60 fps + eventos sonoros con su instante. */
class Grabacion(
    val tiempos: FloatArray,
    val cuadros: List<List<EstadoCuerpo>>,
    val eventos: List<Pair<Float, EventoColision>>,
    val lecturas: Map<Int, LecturaCara>,
    val pasos: Int,
    val empujones: Int,
    val forzados: Int,
    val golpecitos: Int,
    /** Tiempo de CPU que llevó calcularla (lo que el jugador espera antes de ver movimiento). */
    val milisegundosCalculo: Long
) {
    val duracion: Float get() = tiempos.last()
}

/**
 * Corre la [SimulacionTirada] de principio a fin SIN dibujar, lo más rápido posible, y guarda
 * una "película" de la tirada. Es el paso que introduce latencia en el modo predeterminado:
 * la animación no puede empezar hasta saber en qué cara cae cada dado.
 */
object PreSimulador {

    fun grabar(simulacion: SimulacionTirada, fps: Int = 60): Grabacion {
        val inicio = System.nanoTime()
        val pasosPorCuadro = Math.round((1f / fps) / SimulacionTirada.SUBPASO).coerceAtLeast(1)
        val tiempos = ArrayList<Float>()
        val cuadros = ArrayList<List<EstadoCuerpo>>()
        val eventos = ArrayList<Pair<Float, EventoColision>>()

        tiempos += 0f
        cuadros += simulacion.estados()
        var contador = 0
        while (!simulacion.terminada) {
            simulacion.paso().forEach { eventos += simulacion.tiempo to it }
            contador++
            if (contador % pasosPorCuadro == 0 || simulacion.terminada) {
                tiempos += simulacion.tiempo
                cuadros += simulacion.estados()
            }
        }
        return Grabacion(
            tiempos = tiempos.toFloatArray(),
            cuadros = cuadros,
            eventos = eventos,
            lecturas = simulacion.lecturas(),
            pasos = simulacion.pasos,
            empujones = simulacion.empujones,
            forzados = simulacion.forzados,
            golpecitos = simulacion.golpecitos,
            milisegundosCalculo = (System.nanoTime() - inicio) / 1_000_000
        )
    }
}
