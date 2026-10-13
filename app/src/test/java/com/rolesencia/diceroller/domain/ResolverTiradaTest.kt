package com.rolesencia.diceroller.domain

import com.rolesencia.diceroller.domain.dados.FuenteResultados
import com.rolesencia.diceroller.domain.dados.LanzadorAleatorio
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.CatalogoSistemas
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import com.rolesencia.diceroller.domain.reglas.pool
import com.rolesencia.diceroller.domain.reglas.resolverTirada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ResolverTiradaTest {

    @Test fun las_explosiones_se_relanzan_hasta_que_no_explote_ninguno() {
        // Primer lanzamiento: un 10 y un 3. Luego el 10 explota → 10 otra vez → 4.
        val guion = ArrayDeque(listOf(10, 3, 10, 4))
        val fuente = FuenteResultados { dados -> dados.map { ResultadoDado(it, guion.removeFirst()) } }
        val tirada = resolverTirada(
            CatalogoSistemas.CRONICAS_DE_LA_OSCURIDAD,
            pool { agregar(TipoDado.D10, 2) },
            ParametrosTirada(objetivo = 1),
            fuente
        )
        assertEquals(4, tirada.resultados.size)
        assertEquals(2, tirada.rondasExtra)
        assertEquals(2, tirada.veredicto.total) // 10 y 10 son éxitos (≥8); 3 y 4 no
        assertEquals(tirada.resultados.size, tirada.resultados.map { it.dado.id }.toSet().size)
    }

    @Test fun el_lanzador_aleatorio_respeta_las_caras() {
        val fuente = LanzadorAleatorio(Random(42))
        TipoDado.entries.forEach { tipo ->
            repeat(200) {
                val r = fuente.lanzar(pool { agregar(tipo) }).single()
                assertTrue("${tipo.etiqueta} sacó ${r.valor}", r.valor in tipo.valoresCaras)
            }
        }
    }
}
