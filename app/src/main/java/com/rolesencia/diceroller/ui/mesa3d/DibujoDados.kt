package com.rolesencia.diceroller.ui.mesa3d

import android.content.Context
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.rolesencia.diceroller.R
import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.Tipografia
import com.rolesencia.diceroller.domain.apariencia.aRomano
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.sistemas.SimboloLegendario
import com.rolesencia.diceroller.fisica.Poliedros
import com.rolesencia.diceroller.fisica.Vec3
import com.rolesencia.diceroller.tirada.DadoVisual

internal val LUZ = Vec3(-0.35f, 1f, 0.45f).normalizado()

/** Color del cuerpo según la skin del dado (el Legendario siempre es dorado: es su identidad). */
fun colorCuerpo(dado: DadoEnMesa): Color =
    if (dado.tipo == TipoDado.LEGENDARIO) Color(CatalogoSkins.colorPorTipo(TipoDado.LEGENDARIO))
    else Color(dado.skin.cuerpoPara(dado.tipo))

/**
 * Color de números y símbolos: la tinta de la skin si se lee sobre el cuerpo; si no, negro o blanco
 * (la misma regla que en la mesa 3D). Así un dado marfil o dorado de "Por tipo" lleva números negros.
 */
fun colorTinta(dado: DadoEnMesa): Color =
    if (dado.tipo == TipoDado.LEGENDARIO) Color(0xFF2B1D05)
    else Color(dado.skin.tintaEfectiva(dado.skin.cuerpoPara(dado.tipo)))

/** Aristas: rojas en los dados especiales (Hambre, Miedo…), si no las de la skin. */
fun colorBorde(dado: DadoEnMesa): Color = when {
    dado.rol == RolDado.ESPECIAL -> Color(0xFFFF1744)
    dado.tipo == TipoDado.LEGENDARIO -> Color(0xFF7A5A0E)
    else -> Color(dado.skin.bordePara(dado.tipo))
}

/**
 * Los tres símbolos del Dado Legendario (vectores originales del Lanzador), cada uno con el tono
 * del dado físico: Mega cobrizo, Ying-Yang marrón, Heptagrama bordó.
 */
class SimbolosLegendarios(context: Context) {
    private val drawables: Map<SimboloLegendario, Drawable> = SimboloLegendario.entries.associateWith {
        ContextCompat.getDrawable(context, recurso(it))!!.mutate()
    }

    fun drawable(simbolo: SimboloLegendario): Drawable = drawables.getValue(simbolo)

    companion object {
        fun color(simbolo: SimboloLegendario): Color = when (simbolo) {
            SimboloLegendario.MEGA -> Color(0xFF9C4A12)
            SimboloLegendario.YING_YANG -> Color(0xFF4E2A14)
            SimboloLegendario.HEPTAGRAMA -> Color(0xFF6E1E2E)
        }

        fun recurso(simbolo: SimboloLegendario): Int = when (simbolo) {
            SimboloLegendario.HEPTAGRAMA -> R.drawable.heptagrama
            SimboloLegendario.YING_YANG -> R.drawable.yingyang
            SimboloLegendario.MEGA -> R.drawable.mega
        }
    }
}

/** Texto impreso en la cara [indice]. El 6 y el 9 llevan punto en los dados donde se confunden. */
fun etiquetaCara(tipo: TipoDado, indice: Int, tipografia: Tipografia = Tipografia.CLASICA): String {
    val v = tipo.valoresCaras[indice]
    if (tipografia == Tipografia.ROMANA && tipo != TipoDado.FATE) return aRomano(if (tipo == TipoDado.D10 && v == 10) 0 else v)
    return when (tipo) {
        TipoDado.FATE -> when { v > 0 -> "+"; v < 0 -> "−"; else -> "" }
        TipoDado.D100_DECENAS -> if (v == 0) "00" else "$v"
        TipoDado.D10 -> if (v == 10) "0" else if (v == 6 || v == 9) "$v." else "$v"
        TipoDado.D6 -> "$v"
        else -> if (v == 6 || v == 9) "$v." else "$v"
    }
}
