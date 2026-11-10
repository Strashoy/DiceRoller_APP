package com.rolesencia.diceroller.data

import com.rolesencia.diceroller.data.discord.AlmacenCola
import com.rolesencia.diceroller.data.discord.DesenlaceDiscord
import com.rolesencia.diceroller.data.discord.FaseDiscord
import com.rolesencia.diceroller.data.discord.PublicadorDiscord
import com.rolesencia.diceroller.domain.discord.ColaDiscord
import com.rolesencia.diceroller.domain.discord.ConfigDiscord
import com.rolesencia.diceroller.domain.discord.MensajeTirada
import com.rolesencia.diceroller.domain.discord.PendienteDiscord
import com.rolesencia.diceroller.domain.discord.PoliticaReintento
import com.rolesencia.diceroller.domain.discord.ResultadoEnvio
import com.rolesencia.diceroller.domain.reglas.NivelResultado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

/** El publicador con un Discord de mentira: sin red y con esperas de milisegundos. */
class PublicadorDiscordTest {

    private val webhook = "https://discord.com/api/webhooks/1/abc"
    private val activa = ConfigDiscord(habilitado = true, webhook = webhook, jugador = "Thorin")
    private val alcance = CoroutineScope(Dispatchers.Default + Job())

    private class AlmacenEnMemoria(inicial: List<PendienteDiscord> = emptyList()) : AlmacenCola {
        @Volatile var cola: List<PendienteDiscord> = inicial
        override suspend fun leerCola() = cola
        override suspend fun guardarCola(cola: List<PendienteDiscord>) { this.cola = cola }
    }

    /** Lo que se "envió", en orden: el texto de cada cuerpo. */
    private val enviados: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun publicador(
        almacen: AlmacenCola = AlmacenEnMemoria(),
        ahora: () -> Long = { 0L },
        respuesta: (intento: Int, cuerpo: String) -> ResultadoEnvio = { _, _ -> ResultadoEnvio.Enviado }
    ): PublicadorDiscord {
        var intentos = 0
        return PublicadorDiscord(
            almacen = almacen,
            enviar = { _, cuerpo ->
                intentos++
                respuesta(intentos, cuerpo).also { if (it == ResultadoEnvio.Enviado) enviados += cuerpo }
            },
            alcance = alcance,
            reintento = PoliticaReintento(baseMs = 1, topeMs = 5),
            ahora = ahora
        )
    }

    private fun tirada(instante: Long, titulo: String = "Tirada $instante") = MensajeTirada(
        instante = instante, titulo = titulo, detalle = "", notacion = "1d20", valores = "7",
        sistema = "Suma libre", nivel = NivelResultado.NEUTRO
    )

    private fun desenlaces(publicador: PublicadorDiscord, cantidad: Int): List<DesenlaceDiscord> =
        runBlocking { withTimeout(5_000) { publicador.desenlaces.take(cantidad).toList() } }

    private fun esperarFase(publicador: PublicadorDiscord, fase: FaseDiscord) =
        runBlocking { withTimeout(5_000) { publicador.estado.first { it.fase == fase } } }

    @After fun cerrar() = alcance.cancel()

    @Test fun publica_en_orden_y_vacia_la_cola() {
        val almacen = AlmacenEnMemoria()
        val p = publicador(almacen)
        p.configurar(activa)
        (1L..3L).forEach { p.publicar(tirada(it)) }
        assertEquals(listOf(1L, 2L, 3L).map { DesenlaceDiscord(it, true) }, desenlaces(p, 3))
        assertEquals(3, enviados.size)
        assertTrue(enviados[0].contains("Tirada 1") && enviados[2].contains("Tirada 3"))
        assertTrue("usa el nombre configurado", enviados[0].contains("\"username\":\"Thorin\""))
        assertTrue(almacen.cola.isEmpty())
    }

    @Test fun sin_red_reintenta_la_misma_tirada_antes_de_pasar_a_la_siguiente() {
        val p = publicador { intento, _ -> if (intento <= 3) ResultadoEnvio.Reintentar() else ResultadoEnvio.Enviado }
        p.configurar(activa)
        p.publicar(tirada(1))
        p.publicar(tirada(2))
        assertEquals(listOf(DesenlaceDiscord(1, true), DesenlaceDiscord(2, true)), desenlaces(p, 2))
        assertTrue(enviados[0].contains("Tirada 1") && enviados[1].contains("Tirada 2"))
    }

    @Test fun mientras_espera_la_tirada_queda_guardada() {
        val almacen = AlmacenEnMemoria()
        val p = publicador(almacen) { _, _ -> ResultadoEnvio.Reintentar(60_000) }   // Discord pide un minuto
        p.configurar(activa)
        p.publicar(tirada(1))
        val estado = esperarFase(p, FaseDiscord.REINTENTANDO)
        assertEquals(setOf(1L), estado.pendientes)
        assertEquals(listOf(1L), almacen.cola.map { it.instante })
        assertTrue(enviados.isEmpty())
    }

