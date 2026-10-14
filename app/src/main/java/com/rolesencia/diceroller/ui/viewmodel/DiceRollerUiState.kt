package com.rolesencia.diceroller.ui.viewmodel

import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.ResultadoDado
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

    // Historial (más reciente primero) y tiradas frecuentes guardadas con nombre
    val historial: List<EntradaHistorial> = emptyList(),
    val tiradasGuardadas: List<TiradaGuardada> = emptyList()
) {
    companion object {
        const val MAX_DADOS = 30
    }
}
