package com.rolesencia.diceroller.data.discord

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rolesencia.diceroller.domain.discord.ColaDiscord
import com.rolesencia.diceroller.domain.discord.ConfigDiscord
import com.rolesencia.diceroller.domain.discord.PendienteDiscord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// Archivo aparte del resto de las preferencias: guarda la URL del webhook, que es un secreto, y así
// se puede dejar fuera de la copia de seguridad en la nube (ver res/xml/data_extraction_rules.xml).
private val Context.almacenDiscord: DataStore<Preferences> by preferencesDataStore(name = GestorDiscord.ARCHIVO)

/** Dónde persiste la bandeja de salida (para poder probar el publicador sin Android). */
interface AlmacenCola {
    suspend fun leerCola(): List<PendienteDiscord>
    suspend fun guardarCola(cola: List<PendienteDiscord>)
}

class GestorDiscord(private val context: Context) : AlmacenCola {

    companion object {
        const val ARCHIVO = "discord"
        private val HABILITADO = booleanPreferencesKey("habilitado")
        private val WEBHOOK = stringPreferencesKey("webhook")
        private val JUGADOR = stringPreferencesKey("jugador")
        private val COLA = stringPreferencesKey("cola")
    }

    private val datos: Flow<Preferences> = context.almacenDiscord.data.catch { e ->
        if (e is IOException) {
            Log.e("GestorDiscord", "Error leyendo la configuración de Discord", e)
            emit(emptyPreferences())
        } else throw e
    }

    val configFlow: Flow<ConfigDiscord> = datos.map { p ->
        ConfigDiscord(
            habilitado = p[HABILITADO] ?: false,
            webhook = p[WEBHOOK].orEmpty(),
            jugador = p[JUGADOR].orEmpty()
        )
    }

    suspend fun guardarConfig(config: ConfigDiscord) {
        context.almacenDiscord.edit { p ->
            p[HABILITADO] = config.habilitado
            p[WEBHOOK] = config.webhook
            p[JUGADOR] = config.jugador
        }
    }

    override suspend fun leerCola(): List<PendienteDiscord> = ColaDiscord.decodificar(datos.first()[COLA].orEmpty())

    override suspend fun guardarCola(cola: List<PendienteDiscord>) {
        context.almacenDiscord.edit { it[COLA] = ColaDiscord.codificar(cola) }
    }
}
