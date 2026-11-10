package com.rolesencia.diceroller.ui.screen

import com.rolesencia.diceroller.ui.theme.LocalAmbiente
import com.rolesencia.diceroller.ui.theme.LocalAcento
import com.rolesencia.diceroller.sensorial.MaterialMesa
import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.PaletasAmbiente
import androidx.compose.runtime.CompositionLocalProvider
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.R
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.historial.Notacion
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.sensorial.AgitarParaLanzar
import com.rolesencia.diceroller.sensorial.FeedbackTactil
import com.rolesencia.diceroller.sensorial.rememberLectorVoz
import com.rolesencia.diceroller.tirada.Gesto
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.rolesencia.diceroller.tirada.torre.ModelosTorre
import com.rolesencia.diceroller.ui.components.CarruselDados
import com.rolesencia.diceroller.ui.components.HojaAjustes
import com.rolesencia.diceroller.ui.components.HojaHistorial
import com.rolesencia.diceroller.ui.components.HojaParametros
import com.rolesencia.diceroller.ui.components.SelectorSistema
import com.rolesencia.diceroller.ui.components.SolapaDados
import com.rolesencia.diceroller.ui.components.resumenParametros
import com.rolesencia.diceroller.ui.mesa3d.MesaDados3D
import com.rolesencia.diceroller.ui.skins.CreadorSkin
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalConfiguration
import com.rolesencia.diceroller.domain.historial.Atajos
import com.rolesencia.diceroller.domain.reglas.Veredicto
import com.rolesencia.diceroller.domain.reglas.totalFueraDelTitulo
import com.rolesencia.diceroller.ui.components.FilaAtajos
import com.rolesencia.diceroller.ui.theme.DarkBlueFantasia
import com.rolesencia.diceroller.ui.theme.Ivory
import com.rolesencia.diceroller.ui.theme.NaranjaCostoso
import com.rolesencia.diceroller.ui.theme.RojoSobreOscuro
import com.rolesencia.diceroller.ui.theme.VerdeExito
import com.rolesencia.diceroller.ui.theme.colorDe
import com.rolesencia.diceroller.ui.viewmodel.AvisoDiscord
import com.rolesencia.diceroller.ui.viewmodel.avisoDiscord
import com.rolesencia.diceroller.ui.viewmodel.DiceRollerUiState
import com.rolesencia.diceroller.ui.viewmodel.DiceRollerViewModel

/**
 * Pantalla única, a pantalla completa: la mesa 3D ocupa todo y la interfaz flota encima.
 *
 * Cómo se reparte (de arriba hacia abajo, ver capítulo 12 de la bitácora):
 * - Barra superior: sistema de juego, dónde se lanza (bandeja o torre) y ajustes.
 * - Marcador (HUD): el veredicto de la tirada, o una pista de qué hacer.
 * - La mesa, encuadrada en el área que no tapa la interfaz.
 * - Barra inferior, en la zona del pulgar: fichas de los dados y, debajo, Tirada · LANZAR · Dados.
 * Lo que se usa poco (modo de decisión, sonido, pantalla) vive en hojas inferiores.
 */
