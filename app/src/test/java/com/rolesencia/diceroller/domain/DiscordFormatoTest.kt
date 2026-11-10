package com.rolesencia.diceroller.domain

import com.rolesencia.diceroller.domain.discord.ConfigDiscord
import com.rolesencia.diceroller.domain.discord.FormatoDiscord
import com.rolesencia.diceroller.domain.discord.MensajeTirada
import com.rolesencia.diceroller.domain.discord.WebhookDiscord
import com.rolesencia.diceroller.domain.reglas.NivelResultado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordFormatoTest {

    private val dado = "🎲"

    private val ataque = MensajeTirada(
        instante = 0, titulo = "Éxito", detalle = "14 +5 = 19 vs CD 15",
        notacion = "1d20 + 5", valores = "14", sistema = "d20 contra CD (D&D 5e)",
        nivel = NivelResultado.EXITO, total = 19
    )

    // --- El webhook ---

    @Test fun acepta_las_formas_que_entrega_discord() {
        listOf(
            "https://discord.com/api/webhooks/123456789012345678/abc-DEF_123",
            "https://discordapp.com/api/webhooks/123/abc",
            "https://canary.discord.com/api/webhooks/123/abc",
            "https://ptb.discord.com/api/v10/webhooks/123/abc",
            "https://discord.com/api/webhooks/123/abc?thread_id=456",
            "  https://discord.com/api/webhooks/123/abc  "
        ).forEach { assertTrue(it, WebhookDiscord.esValido(it)) }
    }

    @Test fun rechaza_lo_que_no_es_un_webhook_de_discord() {
        listOf(
            "",
            "http://discord.com/api/webhooks/123/abc",                 // sin HTTPS
            "https://discord.com.ejemplo.com/api/webhooks/123/abc",    // otro dominio disfrazado
            "https://ejemplo.com/api/webhooks/123/abc",
            "https://discord.com/api/webhooks/123",                    // falta el token
            "https://discord.com/api/webhooks/abc/def",                // el id no es un número
            "https://discord.com/channels/123/456",
            "https://discord.com/api/webhooks/123/abc y algo más"
        ).forEach { assertFalse(it, WebhookDiscord.esValido(it)) }
    }

    @Test fun enmascarado_no_muestra_el_token() {
        val visible = WebhookDiscord.enmascarar("https://discord.com/api/webhooks/123456789012345678/tokenSecreto")
        assertFalse(visible.contains("tokenSecreto"))
        assertFalse(visible.contains("123456789012345678"))
        assertEquals("", WebhookDiscord.enmascarar("cualquier cosa"))
    }

    @Test fun sin_webhook_valido_no_esta_activo_aunque_este_encendido() {
        assertFalse(ConfigDiscord(habilitado = true, webhook = "hola").activo)
        assertFalse(ConfigDiscord(habilitado = false, webhook = "https://discord.com/api/webhooks/1/a").activo)
        assertTrue(ConfigDiscord(habilitado = true, webhook = "https://discord.com/api/webhooks/1/a").activo)
    }

    @Test fun nombres_que_discord_rechaza() {
        assertTrue(WebhookDiscord.nombreValido("Thorin Escudo de Roble"))
        assertTrue(WebhookDiscord.nombreValido(""))
        assertFalse(WebhookDiscord.nombreValido("Bot de Discord"))
        assertFalse(WebhookDiscord.nombreValido("clyde"))
        assertFalse(WebhookDiscord.nombreValido("x".repeat(81)))
    }

    // --- El mensaje ---

    @Test fun una_tirada_de_d20_completa() {
        assertEquals(
            "{\"content\":\"$dado **Éxito · 19** — 1d20 + 5 → 14\",\"username\":\"Thorin\"," +
                "\"allowed_mentions\":{\"parse\":[]}," +
                "\"embeds\":[{\"description\":\"14 +5 = 19 vs CD 15\",\"color\":3526285," +
                "\"footer\":{\"text\":\"d20 contra CD (D&D 5e)\"},\"timestamp\":\"1970-01-01T00:00:00.000Z\"}]}",
            FormatoDiscord.cuerpo(ataque, "Thorin")
        )
    }

    @Test fun la_cabecera_no_repite_el_numero() {
        fun cabecera(titulo: String, total: Int?) = FormatoDiscord.cabecera(ataque.copy(titulo = titulo, total = total))
        assertEquals("Éxito · 19", cabecera("Éxito", 19))
        assertEquals("54", cabecera("54", 54))                    // suma libre: el título ya es el total
        assertEquals("3 éxitos", cabecera("3 éxitos", 3))
        assertEquals("¡20 natural! · 25", cabecera("¡20 natural!", 25))
        assertEquals("Fallo", cabecera("Fallo", null))
    }

    @Test fun con_un_solo_dado_el_valor_no_se_repite() {
        val uno = ataque.copy(titulo = "4", total = 4, notacion = "1d6", valores = "4", detalle = "4")
        assertTrue(FormatoDiscord.cuerpo(uno).startsWith("{\"content\":\"$dado **4** — 1d6\","))
    }

    @Test fun el_nombre_de_la_tirada_guardada_va_adelante_y_sin_formato() {
        val cuerpo = FormatoDiscord.cuerpo(ataque.copy(etiqueta = "**Espada** [larga]"))
        assertTrue(cuerpo, cuerpo.contains("""$dado \\*\\*Espada\\*\\* \\[larga\\]: **Éxito · 19**"""))
    }

    @Test fun nunca_menciona_a_nadie() {
        val cuerpo = FormatoDiscord.cuerpo(ataque.copy(etiqueta = "@everyone"), "@here")
        assertTrue(cuerpo.contains("\"allowed_mentions\":{\"parse\":[]}"))
    }

    @Test fun sin_nombre_o_con_nombre_prohibido_usa_el_del_webhook() {
        assertFalse(FormatoDiscord.cuerpo(ataque, "  ").contains("\"username\""))
        assertFalse(FormatoDiscord.cuerpo(ataque, "Discord oficial").contains("\"username\""))
    }

    @Test fun comillas_barras_y_saltos_quedan_escapados() {
        assertEquals("\"dijo \\\"hola\\\"\\ny\\\\o\\u0001\"", FormatoDiscord.textoJson("dijo \"hola\"\ny\\o\u0001"))
        val cuerpo = FormatoDiscord.cuerpo(ataque.copy(detalle = "una\nlínea \"rara\""))
        assertFalse("el JSON va en una sola línea", cuerpo.contains('\n'))
        assertTrue(cuerpo.contains("\"description\":\"una\\nlínea \\\"rara\\\"\""))
    }

    @Test fun el_contenido_respeta_el_limite_de_discord() {
        val largo = ataque.copy(valores = "20 · ".repeat(600))
        val contenido = FormatoDiscord.cuerpo(largo).substringAfter("{\"content\":\"").substringBefore("\",\"allowed_mentions\"")
        assertEquals(2000, contenido.length)
        assertTrue(contenido.endsWith("…"))
    }

    @Test fun cada_nivel_tiene_su_color() {
        assertEquals(NivelResultado.entries.size, NivelResultado.entries.map { FormatoDiscord.color(it) }.toSet().size)
    }

    @Test fun la_hora_va_en_iso_y_utc() {
        assertEquals("1970-01-02T01:02:03.004Z", FormatoDiscord.instanteIso(86_400_000L + 3_723_004L))
    }

    @Test fun el_mensaje_de_prueba_aclara_que_no_es_una_tirada() {
        val cuerpo = FormatoDiscord.cuerpoPrueba("Thorin", 0)
        assertTrue(cuerpo.contains("\"username\":\"Thorin\""))
        assertTrue(cuerpo.contains("no es una tirada"))
    }
}
