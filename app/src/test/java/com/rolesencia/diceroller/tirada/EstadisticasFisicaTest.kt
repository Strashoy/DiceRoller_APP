package com.rolesencia.diceroller.tirada

import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.pool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Mide cuánto duran las tiradas y cuántas veces actúan las garantías, según la cantidad de dados. */
class EstadisticasFisicaTest {
    @Test fun hasta_diez_dados_no_hace_falta_forzar_nada() {
        for (n in listOf(2, 5, 10, 20, 30)) {
            val mesa = ConfigMesa.para(n, 1.2f)
            var tiempo = 0f
            var empujones = 0
            var forzados = 0
            var maximo = 0f
            repeat(20) { k ->
                val semilla = k + 100L * n
                val dados = pool { repeat(n) { i -> agregar(TipoDado.entries[i % 6]) } }
                val sim = SimulacionTirada(dados, GeneradorLanzamiento.impulsos(dados, mesa, null, Random(semilla)), emptyList(), mesa, semilla)
                while (!sim.terminada) sim.paso()
                tiempo += sim.tiempo
                empujones += sim.empujones
                forzados += sim.forzados
                maximo = maxOf(maximo, sim.tiempo)
            }
            println(
                "n=$n: media ${"%.2f".format(tiempo / 20)} s, máx ${"%.2f".format(maximo)} s, " +
                    "empujones/tirada ${"%.1f".format(empujones / 20f)}, forzados/tirada ${"%.2f".format(forzados / 20f)}"
            )
            if (n <= 10) {
                assertEquals("n=$n", 0, forzados)
                assertTrue("n=$n", maximo < 3f)
            }
        }
    }
}
