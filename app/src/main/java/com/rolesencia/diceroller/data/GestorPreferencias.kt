package com.rolesencia.diceroller.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.CategoriaSkin
import com.rolesencia.diceroller.domain.apariencia.ColorArgb
import com.rolesencia.diceroller.domain.apariencia.EstiloSkin
import com.rolesencia.diceroller.domain.apariencia.Tipografia
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.sensorial.MaterialMesa
import com.rolesencia.diceroller.tirada.ModoDecision
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Misma receta que el Lanzador Legendario (DataStore Preferences), con la mesa generalizada a N dados.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dice_roller_prefs")

data class PreferenciasGuardadas(
    val sistemaId: String?,
    val pool: List<DadoEnMesa>,
    val skinsPersonalizadas: List<SkinDado>,
    val skinSeleccionada: String?,
    val modificador: Int,
    val objetivo: Int?,
    val conservar: Conservar,
    val modo: ModoDecision,
    val materialMesa: MaterialMesa,
    val modeloTorre: String?,
    val skinsDelSistema: Boolean,
    val audioHabilitado: Boolean,
    val vibracionHabilitada: Boolean,
    val pantallaEncendida: Boolean
)

class GestorPreferencias(private val context: Context) {

    private companion object {
        val SISTEMA = stringPreferencesKey("sistema_id")
        val POOL = stringPreferencesKey("pool")
        val SKINS = stringPreferencesKey("skins_personalizadas")
        val SKIN_SELECCIONADA = stringPreferencesKey("skin_seleccionada")
        val MODIFICADOR = intPreferencesKey("modificador")
        val OBJETIVO = intPreferencesKey("objetivo")
        val CONSERVAR = stringPreferencesKey("conservar")
        val MODO = stringPreferencesKey("modo_decision")
        val MATERIAL = stringPreferencesKey("material_mesa")
        val TORRE = stringPreferencesKey("modelo_torre")
        val SKINS_SISTEMA = booleanPreferencesKey("skins_del_sistema")
        val AUDIO = booleanPreferencesKey("audio_habilitado")
        val VIBRACION = booleanPreferencesKey("vibracion_habilitada")
        val PANTALLA = booleanPreferencesKey("pantalla_encendida")
        const val SIN_OBJETIVO = Int.MIN_VALUE
    }

    val preferenciasFlow: Flow<PreferenciasGuardadas> = context.dataStore.data
        .catch { e ->
            if (e is IOException) {
                Log.e("GestorPreferencias", "Error leyendo preferencias", e)
                emit(emptyPreferences())
            } else throw e
        }
        .map { p ->
            PreferenciasGuardadas(
                sistemaId = p[SISTEMA],
                pool = decodificarPool(p[POOL].orEmpty(), decodificarSkins(p[SKINS].orEmpty())),
                skinsPersonalizadas = decodificarSkins(p[SKINS].orEmpty()),
                skinSeleccionada = p[SKIN_SELECCIONADA],
                modificador = p[MODIFICADOR] ?: 0,
                objetivo = p[OBJETIVO]?.takeIf { it != SIN_OBJETIVO },
                conservar = p[CONSERVAR]?.let { runCatching { Conservar.valueOf(it) }.getOrNull() } ?: Conservar.TODOS,
                modo = p[MODO]?.let { runCatching { ModoDecision.valueOf(it) }.getOrNull() } ?: ModoDecision.FISICA_REAL,
                materialMesa = p[MATERIAL]?.let { runCatching { MaterialMesa.valueOf(it) }.getOrNull() } ?: MaterialMesa.LONA,
                modeloTorre = p[TORRE]?.takeIf { it.isNotEmpty() },
                skinsDelSistema = p[SKINS_SISTEMA] ?: true,
                audioHabilitado = p[AUDIO] ?: true,
                vibracionHabilitada = p[VIBRACION] ?: true,
                pantallaEncendida = p[PANTALLA] ?: true
            )
        }

