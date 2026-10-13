package com.rolesencia.diceroller.tirada.predeterminada

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.fisica.EventoColision
import com.rolesencia.diceroller.fisica.Quat
import com.rolesencia.diceroller.fisica.Simetrias
import com.rolesencia.diceroller.tirada.DadoVisual
import com.rolesencia.diceroller.tirada.FrameMesa
import com.rolesencia.diceroller.tirada.MetricasSesion
import com.rolesencia.diceroller.tirada.ModoDecision
import com.rolesencia.diceroller.tirada.SesionTirada
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * MODO 2 — RESULTADO PREDETERMINADO.
 *
 * Proceso:
 *  1. Los números se deciden ANTES (al azar o elegidos a mano): [valoresDeseados].
 *  2. [PreSimulador] calcula la tirada física completa sin dibujarla → [Grabacion].
 *     Esto es la latencia del modo: nada se mueve hasta que termina.
 *  3. Para cada dado se compara la cara que dejó la física (L) con la deseada (D) y se busca una
 *     **rotación de simetría** S del poliedro que lleve D a L (`Simetrias`).
 *  4. Se reproduce la película dibujando cada dado con orientación `q(t) · S`. Como S deja la malla
 *     idéntica, rebotes, choques y sonidos son exactamente los de la física; lo único que cambia
 *     es qué número está impreso en cada cara. Al final, arriba se ve D.
 */
class SesionResultadoPredeterminado(
    override val dados: List<DadoEnMesa>,
    private val grabacion: Grabacion,
    valoresDeseados: Map<Int, Int>,
    random: Random
) : SesionTirada {

    override val modo = ModoDecision.PREDETERMINADO

    private class Ajuste(val caraDeseada: Int, val caraFisica: Int, val simetria: Quat)

    private val ajustes: Map<Int, Ajuste> = dados.associate { dado ->
        val tipo = dado.tipo
        val fisica = grabacion.lecturas.getValue(dado.id).indice
        val deseado = valoresDeseados[dado.id] ?: tipo.valoresCaras[fisica]
        val candidatas = tipo.valoresCaras.indices.filter { tipo.valoresCaras[it] == deseado }
        require(candidatas.isNotEmpty()) { "${tipo.etiqueta} no tiene la cara $deseado" }
        // dF tiene dos caras por valor: si la física ya cayó en una válida, no se toca.
        val cara = if (fisica in candidatas) fisica else candidatas[random.nextInt(candidatas.size)]
        dado.id to Ajuste(cara, fisica, Simetrias.rotacionEntreCaras(tipo, cara, fisica))
    }

    private var t = 0f
    private var indiceEvento = 0

    override fun avanzar(dtReal: Float): FrameMesa {
        t = min(t + min(dtReal, 0.1f), grabacion.duracion)
        val eventos = ArrayList<EventoColision>()
        while (indiceEvento < grabacion.eventos.size && grabacion.eventos[indiceEvento].first <= t) {
            eventos += grabacion.eventos[indiceEvento++].second
        }
        return FrameMesa(cuadroEn(t), eventos)
    }

    override val terminada: Boolean get() = t >= grabacion.duracion

    override fun resultados(): List<ResultadoDado> =
        dados.map { ResultadoDado(it, it.tipo.valoresCaras[ajustes.getValue(it.id).caraDeseada]) }

    override fun estadoFinal(): List<DadoVisual> = cuadroEn(grabacion.duracion)

    /** Interpola entre los dos cuadros grabados que rodean al instante [t] y aplica la simetría. */
    private fun cuadroEn(t: Float): List<DadoVisual> {
        val tiempos = grabacion.tiempos
        var k = tiempos.binarySearch(t).let { if (it >= 0) it else -it - 2 }
        k = k.coerceIn(0, tiempos.size - 1)
        val k2 = min(k + 1, tiempos.size - 1)
        val f = if (k2 == k) 0f else ((t - tiempos[k]) / (tiempos[k2] - tiempos[k])).coerceIn(0f, 1f)
        val a = grabacion.cuadros[k]
        val b = grabacion.cuadros[k2]
        return a.indices.map { i ->
            val e1 = a[i]
            val e2 = b[i]
            val q = nlerp(e1.orientacion, e2.orientacion, f)
            val s = ajustes[e1.dado.id]?.simetria ?: Quat.IDENTIDAD
            DadoVisual(
                e1.dado,
                e1.posicion + (e2.posicion - e1.posicion) * f,
                (q * s).normalizado(),
                e2.enReposo
            )
        }
    }

    private fun nlerp(a: Quat, b: Quat, f: Float): Quat {
        val signo = if (a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w < 0f) -1f else 1f
        return Quat(
            a.x + (b.x * signo - a.x) * f,
            a.y + (b.y * signo - a.y) * f,
            a.z + (b.z * signo - a.z) * f,
            a.w + (b.w * signo - a.w) * f
        ).normalizado()
    }

    override fun metricas(): MetricasSesion = MetricasSesion(
        modo = modo,
        preSimulacionMs = grabacion.milisegundosCalculo,
        segundosSimulados = grabacion.duracion,
        pasos = grabacion.pasos,
        empujones = grabacion.empujones,
        forzados = grabacion.forzados,
        golpecitos = grabacion.golpecitos,
        detalle = dados.map { d ->
            val a = ajustes.getValue(d.id)
            val v = d.tipo.valoresCaras
            if (a.caraDeseada == a.caraFisica) {
                "${d.tipo.etiqueta} #${d.id}: la física ya dejó ${v[a.caraFisica]} arriba, sin ajuste"
            } else {
                val grados = Math.round(Math.toDegrees(2.0 * acos(abs(a.simetria.w).coerceAtMost(1f).toDouble())))
                "${d.tipo.etiqueta} #${d.id}: la física dejó ${v[a.caraFisica]}, se quería ${v[a.caraDeseada]} " +
                    "→ simetría de $grados° aplicada desde el primer cuadro"
            }
        }
    )
}

