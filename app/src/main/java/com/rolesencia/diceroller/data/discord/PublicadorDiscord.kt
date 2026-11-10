package com.rolesencia.diceroller.data.discord

import com.rolesencia.diceroller.domain.discord.ColaDiscord
import com.rolesencia.diceroller.domain.discord.ConfigDiscord
import com.rolesencia.diceroller.domain.discord.FormatoDiscord
import com.rolesencia.diceroller.domain.discord.MensajeTirada
import com.rolesencia.diceroller.domain.discord.PendienteDiscord
import com.rolesencia.diceroller.domain.discord.PoliticaReintento
import com.rolesencia.diceroller.domain.discord.ResultadoEnvio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** En qué anda el publicador, para el indicador de la pantalla. */
enum class FaseDiscord { REPOSO, ENVIANDO, REINTENTANDO, WEBHOOK_INVALIDO }

data class EstadoDiscord(
    /** Instantes de las tiradas que todavía esperan salir. */
    val pendientes: Set<Long> = emptySet(),
    val fase: FaseDiscord = FaseDiscord.REPOSO
)

/** Cómo terminó una tirada publicada: enviada, o descartada (caducó, la rechazaron, se apagó Discord). */
data class DesenlaceDiscord(val instante: Long, val enviada: Boolean)

/**
 * Vacía la bandeja de salida hacia Discord, de a un mensaje y en orden.
 *
 * Todo pasa en **una sola corrutina** que atiende comandos (configurar, publicar, despertar) y, entre
 * comando y comando, intenta enviar la primera tirada de la cola. Al no haber dos corrutinas tocando
 * la cola no hacen falta candados, y nunca hay dos envíos a la vez (que es lo que evita desordenar el
 * canal y pasarse de los límites de Discord).
 *
 * [enviar], [reintento] y [ahora] se inyectan para probarlo sin red ni esperas reales.
 */
