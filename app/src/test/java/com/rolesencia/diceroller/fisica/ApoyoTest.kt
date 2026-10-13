package com.rolesencia.diceroller.fisica

import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.pool
import com.rolesencia.diceroller.fisica.motor.MotorFisicoPropio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** La cara de arriba se lee respecto de lo que sostiene al dado (rampa, otro dado), no sólo la vertical. */
class ApoyoTest {

    private val inclinacion = Math.toRadians(18.0).toFloat()
    private val giroRampa = Quat.desdeEjeAngulo(Vec3(1f, 0f, 0f), inclinacion)
    private val normalRampa = giroRampa.rotar(Vec3.ARRIBA)

    @Test fun un_dado_plano_sobre_una_rampa_muestra_la_cara_paralela_a_la_rampa() {
        TipoDado.entries.forEach { tipo ->
            repeat(tipo.caras) { indice ->
                val orientacion = giroRampa * LectorCara.orientacionParaCara(tipo, indice)
                val respectoRampa = LectorCara.leer(tipo, orientacion, normalRampa)
                assertEquals("${tipo.etiqueta} cara $indice", indice, respectoRampa.indice)
                assertTrue(!respectoRampa.estaMontado(0.95f))
            }
        }
        // Respecto de la vertical, en cambio, el mismo d6 parece inclinado.
        val d6 = giroRampa * LectorCara.orientacionParaCara(TipoDado.D6, 0)
        assertTrue(LectorCara.leer(TipoDado.D6, d6).alineacion < LectorCara.leer(TipoDado.D6, d6, normalRampa).alineacion)
    }

    @Test fun el_motor_informa_la_normal_de_la_rampa_que_sostiene_al_dado() {
        val dado = pool { agregar(TipoDado.D6) }.first()
        val motor = MotorFisicoPropio(semilla = 3)
        motor.prepararMesa(40f, 40f)
        val centroRampa = Vec3(0f, 3f, 0f)
        motor.agregarEscenario(
            EscenarioFisico(
                piezas = listOf(PiezaEscenario(centroRampa, Vec3(8f, 0.5f, 8f), giroRampa, friccion = 0.9f)),
                altoTecho = 30f,
                zonaTorre = floatArrayOf(-8f, -8f, 8f, 8f),
                puntoRescate = Vec3(0f, 0f, 12f)
            )
        )
        val orientacion = giroRampa * LectorCara.orientacionParaCara(TipoDado.D6, 2)
        val alto = Poliedros.de(TipoDado.D6).vertices.maxOf { it.y }
        motor.agregarDado(
            dado,
            ImpulsoLanzamiento(centroRampa + normalRampa * (0.5f + alto + 0.05f), orientacion, Vec3.CERO, Vec3.CERO)
        )
        repeat(240) { motor.paso(1f / 240f) }
        val estado = motor.estados().first()
        assertTrue("apoyo ${estado.apoyo}", (estado.apoyo dot normalRampa) > 0.98f)
        assertEquals(2, LectorCara.leer(TipoDado.D6, estado.orientacion, estado.apoyo).indice)
    }
}
