package com.rolesencia.diceroller.tirada

import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.pool
import com.rolesencia.diceroller.fisica.LectorCara
import com.rolesencia.diceroller.fisica.motor.MotorFisicoPropio
import com.rolesencia.diceroller.tirada.predeterminada.PreSimulador
import com.rolesencia.diceroller.tirada.predeterminada.SesionResultadoPredeterminado
import com.rolesencia.diceroller.tirada.torre.ModelosTorre
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Las tres torres: los dados bajan por las rampas, salen a la bandeja, no se superponen y la tirada termina. */
class TorreTest {

    private fun mesa(n: Int) = ConfigMesa.para(n, 1.2f).let { it.copy(profundidad = maxOf(it.profundidad, ModelosTorre.PROFUNDIDAD_MINIMA)) }

    @Test fun los_dados_salen_de_cada_torre_a_la_bandeja() {
        for (id in ModelosTorre.ids) {
            for (n in listOf(1, 3, 6, 10, 15, 20)) {
                var tiempo = 0f; var forzados = 0; var empujones = 0; var golpecitos = 0; var adentro = 0; var peorSolape = 0f; var maximo = 0f
                repeat(10) { k ->
                    val semilla = 900L + n * 31 + k
                    val dados = pool { repeat(n) { i -> agregar(TipoDado.entries[(i + k) % TipoDado.entries.size]) } }
                    val m = mesa(n)
                    val modelo = ModelosTorre.construir(id, m)
                    val motor = MotorFisicoPropio(semilla = semilla)
                    val sim = SimulacionTirada(
                        dados, GeneradorLanzamiento.impulsosTorre(dados, modelo, Random(semilla)), emptyList(), m, semilla,
                        motor = motor, escenario = modelo.escenarioFisico()
                    )
                    while (!sim.terminada) sim.paso()
                    tiempo += sim.tiempo; forzados += sim.forzados; empujones += sim.empujones; golpecitos += sim.golpecitos
                    maximo = maxOf(maximo, sim.tiempo)
                    peorSolape = maxOf(peorSolape, motor.solapamientoMaximo())
                    adentro += sim.estados().count { modelo.escenarioFisico().dentroDeTorre(it.posicion) }
                    sim.estados().forEach { e ->
                        assertTrue("$id n=$n montado", !LectorCara.leer(e.dado.tipo, e.orientacion).estaMontado(SimulacionTirada.ALINEACION_MINIMA))
                    }
                }
                println("$id n=$n: media ${"%.2f".format(tiempo / 10)} s, máx ${"%.2f".format(maximo)} s, empujones $empujones, golpecitos $golpecitos, forzados $forzados, adentro $adentro, solape $peorSolape")
                assertEquals("$id n=$n: dados que quedaron dentro de la torre", 0, adentro)
                assertTrue("$id n=$n solape $peorSolape", peorSolape <= SimulacionTirada.SOLAPAMIENTO_TOLERADO)
            }
        }
    }

    @Test fun el_modo_predeterminado_tambien_funciona_en_la_torre() {
        val random = Random(4)
        val dados = pool { TipoDado.entries.forEach { agregar(it) } }
        val m = mesa(dados.size)
        for (id in ModelosTorre.ids) {
            val modelo = ModelosTorre.construir(id, m)
            val deseados = dados.associate { it.id to it.tipo.valoresCaras.random(random) }
            val sim = SimulacionTirada(dados, GeneradorLanzamiento.impulsosTorre(dados, modelo, Random(1)), emptyList(), m, 1, escenario = modelo.escenarioFisico())
            val sesion = SesionResultadoPredeterminado(dados, PreSimulador.grabar(sim), deseados, random)
            while (!sesion.terminada) sesion.avanzar(1f / 60f)
            sesion.estadoFinal().forEach { assertEquals(deseados[it.dado.id], LectorCara.leer(it.dado.tipo, it.orientacion).valor) }
        }
    }
}
