package com.rolesencia.diceroller.ui.screen

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
import com.rolesencia.diceroller.ui.components.FilaFichas
import com.rolesencia.diceroller.ui.components.HojaAjustes
import com.rolesencia.diceroller.ui.components.HojaHistorial
import com.rolesencia.diceroller.ui.components.HojaParametros
import com.rolesencia.diceroller.ui.components.SelectorSistema
import com.rolesencia.diceroller.ui.components.SolapaDados
import com.rolesencia.diceroller.ui.components.resumenParametros
import com.rolesencia.diceroller.ui.mesa3d.MesaDados3D
import com.rolesencia.diceroller.ui.skins.CreadorSkin
import com.rolesencia.diceroller.ui.theme.DarkBlueFantasia
import com.rolesencia.diceroller.ui.theme.Ivory
import com.rolesencia.diceroller.ui.theme.colorDe
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

    // Alto real de las barras superpuestas: la cámara encuadra la mesa en lo que queda libre.
    val densidad = LocalDensity.current
    var altoSuperior by remember { mutableIntStateOf(0) }
    var altoInferior by remember { mutableIntStateOf(0) }
    val areaUtil = with(densidad) { PaddingValues(top = altoSuperior.toDp(), bottom = altoInferior.toDp()) }

    // Mientras ruedan los dados la interfaz se aparta: lo único que importa es la tirada.
    val opacidadControles by animateFloatAsState(if (ocupado) 0.25f else 1f, tween(350), label = "controles")

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
            modifier = Modifier.fillMaxSize(),
            modelo = torre,
            areaUtil = areaUtil
        )

        // --- Arriba: barra + marcador, sobre un degradé que asegura la lectura encima de la mesa ---
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .onSizeChanged { altoSuperior = it.height }
        ) {
            BarraSuperior(
                state = state,
                ocupado = ocupado,
                opacidad = opacidadControles,
                onSistema = { hoja = Hoja.SISTEMA },
                onLanzamiento = viewModel::seleccionarLanzamiento,
                onHistorial = { hoja = Hoja.HISTORIAL },
                onAjustes = { hoja = Hoja.AJUSTES }
            )
            Marcador(state, hayTorre = torre != null)
        }

        // --- Abajo: zona del pulgar ---
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(bottom = 12.dp)
                .onSizeChanged { altoInferior = it.height },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilaFichas(
                pool = state.pool,
                resultados = state.resultados,
                destacados = state.veredicto?.dadosDestacados.orEmpty(),
                estaRodando = ocupado,
                onQuitarDado = { id ->
                    val quitado = state.pool.firstOrNull { it.id == id }
                    viewModel.quitarDado(id)
                    if (quitado != null) alcance.launch {
                        avisos.currentSnackbarData?.dismiss()
                        val r = avisos.showSnackbar(
                            "Quitaste un ${quitado.tipo.etiqueta}", actionLabel = "Deshacer",
                            duration = SnackbarDuration.Short
                        )
                        if (r == SnackbarResult.ActionPerformed) viewModel.restaurarDado(quitado)
                    }
                },
                textoVacio = "Tocá «Dados» para armar la tirada"
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BotonSecundario(
                    icono = R.drawable.ic_parametros,
                    texto = resumenParametros(state.parametros, state.sistema.usaModificador, state.sistema.campoObjetivo),
                    habilitado = !ocupado,
                    modifier = Modifier.weight(1f).alpha(opacidadControles),
                    onClick = { hoja = Hoja.PARAMETROS }
                )
                BotonLanzar(
                    ocupado = ocupado,
                    habilitado = !ocupado && state.pool.isNotEmpty(),
                    conservar = state.parametros.conservar,
                    onLanzar = { FeedbackTactil.clickBoton(vista); viewModel.lanzar() },
                    onLanzarCon = viewModel::lanzarCon
                )
                BotonSecundario(
                    icono = R.drawable.ic_dados,
                    texto = if (state.pool.isEmpty()) "Dados" else "Dados · ${state.pool.size}",
                    habilitado = !ocupado,
                    modifier = Modifier.weight(1f).alpha(opacidadControles),
                    onClick = { abrirSolapa++ }
                )
            }
        }

        // Solapa de dados: deslizando desde el borde derecho o con el botón "Dados".
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
            solicitudApertura = abrirSolapa
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

    val cerrar = { hoja = Hoja.NINGUNA }
    when (hoja) {
        Hoja.NINGUNA -> Unit
        Hoja.SISTEMA -> SelectorSistema(state.sistema, viewModel::seleccionarSistema, cerrar)
        Hoja.HISTORIAL -> HojaHistorial(
            historial = state.historial,
            guardadas = state.tiradasGuardadas,
            notacionActual = Notacion.de(state.pool, if (state.sistema.usaModificador) state.parametros.modificador else 0),
            habilitado = !ocupado,
            onRepetir = viewModel::repetir,
            onBorrarHistorial = viewModel::borrarHistorial,
            onLanzarGuardada = viewModel::lanzarGuardada,
            onGuardarActual = viewModel::guardarTiradaActual,
            onBorrarGuardada = viewModel::borrarTiradaGuardada,
            onCerrar = cerrar
        )
        Hoja.PARAMETROS -> HojaParametros(
            parametros = state.parametros,
            usaModificador = state.sistema.usaModificador,
            campoObjetivo = state.sistema.campoObjetivo,
            habilitado = !ocupado,
            onModificador = viewModel::cambiarModificador,
            onObjetivo = viewModel::cambiarObjetivo,
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
    onHistorial: () -> Unit,
    onAjustes: () -> Unit
) {
    var menuLanzamiento by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).alpha(opacidad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sistema de juego: es lo que define cómo se lee la tirada, por eso va primero y con nombre.
        // Ocupa el espacio libre y empuja lo demás a la derecha; si el nombre es largo, se recorta.
        Box(Modifier.weight(1f)) {
            PildoraSuperior(
                icono = R.drawable.ic_sistema,
                texto = state.sistema.nombre,
                habilitado = !ocupado,
                onClick = onSistema
            )
        }
        Box {
            PildoraSuperior(
                icono = R.drawable.ic_torre,
                texto = state.modeloTorre?.let { ModelosTorre.nombre(it) } ?: "Bandeja",
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
        IconButton(onClick = onHistorial) {
            Icon(painterResource(R.drawable.ic_historial), contentDescription = "Historial y tiradas guardadas", tint = Ivory)
        }
        IconButton(onClick = onAjustes) {
            Icon(painterResource(R.drawable.ic_ajustes), contentDescription = "Ajustes", tint = Ivory)
        }
    }
}

/** Botón tipo "píldora" translúcido: se lee sobre la mesa sin taparla. */
@Composable
private fun PildoraSuperior(
    @DrawableRes icono: Int,
    texto: String,
    habilitado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = habilitado,
        contentPadding = PaddingValues(horizontal = 14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color.Black.copy(alpha = 0.45f),
            contentColor = Ivory
        ),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text(" ▾", color = Ivory.copy(alpha = 0.6f))
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
    onLanzar: () -> Unit,
    onLanzarCon: (Conservar) -> Unit
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
        }
    }
}

