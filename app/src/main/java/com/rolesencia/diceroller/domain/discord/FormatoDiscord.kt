package com.rolesencia.diceroller.domain.discord

import com.rolesencia.diceroller.domain.reglas.NivelResultado
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/** Lo que se publica de una tirada ya resuelta. */
data class MensajeTirada(
    /** Momento de la tirada: Discord lo muestra en la hora local de cada lector. */
    val instante: Long,
    val titulo: String,
    val detalle: String,
    /** "1d20 + 5" */
    val notacion: String,
    /** "14 · 3" */
    val valores: String,
    val sistema: String,
    val nivel: NivelResultado,
    /** Suma, cantidad de éxitos o valor clave, si el sistema lo da. */
    val total: Int? = null,
    /** Nombre de la tirada guardada con la que se lanzó ("Espada larga"), si la hubo. */
    val etiqueta: String? = null
)

/**
 * Arma el cuerpo JSON que espera el webhook de Discord.
 *
 * - `content` lleva **todo lo esencial en una línea** (resultado, notación y valores): es lo que
 *   aparece en la notificación del teléfono y lo único que ve quien tiene las tarjetas desactivadas.
 * - Un `embed` agrega el color del veredicto, el detalle del sistema y la hora real de la tirada.
 * - `allowed_mentions` vacío: una tirada guardada que se llame "@everyone" no menciona a nadie.
 *
 * El JSON se escribe a mano (como los códecs de las preferencias): son seis campos y así la app no
 * suma ninguna biblioteca.
 */
object FormatoDiscord {
    // Límites de Discord para cada campo.
    private const val MAX_CONTENIDO = 2000
    private const val MAX_DESCRIPCION = 4096
    private const val MAX_PIE = 2048

    private const val DADO = "🎲"   // 🎲

    fun cuerpo(mensaje: MensajeTirada, jugador: String = ""): String {
        val etiqueta = mensaje.etiqueta?.trim().orEmpty()
        val contenido = buildString {
            append(DADO).append(' ')
            if (etiqueta.isNotEmpty()) append(escaparMarkdown(etiqueta)).append(": ")
            append("**").append(cabecera(mensaje)).append("**")
            append(" — ").append(mensaje.notacion)
            // Con un solo dado y sin modificador el valor ya es la cabecera: no se repite.
            if (mensaje.valores.isNotBlank() && mensaje.valores != mensaje.titulo) append(" → ").append(mensaje.valores)
        }
        val descripcion = mensaje.detalle.ifBlank { mensaje.valores }
        return armar(contenido, jugador, descripcion, color(mensaje.nivel), mensaje.sistema, mensaje.instante)
    }

    /** Mensaje del botón "Enviar prueba": confirma que el webhook funciona sin tirar nada. */
    fun cuerpoPrueba(jugador: String, instante: Long): String = armar(
        contenido = "$DADO **Dice Roller conectado.** Las tiradas van a aparecer en este canal.",
        jugador = jugador,
        descripcion = "Mensaje de prueba: no es una tirada.",
        color = COLOR_NEUTRO,
        pie = "Dice Roller",
        instante = instante
    )

    /** "Éxito · 19": el veredicto y, si el título no lo dice, el número. En suma libre, sólo "54". */
    internal fun cabecera(mensaje: MensajeTirada): String {
        val total = mensaje.total ?: return mensaje.titulo
        val buscado = abs(total).toString()
        val yaEsta = Regex("""\d+""").findAll(mensaje.titulo).any { it.value == buscado }
        return if (yaEsta) mensaje.titulo else "${mensaje.titulo} · $total"
    }

    private const val COLOR_NEUTRO = 0x00BFFF

    /** Los mismos colores que el marcador de la app; lo neutro va con el celeste de LANZAR. */
    internal fun color(nivel: NivelResultado): Int = when (nivel) {
        NivelResultado.FALLO_CRITICO -> 0x9C27B0
        NivelResultado.FALLO -> 0xC20114
        NivelResultado.EXITO_PARCIAL -> 0xFFB020
        NivelResultado.EXITO -> 0x35CE8D
        NivelResultado.EXITO_CRITICO -> 0xFFD700
        NivelResultado.NEUTRO -> COLOR_NEUTRO
    }

    private fun armar(contenido: String, jugador: String, descripcion: String, color: Int, pie: String, instante: Long): String =
        buildString {
            append("{\"content\":").append(textoJson(recortar(contenido, MAX_CONTENIDO)))
            val nombre = jugador.trim()
            if (nombre.isNotEmpty() && WebhookDiscord.nombreValido(nombre)) append(",\"username\":").append(textoJson(nombre))
            append(",\"allowed_mentions\":{\"parse\":[]}")
            append(",\"embeds\":[{")
            if (descripcion.isNotBlank()) append("\"description\":").append(textoJson(recortar(descripcion, MAX_DESCRIPCION))).append(',')
            append("\"color\":").append(color)
            if (pie.isNotBlank()) append(",\"footer\":{\"text\":").append(textoJson(recortar(pie, MAX_PIE))).append('}')
            append(",\"timestamp\":").append(textoJson(instanteIso(instante)))
            append("}]}")
        }

    private fun recortar(texto: String, maximo: Int) = if (texto.length <= maximo) texto else texto.take(maximo - 1) + "…"

    /** Un texto como cadena JSON: comillas, barras y caracteres de control escapados. */
    internal fun textoJson(texto: String): String = buildString(texto.length + 2) {
        append('"')
        for (c in texto) when {
            c == '"' -> append("\\\"")
            c == '\\' -> append("\\\\")
            c == '\n' -> append("\\n")
            c == '\r' -> append("\\r")
            c == '\t' -> append("\\t")
            c < ' ' -> append("\\u").append(c.code.toString(16).padStart(4, '0'))
            else -> append(c)
        }
        append('"')
    }

    /** Que un nombre puesto por el usuario no se convierta en negrita, enlace o cita en el canal. */
    internal fun escaparMarkdown(texto: String): String = buildString(texto.length) {
        for (c in texto) {
            if (c in "\\*_~`|>[]") append('\\')
            append(c)
        }
    }

    /** ISO 8601 en UTC, como pide Discord. `SimpleDateFormat` existe en todas las versiones de Android. */
    internal fun instanteIso(instante: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(instante))
}