@Composable
fun DiceRollerScreen(viewModel: DiceRollerViewModel) {
    val state by viewModel.uiState.collectAsState()
    var hoja by remember { mutableStateOf(Hoja.NINGUNA) }
    var pestanaHistorial by remember { mutableIntStateOf(0) }
    var mostrarCreador by remember { mutableStateOf(false) }
    var skinEnEdicion by remember { mutableStateOf<SkinDado?>(null) }
    var abrirSolapa by remember { mutableIntStateOf(0) }
    val ocupado = state.estaRodando || state.preparando
    val torre = remember(state.modeloTorre, state.mesa) { state.modeloTorre?.let { ModelosTorre.construir(it, state.mesa) } }
    val vista = LocalView.current

    DisposableEffect(state.pantallaEncendida) {
        vista.keepScreenOn = state.pantallaEncendida
        onDispose { vista.keepScreenOn = false }
    }
    ModoInmersivo(state.pantallaCompleta)

    // Voz: lee cada veredicto nuevo (no el último guardado al abrir la app).
    val lector = rememberLectorVoz(state.vozHabilitada)
    val abiertaEn = remember { System.currentTimeMillis() }
    val ultimaTirada = state.historial.firstOrNull()
    LaunchedEffect(ultimaTirada?.instante, lector) {
        if (lector != null && ultimaTirada != null && ultimaTirada.instante > abiertaEn) {
            lector.decir(listOf(ultimaTirada.titulo, ultimaTirada.detalle).filter { it.isNotBlank() }.joinToString(". "))
        }
    }

    // Agitar el teléfono = lanzar, sólo con la mesa a la vista y algo para tirar.
    AgitarParaLanzar(
        activo = state.agitarHabilitado && !ocupado && hoja == Hoja.NINGUNA && state.pool.isNotEmpty()
    ) { fuerza ->
        vista.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        viewModel.lanzar(Gesto(Random.nextFloat() * 2f - 1f, -1f, fuerza))
    }

    // Quitar un dado con pulsación larga se puede deshacer (patrón de Material: acción + "Deshacer").
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    /** Quitar un dado (desde el carrusel o manteniéndolo apretado en la mesa) siempre se puede deshacer. */
    fun avisarQuitado(quitado: DadoEnMesa) {
        vista.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        alcance.launch {
            avisos.currentSnackbarData?.dismiss()
            val r = avisos.showSnackbar(
                "Quitaste un ${quitado.tipo.etiqueta}", actionLabel = "Deshacer",
                duration = SnackbarDuration.Short
            )
            if (r == SnackbarResult.ActionPerformed) viewModel.restaurarDado(quitado)
        }
    }

    // Discord rechazó el webhook (lo borraron o cambió): el envío ya se apagó; se avisa una vez.
    LaunchedEffect(state.avisoWebhookInvalido) {
        if (state.avisoWebhookInvalido) {
            avisos.currentSnackbarData?.dismiss()
            avisos.showSnackbar(
                "Discord ya no reconoce el webhook y se apagó el envío. Pegá uno nuevo en Ajustes.",
                duration = SnackbarDuration.Long
            )
            // Recién al cerrarse: marcarlo antes reiniciaría este efecto y cortaría el aviso.
            viewModel.avisoWebhookVisto()
        }
    }

    // Alto real de las barras superpuestas: la cámara encuadra la mesa en lo que queda libre.
    val densidad = LocalDensity.current
    // Se miden por fuera de los márgenes del sistema (recorte de la cámara, barra de gestos): si no,
    // en un teléfono con recorte la mesa quedaba encuadrada 36 dp más arriba de lo que se ve libre.
    var altoSuperior by remember { mutableIntStateOf(0) }
    var altoInferior by remember { mutableIntStateOf(0) }
    val areaUtil = with(densidad) { PaddingValues(top = altoSuperior.toDp(), bottom = altoInferior.toDp()) }

    // Mientras ruedan los dados la interfaz se aparta: lo único que importa es la tirada.
    val opacidadControles by animateFloatAsState(if (ocupado) 0.25f else 1f, tween(350), label = "controles")

    // Ambiente donde se lanza: define los colores "Por tipo" y el acento de los resaltados.
    val ambiente = PaletasAmbiente.clave(state.modeloTorre, state.materialMesa == MaterialMesa.MADERA)
    CatalogoSkins.ambienteActivo = ambiente
    CompositionLocalProvider(
        LocalAmbiente provides ambiente,
        LocalAcento provides Color(PaletasAmbiente.acento(ambiente))
    ) {
    Box(Modifier.fillMaxSize().background(DarkBlueFantasia)) {
        MesaDados3D(
            mesa = state.mesa,
            material = state.materialMesa,
            sesion = state.sesion,
            dadosQuietos = state.dadosEnMesa,
            destacados = state.veredicto?.dadosDestacados.orEmpty(),
            preparando = state.preparando,
            puedeLanzar = !ocupado && state.pool.isNotEmpty(),
            onCuadro = viewModel::onCuadro,
            onSesionTerminada = viewModel::onSesionTerminada,
            onGesto = viewModel::lanzar,
            onProporcion = viewModel::actualizarProporcionMesa,
            onAlturaEntrada = viewModel::actualizarAlturaEntrada,
            onMantenerDado = { id ->
                val quitado = state.pool.firstOrNull { it.id == id }
                if (quitado != null) {
                    viewModel.quitarDado(id)
                    avisarQuitado(quitado)
                }
            },
            modifier = Modifier.fillMaxSize(),
            modelo = torre,
            areaUtil = areaUtil
        )

        // --- Arriba: barra + marcador, sobre un degradé que asegura la lectura encima de la mesa ---
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { altoSuperior = it.height }
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        ) {
            BarraSuperior(
                state = state,
                ocupado = ocupado,
                opacidad = opacidadControles,
                onSistema = { hoja = Hoja.SISTEMA },
                onLanzamiento = viewModel::seleccionarLanzamiento,
                onAjustes = { hoja = Hoja.AJUSTES }
            )
            Marcador(state, hayTorre = torre != null)
        }

        // Discord: qué pasó con la publicación de la tirada que está en pantalla. Flota debajo del
        // marcador sin ocupar lugar, para que aparecer y desaparecer no mueva el encuadre de la mesa.
        state.avisoDiscord?.let { aviso ->
            IndicadorDiscord(
                aviso = aviso,
                enCola = state.estadoDiscord.pendientes.size,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = with(densidad) { altoSuperior.toDp() })
            )
        }

        // --- Abajo: zona del pulgar ---
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { altoInferior = it.height }
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Atajos: historial, tiradas guardadas y las últimas tiradas distintas, a un toque.
            FilaAtajos(
                guardadas = state.tiradasGuardadas,
                recientes = remember(state.historial, state.tiradasGuardadas, state.sistema, state.pool, state.parametros) {
                    Atajos.recientes(state.historial, state.tiradasGuardadas, state.sistema.id, state.pool, state.parametros)
                },
                habilitado = !ocupado,
                onHistorial = { pestanaHistorial = 0; hoja = Hoja.HISTORIAL },
                onGuardar = { pestanaHistorial = 1; hoja = Hoja.HISTORIAL },
                onLanzarGuardada = { FeedbackTactil.clickBoton(vista); viewModel.lanzarGuardada(it) },
                onRepetir = { FeedbackTactil.clickBoton(vista); viewModel.repetir(it) },
                modifier = Modifier.alpha(opacidadControles)
            )
            // Fila de dados: tocar suma uno, mantener quita uno (y se puede deshacer).
            CarruselDados(
                pool = state.pool,
                resultados = state.resultados,
                destacados = state.veredicto?.dadosDestacados.orEmpty(),
                estaRodando = ocupado,
                // Cambia también al terminar de cargar las preferencias: la fila se ordena con el pool real.
                sistemaId = if (state.estaCargando) "" else state.sistema.id,
                skinActual = state.skinActual,
                onAgregar = { tipo ->
                    FeedbackTactil.clickBoton(vista)
                    viewModel.agregarDado(tipo)
                    if (state.pistasQuitar < PISTAS_QUITAR) {
                        viewModel.registrarPistaQuitar()
                        alcance.launch {
                            avisos.currentSnackbarData?.dismiss()
                            avisos.showSnackbar(
                                "Para quitar un dado, mantenelo apretado (acá o en la mesa) o tocá su −",
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                },
                onQuitar = { tipo ->
                    val quitado = viewModel.dadoAQuitar(tipo)
                    viewModel.quitarUnoDeTipo(tipo)
                    if (quitado != null) avisarQuitado(quitado)
                }
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // En 66 dp de ancho no entra "+0 · CD 15 · Mayor": el botón muestra lo que más cambia
                // (el modificador, grande) y debajo el objetivo en corto. TalkBack lee el resumen entero.
                val campo = state.sistema.campoObjetivo
                val botonTirada: @Composable RowScope.() -> Unit = {
                    BotonSecundario(
                        icono = R.drawable.ic_parametros,
                        destacado = if (state.sistema.usaModificador) conSignoTirada(state.parametros.modificador) else null,
                        texto = if (campo != null && state.parametros.objetivo != null) "${campo.etiquetaCorta} ${state.parametros.objetivo}" else "Tirada",
                        descripcion = "Tirada: " + resumenParametros(state.parametros, state.sistema.usaModificador, campo),
                        habilitado = !ocupado,
                        modifier = Modifier.weight(1f).alpha(opacidadControles),
                        onClick = { hoja = Hoja.PARAMETROS }
                    )
                }
                val botonDados: @Composable RowScope.() -> Unit = {
                    BotonSecundario(
                        icono = R.drawable.ic_dados,
                        texto = "Dados",
                        contador = state.pool.size,
                        descripcion = "Dados y skins: ${state.pool.size} en la mesa",
                        habilitado = !ocupado,
                        modifier = Modifier.weight(1f).alpha(opacidadControles),
                        onClick = { abrirSolapa++ }
                    )
                }
                if (state.manoIzquierda) botonDados() else botonTirada()
                BotonLanzar(
                    ocupado = ocupado,
                    habilitado = !ocupado && state.pool.isNotEmpty(),
                    conservar = state.parametros.conservar,
                    discordActivo = state.discord.activo,
                    onLanzar = { FeedbackTactil.clickBoton(vista); viewModel.lanzar() },
                    onLanzarCon = viewModel::lanzarCon,
                    onLanzarOculta = viewModel::lanzarOculta
                )
                if (state.manoIzquierda) botonTirada() else botonDados()
            }
        }

        // Solapa de dados (Legendario, especiales, skins): se abre con el botón "Dados".
        SolapaDados(
            pool = state.pool,
            habilitado = !ocupado,
            skinActual = state.skinActual,
            skinsPersonalizadas = state.skinsPersonalizadas,
            especial = state.especialSiguiente,
            skinsDelSistema = state.skinsDelSistema,
            onAgregar = viewModel::agregarDado,
            onQuitar = viewModel::quitarUnoDeTipo,
            onElegirSkin = viewModel::seleccionarSkin,
            onToggleEspecial = viewModel::toggleEspecialSiguiente,
            onToggleSkinsDelSistema = viewModel::toggleSkinsDelSistema,
            onAplicarSkinATodos = viewModel::aplicarSkinATodos,
            onCrearSkin = { skinEnEdicion = null; mostrarCreador = true },
            onEditarSkin = { skinEnEdicion = it; mostrarCreador = true },
            onBorrarSkin = viewModel::borrarSkin,
            solicitudApertura = abrirSolapa,
            aLaIzquierda = state.manoIzquierda
        )

        SnackbarHost(
            avisos,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = with(densidad) { altoInferior.toDp() } + 8.dp)
        )

        if (mostrarCreador) {
            CreadorSkin(
                inicial = skinEnEdicion,
                base = state.skinActual,
                cantidadPersonalizadas = state.skinsPersonalizadas.size,
                onGuardar = { viewModel.guardarSkin(it); mostrarCreador = false },
                onCerrar = { mostrarCreador = false }
            )
        }
    }

    }

    val cerrar = { hoja = Hoja.NINGUNA }
    when (hoja) {
        Hoja.NINGUNA -> Unit
        Hoja.SISTEMA -> SelectorSistema(state.sistema, viewModel::seleccionarSistema, cerrar)
        Hoja.HISTORIAL -> HojaHistorial(
            historial = state.historial,
            guardadas = state.tiradasGuardadas,
            notacionActual = Notacion.de(state.pool, if (state.sistema.usaModificador) state.parametros.modificador else 0),
            habilitado = !ocupado,
            pendientesDiscord = state.estadoDiscord.pendientes,
            pestanaInicial = pestanaHistorial,
            onRepetir = viewModel::repetir,
            onBorrarHistorial = viewModel::borrarHistorial,
            onRestaurarHistorial = viewModel::restaurarHistorial,
            onLanzarGuardada = viewModel::lanzarGuardada,
            onGuardarActual = viewModel::guardarTiradaActual,
            onBorrarGuardada = viewModel::borrarTiradaGuardada,
            onRestaurarGuardada = viewModel::restaurarTiradaGuardada,
            onCerrar = cerrar
        )
        Hoja.PARAMETROS -> HojaParametros(
            parametros = state.parametros,
            usaModificador = state.sistema.usaModificador,
            campoObjetivo = state.sistema.campoObjetivo,
            habilitado = !ocupado,
            onModificador = viewModel::cambiarModificador,
            onObjetivo = viewModel::cambiarObjetivo,
            onQuitarObjetivo = viewModel::quitarObjetivo,
            onConservar = viewModel::cambiarConservar,
            onTiradaSugerida = viewModel::restaurarTiradaSugerida,
            onCerrar = cerrar
        )
        Hoja.AJUSTES -> HojaAjustes(
            modo = state.modo,
            habilitado = !ocupado,
            onModo = viewModel::seleccionarModo,
            pool = state.pool,
            elegirValores = state.elegirValores,
            valoresElegidos = state.valoresElegidos,
            onToggleElegir = viewModel::toggleElegirValores,
            onCiclarValor = viewModel::ciclarValor,
            metricas = state.metricas,
            hayTorre = torre != null,
            materialMesa = state.materialMesa,
            onMaterialMesa = viewModel::cambiarMaterialMesa,
            audio = state.audioHabilitado,
            onAudio = viewModel::toggleAudio,
            vibracion = state.vibracionHabilitada,
            onVibracion = viewModel::toggleVibracion,
            pantallaEncendida = state.pantallaEncendida,
            onPantallaEncendida = viewModel::togglePantallaEncendida,
            pantallaCompleta = state.pantallaCompleta,
            onPantallaCompleta = viewModel::togglePantallaCompleta,
            voz = state.vozHabilitada,
            onVoz = viewModel::toggleVoz,
            agitar = state.agitarHabilitado,
            onAgitar = viewModel::toggleAgitar,
            zurdo = state.manoIzquierda,
            onZurdo = viewModel::toggleZurdo,
            discord = state.discord,
            pruebaDiscord = state.pruebaDiscord,
            onDiscord = viewModel::cambiarDiscord,
            onProbarDiscord = viewModel::probarDiscord,
            onCerrar = cerrar
        )
    }
}

