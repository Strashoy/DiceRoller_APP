package com.rolesencia.diceroller.fisica

import com.rolesencia.diceroller.domain.dados.TipoDado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LectorCaraTest {

    @Test fun cada_tipo_tiene_una_normal_por_cara() {
        TipoDado.entries.forEach { assertEquals(it.etiqueta, it.caras, GeometriaDados.normales(it).size) }
    }

    @Test fun las_caras_opuestas_suman_como_en_un_dado_real() {
        val sumaOpuestas = mapOf(TipoDado.D6 to 7, TipoDado.D8 to 9, TipoDado.D12 to 13, TipoDado.D20 to 21)
        sumaOpuestas.forEach { (tipo, suma) ->
            val normales = GeometriaDados.normales(tipo)
            normales.forEachIndexed { i, n ->
                val opuesta = normales.indexOfFirst { (it + n).longitud < 1e-4f }
                assertTrue("${tipo.etiqueta}: cara $i sin opuesta", opuesta >= 0)
                assertEquals(suma, tipo.valoresCaras[i] + tipo.valoresCaras[opuesta])
            }
        }
    }

    @Test fun orientar_una_cara_hacia_arriba_y_leerla_devuelve_esa_cara() {
        val aleatorio = Random(7)
        TipoDado.entries.forEach { tipo ->
            repeat(tipo.caras) { indice ->
                // Giro aleatorio alrededor de la vertical: no debe cambiar la lectura.
                val giroY = Quat.desdeEjeAngulo(Vec3.ARRIBA, aleatorio.nextFloat() * 6.28f)
                val orientacion = giroY * LectorCara.orientacionParaCara(tipo, indice)
                val lectura = LectorCara.leer(tipo, orientacion)
                assertEquals("${tipo.etiqueta} cara $indice", indice, lectura.indice)
                assertTrue(lectura.alineacion > 0.999f)
            }
        }
    }

    @Test fun un_d6_en_identidad_muestra_el_1() {
        assertEquals(1, LectorCara.leer(TipoDado.D6, Quat.IDENTIDAD).valor)
    }

    @Test fun un_dado_inclinado_se_detecta_como_montado() {
        val inclinado = Quat.desdeEjeAngulo(Vec3(1f, 0f, 0f), Math.toRadians(40.0).toFloat())
        assertTrue(LectorCara.leer(TipoDado.D6, inclinado).estaMontado())
    }
}
