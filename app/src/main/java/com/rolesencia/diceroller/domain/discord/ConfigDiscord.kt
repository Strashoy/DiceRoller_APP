package com.rolesencia.diceroller.domain.discord

/**
 * Publicar las tiradas en un canal de Discord por medio de un **webhook**: una URL que crea quien
 * administra el canal (Editar canal → Integraciones → Webhooks) y que acepta mensajes por HTTP.
 * No hace falta un bot, una cuenta ni un servidor propio: la app sólo necesita esa URL.
 *
 * Todo este paquete es Kotlin puro y se testea sin Android.
 */
data class ConfigDiscord(
    val habilitado: Boolean = false,
    /** URL del webhook del canal. Es un secreto: quien la tenga puede escribir en ese canal. */
    val webhook: String = "",
    /** Cómo figura quien tira (personaje o jugador). Vacío: el nombre que tenga el webhook. */
    val jugador: String = ""
) {
    /** Sólo se publica con el interruptor encendido y un webhook con forma válida. */
    val activo: Boolean get() = habilitado && WebhookDiscord.esValido(webhook)
}

object WebhookDiscord {
    const val MAX_NOMBRE = 80

    // Sólo HTTPS y sólo los dominios de Discord: un enlace pegado por error (o con mala intención)
    // no puede hacer que las tiradas se manden a otro lado. Se acepta "?thread_id=…" para hilos.
    private val FORMA = Regex(
        """https://(?:(?:canary|ptb)\.)?discord(?:app)?\.com/api/(?:v\d+/)?webhooks/(\d+)/([\w-]+)(\?[\w=&%.-]*)?"""
    )

    fun esValido(url: String): Boolean = FORMA.matches(url.trim())

    /** Para mostrar en pantalla sin revelar el secreto: "discord.com/…/webhooks/1234…/••••". */
    fun enmascarar(url: String): String {
        val id = FORMA.matchEntire(url.trim())?.groupValues?.get(1) ?: return ""
        return "discord.com/…/webhooks/${id.take(4)}…/••••"
    }

    /** Discord rechaza los nombres que contienen "discord" o "clyde", y los de más de 80 letras. */
    fun nombreValido(nombre: String): Boolean {
        val n = nombre.trim().lowercase()
        return n.length <= MAX_NOMBRE && "discord" !in n && "clyde" !in n
    }
}

/** Cómo terminó la publicación de una tirada; queda guardado en su entrada del historial. */
enum class EnvioDiscord { ENVIADA, NO_ENVIADA }
