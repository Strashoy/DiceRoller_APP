package com.rolesencia.diceroller.domain.discord

import kotlin.math.min
import kotlin.math.roundToLong

/** Una tirada esperando para publicarse: su instante (que también la identifica) y el JSON listo. */
data class PendienteDiscord(val instante: Long, val cuerpo: String)

/** Resultado de recortar la cola: lo que sigue esperando y lo que ya no se va a enviar. */
data class Corte(val cola: List<PendienteDiscord>, val descartadas: List<PendienteDiscord>)

/**
 * Bandeja de salida: la tirada se resuelve y se guarda en el historial sin red; lo que espera a la
 * conexión es sólo su publicación.
 *
 * Una tirada que aparece en el canal una hora después confunde más de lo que ayuda, así que **caduca**
 * a los diez minutos. La cola respeta el orden de las tiradas y tiene un tope.
 */
object ColaDiscord {
    const val CADUCIDAD_MS = 10 * 60_000L
    const val MAXIMO = 50

    fun agregar(cola: List<PendienteDiscord>, nueva: PendienteDiscord): Corte {
        val todas = cola + nueva
        val sobran = (todas.size - MAXIMO).coerceAtLeast(0)
        return Corte(todas.drop(sobran), todas.take(sobran))
    }

    fun vigentes(cola: List<PendienteDiscord>, ahora: Long): Corte {
        val (viejas, vivas) = cola.partition { ahora - it.instante > CADUCIDAD_MS }
        return Corte(vivas, viejas)
    }

    // Una por línea: "instante|json". El JSON nunca trae saltos de línea sin escapar.
    private val SALTO = Char(10).toString()

    fun codificar(cola: List<PendienteDiscord>): String = cola.joinToString(SALTO) { "${it.instante}|${it.cuerpo}" }

    fun decodificar(texto: String): List<PendienteDiscord> = texto.lines().mapNotNull { linea ->
        val partes = linea.split("|", limit = 2)
        val instante = partes.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        val cuerpo = partes.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        PendienteDiscord(instante, cuerpo)
    }
}

/** Qué hacer después de un intento de envío. */
sealed interface ResultadoEnvio {
    data object Enviado : ResultadoEnvio

    /** Probar de nuevo. [esperaMs] es lo que pidió Discord (429); null = sin red o error del servidor. */
    data class Reintentar(val esperaMs: Long? = null) : ResultadoEnvio

    /** El webhook no existe más o no acepta el pedido (404, 401, 403): Discord pide no insistir. */
    data object WebhookInvalido : ResultadoEnvio

    /** Discord rechazó este mensaje en particular: se descarta y se sigue con el próximo. */
    data class Rechazado(val codigo: Int) : ResultadoEnvio
}

object RespuestaDiscord {
    private val RETRY_AFTER = Regex(""""retry_after"\s*:\s*([0-9]+(?:\.[0-9]+)?)""")

    /**
     * Traduce la respuesta HTTP. Los límites de Discord no se escriben en el código (su documentación
     * lo pide): ante un 429 se espera lo que diga `retry_after`, en segundos.
     */
    fun interpretar(codigo: Int, retryAfter: String? = null, cuerpo: String? = null): ResultadoEnvio = when (codigo) {
        in 200..299 -> ResultadoEnvio.Enviado
        429 -> {
            val segundos = cuerpo?.let { RETRY_AFTER.find(it)?.groupValues?.get(1)?.toDoubleOrNull() }
                ?: retryAfter?.trim()?.toDoubleOrNull()
            ResultadoEnvio.Reintentar(segundos?.let { (it * 1000).roundToLong().coerceAtLeast(250) })
        }
        401, 403, 404 -> ResultadoEnvio.WebhookInvalido
        in 500..599 -> ResultadoEnvio.Reintentar()
        else -> ResultadoEnvio.Rechazado(codigo)
    }
}

/** Espera creciente entre reintentos cuando no hay red: 2 s, 4 s, 8 s… hasta 30 s. */
data class PoliticaReintento(val baseMs: Long = 2_000, val topeMs: Long = 30_000) {
    fun espera(intento: Int): Long {
        val paso = (intento - 1).coerceIn(0, 20)
        return min(baseMs shl paso, topeMs)
    }
}
