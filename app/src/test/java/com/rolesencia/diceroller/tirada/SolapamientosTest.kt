package com.rolesencia.diceroller.tirada

import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.pool
import com.rolesencia.diceroller.fisica.motor.MotorFisicoPropio
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Los dados nunca deben verse metidos uno dentro de otro: ni al soltarlos, ni rodando, ni al final. */
class SolapamientosTest {

    @Test fun ningun_par_de_dados_se_superpone() {
        var peorInicio = 0f
        var peorDurante = 0f
        var peorFinal = 0f
        for (n in listOf(2, 4, 8, 12, 20)) repeat(10) { k ->
            val semilla = 7000L + n * 100 + k
            val dados = pool { repeat(n) { i -> agregar(TipoDado.entries[(i + k) % TipoDado.entries.size]) } }
            val mesa = ConfigMesa.para(n, 1.2f)
            val motor = MotorFisicoPropio(semilla = semilla)
            val sim = SimulacionTirada(dados, GeneradorLanzamiento.impulsos(dados, mesa, null, Random(semilla)), emptyList(), mesa, semilla, motor = motor)
            peorInicio = maxOf(peorInicio, motor.solapamientoMaximo())
            while (!sim.terminada) {
                sim.paso()
                peorDurante = maxOf(peorDurante, motor.solapamientoMaximo())
            }
            peorFinal = maxOf(peorFinal, motor.solapamientoMaximo())
        }
        println("Superposición máxima (cm): inicio=$peorInicio durante=$peorDurante final=$peorFinal")
        assertTrue("inicio $peorInicio", peorInicio == 0f)
        assertTrue("durante $peorDurante", peorDurante < 0.1f)   // 1 mm, sólo dentro de un sub-paso
        assertTrue("final $peorFinal", peorFinal <= SimulacionTirada.SOLAPAMIENTO_TOLERADO)
    }
}
