package com.rolesencia.diceroller.sensorial

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.fisica.EventoColision
import com.rolesencia.diceroller.fisica.Superficie
import com.rolesencia.diceroller.fisica.TipoEvento
import com.rolesencia.diceroller.fisica.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlanificadorSonidoTest {

    @Test fun la_bolsa_reparte_todas_las_tomas_y_nunca_repite_seguido() {
        val bolsa = BolsaAleatoria(5, Random(1))
        val salida = List(500) { bolsa.siguiente() }
        salida.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
        salida.chunked(5).forEach { assertEquals((0 until 5).toSet(), it.toSet()) }
    }

    @Test fun la_intensidad_elige_capa_suave_o_fuerte_y_material() {
        val p = PlanificadorSonido(Random(2))
        val d6 = DadoEnMesa(0, TipoDado.D6)
        fun cat(v: Float, sup: Superficie, mesa: MaterialMesa, t: Float) =
            p.planificar(EventoColision(d6, sup, v, posicion = Vec3.CERO), mesa, 15f, t).first().categoria
        assertEquals(Categoria.LIGERO_LONA_SUAVE, cat(20f, Superficie.MESA, MaterialMesa.LONA, 0f))
        assertEquals(Categoria.LIGERO_LONA_FUERTE, cat(240f, Superficie.MESA, MaterialMesa.LONA, 1f))
        assertEquals(Categoria.LIGERO_MADERA_FUERTE, cat(240f, Superficie.MESA, MaterialMesa.MADERA, 2f))
        assertEquals(Categoria.LIGERO_MADERA_SUAVE, cat(20f, Superficie.PARED, MaterialMesa.LONA, 3f))
    }

    @Test fun una_tirada_tipica_usa_muchas_tomas_distintas() {
        val p = PlanificadorSonido(Random(3))
        val random = Random(4)
        val dados = TipoDado.entries.mapIndexed { i, t -> DadoEnMesa(i, t) }
        val usadas = HashSet<Pair<Categoria, Int>>()
        var t = 0f
        repeat(400) {
            t += 0.05f
            val a = dados.random(random)
            val sup = Superficie.entries.random(random)
            val ev = EventoColision(
                a, sup, random.nextFloat() * 250f,
                if (random.nextInt(10) == 0) TipoEvento.ASENTAMIENTO else TipoEvento.IMPACTO,
                Vec3(random.nextFloat() * 30f - 15f, 0f, 0f),
                if (sup == Superficie.DADO) dados.random(random) else null
            )
            MaterialMesa.entries.forEach { mesa -> p.planificar(ev, mesa, 15f, t).forEach { usadas += it.categoria to it.muestra } }
        }
        println("Tomas distintas usadas: ${usadas.size}/65")
        assertTrue(usadas.size >= 60)
    }

    @Test fun la_misma_fuente_no_suena_dos_veces_en_35_ms() {
        val p = PlanificadorSonido(Random(5))
        val ev = EventoColision(DadoEnMesa(0, TipoDado.D20), Superficie.MESA, 100f)
        assertEquals(1, p.planificar(ev, MaterialMesa.LONA, 15f, 0f).size)
        assertTrue(p.planificar(ev, MaterialMesa.LONA, 15f, 0.01f).isEmpty())
        assertTrue(p.planificar(ev, MaterialMesa.LONA, 15f, 0.05f).isNotEmpty())
    }

    @Test fun el_paneo_sigue_la_posicion_del_choque() {
        val p = PlanificadorSonido(Random(6))
        val izquierda = p.planificar(EventoColision(DadoEnMesa(0, TipoDado.D6), Superficie.MESA, 100f, posicion = Vec3(-14f, 0f, 0f)), MaterialMesa.LONA, 15f, 0f).first()
        val derecha = p.planificar(EventoColision(DadoEnMesa(1, TipoDado.D6), Superficie.MESA, 100f, posicion = Vec3(14f, 0f, 0f)), MaterialMesa.LONA, 15f, 0f).first()
        assertTrue(izquierda.volumenIzq > izquierda.volumenDer)
        assertTrue(derecha.volumenDer > derecha.volumenIzq)
    }
}
