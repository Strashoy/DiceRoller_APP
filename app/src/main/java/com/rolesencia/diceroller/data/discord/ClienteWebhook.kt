package com.rolesencia.diceroller.data.discord

import com.rolesencia.diceroller.domain.discord.FormatoDiscord
import com.rolesencia.diceroller.domain.discord.RespuestaDiscord
import com.rolesencia.diceroller.domain.discord.ResultadoEnvio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * El único pedido de red de la app: un POST con el JSON al webhook. Usa `HttpURLConnection`, que
 * viene con Android, para no sumar una biblioteca de red por un solo pedido.
 *
 * Si la conexión falla a mitad de camino no se sabe si el mensaje llegó; se reintenta igual, porque
 * una tirada repetida molesta menos que una que nunca aparece.
 */
class ClienteWebhook(
    private val tiempoConexionMs: Int = 8_000,
    private val tiempoLecturaMs: Int = 8_000
) {
    /** [imagen]: la foto de la tirada en JPEG, o null para un mensaje de sólo texto. */
    suspend fun enviar(url: String, cuerpo: String, imagen: ByteArray? = null): ResultadoEnvio =
        withContext(Dispatchers.IO) { enviarBloqueante(url, cuerpo, imagen) }

    internal fun enviarBloqueante(url: String, cuerpo: String, imagen: ByteArray? = null): ResultadoEnvio {
        var conexion: HttpURLConnection? = null
        return try {
            // Con foto, Discord espera un formulario de dos partes: el JSON y el archivo.
            val limite = "----DiceRoller" + java.lang.Long.toHexString(System.nanoTime())
            val bytes = if (imagen == null) cuerpo.toByteArray(Charsets.UTF_8) else multiparte(limite, cuerpo, imagen)
            val tipo = if (imagen == null) "application/json; charset=utf-8" else "multipart/form-data; boundary=$limite"
            conexion = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = tiempoConexionMs
                // Con adjunto la respuesta tarda más: Discord tiene que recibir y procesar el archivo.
                readTimeout = if (imagen == null) tiempoLecturaMs else tiempoLecturaMs * 3
                instanceFollowRedirects = false
                doOutput = true
                setRequestProperty("Content-Type", tipo)
                setRequestProperty("User-Agent", "DiceRoller-Android")
                setFixedLengthStreamingMode(bytes.size)
            }
            conexion.outputStream.use { it.write(bytes) }
            val codigo = conexion.responseCode
            // El cuerpo sólo interesa en los errores (trae `retry_after`); se lee con tope.
            val texto = if (codigo >= 400) conexion.errorStream?.use { String(it.readNBytesSeguro(4096), Charsets.UTF_8) } else null
            RespuestaDiscord.interpretar(codigo, conexion.getHeaderField("Retry-After"), texto)
        } catch (_: IOException) {
            // Sin red, sin DNS, tiempo agotado: la tirada queda en la cola.
            ResultadoEnvio.Reintentar()
        } finally {
            conexion?.disconnect()
        }
    }
}

/** El cuerpo de un formulario `multipart/form-data` con el JSON (`payload_json`) y la foto (`files[0]`). */
internal fun multiparte(limite: String, cuerpo: String, imagen: ByteArray): ByteArray {
    val salida = java.io.ByteArrayOutputStream(imagen.size + cuerpo.length + 512)
    fun texto(s: String) = salida.write(s.toByteArray(Charsets.UTF_8))
    texto("--$limite\r\nContent-Disposition: form-data; name=\"payload_json\"\r\n")
    texto("Content-Type: application/json; charset=utf-8\r\n\r\n")
    texto(cuerpo)
    // La tirada animada es un GIF (empieza con "GIF"); la foto, un JPEG. El nombre tiene que
    // coincidir con el que nombra la tarjeta del mensaje.
    val esGif = imagen.size > 3 && imagen[0] == 'G'.code.toByte() && imagen[1] == 'I'.code.toByte() && imagen[2] == 'F'.code.toByte()
    val archivo = if (esGif) FormatoDiscord.ARCHIVO_ANIMACION else FormatoDiscord.ARCHIVO_IMAGEN
    texto("\r\n--$limite\r\nContent-Disposition: form-data; name=\"files[0]\"; filename=\"$archivo\"\r\n")
    texto("Content-Type: ${if (esGif) "image/gif" else "image/jpeg"}\r\n\r\n")
    salida.write(imagen)
    texto("\r\n--$limite--\r\n")
    return salida.toByteArray()
}

/** Lee hasta [maximo] bytes (`InputStream.readNBytes` recién existe desde Android 13). */
private fun java.io.InputStream.readNBytesSeguro(maximo: Int): ByteArray {
    val bufer = ByteArray(maximo)
    var leidos = 0
    while (leidos < maximo) {
        val n = read(bufer, leidos, maximo - leidos)
        if (n < 0) break
        leidos += n
    }
    return bufer.copyOf(leidos)
}