    suspend fun guardar(prefs: PreferenciasGuardadas) {
        context.dataStore.edit { p ->
            prefs.sistemaId?.let { p[SISTEMA] = it }
            p[POOL] = codificarPool(prefs.pool)
            p[SKINS] = codificarSkins(prefs.skinsPersonalizadas)
            prefs.skinSeleccionada?.let { p[SKIN_SELECCIONADA] = it }
            p[MODIFICADOR] = prefs.modificador
            p[OBJETIVO] = prefs.objetivo ?: SIN_OBJETIVO
            p[CONSERVAR] = prefs.conservar.name
            p[MODO] = prefs.modo.name
            p[MATERIAL] = prefs.materialMesa.name
            p[TORRE] = prefs.modeloTorre.orEmpty()
            p[SKINS_SISTEMA] = prefs.skinsDelSistema
            p[AUDIO] = prefs.audioHabilitado
            p[VIBRACION] = prefs.vibracionHabilitada
            p[PANTALLA] = prefs.pantallaEncendida
        }
    }
}

/** "D6:N:rojo,D6:N:rojo,LEGENDARIO:N:por_tipo" — tipo, rol y skin de cada dado. */
internal fun codificarPool(pool: List<DadoEnMesa>): String =
    pool.joinToString(",") { "${it.tipo.name}:${if (it.rol == RolDado.ESPECIAL) "E" else "N"}:${it.skin.id}" }

internal fun decodificarPool(texto: String, skins: List<SkinDado> = emptyList()): List<DadoEnMesa> =
    texto.split(",").filter { it.isNotBlank() }.mapIndexedNotNull { i, token ->
        val partes = token.split(":")
        val tipo = runCatching { TipoDado.valueOf(partes[0]) }.getOrNull() ?: return@mapIndexedNotNull null
        DadoEnMesa(
            i, tipo,
            if (partes.getOrNull(1) == "E") RolDado.ESPECIAL else RolDado.NORMAL,
            CatalogoSkins.porId(partes.getOrNull(2), skins)
        )
    }

/**
 * Skins del usuario, una por línea: "id|nombre|cuerpo|tinta|borde|estilo|color2|tipografia|imagenes"
 * (colores en hex; borde y color2 opcionales; imágenes separadas por ";"). Las líneas viejas de
 * 4-5 campos se siguen leyendo.
 */
private val SEPARADOR_SKINS = Char(10).toString()   // salto de línea

internal fun codificarSkins(skins: List<SkinDado>): String = skins.joinToString(SEPARADOR_SKINS) { s ->
    listOf(
        s.id, s.nombre.replace("|", " ").replace(SEPARADOR_SKINS, " "),
        s.cuerpo?.let { ColorArgb.hex(it) }.orEmpty(), ColorArgb.hex(s.tinta), s.borde?.let { ColorArgb.hex(it) }.orEmpty(),
        s.estilo.name, s.color2?.let { ColorArgb.hex(it) }.orEmpty(), s.tipografia.name, s.imagenes.joinToString(";")
    ).joinToString("|")
}

internal fun decodificarSkins(texto: String): List<SkinDado> = texto.lines().filter { it.isNotBlank() }.mapNotNull { linea ->
    val p = linea.split("|")
    if (p.size < 4) return@mapNotNull null
    SkinDado(
        id = p[0], nombre = p[1], categoria = CategoriaSkin.PERSONALIZADAS,
        cuerpo = ColorArgb.desdeHex(p[2]) ?: return@mapNotNull null,
        tinta = ColorArgb.desdeHex(p[3]) ?: return@mapNotNull null,
        borde = p.getOrNull(4)?.let { ColorArgb.desdeHex(it) },
        estilo = p.getOrNull(5)?.let { runCatching { EstiloSkin.valueOf(it) }.getOrNull() } ?: EstiloSkin.SOLIDO,
        color2 = p.getOrNull(6)?.let { ColorArgb.desdeHex(it) },
        tipografia = p.getOrNull(7)?.let { runCatching { Tipografia.valueOf(it) }.getOrNull() } ?: Tipografia.CLASICA,
        imagenes = p.getOrNull(8)?.split(";")?.filter { it.isNotBlank() }.orEmpty()
    )
}
