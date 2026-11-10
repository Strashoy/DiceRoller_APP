package com.rolesencia.diceroller.ui.viewmodel

import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.data.discord.EstadoDiscord
import com.rolesencia.diceroller.data.discord.FaseDiscord
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.discord.ConfigDiscord
import com.rolesencia.diceroller.domain.discord.EnvioDiscord
import com.rolesencia.diceroller.domain.historial.EntradaHistorial
import com.rolesencia.diceroller.domain.historial.TiradaGuardada
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import com.rolesencia.diceroller.domain.reglas.SistemaReglas
import com.rolesencia.diceroller.domain.reglas.Veredicto
import com.rolesencia.diceroller.domain.reglas.sistemas.SumaLibre
import com.rolesencia.diceroller.sensorial.EstadisticasSonido
import com.rolesencia.diceroller.sensorial.MaterialMesa
import com.rolesencia.diceroller.tirada.ConfigMesa
import com.rolesencia.diceroller.tirada.DadoVisual
import com.rolesencia.diceroller.tirada.MetricasSesion
import com.rolesencia.diceroller.tirada.ModoDecision
import com.rolesencia.diceroller.tirada.SesionTirada

/** Mediciones de la última tirada, para comparar los dos modos. */
data class MetricasTirada(
    val sesion: MetricasSesion,
    /** Del toque a tener la sesión lista (crear cuerpos; en el modo 2 incluye la pre-simulación). */
    val preparacionMs: Long?,
    /** Del toque en "Lanzar" (o el fin del gesto) al primer cuadro con dados en movimiento. */
    val latenciaPrimerMovimientoMs: Long?,
    /** Del toque al veredicto final. */
    val duracionTotalMs: Long?,
    val rondas: Int,
    val sonido: EstadisticasSonido
)

data class DiceRollerUiState(
    val estaCargando: Boolean = true,

    // Configuración de la tirada
    val sistema: SistemaReglas = SumaLibre,
    val pool: List<DadoEnMesa> = SumaLibre.tiradaSugerida(),
    val parametros: ParametrosTirada = ParametrosTirada(),

    // Modo de decisión del número (cada uno en su apartado)
    val modo: ModoDecision = ModoDecision.FISICA_REAL,
    /** Sólo modo predeterminado: elegir a mano el número de cada dado (si no, se sortea). */
    val elegirValores: Boolean = false,
    val valoresElegidos: Map<Int, Int> = emptyMap(),

    // Mesa 3D
    val mesa: ConfigMesa = ConfigMesa(),
    val materialMesa: MaterialMesa = MaterialMesa.LONA,
    /** Torre de dados elegida ("madera", "castillo", "cuero"); null = lanzar sobre la bandeja. */
    val modeloTorre: String? = null,
    /** Tirada en curso: la mesa 3D la avanza cuadro a cuadro. */
    val sesion: SesionTirada? = null,
    /** Dados quietos sobre la mesa (antes de lanzar o después de terminar). */
    val dadosEnMesa: List<DadoVisual> = emptyList(),
    /** Calculando la pre-simulación (sólo modo predeterminado). */
    val preparando: Boolean = false,

    // Estado del juego
    val estaRodando: Boolean = false,
    val resultados: List<ResultadoDado> = emptyList(),
    val veredicto: Veredicto? = null,
    val metricas: MetricasTirada? = null,

    // Apariencia: con qué skin se agregan los dados nuevos
    val skinActual: SkinDado = CatalogoSkins.POR_TIPO,
    val skinsPersonalizadas: List<SkinDado> = emptyList(),
    /** Los dados nuevos se agregan como ESPECIALES (Hambre, Miedo…). */
    val especialSiguiente: Boolean = false,
    /** Los dados de un sistema que propone skins (V5, Daggerheart…) las usan automáticamente. */
    val skinsDelSistema: Boolean = true,

    // Ajustes (mismos que el Lanzador Legendario)
    val audioHabilitado: Boolean = true,
    val vibracionHabilitada: Boolean = true,
    val pantallaEncendida: Boolean = true,
    /** Modo inmersivo: oculta las barras del sistema (vuelven deslizando desde el borde). */
    val pantallaCompleta: Boolean = true,
    /** Leer el veredicto en voz alta (como el "speech" de Dice by PCalc). */
    val vozHabilitada: Boolean = false,
    /** Agitar el teléfono lanza los dados, como sacudir un cubilete. */
    val agitarHabilitado: Boolean = true,
    /** Controles para zurdos: los botones de los costados de LANZAR cambian de lado. */
    val manoIzquierda: Boolean = false,
    /** Veces que se mostró "cómo quitar un dado" (se muestra sólo las primeras). */
    val pistasQuitar: Int = 0,

    // Historial (más reciente primero) y tiradas frecuentes guardadas con nombre
    val historial: List<EntradaHistorial> = emptyList(),
    val tiradasGuardadas: List<TiradaGuardada> = emptyList(),

    // Discord: publicar cada tirada en el canal de la mesa
    val discord: ConfigDiscord = ConfigDiscord(),
    val estadoDiscord: EstadoDiscord = EstadoDiscord(),
    /** Por qué la última tirada no se publicó aunque Discord esté encendido. */
    val noPublicada: MotivoNoPublicada? = null,
    /** Resultado del botón "Enviar prueba" de Ajustes. */
    val pruebaDiscord: PruebaDiscord? = null,
    /** Discord rechazó el webhook y se apagó el envío: falta avisarle al usuario. */
    val avisoWebhookInvalido: Boolean = false
) {
    companion object {
        const val MAX_DADOS = 30
    }
}

