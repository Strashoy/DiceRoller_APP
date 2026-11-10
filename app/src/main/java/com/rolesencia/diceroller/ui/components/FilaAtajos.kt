package com.rolesencia.diceroller.ui.components

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
            AssistChip(
                onClick = onHistorial,
                label = { Text("Historial", fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_historial), contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = colores,
                border = AssistChipDefaults.assistChipBorder(true, borderColor = Ivory.copy(alpha = 0.25f)),
                modifier = Modifier.semantics { contentDescription = "Historial y tiradas guardadas" }
            )
        }
        // Las guardadas llevan el borde del acento del ambiente: son "las mías".
        items(guardadas, key = { "g:" + it.nombre }) { tirada ->
            AssistChip(
                onClick = { onLanzarGuardada(tirada) },
                enabled = habilitado,
                label = { Text(tirada.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold) },
                colors = colores,
                border = AssistChipDefaults.assistChipBorder(habilitado, borderColor = acento.copy(alpha = 0.7f)),
                modifier = Modifier.widthIn(max = 168.dp).semantics { contentDescription = "Lanzar ${tirada.nombre}" }
            )
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
                    label = { Text("Guardar esta tirada") },
                    leadingIcon = { Text("+", color = Ivory) },
                    colors = colores,
                    border = AssistChipDefaults.assistChipBorder(true, borderColor = Ivory.copy(alpha = 0.25f))
                )
            }
        }
    }
}

/** Alto de la fila: 48 dp de zona táctil, aunque el atajo se vea de 32. */
private val ALTO_ATAJOS = 48.dp
