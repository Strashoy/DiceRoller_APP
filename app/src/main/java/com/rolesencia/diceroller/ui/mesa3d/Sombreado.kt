package com.rolesencia.diceroller.ui.mesa3d

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.rolesencia.diceroller.domain.apariencia.EstiloSkin
import com.rolesencia.diceroller.fisica.Vec3
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Iluminación de una cara, separada del dibujo para poder probarla.
 *
 * Sólo los **reflejos** (metal y gema) dependen de la cámara: la luz difusa está fija al mundo.
 * [plana] (0..1) apaga esos reflejos en las caras de arriba durante la toma cenital, para que la
 * cara superior se vea igual que en la vista general.
 */
internal object Sombreado {
    fun difusa(normal: Vec3): Float = max(0f, normal dot LUZ)

    fun luz(normal: Vec3): Float = 0.42f + 0.58f * difusa(normal)

    /** Reflejo especular (Blinn-Phong) visto desde [vista]; [potencia] más alta = reflejo más chico. */
    fun reflejo(normal: Vec3, vista: Vec3, potencia: Int, plana: Float): Float =
        max(0f, normal dot (LUZ + vista).normalizado()).pow(potencia) * (1f - plana)

    fun metal(base: Color, normal: Vec3, vista: Vec3, plana: Float): Color =
        lerp(lerp(Color.Black, base, 0.3f + 0.55f * difusa(normal)), Color.White, min(1f, reflejo(normal, vista, 28, plana) * 0.95f))

    fun destelloGema(normal: Vec3, vista: Vec3, plana: Float): Float = reflejo(normal, vista, 40, plana) * 0.55f

    /** Color con que se ve una cara lisa (sin vetas ni destellos de purpurina) de ese estilo. */
    fun tonoCara(estilo: EstiloSkin, base: Color, normal: Vec3, vista: Vec3, plana: Float): Color = when (estilo) {
        EstiloSkin.METALICO -> metal(base, normal, vista, plana)
        EstiloSkin.GEMA -> lerp(lerp(Color.Black, base, luz(normal)), Color.White, destelloGema(normal, vista, plana))
        EstiloSkin.BRILLO_NOCTURNO -> lerp(Color.Black, base, 0.6f + 0.4f * luz(normal))
        else -> lerp(Color.Black, base, luz(normal))
    }
}
