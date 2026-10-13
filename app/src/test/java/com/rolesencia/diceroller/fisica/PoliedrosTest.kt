package com.rolesencia.diceroller.fisica

import com.rolesencia.diceroller.domain.dados.TipoDado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliedrosTest {

    private val esperado = mapOf(
        TipoDado.D4 to (4 to 3), TipoDado.D6 to (8 to 4), TipoDado.D8 to (6 to 3), TipoDado.D10 to (12 to 4),
        TipoDado.D12 to (20 to 5), TipoDado.D20 to (12 to 3)
    )

    @Test fun cada_malla_tiene_los_vertices_y_caras_correctos() {
        esperado.forEach { (tipo, vc) ->
            val p = Poliedros.de(tipo)
            assertEquals(tipo.etiqueta, vc.first, p.vertices.size)
            assertEquals(tipo.etiqueta, tipo.caras, p.caras.size)
            p.caras.forEach { assertEquals(tipo.etiqueta, vc.second, it.vertices.size) }
        }
    }

    @Test fun las_caras_son_planas_y_convexas() {
        TipoDado.entries.forEach { tipo ->
            val p = Poliedros.de(tipo)
            p.caras.forEach { cara ->
                cara.vertices.forEach { i ->
                    assertEquals("${tipo.etiqueta} plano", cara.distancia, cara.normal dot p.vertices[i], 1e-3f)
                }
                // Todos los demás vértices quedan detrás del plano de la cara.
                p.vertices.forEach { v -> assertTrue((cara.normal dot v) <= cara.distancia + 1e-3f) }
            }
        }
    }

    @Test fun hay_una_simetria_entre_cualquier_par_de_caras() {
        TipoDado.entries.forEach { tipo ->
            val p = Poliedros.de(tipo)
            for (desde in p.caras.indices) for (hacia in p.caras.indices) {
                val s = Simetrias.rotacionEntreCaras(tipo, desde, hacia)
                assertTrue("${tipo.etiqueta} $desde→$hacia", Simetrias.esSimetria(p, s))
                val n = s.rotar(p.caras[desde].normal)
                assertTrue((n - p.caras[hacia].normal).longitud < 1e-3f)
            }
        }
    }
}
