package com.rolesencia.diceroller.domain.apariencia

import com.rolesencia.diceroller.domain.apariencia.ColorArgb.aHsl
import com.rolesencia.diceroller.domain.apariencia.ColorArgb.contraste
import com.rolesencia.diceroller.domain.apariencia.ColorArgb.desdeHsl
import kotlin.random.Random

/** Colores de una skin en edición (sin nombre ni id todavía). */
data class Colores(val cuerpo: Long, val tinta: Long, val borde: Long)

/**
 * Generadores creativos para el creador de skins. Cada idea parte de un color y arma
 * cuerpo, tinta y aristas siguiendo una regla de armonía. Todas garantizan que la tinta
 * se lea (contraste ≥ 4,5) con [asegurarContraste].
 */
enum class IdeaSkin(val titulo: String, val explicacion: String) {
    COMPLEMENTARIO("Complementario", "Números del color opuesto en el círculo cromático."),
    ANALOGO("Análogo", "Números de un tono vecino: armonía suave."),
    TRIADA("Tríada", "Cuerpo, números y aristas repartidos cada 120°."),
    PASTEL("Pastel", "Cuerpo claro y suave, números del mismo tono bien oscuro."),
    NEON("Neón", "Cuerpo casi negro con números y aristas que brillan."),
    MONOCROMO("Monocromo", "Un solo tono en tres luces distintas."),
    METALICO("Metálico", "Poco saturado y medio claro, como metal pulido."),
    SORPRESA("Sorpresa", "Un tono al azar con una armonía al azar.");

    fun aplicar(base: Long, random: Random = Random.Default): Colores {
        val hsl = aHsl(base)
        val h = hsl.h
        val s = hsl.s.coerceAtLeast(0.35f)
        return when (this) {
            COMPLEMENTARIO -> armar(base, desdeHsl(h + 180f, s, 0.5f), desdeHsl(h, s, 0.22f))
            ANALOGO -> armar(base, desdeHsl(h + 35f, s, 0.5f), desdeHsl(h - 35f, s, 0.25f))
            TRIADA -> armar(base, desdeHsl(h + 240f, s, 0.5f), desdeHsl(h + 120f, s, 0.35f))
            PASTEL -> armar(desdeHsl(h, 0.55f, 0.82f), desdeHsl(h, 0.6f, 0.22f), desdeHsl(h, 0.4f, 0.62f))
            NEON -> armar(desdeHsl(h, 0.35f, 0.07f), desdeHsl(h, 1f, 0.6f), desdeHsl(h + 180f, 1f, 0.5f))
            MONOCROMO -> armar(desdeHsl(h, s, 0.45f), desdeHsl(h, s, 0.92f), desdeHsl(h, s, 0.22f))
            METALICO -> armar(desdeHsl(h, 0.16f, 0.62f), desdeHsl(h, 0.25f, 0.1f), desdeHsl(h, 0.2f, 0.32f))
            SORPRESA -> {
                val otra = entries.filter { it != SORPRESA }.random(random)
                otra.aplicar(desdeHsl(random.nextFloat() * 360f, 0.5f + random.nextFloat() * 0.5f, 0.3f + random.nextFloat() * 0.4f), random)
            }
        }
    }

    private fun armar(cuerpo: Long, tinta: Long, borde: Long) = Colores(cuerpo, asegurarContraste(tinta, cuerpo), borde)
}

/**
 * Aclara u oscurece la [tinta] (sin cambiar su tono) hasta que contraste al menos [minimo] con el
 * [fondo]. Si ni así alcanza, devuelve negro o blanco.
 */
fun asegurarContraste(tinta: Long, fondo: Long, minimo: Double = 4.5): Long {
    if (contraste(tinta, fondo) >= minimo) return tinta
    val hsl = aHsl(tinta)
    val haciaClaro = ColorArgb.luminancia(fondo) < 0.18
    var l = hsl.l
    repeat(40) {
        l = (if (haciaClaro) l + 0.025f else l - 0.025f).coerceIn(0f, 1f)
        val candidato = desdeHsl(hsl.h, hsl.s, l)
        if (contraste(candidato, fondo) >= minimo) return candidato
    }
    return ColorArgb.tintaLegible(fondo)
}

/**
 * Skin sugerida a partir de los colores de una imagen: el color dominante va al cuerpo, la tinta es
 * el color de la imagen que mejor contrasta (si ninguno llega a 4,5, negro o blanco) y las aristas
 * el más oscuro de la imagen.
 */
fun coloresDesdeImagen(dominante: Long, otros: List<Long>): Colores {
    val tinta = otros.filter { contraste(it, dominante) >= 4.5 }.maxByOrNull { contraste(it, dominante) }
        ?: ColorArgb.tintaLegible(dominante)
    val borde = (otros + dominante).minByOrNull { ColorArgb.luminancia(it) }
        ?.takeIf { it != dominante } ?: ColorArgb.mezclar(dominante, 0xFF000000L, 0.45f)
    return Colores(dominante, tinta, borde)
}
