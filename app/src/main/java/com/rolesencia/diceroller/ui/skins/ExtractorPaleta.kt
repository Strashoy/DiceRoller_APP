package com.rolesencia.diceroller.ui.skins

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Un color sacado de la imagen, con el nombre que le da Palette y cuánto ocupa. */
data class MuestraPaleta(val nombre: String, val color: Long, val poblacion: Int)

class ResultadoPaleta(val miniatura: ImageBitmap, val dominante: Long, val muestras: List<MuestraPaleta>)

/**
 * Lee una imagen elegida por el usuario (selector de fotos del sistema, sin pedir permisos), la
 * achica y extrae su paleta con AndroidX Palette: dominante, vivos, apagados, claros y oscuros.
 */
suspend fun extraerPaleta(context: Context, uri: Uri): ResultadoPaleta? = withContext(Dispatchers.Default) {
    val bitmap = cargarReducida(context, uri, ladoMaximo = 400) ?: return@withContext null
    val paleta = Palette.from(bitmap).maximumColorCount(16).generate()
    fun color(s: Palette.Swatch?) = s?.rgb?.toLong()?.and(0xFFFFFFFFL)
    val nombradas = listOfNotNull(
        paleta.dominantSwatch?.let { MuestraPaleta("Dominante", color(it)!!, it.population) },
        paleta.vibrantSwatch?.let { MuestraPaleta("Vivo", color(it)!!, it.population) },
        paleta.darkVibrantSwatch?.let { MuestraPaleta("Vivo oscuro", color(it)!!, it.population) },
        paleta.lightVibrantSwatch?.let { MuestraPaleta("Vivo claro", color(it)!!, it.population) },
        paleta.mutedSwatch?.let { MuestraPaleta("Apagado", color(it)!!, it.population) },
        paleta.darkMutedSwatch?.let { MuestraPaleta("Apagado oscuro", color(it)!!, it.population) },
        paleta.lightMutedSwatch?.let { MuestraPaleta("Apagado claro", color(it)!!, it.population) }
    )
    val resto = paleta.swatches.sortedByDescending { it.population }
        .map { MuestraPaleta("Color", color(it)!!, it.population) }
    val muestras = (nombradas + resto).distinctBy { it.color }.take(12)
    val dominante = muestras.firstOrNull()?.color ?: 0xFF808080L
    ResultadoPaleta(bitmap.asImageBitmap(), dominante, muestras)
}

private fun cargarReducida(context: Context, uri: Uri, ladoMaximo: Int): Bitmap? = runCatching {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
    var muestreo = 1
    while (limites.outWidth / muestreo > ladoMaximo * 2 || limites.outHeight / muestreo > ladoMaximo * 2) muestreo *= 2
    val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
}.getOrNull()

/**
 * Copia las imágenes elegidas al almacenamiento interno de la app (achicadas a 512 px, JPEG):
 * los permisos del selector de fotos son temporales, así que la skin guarda su propia copia.
 */
suspend fun guardarImagenesSkin(context: Context, uris: List<Uri>, idSkin: String): List<String> = withContext(Dispatchers.IO) {
    val carpeta = java.io.File(context.filesDir, "skins").apply { mkdirs() }
    uris.mapIndexedNotNull { k, uri ->
        val bmp = cargarReducida(context, uri, ladoMaximo = 256) ?: return@mapIndexedNotNull null
        val archivo = java.io.File(carpeta, "${idSkin}_$k.jpg")
        archivo.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        archivo.absolutePath
    }
}
