package com.rolesencia.diceroller.tirada

import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.pool
import com.rolesencia.diceroller.fisica.LectorCara
import com.rolesencia.diceroller.tirada.fisicareal.SesionFisicaReal
import com.rolesencia.diceroller.tirada.predeterminada.PreSimulador
import com.rolesencia.diceroller.tirada.predeterminada.SesionResultadoPredeterminado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ModosDecisionTest {

    private val mesa = ConfigMesa(18f, 22f)

    private fun simulacion(semilla: Long, dados: List<com.rolesencia.diceroller.domain.dados.DadoEnMesa>) =
        SimulacionTirada(dados, GeneradorLanzamiento.impulsos(dados, mesa, null, Random(semilla)), emptyList(), mesa, semilla)

    private val todos = pool { TipoDado.entries.forEach { agregar(it) } }

    @Test fun la_fisica_real_siempre_termina_con_dados_bien_apoyados() {
        repeat(15) { semilla ->
            val sesion = SesionFisicaReal(simulacion(semilla.toLong(), todos))
            var cuadros = 0
            while (!sesion.terminada && cuadros < 60 * 12) { sesion.avanzar(1f / 60f); cuadros++ }
            assertTrue("semilla $semilla no terminó", sesion.terminada)
            sesion.estadoFinal().forEach { d ->
                val l = LectorCara.leer(d.dado.tipo, d.orientacion)
                assertTrue("semilla $semilla ${d.dado.tipo.etiqueta} montado", !l.estaMontado(SimulacionTirada.ALINEACION_MINIMA))
                assertTrue(d.posicion.y < 3f)
            }
            assertEquals(todos.size, sesion.resultados().size)
        }
    }

    @Test fun la_simulacion_es_determinista() {
        val a = PreSimulador.grabar(simulacion(99, todos))
        val b = PreSimulador.grabar(simulacion(99, todos))
        assertEquals(a.pasos, b.pasos)
        assertEquals(a.cuadros.last(), b.cuadros.last())
    }

    @Test fun el_modo_predeterminado_muestra_exactamente_el_valor_pedido() {
        val random = Random(5)
        repeat(20) { semilla ->
            val deseados = todos.associate { it.id to it.tipo.valoresCaras.random(random) }
            val grabacion = PreSimulador.grabar(simulacion(1000L + semilla, todos))
            val sesion = SesionResultadoPredeterminado(todos, grabacion, deseados, random)
            while (!sesion.terminada) sesion.avanzar(1f / 60f)
            sesion.estadoFinal().forEach { d ->
                assertEquals("semilla $semilla ${d.dado.tipo.etiqueta}", deseados[d.dado.id], LectorCara.leer(d.dado.tipo, d.orientacion).valor)
            }
            assertEquals(deseados, sesion.resultados().associate { it.dado.id to it.valor })
        }
    }

    @Test fun cuanto_tarda_la_pre_simulacion() {
        listOf(1, 5, 10, 20, 30).forEach { n ->
            val dados = pool { agregar(TipoDado.D6, n) }
            PreSimulador.grabar(simulacion(1, dados)) // calentamiento de la JVM
            val g = PreSimulador.grabar(simulacion(7, dados))
            println("Pre-simulación $n d6: ${g.milisegundosCalculo} ms de CPU para ${"%.2f".format(g.duracion)} s de tirada")
        }
    }

    /** Equiprobabilidad aproximada de la física real (χ² con 5 grados de libertad, p ≈ 0,001 → 20,5). */
    @Test fun el_d6_fisico_no_esta_cargado() {
        val conteo = IntArray(6)
        val n = 600
        repeat(n) { semilla ->
            val dados = pool { agregar(TipoDado.D6) }
            val sim = simulacion(5000L + semilla, dados)
            while (!sim.terminada) sim.paso()
            conteo[sim.lecturas().values.single().valor - 1]++
        }
        val esperado = n / 6.0
        val chi2 = conteo.sumOf { (it - esperado) * (it - esperado) / esperado }
        println("d6 físico: ${conteo.toList()} χ²=${"%.2f".format(chi2)}")
        assertTrue("χ² = $chi2", chi2 < 20.5)
    }
}
