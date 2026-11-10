package com.rolesencia.diceroller.data.discord

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
    suspend fun enviar(url: String, cuerpo: String): ResultadoEnvio =
        withContext(Dispatchers.IO) { enviarBloqueante(url, cuerpo) }

    internal fun enviarBloqueante(url: String, cuerpo: String): ResultadoEnvio {
        var conexion: HttpURLConnection? = null
        return try {
            val bytes = cuerpo.toByteArray(Charsets.UTF_8)
            conexion = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = tiempoConexionMs
                readTimeout = tiempoLecturaMs
                instanceFollowRedirects = false
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
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
