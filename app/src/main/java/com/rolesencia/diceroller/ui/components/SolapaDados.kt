package com.rolesencia.diceroller.ui.components

import com.rolesencia.diceroller.ui.theme.LocalAcento
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
import androidx.compose.material3.HorizontalDivider
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

/**
 * Solapa lateral con los dados, el Legendario, los especiales y las skins. Se abre con el botón
 * "Dados" de la barra inferior y se cierra deslizando hacia la derecha, tocando fuera o con "atrás".
 * No tiene manija ni se abre desde el borde: con la navegación por gestos de Android, deslizar
 * desde un borde lateral es "Atrás", y el sistema sólo deja excluir los 200 dp de abajo de cada borde.
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
    /** Cada vez que cambia (y es > 0) la solapa se abre. Lo usa el botón "Dados" de la barra inferior. */
    solicitudApertura: Int = 0,
    /** Controles para zurdos: el botón "Dados" está a la izquierda y la solapa sale de ese mismo lado. */
    aLaIzquierda: Boolean = false
) {
    val anchoPx = with(LocalDensity.current) { ANCHO_PANEL.toPx() }
    // Cuánto le falta al panel para estar abierto: anchoPx = cerrada (fuera de la pantalla), 0 = abierta.
    // Hacia qué lado se esconde lo decide [haciaAfuera]: +1 a la derecha, −1 a la izquierda.
    val desplazamiento = remember { Animatable(anchoPx) }
    val haciaAfuera = if (aLaIzquierda) -1f else 1f
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
        scope.launch { desplazamiento.snapTo((desplazamiento.value + delta * haciaAfuera).coerceIn(0f, anchoPx)) }
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
                .align(if (aLaIzquierda) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight()
                .offset { IntOffset((desplazamiento.value * haciaAfuera).roundToInt(), 0) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // El panel: los dados se desplazan arriba y las skins quedan fijas abajo.
            Column(
                Modifier
                    .width(ANCHO_PANEL)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        if (aLaIzquierda) RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
                        else RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
                    .draggable(arrastre, Orientation.Horizontal, onDragStopped = { asentar(it * haciaAfuera) })
                    .safeDrawingPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                                        .background(LocalAcento.current, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) { Text("$cantidad", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Text(if (tipo == TipoDado.LEGENDARIO) "Legendario" else tipo.etiqueta, color = Ivory, fontSize = 12.sp)
                    }
                }
            }
            // Skins: con qué colores se agregan los dados. Abajo, donde llega el pulgar (antes estaba
            // arriba de todo, al 11 % de la altura); su menú se despliega hacia arriba.
            HorizontalDivider(color = Ivory.copy(alpha = 0.12f))
            Box(Modifier.padding(vertical = 8.dp)) {
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
            }
            }
        }
    }
}
