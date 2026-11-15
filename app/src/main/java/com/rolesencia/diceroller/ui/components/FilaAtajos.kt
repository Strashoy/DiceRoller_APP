package com.rolesencia.diceroller.ui.components

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.historial.IndicioHistorial
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rolesencia.diceroller.R
import com.rolesencia.diceroller.domain.historial.EntradaHistorial
import com.rolesencia.diceroller.domain.historial.TiradaGuardada
import com.rolesencia.diceroller.ui.theme.Ivory
import com.rolesencia.diceroller.ui.theme.LocalAcento

/**
 * Atajos en la franja del pulgar, encima de los dados: lo que se repite durante una partida queda a
 * **un toque** y sin estirar la mano.
 *
 * - **Historial** siempre primero y en el mismo lugar (antes era un ícono arriba de todo).
 * - Las **tiradas guardadas** ("Espada larga"): tocar una la carga y la lanza.
 * - Las últimas **tiradas distintas** de la que está en la mesa, con su notación: sirven para alternar
 *   ataque y daño sin guardar nada.
 * - Si todavía no hay ninguna guardada, un atajo que lleva a guardar la mesa actual.
 *
 * Tocar un atajo lanza de inmediato, igual que "Otra vez" y "Lanzar" en la hoja del historial.
 */
@Composable
fun FilaAtajos(
    guardadas: List<TiradaGuardada>,
    recientes: List<EntradaHistorial>,
    habilitado: Boolean,
    onHistorial: () -> Unit,
    /** Cuántas tiradas hay en el historial (el contador del chip). */
    tiradasEnHistorial: Int,
    /** La persona todavía no abrió el historial: el chip se resalta hasta que lo haga. */
    resaltarHistorial: Boolean,
    onGuardar: () -> Unit,
    onLanzarGuardada: (TiradaGuardada) -> Unit,
    onRepetir: (EntradaHistorial) -> Unit,
    modifier: Modifier = Modifier
) {
    val colores = AssistChipDefaults.assistChipColors(
        containerColor = Color.Black.copy(alpha = 0.5f),
        labelColor = Ivory,
        leadingIconContentColor = Ivory,
        disabledContainerColor = Color.Black.copy(alpha = 0.3f),
        disabledLabelColor = Ivory.copy(alpha = 0.45f),
        disabledLeadingIconContentColor = Ivory.copy(alpha = 0.45f)
    )
    val acento = LocalAcento.current
    LazyRow(
        modifier.fillMaxWidth().height(ALTO_ATAJOS),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "historial") {
            val contador = IndicioHistorial.contador(tiradasEnHistorial)
            // Mientras no lo abrió, el contador late y el borde toma el acento: "acá queda todo".
            val latido by rememberInfiniteTransition(label = "latidoHistorial").animateFloat(
                initialValue = 1f, targetValue = 0.35f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "latido"
            )
            AssistChip(
                onClick = onHistorial,
                enabled = habilitado,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Historial", fontWeight = FontWeight.SemiBold)
                        if (contador != null) {
                            Box(
                                Modifier
                                    .padding(start = 8.dp)
                                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                    .alpha(if (resaltarHistorial && habilitado) latido else 1f)
                                    .background(acento, CircleShape)
                                    .padding(horizontal = 5.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(contador, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_historial), contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = colores,
                border = if (resaltarHistorial) AssistChipDefaults.assistChipBorder(habilitado, borderColor = acento, borderWidth = 2.dp)
                else AssistChipDefaults.assistChipBorder(habilitado, borderColor = Ivory.copy(alpha = 0.25f)),
                modifier = Modifier.semantics {
                    contentDescription = "Historial y tiradas guardadas" + (if (contador != null) ", $contador tiradas" else "")
                }
            )
        }
        // Las guardadas llevan el borde del acento del ambiente: son "las mías".
        items(guardadas, key = { "g:" + it.nombre }) { tirada ->
            ChipGuardada(tirada.nombre, habilitado, acento, onClick = { onLanzarGuardada(tirada) })
        }
        items(recientes, key = { "r:" + it.instante }) { entrada ->
            AssistChip(
                onClick = { onRepetir(entrada) },
                enabled = habilitado,
                label = { Text(entrada.notacion, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Text("↻", color = Ivory.copy(alpha = if (habilitado) 0.9f else 0.45f)) },
                colors = colores,
                border = AssistChipDefaults.assistChipBorder(habilitado, borderColor = Ivory.copy(alpha = 0.25f)),
                modifier = Modifier.widthIn(max = 168.dp).semantics { contentDescription = "Repetir ${entrada.notacion}" }
            )
        }
        if (guardadas.isEmpty()) {
            item(key = "guardar") {
                AssistChip(
                    onClick = onGuardar,
                    enabled = habilitado,
                    label = { Text("Guardar esta tirada") },
                    leadingIcon = { Text("+", color = Ivory) },
                    colors = colores,
                    border = AssistChipDefaults.assistChipBorder(habilitado, borderColor = Ivory.copy(alpha = 0.25f))
                )
            }
        }
    }
}

/** Alto de la fila: 48 dp de zona táctil, aunque el atajo se vea de 32. */
private val ALTO_ATAJOS = 48.dp

/**
 * Atajo de una tirada guardada. El nombre cabe en hasta ~28 letras; si aun así se corta, mantenerlo
 * apretado lo muestra completo (un chip de Material no tiene toque largo, por eso es propio).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChipGuardada(nombre: String, habilitado: Boolean, acento: Color, onClick: () -> Unit) {
    val contexto = LocalContext.current
    val tinta = if (habilitado) Ivory else Ivory.copy(alpha = 0.45f)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = if (habilitado) 0.5f else 0.3f),
        border = BorderStroke(1.dp, acento.copy(alpha = if (habilitado) 0.7f else 0.25f)),
        modifier = Modifier
            .widthIn(max = 232.dp)
            .minimumInteractiveComponentSize()   // 48 dp de zona táctil, como los demás chips
            .height(32.dp)
            .semantics { contentDescription = "Lanzar $nombre" }
            .combinedClickable(
                enabled = habilitado,
                onClick = onClick,
                onLongClick = { Toast.makeText(contexto, nombre, Toast.LENGTH_SHORT).show() }
            )
    ) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(nombre, color = tinta, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
