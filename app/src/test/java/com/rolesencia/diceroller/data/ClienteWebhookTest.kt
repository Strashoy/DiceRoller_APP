package com.rolesencia.diceroller.data

import com.rolesencia.diceroller.data.discord.ClienteWebhook
import com.rolesencia.diceroller.domain.discord.ResultadoEnvio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

/**
 * El cliente contra un servidor HTTP de verdad (local, de un solo pedido): se comprueba lo que sale
 * por el cable y cómo se lee cada respuesta, sin tocar Discord.
 */
class ClienteWebhookTest {

    private class ServidorFalso(private val respuesta: String) : AutoCloseable {
        private val socket = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        val url: String get() = "http://127.0.0.1:${socket.localPort}/api/webhooks/1/abc"
        @Volatile var pedido: String = ""
        private val hilo = thread {
            runCatching {
                socket.accept().use { conexion ->
                    val entrada = conexion.getInputStream()
                    val cabeceras = StringBuilder()
                    while (!cabeceras.endsWith("\r\n\r\n")) {
                        val b = entrada.read()
                        if (b < 0) break
                        cabeceras.append(b.toChar())
                    }
                    val largo = Regex("(?i)content-length: (\\d+)").find(cabeceras)?.groupValues?.get(1)?.toInt() ?: 0
                    val cuerpo = ByteArray(largo)
                    var leidos = 0
                    while (leidos < largo) {
                        val n = entrada.read(cuerpo, leidos, largo - leidos)
                        if (n < 0) break
                        leidos += n
                    }
                    pedido = cabeceras.toString() + String(cuerpo, Charsets.UTF_8)
                    conexion.getOutputStream().apply { write(respuesta.toByteArray(Charsets.UTF_8)); flush() }
                }
            }
        }

        override fun close() {
            socket.close()
            hilo.join(2_000)
        }
    }

    private fun respuesta(estado: String, cuerpo: String = "", extra: String = ""): String =
        "HTTP/1.1 $estado\r\nContent-Type: application/json\r\nContent-Length: ${cuerpo.toByteArray().size}\r\n" +
            extra + "Connection: close\r\n\r\n" + cuerpo

    private fun enviar(respuesta: String, cuerpo: String = "{}"): Pair<ResultadoEnvio, String> =
        ServidorFalso(respuesta).use { servidor ->
            val resultado = ClienteWebhook(2_000, 2_000).enviarBloqueante(servidor.url, cuerpo)
            resultado to servidor.pedido
        }

    @Test fun manda_un_post_con_el_json_en_utf8() {
        val json = "{\"content\":\"🎲 **Éxito · 19** — 1d20 + 5\"}"
        val (resultado, pedido) = enviar(respuesta("204 No Content"), json)
        assertEquals(ResultadoEnvio.Enviado, resultado)
        assertTrue(pedido, pedido.startsWith("POST /api/webhooks/1/abc HTTP/1.1"))
        assertTrue(pedido, pedido.contains("Content-Type: application/json; charset=utf-8", ignoreCase = true))
        assertTrue("el cuerpo llega entero, con acentos y emoji", pedido.endsWith(json))
    }

    @Test fun un_429_devuelve_la_espera_que_pide_discord() {
        val (resultado, _) = enviar(respuesta("429 Too Many Requests", """{"message":"You are being rate limited.","retry_after":0.5,"global":false}"""))
        assertEquals(ResultadoEnvio.Reintentar(500), resultado)
    }

    @Test fun un_429_sin_cuerpo_usa_la_cabecera() {
        val (resultado, _) = enviar(respuesta("429 Too Many Requests", extra = "Retry-After: 3\r\n"))
        assertEquals(ResultadoEnvio.Reintentar(3_000), resultado)
    }

    @Test fun un_404_es_un_webhook_que_ya_no_existe() {
        val (resultado, _) = enviar(respuesta("404 Not Found", """{"message":"Unknown Webhook","code":10015}"""))
        assertEquals(ResultadoEnvio.WebhookInvalido, resultado)
    }

    @Test fun un_error_del_servidor_se_reintenta() {
        assertEquals(ResultadoEnvio.Reintentar(), enviar(respuesta("502 Bad Gateway")).first)
    }

    @Test fun un_400_descarta_el_mensaje() {
        assertEquals(ResultadoEnvio.Rechazado(400), enviar(respuesta("400 Bad Request", """{"content":["Must be 2000 or fewer in length."]}""")).first)
    }

    @Test fun sin_nadie_del_otro_lado_la_tirada_queda_para_despues() {
        val puertoLibre = ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { it.localPort }
        val resultado = ClienteWebhook(1_000, 1_000).enviarBloqueante("http://127.0.0.1:$puertoLibre/api/webhooks/1/abc", "{}")
        assertEquals(ResultadoEnvio.Reintentar(), resultado)
    }
}
