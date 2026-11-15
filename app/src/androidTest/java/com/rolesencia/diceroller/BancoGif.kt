package com.rolesencia.diceroller

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.fisica.Quat
import com.rolesencia.diceroller.fisica.Vec3
import com.rolesencia.diceroller.sensorial.MaterialMesa
import com.rolesencia.diceroller.tirada.ConfigMesa
import com.rolesencia.diceroller.tirada.DadoVisual
import com.rolesencia.diceroller.tirada.torre.ModelosTorre
import com.rolesencia.diceroller.ui.mesa3d.AnimacionTirada
import com.rolesencia.diceroller.ui.mesa3d.TarjetaTirada
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/**
 * Banco de medición del GIF: la MISMA tirada inventada (d20, d6, d12 y d8, dos resaltados) compuesta
 * como lo hace la app, midiendo tiempo y peso. Deja el GIF en `files/banco/tirada.gif` del teléfono.
 * No es un test que afirme nada: sirve para comparar antes y después de cada cambio del dibujo.
 */
@RunWith(AndroidJUnit4::class)
class BancoGif {
    @Test fun componer_la_misma_tirada() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val tipos = listOf(TipoDado.D20, TipoDado.D6, TipoDado.D12, TipoDado.D8)
        val dados = tipos.mapIndexed { i, t -> DadoEnMesa(id = i, tipo = t, rol = if (i == 0) RolDado.ESPECIAL else RolDado.NORMAL) }
        val mesa = ConfigMesa.para(dados.size, 1.6f)
        val modelo = ModelosTorre.construir("madera", mesa)
        val destino = listOf(Vec3(-3f, 1.2f, 4f), Vec3(3f, 1.0f, 3f), Vec3(-2f, 1.2f, -1f), Vec3(3.5f, 1.0f, -2f))
        fun giro(a: Float) = Quat(0f, sin(a / 2), 0f, cos(a / 2)).normalizado()
        val grabados = (0 until 40).map { f ->
            val k = f / 39f
            val alto = (1f - k) * (1f - k) * 9f
            (f * 16_666_667L) to dados.mapIndexed { i, d ->
                DadoVisual(d, Vec3(destino[i].x, destino[i].y + alto, destino[i].z - (1f - k) * 6f),
                    giro(i + (1f - k) * 9f + 0.7f), enReposo = k >= 1f)
            }
        }
        val finales = grabados.last().second
        val datos = TarjetaTirada.Datos("34", "Éxito", 0xFF4CAF50.toInt(), "1d20 + 1d6 + 1d12 + 1d8 · D&D 5e")
        val variante = InstrumentationRegistry.getArguments().getString("variante") ?: "p3"
        AnimacionTirada.ajustes = when (variante) {
            "base" -> AnimacionTirada.Ajustes(tramado = false, escalones = 0, paletaRepartida = false)
            "p2" -> AnimacionTirada.Ajustes(tramado = true, escalones = 0, paletaRepartida = true)
            "p3" -> AnimacionTirada.Ajustes()
            "p5" -> AnimacionTirada.Ajustes(supermuestreo = 2)
            else -> error("variante $variante")
        }
        val inicio = System.nanoTime()
        val gif = AnimacionTirada.componer(
            contexto, mesa, MaterialMesa.MADERA, modelo, grabados, finales, setOf(0, 2), Color(0xFFFF9800), 1.6f, datos
        )
        // Una pasada de calentamiento no cuenta: la primera siempre es más lenta (compilación, cachés).
        val ms = (System.nanoTime() - inicio) / 1_000_000
        val carpeta = File(contexto.filesDir, "banco").apply { mkdirs() }
        File(carpeta, "$variante.gif").writeBytes(gif)
        Log.i("BancoGif", "RESULTADO $variante ms=$ms bytes=${gif.size}")
    }
}
