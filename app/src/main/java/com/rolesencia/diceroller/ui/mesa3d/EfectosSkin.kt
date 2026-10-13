package com.rolesencia.diceroller.ui.mesa3d

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.util.LruCache
import com.rolesencia.diceroller.domain.apariencia.Tipografia
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Fuente del sistema para cada tipografía de skin. */
object Fuentes {
    private val cache = HashMap<Tipografia, Typeface>()

    fun de(t: Tipografia): Typeface = cache.getOrPut(t) {
        when (t) {
            Tipografia.CLASICA, Tipografia.ROMANA -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            Tipografia.MODERNA -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            Tipografia.TECNICA -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            Tipografia.MANUSCRITA -> Typeface.create("cursive", Typeface.BOLD)
            Tipografia.ANTIGUA -> Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            Tipografia.CONDENSADA -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
        }
    }
}

/**
 * Imágenes de las skins "Imagen por cara". El creador las guarda ya achicadas en el almacenamiento
 * interno de la app; acá se leen una vez y quedan en caché (como mucho ~12 MB).
 */
object ImagenesSkins {
    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun bitmap(ruta: String): Bitmap? = cache.get(ruta) ?: runCatching { BitmapFactory.decodeFile(ruta) }.getOrNull()?.also { cache.put(ruta, it) }
}

/**
 * Dibujos procedurales en coordenadas de la CARA (u, v en [-1, 1]): quedan pegados al dado y
 * giran con él. Se generan una vez por skin, tipo de dado y cara, con una semilla fija.
 */
object PatronesCara {
    private val vetas = HashMap<String, List<List<Pair<Float, Float>>>>()
    private val destellos = HashMap<String, List<Triple<Float, Float, Float>>>()

    /** Vetas de mármol: 3 líneas que serpentean de un borde al otro. */
    fun vetas(clave: String): List<List<Pair<Float, Float>>> = vetas.getOrPut(clave) {
        val r = Random(clave.hashCode())
        List(3) {
            var x = r.nextFloat() * 2f - 1f
            var y = -1.1f
            var angulo = (r.nextFloat() - 0.5f) * 1.2f + (Math.PI / 2).toFloat()
            buildList {
                add(x to y)
                repeat(10) {
                    angulo += (r.nextFloat() - 0.5f) * 0.9f
                    x += cos(angulo) * 0.25f
                    y += sin(angulo) * 0.25f
                    add(x to y)
                }
            }
        }
    }

    /** Destellos de purpurina: (u, v, fase) dentro del círculo unidad. */
    fun destellos(clave: String): List<Triple<Float, Float, Float>> = destellos.getOrPut(clave) {
        val r = Random(clave.hashCode() * 31 + 7)
        List(16) {
            val a = r.nextFloat() * 6.2832f
            val d = sqrt(r.nextFloat()) * 0.9f
            Triple(cos(a) * d, sin(a) * d, r.nextFloat() * 6.2832f)
        }
    }
}
