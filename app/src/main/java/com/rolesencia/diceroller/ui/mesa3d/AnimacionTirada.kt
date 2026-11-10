package com.rolesencia.diceroller.ui.mesa3d

import android.content.Context
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.rolesencia.diceroller.sensorial.MaterialMesa
import com.rolesencia.diceroller.tirada.ConfigMesa
import com.rolesencia.diceroller.tirada.DadoVisual
import com.rolesencia.diceroller.tirada.torre.ModeloTorre

/**
 * La tirada como GIF, para que quien está en el canal de Discord vea rodar los dados sin abrir nada:
 * el final de la caída, la cámara bajando a la toma cenital y la tarjeta con el resultado, sostenida
 * un momento antes de repetirse.
 *
 * **No se graba la pantalla.** Copiar el lienzo en cada cuadro mientras ruedan los dados trabaría la
 * tirada. Durante la tirada sólo se guardan las posiciones de los dados (una lista por cuadro); acá,
 * con la tirada ya terminada y en un hilo de fondo, la escena se vuelve a dibujar en chico con el
 * mismo código que la dibuja en pantalla.
 */
object AnimacionTirada {
    const val ANCHO = 400
    private const val CUADROS_POR_SEGUNDO = 12
    private const val CENTESIMAS = 100 / CUADROS_POR_SEGUNDO
    /** Cuánto del final de la caída se muestra. */
    private const val NANOS_DE_CAIDA = 1_800_000_000L
    private const val CUADROS_BAJADA = 12
    private const val CENTESIMAS_RESULTADO = 280

    /** Un cuadro a dibujar: dónde están los dados, cuánto bajó la cámara (0 general, 1 cenital) y cuánto dura. */
    private class Cuadro(val dados: List<DadoVisual>, val t: Float, val centesimas: Int, val conResultado: Boolean = false)

    /**
     * @param grabados posiciones de los dados durante la tirada, con el instante (ns) de cada cuadro.
     * @param finales los dados ya quietos.
     * @param proporcion alto / ancho del área donde la pantalla muestra la mesa.
     */
    fun componer(
        contexto: Context, mesa: ConfigMesa, material: MaterialMesa, modelo: ModeloTorre?,
        grabados: List<Pair<Long, List<DadoVisual>>>, finales: List<DadoVisual>,
        destacados: Set<Int>, acento: Color, proporcion: Float, datos: TarjetaTirada.Datos
    ): ByteArray {
        val altoFoto = (ANCHO * proporcion.coerceIn(0.6f, 1f)).toInt()
        val alto = altoFoto + TarjetaTirada.altoFranja(ANCHO)
        // Piezas propias: las de la pantalla guardan pinceles y matrices que se están usando en otro hilo.
        val pintor = PintorDados(SimbolosLegendarios(contexto))
        val escena = modelo?.let { EscenaTorre(it) }
        val imagen = ImageBitmap(ANCHO, alto)
        val bitmap = imagen.asAndroidBitmap()
        val lienzoCompose = androidx.compose.ui.graphics.Canvas(imagen)
        val lienzo = android.graphics.Canvas(bitmap)
        val alcance = CanvasDrawScope()
        val pixeles = IntArray(ANCHO * alto)

        fun dibujar(c: Cuadro): IntArray {
            alcance.draw(Density(1f), LayoutDirection.Ltr, lienzoCompose, Size(ANCHO.toFloat(), altoFoto.toFloat())) {
                drawRect(Color(TarjetaTirada.AZUL_NOCHE))
                // El resaltado de los dados que deciden aparece cuando la tirada terminó, como en pantalla.
                dibujarMesa(mesa, material, escena, c.dados, if (c.t > 0f) destacados else emptySet(), pintor, acento, c.t, 0f, 0f, 0f, 0f)
            }
            TarjetaTirada.dibujarFirma(lienzo, ANCHO)
            TarjetaTirada.dibujarFranja(lienzo, ANCHO, altoFoto, datos, c.conResultado)
            bitmap.getPixels(pixeles, 0, ANCHO, 0, 0, ANCHO, alto)
            return pixeles
        }

        val cuadros = ArrayList<Cuadro>()
        // 1. El final de la caída, a 12 cuadros por segundo (la grabación viene a la cadencia de la pantalla).
        val caida = remuestrear(grabados)
        if (caida.isEmpty()) cuadros += Cuadro(finales, 0f, 50)     // se saltó la tirada: un instante en la vista general
        else caida.forEach { cuadros += Cuadro(it, 0f, CENTESIMAS) }
        // 2. La cámara baja a la toma cenital (acelera y frena, como en pantalla).
        for (k in 1 until CUADROS_BAJADA) {
            val x = k / CUADROS_BAJADA.toFloat()
            cuadros += Cuadro(finales, x * x * (3f - 2f * x), CENTESIMAS)
        }
        // 3. El resultado, sostenido.
        val ultimo = Cuadro(finales, 1f, CENTESIMAS_RESULTADO, conResultado = true)
        cuadros += ultimo

        // La paleta sale de tres cuadros representativos: el primero, la mitad de la bajada y el resultado.
        val muestras = listOf(cuadros.first(), cuadros[cuadros.size - 1 - CUADROS_BAJADA / 2], ultimo).map { dibujar(it).copyOf() }
        val gif = CodificadorGif(ANCHO, alto, PaletaGif.de(muestras))
        cuadros.forEach { gif.agregar(dibujar(it), it.centesimas) }
        return gif.terminar()
    }

    /** Los últimos [NANOS_DE_CAIDA] de la grabación, a 12 cuadros por segundo. Vacío si no alcanza para mostrar algo. */
    private fun remuestrear(grabados: List<Pair<Long, List<DadoVisual>>>): List<List<DadoVisual>> {
        if (grabados.size < 8) return emptyList()
        val fin = grabados.last().first
        val inicio = maxOf(grabados.first().first, fin - NANOS_DE_CAIDA)
        val paso = 1_000_000_000L / CUADROS_POR_SEGUNDO
        if (fin - inicio < paso * 4) return emptyList()
        val salida = ArrayList<List<DadoVisual>>()
        var i = 0
        var momento = inicio
        while (momento < fin) {
            while (i < grabados.size - 1 && grabados[i].first < momento) i++
            salida += grabados[i].second
            momento += paso
        }
        return salida
    }
}
