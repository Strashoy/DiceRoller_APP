package com.rolesencia.diceroller.sensorial

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object FeedbackTactil {

    // 1. TIC MECÁNICO (Para el Selector de Dificultad y Piedras)
    // Usamos CLOCK_TICK que es compatible con todos los Androids
    // y se siente como un "Click" físico o una piedra encajando.
    fun impactoSlector(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    // 2. CLICK SECO (Para botones normales)
    fun clickBoton(view: View) {
        // VIRTUAL_KEY es el estándar clásico, CONTEXT_CLICK es más suave en modernos
        if (Build.VERSION.SDK_INT >= 23) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}