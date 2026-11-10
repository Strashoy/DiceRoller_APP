package com.rolesencia.diceroller.ui.mesa3d

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import java.io.ByteArrayOutputStream

/**
 * Pedido de imagen del resultado que la pantalla le hace a la mesa 3D.
 * @param clave identifica la tirada (su instante).
 * @param animada además de la foto final, los últimos segundos de la tirada como GIF.
 */
data class PedidoFoto(val clave: Long, val datos: TarjetaTirada.Datos, val animada: Boolean)

/**
 * La "foto" de una tirada para compartir: los dados tal como quedaron en la mesa (con su skin y su
 * escenario), y debajo el resultado en grande. Es lo que distingue a estas tiradas de las de un bot
 * de Discord, que sólo puede escribir números: acá se ve **la tirada misma**.
 *
 * Usa `android.graphics` (no Compose) porque se arma fuera de la pantalla, en un hilo de fondo.
 */
object TarjetaTirada {

    /** Lo que se escribe debajo de la foto, igual que en el marcador: el número manda. */
    data class Datos(
        /** El total, grande; null si el sistema no da un número (el Dado Legendario). */
        val numero: String?,
        /** El veredicto ("Éxito", "Fallo con Miedo"); null si el número lo dice todo. */
        val etiqueta: String?,
        /** Color del veredicto (ARGB), el mismo del marcador. */
        val color: Int,
        /** Renglón chico: la notación y el sistema ("1d20 + 5 · D&D 5e (d20)"). */
        val pie: String
    )

    private const val ANCHO = 960
    /** Alto de la franja del resultado para una tarjeta de [ANCHO]; se escala con el ancho. */
    private const val FRANJA = 196
    private val MARFIL = Color.rgb(255, 255, 240)
    val AZUL_NOCHE = Color.rgb(13, 27, 42)

    /** Alto de la franja para una imagen de [ancho] px. */
    fun altoFranja(ancho: Int): Int = FRANJA * ancho / ANCHO

    /**
     * @param foto el lienzo completo de la mesa.
     * @param area la parte que no tapa la interfaz (ahí encuadra la cámara a los dados).
     * @return la tarjeta en JPEG.
     */
    fun componer(foto: Bitmap, area: Rect, datos: Datos): ByteArray {
        // Las capturas de una capa gráfica pueden venir en memoria de video: se pasan a una copia común.
        val origen = if (foto.config == Bitmap.Config.HARDWARE) foto.copy(Bitmap.Config.ARGB_8888, false) else foto
        val recorte = Rect(area)
        if (!recorte.intersect(0, 0, origen.width, origen.height)) recorte.set(0, 0, origen.width, origen.height)
        // Como mucho cuadrada: en un chat, una imagen más alta que ancha ocupa demasiado.
        if (recorte.height() > recorte.width()) {
            val sobra = (recorte.height() - recorte.width()) / 2
            recorte.set(recorte.left, recorte.top + sobra, recorte.right, recorte.top + sobra + recorte.width())
        }
        val altoFoto = (ANCHO.toFloat() * recorte.height() / recorte.width()).toInt().coerceAtLeast(320)
        // La foto arriba y el resultado en su propia franja: escrito encima tapaba el dado de más abajo.
        val tarjeta = Bitmap.createBitmap(ANCHO, altoFoto + FRANJA, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(tarjeta)
        lienzo.drawBitmap(origen, recorte, Rect(0, 0, ANCHO, altoFoto), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        dibujarFirma(lienzo, ANCHO)
        dibujarFranja(lienzo, ANCHO, altoFoto, datos, conResultado = true)
        return ByteArrayOutputStream(160_000).also { tarjeta.compress(Bitmap.CompressFormat.JPEG, 88, it) }.toByteArray()
    }

    private fun pincel(tamano: Float, color: Int, negrita: Boolean = true) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = tamano
        this.color = color
        typeface = Typeface.create(Typeface.DEFAULT, if (negrita) Typeface.BOLD else Typeface.NORMAL)
        setShadowLayer(tamano * 0.08f, 0f, tamano * 0.03f, Color.argb(200, 0, 0, 0))
    }

    /** La firma, arriba a la derecha de la foto: quien ve la imagen en un servidor sabe de dónde salió. */
    fun dibujarFirma(lienzo: Canvas, ancho: Int) {
        val e = ancho / ANCHO.toFloat()
        val pFirma = pincel(26f * e, Color.argb(190, 255, 255, 240)).apply { letterSpacing = 0.18f }
        val firma = "DICE ROLLER"
        lienzo.drawText(firma, ancho - 40f * e - pFirma.measureText(firma), 58f * e, pFirma)
    }

    /**
     * La franja de abajo, desde [y0]: la foto se funde con ella y una línea las separa. Con
     * [conResultado] lleva el total y el veredicto; sin él (mientras los dados todavía ruedan, en la
     * animación) sólo el renglón de la notación.
     */
    fun dibujarFranja(lienzo: Canvas, ancho: Int, y0: Int, datos: Datos, conResultado: Boolean) {
        val e = ancho / ANCHO.toFloat()
        val margen = 40f * e
        val alto = y0 + altoFranja(ancho)
        val velo = Paint().apply {
            shader = LinearGradient(0f, y0 - 90f * e, 0f, y0.toFloat(), Color.TRANSPARENT, AZUL_NOCHE, Shader.TileMode.CLAMP)
        }
        lienzo.drawRect(0f, y0 - 90f * e, ancho.toFloat(), y0.toFloat(), velo)
        lienzo.drawRect(0f, y0.toFloat(), ancho.toFloat(), alto.toFloat(), Paint().apply { color = AZUL_NOCHE })
        val linea = if (conResultado) datos.color else Color.argb(90, 255, 255, 240)
        lienzo.drawRect(0f, y0.toFloat(), ancho.toFloat(), y0 + 5f * e, Paint().apply { color = linea })

        val pPie = pincel(34f * e, Color.argb(220, 255, 255, 240), negrita = false)
        val pie = TextUtils.ellipsize(datos.pie, pPie, ancho - 2 * margen, TextUtils.TruncateAt.END).toString()
        val basePie = alto - 30f * e
        lienzo.drawText(pie, margen, basePie, pPie)
        if (!conResultado) return

        // El número, grande, y el veredicto a su lado; sin número, el veredicto es el titular.
        val baseTitular = basePie - 54f * e
        var x = margen
        if (datos.numero != null) {
            val pNumero = pincel(118f * e, if (datos.etiqueta == null) datos.color else MARFIL)
            lienzo.drawText(datos.numero, x, baseTitular, pNumero)
            x += pNumero.measureText(datos.numero) + 28f * e
        }
        if (datos.etiqueta != null) {
            val pEtiqueta = pincel((if (datos.numero != null) 60f else 92f) * e, datos.color)
            // Si no entra en lo que queda del renglón, se achica en vez de cortarse.
            val disponible = ancho - margen - x
            val anchoTexto = pEtiqueta.measureText(datos.etiqueta)
            if (anchoTexto > disponible) pEtiqueta.textSize = pEtiqueta.textSize * disponible / anchoTexto
            lienzo.drawText(datos.etiqueta, x, baseTitular, pEtiqueta)
        }
    }
}
