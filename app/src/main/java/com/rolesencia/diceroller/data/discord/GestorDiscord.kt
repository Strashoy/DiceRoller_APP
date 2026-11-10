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
import com.rolesencia.diceroller.domain.discord.AnimacionDiscord
import com.rolesencia.diceroller.domain.discord.ColaDiscord
import com.rolesencia.diceroller.domain.discord.ConfigDiscord
import com.rolesencia.diceroller.domain.discord.PendienteDiscord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

// Archivo aparte del resto de las preferencias: guarda la URL del webhook, que es un secreto, y así
// se puede dejar fuera de la copia de seguridad en la nube (ver res/xml/data_extraction_rules.xml).
private val Context.almacenDiscord: DataStore<Preferences> by preferencesDataStore(name = GestorDiscord.ARCHIVO)

/** Dónde persiste la bandeja de salida (para poder probar el publicador sin Android). */
interface AlmacenCola {
    suspend fun leerCola(): List<PendienteDiscord>
    suspend fun guardarCola(cola: List<PendienteDiscord>)

    /** La foto (JPEG) de la tirada de ese instante, si se guardó y sigue estando. */
    suspend fun leerImagen(instante: Long): ByteArray? = null
    suspend fun borrarImagen(instante: Long) {}
}

class GestorDiscord(private val context: Context) : AlmacenCola {

    companion object {
        const val ARCHIVO = "discord"
        const val ULTIMA_FOTO = "ultima.jpg"
        private val HABILITADO = booleanPreferencesKey("habilitado")
        private val WEBHOOK = stringPreferencesKey("webhook")
        private val JUGADOR = stringPreferencesKey("jugador")
        private val CON_FOTO = booleanPreferencesKey("con_foto")
        private val ANIMACION = stringPreferencesKey("animacion")
        private val COLA = stringPreferencesKey("cola")
    }

    // Las fotos van a la caché: si el sistema necesita espacio las borra, y el mensaje sale sin foto.
    private val carpetaFotos: File get() = File(context.cacheDir, "discord").apply { mkdirs() }
    private fun archivoFoto(instante: Long) = File(carpetaFotos, "$instante.jpg")

    /** Guarda la foto de una tirada; false si no se pudo (sin espacio, por ejemplo). */
    suspend fun guardarImagen(instante: Long, jpeg: ByteArray): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            archivoFoto(instante).writeBytes(jpeg)
            // Copia de la última tarjeta enviada: sirve para verla o compartirla después.
            File(carpetaFotos, ULTIMA_FOTO).writeBytes(jpeg)
        }.isSuccess
    }

    override suspend fun leerImagen(instante: Long): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { archivoFoto(instante).takeIf { it.isFile }?.readBytes() }.getOrNull()
    }

    override suspend fun borrarImagen(instante: Long) {
        withContext(Dispatchers.IO) { runCatching { archivoFoto(instante).delete() } }
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
            jugador = p[JUGADOR].orEmpty(),
            conFoto = p[CON_FOTO] ?: true,
            animacion = p[ANIMACION]?.let { runCatching { AnimacionDiscord.valueOf(it) }.getOrNull() } ?: AnimacionDiscord.NUNCA
        )
    }

    suspend fun guardarConfig(config: ConfigDiscord) {
        context.almacenDiscord.edit { p ->
            p[HABILITADO] = config.habilitado
            p[WEBHOOK] = config.webhook
            p[JUGADOR] = config.jugador
            p[CON_FOTO] = config.conFoto
            p[ANIMACION] = config.animacion.name
        }
    }

    override suspend fun leerCola(): List<PendienteDiscord> = ColaDiscord.decodificar(datos.first()[COLA].orEmpty())

    override suspend fun guardarCola(cola: List<PendienteDiscord>) {
        context.almacenDiscord.edit { it[COLA] = ColaDiscord.codificar(cola) }
    }
}
