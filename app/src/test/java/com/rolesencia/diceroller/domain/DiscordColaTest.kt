package com.rolesencia.diceroller.domain

import com.rolesencia.diceroller.domain.discord.ColaDiscord
import com.rolesencia.diceroller.domain.discord.PendienteDiscord
import com.rolesencia.diceroller.domain.discord.PoliticaReintento
import com.rolesencia.diceroller.domain.discord.RespuestaDiscord
import com.rolesencia.diceroller.domain.discord.ResultadoEnvio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordColaTest {

    private fun tirada(instante: Long) = PendienteDiscord(instante, "{\"content\":\"tirada $instante\"}")

    @Test fun la_cola_conserva_el_orden_de_las_tiradas() {
        var cola = emptyList<PendienteDiscord>()
        (1L..3L).forEach { cola = ColaDiscord.agregar(cola, tirada(it)).cola }
        assertEquals(listOf(1L, 2L, 3L), cola.map { it.instante })
    }

    @Test fun al_pasar_el_tope_se_descartan_las_mas_viejas() {
        var cola = emptyList<PendienteDiscord>()
        (1L..ColaDiscord.MAXIMO).forEach { cola = ColaDiscord.agregar(cola, tirada(it)).cola }
        val corte = ColaDiscord.agregar(cola, tirada(999))
        assertEquals(ColaDiscord.MAXIMO, corte.cola.size)
        assertEquals(listOf(1L), corte.descartadas.map { it.instante })
        assertEquals(999L, corte.cola.last().instante)
    }

    @Test fun las_tiradas_caducan_a_los_diez_minutos() {
        val ahora = 100 * 60_000L
        val vieja = tirada(ahora - ColaDiscord.CADUCIDAD_MS - 1)
        val justa = tirada(ahora - ColaDiscord.CADUCIDAD_MS)
        val nueva = tirada(ahora - 1_000)
        val corte = ColaDiscord.vigentes(listOf(vieja, justa, nueva), ahora)
        assertEquals(listOf(justa, nueva), corte.cola)
        assertEquals(listOf(vieja), corte.descartadas)
    }

    @Test fun la_cola_ida_y_vuelta_aunque_el_json_tenga_barras() {
        val cola = listOf(tirada(1), PendienteDiscord(2, "{\"content\":\"a|b \\n c\"}"))
        assertEquals(cola, ColaDiscord.decodificar(ColaDiscord.codificar(cola)))
        assertTrue(ColaDiscord.decodificar("").isEmpty())
        assertTrue(ColaDiscord.decodificar("basura\n|sin instante\n7|").isEmpty())
    }

    @Test fun cada_respuesta_de_discord_tiene_su_accion() {
        assertEquals(ResultadoEnvio.Enviado, RespuestaDiscord.interpretar(204))
        assertEquals(ResultadoEnvio.Enviado, RespuestaDiscord.interpretar(200))
        assertEquals(ResultadoEnvio.WebhookInvalido, RespuestaDiscord.interpretar(404))
        assertEquals(ResultadoEnvio.WebhookInvalido, RespuestaDiscord.interpretar(401))
        assertEquals(ResultadoEnvio.WebhookInvalido, RespuestaDiscord.interpretar(403))
        assertEquals(ResultadoEnvio.Reintentar(), RespuestaDiscord.interpretar(500))
        assertEquals(ResultadoEnvio.Reintentar(), RespuestaDiscord.interpretar(503))
        assertEquals(ResultadoEnvio.Rechazado(400), RespuestaDiscord.interpretar(400))
    }

    @Test fun ante_un_429_espera_lo_que_pide_discord() {
        val cuerpo = """{"message": "You are being rate limited.", "retry_after": 1.337, "global": false}"""
        assertEquals(ResultadoEnvio.Reintentar(1337), RespuestaDiscord.interpretar(429, "2", cuerpo))
        // Sin cuerpo, la cabecera (en segundos); sin ninguno, la espera propia.
        assertEquals(ResultadoEnvio.Reintentar(2000), RespuestaDiscord.interpretar(429, "2", null))
        assertEquals(ResultadoEnvio.Reintentar(), RespuestaDiscord.interpretar(429))
        // Nunca menos de un cuarto de segundo, aunque Discord diga 0.
        assertEquals(ResultadoEnvio.Reintentar(250), RespuestaDiscord.interpretar(429, null, """{"retry_after": 0.0}"""))
    }

    @Test fun la_espera_crece_y_tiene_tope() {
        val politica = PoliticaReintento()
        assertEquals(listOf(2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L), (1..6).map { politica.espera(it) })
        assertEquals(30_000L, politica.espera(1_000))
    }
}
