package com.rolesencia.diceroller.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.historial.EntradaHistorial
import com.rolesencia.diceroller.domain.historial.Notacion
import com.rolesencia.diceroller.domain.historial.TiradaGuardada
import com.rolesencia.diceroller.domain.reglas.CatalogoSistemas
import com.rolesencia.diceroller.ui.theme.Ivory
import com.rolesencia.diceroller.ui.theme.colorDe

/**
 * Historial y tiradas guardadas en una sola hoja, con dos pestañas:
 * - **Historial** (registro de Dice by PCalc, "game log" de D&D Beyond): cada tirada con su
 *   notación, valores y veredicto en color; "Otra vez" la repite tal cual.
 * - **Guardadas** (presets con nombre de PCalc y Dice Roller PRO+): la mesa actual se guarda con un
 *   nombre ("Espada larga") y tocarla la carga y la lanza.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaHistorial(
    historial: List<EntradaHistorial>,
    guardadas: List<TiradaGuardada>,
    notacionActual: String,
    habilitado: Boolean,
    onRepetir: (EntradaHistorial) -> Unit,
    onBorrarHistorial: () -> Unit,
    onLanzarGuardada: (TiradaGuardada) -> Unit,
    onGuardarActual: (String) -> Unit,
    onBorrarGuardada: (TiradaGuardada) -> Unit,
    onCerrar: () -> Unit
) {
    var pestana by remember { mutableIntStateOf(if (historial.isEmpty() && guardadas.isNotEmpty()) 1 else 0) }
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        PrimaryTabRow(selectedTabIndex = pestana, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            Tab(pestana == 0, { pestana = 0 }, text = { Text("Historial") })
            Tab(pestana == 1, { pestana = 1 }, text = { Text("Guardadas · ${guardadas.size}") })
        }
        when (pestana) {
            0 -> PestanaHistorial(historial, habilitado, onRepetir = { onRepetir(it); onCerrar() }, onBorrarHistorial)
            else -> PestanaGuardadas(
                guardadas, notacionActual, habilitado,
                onLanzar = { onLanzarGuardada(it); onCerrar() },
                onGuardar = onGuardarActual,
                onBorrar = onBorrarGuardada
            )
        }
    }
}

@Composable
private fun PestanaHistorial(
    historial: List<EntradaHistorial>,
    habilitado: Boolean,
    onRepetir: (EntradaHistorial) -> Unit,
    onBorrar: () -> Unit
) {
    if (historial.isEmpty()) {
        Vacio("Todavía no hay tiradas", "Cada tirada queda acá con su resultado, para repetirla con un toque.")
        return
    }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
        items(historial, key = { it.instante }) { e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // El punto de color repite el del marcador: se reconoce de un vistazo cómo salió.
                Box(Modifier.size(12.dp).background(colorDe(e.nivel), CircleShape))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(e.titulo, color = colorDe(e.nivel), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Text(hace(e.instante), fontSize = 11.sp, color = Ivory.copy(alpha = 0.55f))
                    }
                    Text("${e.notacion}  →  ${e.valores}", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (e.detalle.isNotBlank()) Text(e.detalle, fontSize = 12.sp, color = Ivory.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = { onRepetir(e) }, enabled = habilitado) { Text("Otra vez") }
            }
            HorizontalDivider(color = Ivory.copy(alpha = 0.08f))
        }
        item {
            TextButton(onClick = onBorrar, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Borrar historial", color = Ivory.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun PestanaGuardadas(
    guardadas: List<TiradaGuardada>,
    notacionActual: String,
    habilitado: Boolean,
    onLanzar: (TiradaGuardada) -> Unit,
    onGuardar: (String) -> Unit,
    onBorrar: (TiradaGuardada) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Guardar la mesa actual: $notacionActual", fontSize = 13.sp, color = Ivory.copy(alpha = 0.75f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it.take(40) },
                placeholder = { Text("Ej.: Espada larga") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(
                onClick = { onGuardar(nombre); nombre = "" },
                enabled = nombre.isNotBlank(),
                modifier = Modifier.heightIn(min = 56.dp)
            ) { Text("Guardar") }
        }
    }
    if (guardadas.isEmpty()) {
        Vacio("Sin tiradas guardadas", "Guardá las que repetís siempre: el ataque de tu espada, una tirada de daño, la de iniciativa.")
        return
    }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
        items(guardadas, key = { it.nombre }) { t ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clickable(enabled = habilitado) { onLanzar(t) }
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(t.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        Notacion.de(t.pool, t.parametros.modificador) + " · " + CatalogoSistemas.porId(t.sistemaId).nombre,
                        fontSize = 12.sp, color = Ivory.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Text("Lanzar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(
                    onClick = { onBorrar(t) },
                    modifier = Modifier.semantics { contentDescription = "Borrar ${t.nombre}" }
                ) { Text("✕", color = Ivory.copy(alpha = 0.6f)) }
            }
            HorizontalDivider(color = Ivory.copy(alpha = 0.08f))
        }
    }
}

@Composable
private fun Vacio(titulo: String, texto: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(texto, fontSize = 13.sp, color = Ivory.copy(alpha = 0.7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** "hace 3 min", "ayer"… en el idioma del teléfono. */
private fun hace(instante: Long): String =
    DateUtils.getRelativeTimeSpanString(instante, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
