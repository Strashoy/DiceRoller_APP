package com.rolesencia.diceroller.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.sensorial.FeedbackTactil
import com.rolesencia.diceroller.ui.mesa3d.IconoDado3D
import com.rolesencia.diceroller.ui.skins.SelectorSkin
import com.rolesencia.diceroller.ui.theme.DoradoLegendario
import com.rolesencia.diceroller.ui.theme.Ivory
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ANCHO_PANEL = 116.dp
private val ANCHO_ASA = 24.dp
private val ALTO_ASA = 180.dp

/**
 * Solapa lateral con los dados. Se abre deslizando desde el borde derecho (o tocando el asa) y
 * se cierra deslizando hacia la derecha, tocando fuera o con "atrás".
 * Los dados van de arriba hacia abajo en orden ascendente (con el Legendario al final) y entran
 * escalonados al abrirse. Tocar un dado lo agrega; el (−) de su esquina izquierda quita uno.
 *
 * Ocupa toda la pantalla: va encima del contenido, como última capa de un Box.
 */
@Composable
fun SolapaDados(
    pool: List<DadoEnMesa>,
    habilitado: Boolean,
    skinActual: SkinDado,
    skinsPersonalizadas: List<SkinDado>,
    especial: Boolean,
    skinsDelSistema: Boolean,
    onAgregar: (TipoDado) -> Unit,
    onQuitar: (TipoDado) -> Unit,
    onElegirSkin: (SkinDado) -> Unit,
    onToggleEspecial: () -> Unit,
    onToggleSkinsDelSistema: () -> Unit,
    onAplicarSkinATodos: () -> Unit,
    onCrearSkin: () -> Unit,
    onEditarSkin: (SkinDado) -> Unit,
    onBorrarSkin: (SkinDado) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Cada vez que cambia (y es > 0) la solapa se abre. Lo usa el botón "Dados" de la barra inferior:
     * el deslizamiento desde el borde sigue funcionando, pero un gesto oculto no debe ser la única
     * forma de llegar a una función (pauta de Material: todo gesto necesita un control visible).
     */
    solicitudApertura: Int = 0
) {
    val anchoPx = with(LocalDensity.current) { ANCHO_PANEL.toPx() }
    // Desplazamiento del panel hacia la derecha: anchoPx = cerrada, 0 = abierta.
    val desplazamiento = remember { Animatable(anchoPx) }
    val scope = rememberCoroutineScope()
    val progreso = (1f - desplazamiento.value / anchoPx).coerceIn(0f, 1f)
    val vista = LocalView.current

    fun asentar(velocidad: Float) {
        val abrir = when {
            velocidad < -600f -> true
            velocidad > 600f -> false
            else -> desplazamiento.value < anchoPx / 2f
        }
        scope.launch {
            desplazamiento.animateTo(
                if (abrir) 0f else anchoPx,
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                initialVelocity = velocidad
            )
        }
    }

    val arrastre = rememberDraggableState { delta ->
        scope.launch { desplazamiento.snapTo((desplazamiento.value + delta).coerceIn(0f, anchoPx)) }
    }

    BackHandler(enabled = progreso > 0.01f) { asentar(1000f) }
    LaunchedEffect(solicitudApertura) { if (solicitudApertura > 0) asentar(-1000f) }

    Box(modifier.fillMaxSize()) {
        // Velo: oscurece el resto y cierra al tocarlo.
        if (progreso > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f * progreso))
                    .pointerInput(Unit) { detectTapGestures { asentar(1000f) } }
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .offset { IntOffset(desplazamiento.value.roundToInt(), 0) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Asa: lo único visible con la solapa cerrada. Se excluye del gesto "atrás" del sistema
            // para que deslizar desde el borde derecho abra la solapa en vez de salir de la app.
            Box(
                Modifier
                    .width(ANCHO_ASA)
                    .height(ALTO_ASA)
                    .systemGestureExclusion()
                    .draggable(arrastre, Orientation.Horizontal, onDragStopped = { asentar(it) })
                    .clickable { asentar(if (progreso > 0.5f) 1000f else -1000f) },
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    Modifier
                        .graphicsLayer { alpha = 1f - progreso }
                        .width(10.dp)
                        .height(ALTO_ASA * 0.55f)
                        .background(DoradoLegendario.copy(alpha = 0.75f), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                )
            }

            Column(
                Modifier
                    .width(ANCHO_PANEL)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                    .draggable(arrastre, Orientation.Horizontal, onDragStopped = { asentar(it) })
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Desplegable de skins: con qué colores se agregan los dados.
                SelectorSkin(
                    skinActual = skinActual,
                    personalizadas = skinsPersonalizadas,
                    especial = especial,
                    skinsDelSistema = skinsDelSistema,
                    habilitado = habilitado,
                    onElegir = onElegirSkin,
                    onToggleEspecial = onToggleEspecial,
                    onToggleSkinsDelSistema = onToggleSkinsDelSistema,
                    onAplicarATodos = onAplicarSkinATodos,
                    onCrear = onCrearSkin,
                    onEditar = onEditarSkin,
                    onBorrar = onBorrarSkin
                )
                val rol = if (especial) RolDado.ESPECIAL else RolDado.NORMAL
                // Orden ascendente de arriba hacia abajo: d4, d6, d8, d10, d12, d20, d%, dF y el Legendario.
                TipoDado.entries.forEachIndexed { i, tipo ->
                    // Cada dado aparece un poco después que el de arriba mientras la solapa se abre.
                    val aparicion = (progreso * 1.8f - i * 0.1f).coerceIn(0f, 1f)
                    val cantidad = pool.count { it.tipo == tipo }
                    Column(
                        Modifier
                            .graphicsLayer {
                                alpha = aparicion
                                translationY = (1f - aparicion) * -24.dp.toPx()
                            }
                            .clickable(enabled = habilitado) {
                                FeedbackTactil.clickBoton(vista)
                                onAgregar(tipo)
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box {
                            IconoDado3D(tipo, rol, Modifier.size(64.dp), skinActual)
                            if (cantidad > 0) {
                                // Del otro lado del contador: quitar un dado de este tipo.
                                Box(
                                    Modifier
                                        .align(Alignment.TopStart)
                                        .size(22.dp)
                                        .background(Color(0xFF8E2430), CircleShape)
                                        .clickable(enabled = habilitado) {
                                            FeedbackTactil.clickBoton(vista)
                                            onQuitar(tipo)
                                        },
                                    contentAlignment = Alignment.Center
                                ) { Text("−", color = Ivory, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .background(DoradoLegendario, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) { Text("$cantidad", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Text(if (tipo == TipoDado.LEGENDARIO) "Legendario" else tipo.etiqueta, color = Ivory, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
