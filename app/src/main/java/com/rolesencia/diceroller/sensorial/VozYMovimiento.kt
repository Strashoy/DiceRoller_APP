package com.rolesencia.diceroller.sensorial

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import kotlin.math.sqrt

/**
 * Lee el veredicto en voz alta, como la opción "speak results" de Dice by PCalc. Sirve a quien
 * tira sin mirar la pantalla (dirigiendo la partida, con el teléfono en la mesa) y complementa
 * a TalkBack, que sólo habla si el usuario lo tiene activado.
 */
class LectorVoz(context: Context) {
    private var listo = false
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { estado ->
            listo = estado == TextToSpeech.SUCCESS
            if (listo) {
                // Español rioplatense si el motor lo tiene; si no, español genérico.
                val rioplatense = Locale.forLanguageTag("es-AR")
                val ok = tts.isLanguageAvailable(rioplatense) >= TextToSpeech.LANG_AVAILABLE
                tts.language = if (ok) rioplatense else Locale.forLanguageTag("es")
            }
        }
    }

    fun decir(texto: String) {
        if (listo && texto.isNotBlank()) tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "veredicto")
    }

    fun cerrar() {
        tts.stop()
        tts.shutdown()
    }
}

@Composable
fun rememberLectorVoz(activo: Boolean): LectorVoz? {
    val contexto = LocalContext.current
    // Sólo se crea el motor de voz si el ajuste está activado: arrancarlo cuesta memoria y tiempo.
    val lector = remember(activo) { if (activo) LectorVoz(contexto) else null }
    DisposableEffect(lector) { onDispose { lector?.cerrar() } }
    return lector
}

/**
 * Agitar el teléfono lanza los dados, como sacudir un cubilete (gesto típico de las apps de dados).
 * Se exige un sacudón fuerte y dos picos seguidos para que caminar o apoyar el teléfono no lance.
 */
@Composable
fun AgitarParaLanzar(activo: Boolean, onAgitar: (fuerza: Float) -> Unit) {
    val contexto = LocalContext.current
    val accion by rememberUpdatedState(onAgitar)
    DisposableEffect(activo) {
        if (!activo) return@DisposableEffect onDispose { }
        val sensores = contexto.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val acelerometro = sensores?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensores?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val restarGravedad = acelerometro?.type == Sensor.TYPE_ACCELEROMETER
        var primerPico = 0L
        var ultimoLanzamiento = 0L
        val oyente = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
                var a = sqrt(x * x + y * y + z * z)
                if (restarGravedad) a = kotlin.math.abs(a - SensorManager.GRAVITY_EARTH)
                if (a < UMBRAL) return
                val ahora = SystemClock.uptimeMillis()
                if (ahora - ultimoLanzamiento < ESPERA_MS) return
                if (ahora - primerPico in 80..600) {
                    ultimoLanzamiento = ahora
                    primerPico = 0L
                    accion((a / (UMBRAL * 2f)).coerceIn(0.3f, 1f))
                } else {
                    primerPico = ahora
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (acelerometro != null) sensores?.registerListener(oyente, acelerometro, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensores?.unregisterListener(oyente) }
    }
}

/** m/s² por encima de la gravedad: un sacudón decidido, no un paso ni un golpe en la mesa. */
private const val UMBRAL = 14f
/** Después de lanzar se ignora el sensor un rato, mientras la mano sigue moviéndose. */
private const val ESPERA_MS = 1500L