/** Tiradas que no van al canal aunque Discord esté encendido. */
enum class MotivoNoPublicada {
    /** El máster la lanzó con "Tirada oculta". */
    OCULTA,
    /** Los números se eligieron a mano (modo predeterminado): no es una tirada, no se publica. */
    VALORES_ELEGIDOS
}

enum class PruebaDiscord(val texto: String) {
    EN_CURSO("Enviando…"),
    LLEGO("Llegó. Fijate en el canal."),
    SIN_CONEXION("No hay conexión. Probá de nuevo."),
    WEBHOOK_INVALIDO("Discord no reconoce ese webhook. Copialo de nuevo."),
    RECHAZADA("Discord rechazó el mensaje.")
}

/** Lo que el indicador de la pantalla dice sobre la publicación de la última tirada. */
enum class AvisoDiscord(val texto: String) {
    ENVIANDO("Discord · enviando…"),
    EN_COLA("Discord · en cola, sin conexión"),
    ENVIADA("Discord · enviada"),
    NO_ENVIADA("Discord · no se envió"),
    OCULTA("Tirada oculta · no va a Discord"),
    VALORES_ELEGIDOS("Valores elegidos · no va a Discord")
}

/**
 * Qué mostrar junto al resultado: sale de la última tirada del historial, no de un estado aparte,
 * así nunca dice "enviada" sobre una tirada que no es la que está en pantalla.
 */
val DiceRollerUiState.avisoDiscord: AvisoDiscord?
    get() {
        if (!discord.activo || veredicto == null || estaRodando || preparando) return null
        val ultima = historial.firstOrNull() ?: return null
        return when {
            noPublicada == MotivoNoPublicada.OCULTA -> AvisoDiscord.OCULTA
            noPublicada == MotivoNoPublicada.VALORES_ELEGIDOS -> AvisoDiscord.VALORES_ELEGIDOS
            ultima.instante in estadoDiscord.pendientes ->
                if (estadoDiscord.fase == FaseDiscord.REINTENTANDO) AvisoDiscord.EN_COLA else AvisoDiscord.ENVIANDO
            ultima.discord == EnvioDiscord.ENVIADA -> AvisoDiscord.ENVIADA
            ultima.discord == EnvioDiscord.NO_ENVIADA -> AvisoDiscord.NO_ENVIADA
            else -> null
        }
    }