@Composable
private fun BotonSecundario(
    @DrawableRes icono: Int,
    texto: String,
    habilitado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            contentColor = Ivory
        ),
        modifier = modifier.height(64.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(22.dp))
            Text(texto, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Marcador: el veredicto en grande, con el color de su nivel. Tiene altura fija para que aparecer y
 * desaparecer no cambie el área de la mesa. Antes de la primera tirada muestra una pista de uso, y
 * TalkBack anuncia cada veredicto nuevo (región "viva").
 */
@Composable
private fun Marcador(state: DiceRollerUiState, hayTorre: Boolean) {
    val contenido: Pair<String, String?> = when {
        state.preparando -> "Pre-simulando la tirada…" to null
        state.estaRodando -> "Rodando…" to null
        state.veredicto != null -> state.veredicto.titulo to state.veredicto.detalle.takeIf { it.isNotBlank() }
        state.pool.isEmpty() -> "Mesa vacía" to "Deslizá desde el borde derecho o tocá «Dados»"
        // Las primeras veces, la pista de uso; después, la tirada en notación ("1d20 + 5").
        state.historial.size < 3 && hayTorre -> "Listo para lanzar" to "LANZAR suelta los dados por la torre · mantenelo para ventaja"
        state.historial.size < 3 -> "Listo para lanzar" to "Deslizá sobre la mesa, agitá el teléfono o mantené LANZAR para ventaja"
        else -> Notacion.de(state.pool, if (state.sistema.usaModificador) state.parametros.modificador else 0) to state.sistema.nombre
    }
    val color = state.veredicto?.takeIf { !state.estaRodando && !state.preparando }?.let { colorDe(it.nivel) } ?: Ivory
    val esVeredicto = state.veredicto != null && !state.estaRodando && !state.preparando
    AnimatedContent(
        targetState = contenido,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
        label = "marcador",
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .padding(horizontal = 20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) { (titulo, detalle) ->
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                titulo,
                color = color,
                fontSize = if (esVeredicto) 30.sp else 18.sp,
                fontWeight = if (esVeredicto) FontWeight.Black else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (detalle != null) {
                Text(
                    detalle,
                    color = Ivory.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