private enum class Hoja { NINGUNA, SISTEMA, PARAMETROS, AJUSTES, HISTORIAL }

@Composable
private fun BarraSuperior(
    state: DiceRollerUiState,
    ocupado: Boolean,
    opacidad: Float,
    onSistema: () -> Unit,
    onLanzamiento: (String?) -> Unit,
    onAjustes: () -> Unit
) {
    var menuLanzamiento by remember { mutableStateOf(false) }
    // En teléfonos angostos (320 dp) el escenario queda sólo con su ícono: la torre ya se ve en la
    // mesa, y ese lugar lo necesita el nombre del sistema para leerse entero.
    val angosta = LocalConfiguration.current.screenWidthDp < 380
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).alpha(opacidad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Sistema de juego: es lo que define cómo se lee la tirada, por eso va primero y con nombre.
        // Ocupa el espacio libre y usa el nombre corto ("D&D 5e (d20)"); TalkBack lee el completo.
        Box(Modifier.weight(1f)) {
            PildoraSuperior(
                icono = R.drawable.ic_sistema,
                texto = state.sistema.nombreCorto,
                descripcion = "Sistema de juego: " + state.sistema.nombre,
                habilitado = !ocupado,
                onClick = onSistema
            )
        }
        Box(Modifier.widthIn(max = 170.dp)) {
            PildoraSuperior(
                icono = R.drawable.ic_torre,
                texto = if (angosta) null else nombreCortoEscenario(state.modeloTorre),
                descripcion = "Dónde se lanza: " + (state.modeloTorre?.let { ModelosTorre.nombre(it) } ?: "Bandeja"),
                habilitado = !ocupado,
                onClick = { menuLanzamiento = true }
            )
            DropdownMenu(expanded = menuLanzamiento, onDismissRequest = { menuLanzamiento = false }) {
                DropdownMenuItem(
                    text = { Text("Bandeja") },
                    onClick = { onLanzamiento(null); menuLanzamiento = false }
                )
                ModelosTorre.ids.forEach { id ->
                    DropdownMenuItem(
                        text = { Text("Torre: ${ModelosTorre.nombre(id)}") },
                        onClick = { onLanzamiento(id); menuLanzamiento = false }
                    )
                }
            }
        }
        // El historial ya no está acá: vive en la fila de atajos, al alcance del pulgar.
        IconButton(onClick = onAjustes) {
            Icon(painterResource(R.drawable.ic_ajustes), contentDescription = "Ajustes", tint = Ivory)
        }
    }
}

