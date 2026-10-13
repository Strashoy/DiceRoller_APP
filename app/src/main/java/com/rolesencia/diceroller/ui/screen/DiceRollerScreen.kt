package com.rolesencia.diceroller.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.sensorial.MaterialMesa
import com.rolesencia.diceroller.ui.skins.CreadorSkin
import com.rolesencia.diceroller.tirada.torre.ModelosTorre
import com.rolesencia.diceroller.tirada.ModoDecision
import com.rolesencia.diceroller.ui.components.ApartadoFisicaReal
import com.rolesencia.diceroller.ui.components.ApartadoPredeterminado
import com.rolesencia.diceroller.ui.components.BotonLanzar
import com.rolesencia.diceroller.ui.components.ControlesParametros
import com.rolesencia.diceroller.ui.components.PanelResultado
import com.rolesencia.diceroller.ui.components.ResumenDados
import com.rolesencia.diceroller.ui.components.SolapaDados
import com.rolesencia.diceroller.ui.components.SelectorSistema
import com.rolesencia.diceroller.ui.mesa3d.MesaDados3D
import com.rolesencia.diceroller.ui.viewmodel.DiceRollerViewModel

@Composable
fun DiceRollerScreen(viewModel: DiceRollerViewModel) {
    val state by viewModel.uiState.collectAsState()
    var mostrarSistemas by remember { mutableStateOf(false) }
    var mostrarCreador by remember { mutableStateOf(false) }
    var skinEnEdicion by remember { mutableStateOf<SkinDado?>(null) }
    val ocupado = state.estaRodando || state.preparando
    val torre = remember(state.modeloTorre, state.mesa) { state.modeloTorre?.let { ModelosTorre.construir(it, state.mesa) } }

    val view = LocalView.current
    DisposableEffect(state.pantallaEncendida) {
        view.keepScreenOn = state.pantallaEncendida
        onDispose { view.keepScreenOn = false }
    }

    if (mostrarSistemas) {
        SelectorSistema(state.sistema, viewModel::seleccionarSistema) { mostrarSistemas = false }
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = { mostrarSistemas = true }, enabled = !ocupado) {
            Text(state.sistema.nombre, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Los dos modos de decisión, cada uno en su apartado.
        PrimaryTabRow(selectedTabIndex = state.modo.ordinal) {
            ModoDecision.entries.forEach { modo ->
                Tab(
                    selected = state.modo == modo,
                    onClick = { viewModel.seleccionarModo(modo) },
                    enabled = !ocupado,
                    text = { Text(modo.titulo) }
                )
            }
        }

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
            modifier = Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(16.dp)),
            modelo = torre
        )
        Text(
            if (torre == null) "Deslizá sobre la mesa para lanzar · desde el borde derecho para agregar dados"
            else "Lanzar suelta los dados por la torre · desde el borde derecho para agregar dados",
            fontSize = 12.sp
        )

        // Dónde se lanzan los dados: la bandeja (DiceTray) o una torre (TowerDice).
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            FilterChip(state.modeloTorre == null, { viewModel.seleccionarLanzamiento(null) }, label = { Text("Bandeja") }, enabled = !ocupado)
            ModelosTorre.ids.forEach { id ->
                FilterChip(
                    selected = state.modeloTorre == id,
                    onClick = { viewModel.seleccionarLanzamiento(id) },
                    label = { Text(ModelosTorre.nombre(id)) },
                    enabled = !ocupado
                )
            }
        }

        PanelResultado(state.veredicto)

        BotonLanzar({ viewModel.lanzar() }, ocupado, habilitado = state.pool.isNotEmpty())

        ResumenDados(
            pool = state.pool,
            resultados = state.resultados,
            destacados = state.veredicto?.dadosDestacados.orEmpty(),
            estaRodando = ocupado,
            onQuitarDado = viewModel::quitarDado,
            modifier = Modifier.fillMaxWidth()
        )

        when (state.modo) {
            ModoDecision.FISICA_REAL -> ApartadoFisicaReal(state.metricas)
            ModoDecision.PREDETERMINADO -> ApartadoPredeterminado(
                pool = state.pool,
                elegirValores = state.elegirValores,
                valoresElegidos = state.valoresElegidos,
                habilitado = !ocupado,
                onToggleElegir = viewModel::toggleElegirValores,
                onCiclarValor = viewModel::ciclarValor,
                metricas = state.metricas
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            FilterChip(state.audioHabilitado, viewModel::toggleAudio, label = { Text("Sonido") })
            FilterChip(state.vibracionHabilitada, viewModel::toggleVibracion, label = { Text("Vibración") })
            FilterChip(state.pantallaEncendida, viewModel::togglePantallaEncendida, label = { Text("Pantalla") })
            if (torre == null) MaterialMesa.entries.forEach { material ->
                FilterChip(
                    selected = state.materialMesa == material,
                    onClick = { viewModel.cambiarMaterialMesa(material) },
                    label = { Text("Mesa: ${material.titulo}") }
                )
            }
        }

        ControlesParametros(
            parametros = state.parametros,
            usaModificador = state.sistema.usaModificador,
            campoObjetivo = state.sistema.campoObjetivo,
            onModificador = viewModel::cambiarModificador,
            onObjetivo = viewModel::cambiarObjetivo,
            onConservar = viewModel::cambiarConservar,
            habilitado = !ocupado
        )

        OutlinedButton(onClick = viewModel::restaurarTiradaSugerida, enabled = !ocupado) {
            Text("Tirada sugerida del sistema")
        }
        Spacer(Modifier.height(24.dp))
    }

    // Solapa de dados: se abre deslizando desde el borde derecho.
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
        onBorrarSkin = viewModel::borrarSkin
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
