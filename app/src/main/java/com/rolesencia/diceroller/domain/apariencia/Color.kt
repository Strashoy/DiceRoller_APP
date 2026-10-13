package com.rolesencia.diceroller.domain.apariencia

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Utilidades de color en Kotlin puro (ARGB empaquetado en Long, como `0xFFRRGGBB`).
 * Se usan en el catálogo de skins, en el creador y en los tests de legibilidad.
 */
object ColorArgb {

    const val NEGRO = 0xFF111111L
    const val BLANCO = 0xFFFFFFFFL

    fun rojo(c: Long) = ((c shr 16) and 0xFF).toInt()
    fun verde(c: Long) = ((c shr 8) and 0xFF).toInt()
    fun azul(c: Long) = (c and 0xFF).toInt()

    fun desdeRgb(r: Int, g: Int, b: Int): Long =
        0xFF000000L or (r.coerceIn(0, 255).toLong() shl 16) or (g.coerceIn(0, 255).toLong() shl 8) or b.coerceIn(0, 255).toLong()

    /** Luminancia relativa WCAG 2.x (0 = negro, 1 = blanco). */
    fun luminancia(c: Long): Double {
        fun canal(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * canal(rojo(c)) + 0.7152 * canal(verde(c)) + 0.0722 * canal(azul(c))
    }

    /** Relación de contraste WCAG entre dos colores (1 a 21). 4,5 o más se lee bien. */
    fun contraste(a: Long, b: Long): Double {
        val la = luminancia(a)
        val lb = luminancia(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Negro o blanco, el que mejor se lea sobre [fondo]. */
    fun tintaLegible(fondo: Long): Long = if (contraste(fondo, NEGRO) >= contraste(fondo, BLANCO)) NEGRO else BLANCO

    /** Mezcla lineal entre dos colores (t = 0 → a, t = 1 → b). */
    fun mezclar(a: Long, b: Long, t: Float): Long = desdeRgb(
        (rojo(a) + (rojo(b) - rojo(a)) * t).roundToInt(),
        (verde(a) + (verde(b) - verde(a)) * t).roundToInt(),
        (azul(a) + (azul(b) - azul(a)) * t).roundToInt()
    )

    /** Tono (0–360), saturación y luz (0–1). */
    data class Hsl(val h: Float, val s: Float, val l: Float)

    fun aHsl(c: Long): Hsl {
        val r = rojo(c) / 255f
        val g = verde(c) / 255f
        val b = azul(c) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val l = (mx + mn) / 2f
        if (mx == mn) return Hsl(0f, 0f, l)
        val d = mx - mn
        val s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
        val h = when (mx) {
            r -> ((g - b) / d + (if (g < b) 6f else 0f))
            g -> ((b - r) / d + 2f)
            else -> ((r - g) / d + 4f)
        } * 60f
        return Hsl(h, s, l)
    }

    fun desdeHsl(h: Float, s: Float, l: Float): Long {
        val hh = ((h % 360f) + 360f) % 360f
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((hh / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            hh < 60f -> Triple(c, x, 0f)
            hh < 120f -> Triple(x, c, 0f)
            hh < 180f -> Triple(0f, c, x)
            hh < 240f -> Triple(0f, x, c)
            hh < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return desdeRgb(((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }

    fun hex(c: Long): String = "#%06X".format(c and 0xFFFFFF)

    fun desdeHex(texto: String): Long? = texto.trim().removePrefix("#").takeIf { it.length == 6 }
        ?.toLongOrNull(16)?.let { 0xFF000000L or it }
}