/** Cuántas veces se muestra la pista de cómo quitar un dado. */
private const val PISTAS_QUITAR = 2

private fun nombreCortoEscenario(id: String?): String = when (id) {
    null -> "Bandeja"
    "madera" -> "Madera"
    "castillo" -> "Castillo"
    "cuero" -> "Cuero"
    else -> ModelosTorre.nombre(id)
}

/** Botón tipo "píldora" translúcido: se lee sobre la mesa sin taparla. */
@Composable
private fun PildoraSuperior(
    @DrawableRes icono: Int,
    /** null = sólo el ícono, en un círculo de 48 dp. */
    texto: String?,
    descripcion: String,
    habilitado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = habilitado,
        contentPadding = PaddingValues(horizontal = if (texto == null) 0.dp else 12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color.Black.copy(alpha = 0.45f),
            contentColor = Ivory
        ),
        // TalkBack lee el nombre completo aunque en pantalla se vea el corto o sólo el ícono.
        modifier = modifier
            .then(if (texto == null) Modifier.size(48.dp) else Modifier.heightIn(min = 48.dp))
            .semantics { contentDescription = descripcion }
    ) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(if (texto == null) 22.dp else 18.dp))
        if (texto != null) {
            Spacer(Modifier.width(6.dp))
            Text(texto, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * LANZAR: un toque lanza; mantenerlo apretado abre ventaja / desventaja / normal y lanza con lo
 * elegido (el botón de tirada de D&D Beyond funciona igual). Si hay ventaja o desventaja activa,
 * el botón lo muestra para que no se olvide.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BotonLanzar(
    ocupado: Boolean,
    habilitado: Boolean,
    conservar: Conservar,
    discordActivo: Boolean,
    onLanzar: () -> Unit,
    onLanzarCon: (Conservar) -> Unit,
    onLanzarOculta: () -> Unit
) {
    val vista = LocalView.current
    var menu by remember { mutableStateOf(false) }
    val fondo = if (habilitado) MaterialTheme.colorScheme.primary else Ivory.copy(alpha = 0.12f)
    val tinta = if (habilitado) MaterialTheme.colorScheme.onPrimary else Ivory.copy(alpha = 0.45f)
    Box {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = fondo,
            contentColor = tinta,
            modifier = Modifier
                .height(64.dp)
                .widthIn(min = 132.dp)
                .clip(RoundedCornerShape(32.dp))
                .combinedClickable(
                    enabled = habilitado,
                    onClickLabel = "Lanzar",
                    onLongClickLabel = "Elegir ventaja o desventaja",
                    onLongClick = {
                        vista.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        menu = true
                    },
                    onClick = onLanzar
                )
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(if (ocupado) "Rodando…" else "LANZAR", fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
                val marca = when (conservar) {
                    Conservar.MAYOR -> "con ventaja"
                    Conservar.MENOR -> "con desventaja"
                    Conservar.TODOS -> null
                }
                if (marca != null && !ocupado) Text(marca, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            listOf(
                Conservar.MAYOR to "Con ventaja (el mayor)",
                Conservar.MENOR to "Con desventaja (el menor)",
                Conservar.TODOS to "Normal"
            ).forEach { (modo, texto) ->
                DropdownMenuItem(
                    text = { Text(if (modo == conservar) "✓ $texto" else texto) },
                    onClick = { menu = false; onLanzarCon(modo) }
                )
            }
            // La tirada del máster detrás de la pantalla: se tira igual pero no va al canal.
            if (discordActivo) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Tirada oculta (no va a Discord)") },
                    onClick = { menu = false; onLanzarOculta() }
                )
            }
        }
    }
}

/**
 * Píldora chica con el estado de la publicación en Discord. El punto de color acompaña al texto
 * (verde: llegó; ámbar: espera o no se publica; rojo claro: no se envió), nunca lo reemplaza.
 */
@Composable
private fun IndicadorDiscord(aviso: AvisoDiscord, enCola: Int, modifier: Modifier = Modifier) {
    val color = when (aviso) {
        AvisoDiscord.ENVIADA -> VerdeExito
        AvisoDiscord.NO_ENVIADA -> RojoSobreOscuro
        else -> NaranjaCostoso
    }
    // Si hay más tiradas esperando además de la que está en pantalla, se dice cuántas.
    val texto = if (aviso == AvisoDiscord.EN_COLA && enCola > 1) "${aviso.texto} · $enCola tiradas" else aviso.texto
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.6f),
        contentColor = Ivory,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(texto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Botón de los costados de LANZAR. Arriba va el ícono, o [destacado] si lo hay (el modificador, que es
 * lo que se mira antes de tirar); el [contador] se muestra como insignia sobre el ícono, con el acento
 * del ambiente, igual que los contadores del carrusel.
 */
@Composable
private fun BotonSecundario(
    @DrawableRes icono: Int,
    texto: String,
    descripcion: String,
    habilitado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destacado: String? = null,
    contador: Int = 0
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            contentColor = Ivory
        ),
        modifier = modifier.height(64.dp).semantics { contentDescription = descripcion }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (destacado != null) {
                Text(destacado, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Black, maxLines = 1)
            } else {
                BadgedBox(
                    badge = {
                        if (contador > 0) Badge(containerColor = LocalAcento.current, contentColor = Color.Black) {
                            Text("$contador", fontWeight = FontWeight.Bold)
                        }
                    }
                ) { Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(22.dp)) }
            }
            Text(texto, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

private fun conSignoTirada(n: Int) = if (n >= 0) "+$n" else "−${-n}"

/**
 * Marcador: el veredicto en grande, con el color de su nivel. Tiene altura fija para que aparecer y
 * desaparecer no cambie el área de la mesa. Antes de la primera tirada muestra una pista de uso, y
 * TalkBack anuncia cada veredicto nuevo (región "viva").
 */
@Composable
private fun Marcador(state: DiceRollerUiState, hayTorre: Boolean) {
    val contenido = when {
        state.preparando -> ContenidoMarcador(titulo = "Pre-simulando la tirada…")
        state.estaRodando -> ContenidoMarcador(titulo = "Rodando…", detalle = "Tocá la mesa para ver el resultado ya")
        state.veredicto != null -> contenidoDe(state.veredicto)
        state.pool.isEmpty() -> ContenidoMarcador(titulo = "Mesa vacía", detalle = "Tocá un dado de abajo para agregarlo")
        // Las primeras veces, la pista de uso; después, la tirada en notación ("1d20 + 5").
        state.historial.size < 3 && hayTorre -> ContenidoMarcador(
            titulo = "Listo para lanzar",
            detalle = "Tocá un dado para sumarlo, mantenelo para quitarlo · LANZAR los suelta por la torre"
        )
        state.historial.size < 3 -> ContenidoMarcador(
            titulo = "Listo para lanzar",
            detalle = "Tocá un dado para sumarlo · deslizá sobre la mesa o tocá LANZAR"
        )
        else -> ContenidoMarcador(
            titulo = Notacion.de(state.pool, if (state.sistema.usaModificador) state.parametros.modificador else 0),
            detalle = state.sistema.nombre
        )
    }
    AnimatedContent(
        targetState = contenido,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
        label = "marcador",
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(ALTO_MARCADOR)
            .padding(horizontal = 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) { c ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Placa oscura: la cámara cenital pone piedra de tono medio justo detrás del texto, y sin
            // ella el contraste depende de la escena (se midió 1,15:1 para "Fallo" sobre el castillo).
            Surface(shape = RoundedCornerShape(18.dp), color = Color.Black.copy(alpha = 0.62f), contentColor = Ivory) {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (c.numero != null) {
                        Text(
                            c.numero,
                            color = c.colorNumero,
                            fontSize = 40.sp,
                            lineHeight = 42.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                    if (c.titulo != null) {
                        Text(
                            c.titulo,
                            color = c.colorTitulo,
                            // Junto al número es una etiqueta; solo, es el titular.
                            fontSize = when {
                                c.numero != null -> 16.sp
                                c.esVeredicto -> 28.sp
                                else -> 17.sp
                            },
                            lineHeight = if (c.esVeredicto) 32.sp else 21.sp,
                            fontWeight = if (c.esVeredicto && c.numero == null) FontWeight.Black else FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            // La notación de una mesa cargada ("1d20 + 1d12 + 1d10 + 1d8 + 2d6") necesita dos renglones.
                            maxLines = if (c.esVeredicto) 1 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (c.detalle != null) {
                        Text(
                            c.detalle,
                            color = Ivory.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            maxLines = if (c.numero != null && c.titulo != null) 1 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** Alto fijo del marcador: que cambie de contenido no mueve el encuadre de la mesa. */
private val ALTO_MARCADOR = 104.dp

private data class ContenidoMarcador(
    /** El total, grande. Siempre en marfil salvo que sea lo único que se muestra. */
    val numero: String? = null,
    val colorNumero: Color = Ivory,
    val titulo: String? = null,
    val colorTitulo: Color = Ivory,
    val detalle: String? = null,
    val esVeredicto: Boolean = false
)

/**
 * Cómo se lee un veredicto de un vistazo: **el número manda**. "Éxito" con total 19 se muestra como
 * un 19 grande en marfil y "Éxito" debajo, en su color. Si el título ya es el número ("54"), va solo;
 * si el sistema no da un total (el Dado Legendario), el título es el titular.
 */
private fun contenidoDe(veredicto: Veredicto): ContenidoMarcador {
    val color = colorDe(veredicto.nivel)
    val detalle = veredicto.detalle.takeIf { it.isNotBlank() }
    val aparte = totalFueraDelTitulo(veredicto.titulo, veredicto.total)
    return when {
        aparte != null -> ContenidoMarcador("$aparte", Ivory, veredicto.titulo, color, detalle, esVeredicto = true)
        veredicto.titulo.matches(Regex("""[+−-]?\d+""")) -> ContenidoMarcador(veredicto.titulo, color, detalle = detalle, esVeredicto = true)
        else -> ContenidoMarcador(titulo = veredicto.titulo, colorTitulo = color, detalle = detalle, esVeredicto = true)
    }
}

/**
 * Modo inmersivo "pegajoso" recomendado por Android para juegos: oculta barra de estado y de
 * navegación; vuelven un momento deslizando desde el borde y se esconden solas.
 */
@Composable
private fun ModoInmersivo(activo: Boolean) {
    val vista = LocalView.current
    DisposableEffect(activo) {
        val ventana = vista.context.buscarActivity()?.window
        val control = ventana?.let { WindowCompat.getInsetsController(it, vista) }
        if (control != null) {
            control.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (activo) control.hide(WindowInsetsCompat.Type.systemBars())
            else control.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { control?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private tailrec fun Context.buscarActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActivity()
    else -> null
}
