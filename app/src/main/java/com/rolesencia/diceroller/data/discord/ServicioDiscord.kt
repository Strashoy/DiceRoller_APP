package com.rolesencia.diceroller.data.discord

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Una sola instancia para todo el proceso: el publicador vive más que la pantalla, así una tirada
 * hecha justo antes de salir de la app igual termina de enviarse.
 */
class ServicioDiscord private constructor(app: Context) {
    val gestor = GestorDiscord(app)
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cliente = ClienteWebhook()
    val publicador = PublicadorDiscord(gestor, cliente::enviar, alcance)

    init {
        // Cuando vuelve la red se intenta enseguida, sin esperar el próximo reintento.
        val redes = app.getSystemService(ConnectivityManager::class.java)
        runCatching {
            redes?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = publicador.despertar()
            })
        }
    }

    companion object {
        @Volatile private var instancia: ServicioDiscord? = null

        fun obtener(context: Context): ServicioDiscord = instancia ?: synchronized(this) {
            instancia ?: ServicioDiscord(context.applicationContext).also { instancia = it }
        }
    }
}