    @Test fun respeta_la_espera_que_pide_discord() {
        val p = publicador { intento, _ -> if (intento == 1) ResultadoEnvio.Reintentar(80) else ResultadoEnvio.Enviado }
        p.configurar(activa)
        val inicio = System.nanoTime()
        p.publicar(tirada(1))
        p.despertar()   // que vuelva la red no acorta una espera pedida por Discord
        assertEquals(listOf(DesenlaceDiscord(1, true)), desenlaces(p, 1))
        assertTrue((System.nanoTime() - inicio) / 1_000_000 >= 75)
    }

    @Test fun una_tirada_vieja_no_se_publica() {
        val ahora = ColaDiscord.CADUCIDAD_MS + 1   // la primera tirada lleva más de diez minutos esperando
        val p = publicador(ahora = { ahora }) { intento, _ -> if (intento == 1) ResultadoEnvio.Reintentar() else ResultadoEnvio.Enviado }
        p.configurar(activa)
        p.publicar(tirada(0))
        p.publicar(tirada(ahora))
        assertEquals(listOf(DesenlaceDiscord(0, false), DesenlaceDiscord(ahora, true)), desenlaces(p, 2))
        assertEquals(1, enviados.size)
    }

    @Test fun si_discord_no_reconoce_el_webhook_deja_de_insistir() {
        val almacen = AlmacenEnMemoria()
        val p = publicador(almacen) { _, _ -> ResultadoEnvio.WebhookInvalido }
        p.configurar(activa)
        p.publicar(tirada(1))
        p.publicar(tirada(2))
        assertEquals(setOf(DesenlaceDiscord(1, false), DesenlaceDiscord(2, false)), desenlaces(p, 2).toSet())
        esperarFase(p, FaseDiscord.WEBHOOK_INVALIDO)
        // Lo que se tire después tampoco sale, hasta que se configure otro webhook.
        p.publicar(tirada(3))
        assertEquals(DesenlaceDiscord(3, false), desenlaces(p, 3).last())
        assertTrue(enviados.isEmpty() && almacen.cola.isEmpty())
    }

    @Test fun un_mensaje_rechazado_no_traba_a_los_demas() {
        val p = publicador { _, cuerpo -> if (cuerpo.contains("Tirada 1")) ResultadoEnvio.Rechazado(400) else ResultadoEnvio.Enviado }
        p.configurar(activa)
        p.publicar(tirada(1))
        p.publicar(tirada(2))
        assertEquals(listOf(DesenlaceDiscord(1, false), DesenlaceDiscord(2, true)), desenlaces(p, 2))
    }

    @Test fun apagar_discord_descarta_lo_que_esperaba() {
        val p = publicador { _, _ -> ResultadoEnvio.Reintentar(60_000) }
        p.configurar(activa)
        p.publicar(tirada(1))
        esperarFase(p, FaseDiscord.REINTENTANDO)
        p.configurar(activa.copy(habilitado = false))
        assertEquals(listOf(DesenlaceDiscord(1, false)), desenlaces(p, 1))
    }

    @Test fun cambiar_de_canal_no_manda_las_tiradas_viejas_al_canal_nuevo() {
        val p = publicador { _, _ -> ResultadoEnvio.Reintentar(60_000) }
        p.configurar(activa)
        p.publicar(tirada(1))
        esperarFase(p, FaseDiscord.REINTENTANDO)
        p.configurar(activa.copy(webhook = "https://discord.com/api/webhooks/2/otro"))
        assertEquals(listOf(DesenlaceDiscord(1, false)), desenlaces(p, 1))
    }

    @Test fun lo_que_quedo_de_la_sesion_anterior_sale_al_abrir_la_app() {
        val almacen = AlmacenEnMemoria(listOf(PendienteDiscord(5, "{\"content\":\"quedó pendiente\"}")))
        val p = publicador(almacen, ahora = { 1_000 })
        p.configurar(activa)
        assertEquals(listOf(DesenlaceDiscord(5, true)), desenlaces(p, 1))
        assertEquals(listOf("{\"content\":\"quedó pendiente\"}"), enviados.toList())
    }

    @Test fun con_discord_apagado_no_sale_nada() {
        val p = publicador()
        p.configurar(ConfigDiscord())
        p.publicar(tirada(1))
        assertEquals(listOf(DesenlaceDiscord(1, false)), desenlaces(p, 1))
        assertTrue(enviados.isEmpty())
    }
}
