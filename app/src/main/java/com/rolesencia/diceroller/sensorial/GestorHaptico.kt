package com.rolesencia.diceroller.sensorial

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class GestorHaptico(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Genera un pulso háptico proporcional a la fuerza del impacto.
     * @param velocidad (0f a 65f aprox)
     */
    fun vibrarImpacto(velocidad: Float, velocidadMaxima: Float = 65f) {
        if (!vibrator.hasVibrator()) return

        val proporcion = (velocidad / velocidadMaxima).coerceIn(0f, 1f)

        // 1. ELEVAMOS EL PISO DE FUERZA:
        // Antes arrancaba en 30. Ahora arranca en 100.
        // Incluso un impacto suave se sentirá con claridad.
        val amplitud = (100 + (proporcion * 155)).toInt().coerceIn(1, 255)

        // 2. AUMENTAMOS EL TIEMPO DE GIRO (INERCIA DEL MOTOR):
        // Pasamos de 15ms a 40ms.
        // Esto le da tiempo al hardware físico a entregar el "golpe" completo sin
        // llegar a convertirse en un zumbido largo y molesto.
        val duracionMs = 40L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duracionMs, amplitud))
        } else {
            vibrator.vibrate(duracionMs)
        }
    }
}