class PublicadorDiscord(
    private val almacen: AlmacenCola,
    private val enviar: suspend (url: String, cuerpo: String) -> ResultadoEnvio,
    alcance: CoroutineScope,
    private val reintento: PoliticaReintento = PoliticaReintento(),
    private val ahora: () -> Long = System::currentTimeMillis
) {
    private sealed interface Comando {
        data class Configurar(val config: ConfigDiscord) : Comando
        data class Publicar(val mensaje: MensajeTirada) : Comando
        data object Despertar : Comando
    }

    private val comandos = Channel<Comando>(Channel.UNLIMITED)

    private val _estado = MutableStateFlow(EstadoDiscord())
    val estado: StateFlow<EstadoDiscord> = _estado.asStateFlow()

    // Con repetición: quien empieza a escuchar tarde (la pantalla recién abierta) se entera igual.
    private val _desenlaces = MutableSharedFlow<DesenlaceDiscord>(
        replay = ColaDiscord.MAXIMO, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val desenlaces: SharedFlow<DesenlaceDiscord> = _desenlaces.asSharedFlow()

    // Estado de la corrutina del bucle: sólo ella lo lee y lo escribe.
    private var config = ConfigDiscord()
    private var configurado = false
    private var cola: List<PendienteDiscord> = emptyList()
    private var fase = FaseDiscord.REPOSO

    init {
        alcance.launch { bucle() }
    }

    /** Avisa con qué webhook publicar. Si cambia o se apaga, lo que esperaba ya no se envía. */
    fun configurar(config: ConfigDiscord) {
        comandos.trySend(Comando.Configurar(config))
    }

    fun publicar(mensaje: MensajeTirada) {
        comandos.trySend(Comando.Publicar(mensaje))
    }

    /** Volvió la red (o se abrió la app): no esperar el próximo reintento. */
    fun despertar() {
        comandos.trySend(Comando.Despertar)
    }

    /** Botón "Enviar prueba": un mensaje directo, fuera de la cola. */
    suspend fun probar(config: ConfigDiscord): ResultadoEnvio =
        enviar(config.webhook.trim(), FormatoDiscord.cuerpoPrueba(config.jugador, ahora()))

    private suspend fun bucle() {
        cola = almacen.leerCola()
        publicarEstado()
        var intento = 0
        var esperaMs = 0L             // cuánto falta para el próximo intento
        var esperaFirme = false       // la pidió Discord (429): un "despertar" no la acorta
        while (true) {
            val hayTrabajo = config.activo && cola.isNotEmpty()
            val comando = when {
                !hayTrabajo -> comandos.receive()
                esperaMs > 0 -> {
                    val inicio = ahora()
                    val recibido = withTimeoutOrNull(esperaMs) { comandos.receive() }
                    // Si llegó un comando antes de tiempo, queda por esperar el resto.
                    esperaMs = if (recibido == null) 0 else (esperaMs - (ahora() - inicio)).coerceAtLeast(0)
                    recibido
                }
                else -> comandos.tryReceive().getOrNull()
            }
            if (comando != null) {
                when (comando) {
                    is Comando.Configurar -> {
                        // La primera configuración (al abrir la app) no es un cambio de canal: lo que
                        // quedó en la cola de la sesión anterior sigue siendo para ese webhook.
                        val cambioDestino = configurado && comando.config.webhook.trim() != config.webhook.trim()
                        config = comando.config
                        configurado = true
                        if (!config.activo || cambioDestino) descartar(cola)
                        fase = FaseDiscord.REPOSO
                        intento = 0
                        esperaMs = 0
                        esperaFirme = false
                    }
                    is Comando.Publicar -> if (config.activo) {
                        val nueva = PendienteDiscord(comando.mensaje.instante, FormatoDiscord.cuerpo(comando.mensaje, config.jugador))
                        val corte = ColaDiscord.agregar(cola, nueva)
                        cola = corte.cola
                        corte.descartadas.forEach { _desenlaces.tryEmit(DesenlaceDiscord(it.instante, false)) }
                        almacen.guardarCola(cola)
                    } else _desenlaces.tryEmit(DesenlaceDiscord(comando.mensaje.instante, false))
                    Comando.Despertar -> if (!esperaFirme) esperaMs = 0
                }
                publicarEstado()
                continue
            }

            // Sin comandos pendientes y sin espera: sale la primera tirada que siga vigente.
            val corte = ColaDiscord.vigentes(cola, ahora())
            if (corte.descartadas.isNotEmpty()) {
                cola = corte.cola
                corte.descartadas.forEach { _desenlaces.tryEmit(DesenlaceDiscord(it.instante, false)) }
                almacen.guardarCola(cola)
                publicarEstado()
            }
            val primera = cola.firstOrNull()
            if (primera == null) {
                fase = FaseDiscord.REPOSO
                publicarEstado()
                continue
            }
            if (fase != FaseDiscord.REINTENTANDO) fase = FaseDiscord.ENVIANDO
            publicarEstado()
            when (val resultado = enviar(config.webhook.trim(), primera.cuerpo)) {
                ResultadoEnvio.Enviado -> {
                    quitar(primera, enviada = true)
                    fase = FaseDiscord.REPOSO
                    intento = 0
                    esperaFirme = false
                }
                is ResultadoEnvio.Reintentar -> {
                    intento++
                    esperaFirme = resultado.esperaMs != null
                    esperaMs = resultado.esperaMs ?: reintento.espera(intento)
                    fase = FaseDiscord.REINTENTANDO
                }
                is ResultadoEnvio.Rechazado -> {
                    quitar(primera, enviada = false)
                    fase = FaseDiscord.REPOSO
                    intento = 0
                    esperaFirme = false
                }
                ResultadoEnvio.WebhookInvalido -> {
                    // El webhook fue borrado: se deja de insistir hasta que el usuario pegue otro.
                    descartar(cola)
                    config = config.copy(habilitado = false)
                    fase = FaseDiscord.WEBHOOK_INVALIDO
                    intento = 0
                    esperaFirme = false
                }
            }
            publicarEstado()
        }
    }

    private suspend fun quitar(pendiente: PendienteDiscord, enviada: Boolean) {
        cola = cola - pendiente
        almacen.guardarCola(cola)
        _desenlaces.tryEmit(DesenlaceDiscord(pendiente.instante, enviada))
    }

    private suspend fun descartar(pendientes: List<PendienteDiscord>) {
        if (pendientes.isEmpty()) return
        cola = cola - pendientes.toSet()
        almacen.guardarCola(cola)
        pendientes.forEach { _desenlaces.tryEmit(DesenlaceDiscord(it.instante, false)) }
    }

    private fun publicarEstado() {
        _estado.value = EstadoDiscord(cola.map { it.instante }.toSet(), fase)
    }
}
