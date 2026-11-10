package com.rolesencia.diceroller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.reglas.CampoParametro
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada

/**
 * Lo que cambia entre tirada y tirada: modificador, objetivo (CD, dificultad…) y ventaja.
 *
 * - Los números se mueven de a 1 y de a 5: llegar a +7 son tres toques (+5, +1, +1), no siete.
 * - Ventaja y desventaja se llaman igual que en el menú de LANZAR y hacen lo mismo.
 * - Si el objetivo es opcional (la CD de D&D), "Sin CD" lo quita y el marcador muestra sólo el total.
 */
@Composable
fun ControlesParametros(
    parametros: ParametrosTirada,
    usaModificador: Boolean,
    campoObjetivo: CampoParametro?,
    onModificador: (Int) -> Unit,
    onObjetivo: (Int) -> Unit,
    onQuitarObjetivo: () -> Unit,
    onConservar: (Conservar) -> Unit,
    habilitado: Boolean
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (usaModificador) {
            Paso("Modificador", conSignoUi(parametros.modificador), conSaltoDeCinco = true, habilitado, onModificador)
        }
        if (campoObjetivo != null) {
            val valor = parametros.objetivo
            Paso(
                etiqueta = campoObjetivo.etiqueta,
                valor = valor?.toString() ?: "—",
                // En rangos cortos (dificultad de 1 a 10) saltar de a 5 no ayuda.
                conSaltoDeCinco = campoObjetivo.rango.last - campoObjetivo.rango.first > 12,
                habilitado = habilitado,
                onDelta = onObjetivo,
                extra = if (campoObjetivo.opcional) {
                    {
                        FilterChip(
                            selected = valor == null,
                            onClick = { if (valor != null) onQuitarObjetivo() },
                            enabled = habilitado,
                            label = { Text("Sin ${campoObjetivo.etiqueta}") }
                        )
                    }
                } else null
            )
            if (campoObjetivo.opcional && valor == null) {
                Text(
                    "Sin ${campoObjetivo.etiqueta} el marcador muestra sólo el total. Tocá + o − para fijarla.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val opciones = listOf(Conservar.TODOS to "Normal", Conservar.MAYOR to "Ventaja", Conservar.MENOR to "Desventaja")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                opciones.forEachIndexed { i, (modo, texto) ->
                    SegmentedButton(
                        selected = parametros.conservar == modo,
                        onClick = { onConservar(modo) },
                        enabled = habilitado,
                        shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                        // Sin el tilde ni el relleno de fábrica: en 272 dp no entraría "Desventaja".
                        icon = {},
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        label = { Text(texto, maxLines = 1, fontSize = 14.sp) }
                    )
                }
            }
            Text(
                "Ventaja se queda con el dado mayor; desventaja, con el menor.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun conSignoUi(n: Int) = if (n >= 0) "+$n" else "−${-n}"

/** Una fila "−5 −1 valor +1 +5" con botones de 48 dp, y su etiqueta arriba. */
@Composable
private fun Paso(
    etiqueta: String,
    valor: String,
    conSaltoDeCinco: Boolean,
    habilitado: Boolean,
    onDelta: (Int) -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(etiqueta, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            extra?.invoke()
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            // 4 botones de 48 + el valor + 4 espacios de 6 = 268 dp: entra en una hoja de 272 (teléfono de 320).
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            if (conSaltoDeCinco) BotonPaso("−5", "$etiqueta: restar 5", habilitado) { onDelta(-5) }
            BotonPaso("−1", "$etiqueta: restar 1", habilitado) { onDelta(-1) }
            Text(
                valor,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 48.dp).padding(horizontal = 2.dp)
            )
            BotonPaso("+1", "$etiqueta: sumar 1", habilitado) { onDelta(+1) }
            if (conSaltoDeCinco) BotonPaso("+5", "$etiqueta: sumar 5", habilitado) { onDelta(+5) }
        }
    }
}

@Composable
private fun BotonPaso(texto: String, descripcion: String, habilitado: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = habilitado,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.widthIn(min = 48.dp).height(48.dp).semantics { contentDescription = descripcion }
    ) { Text(texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1) }
}
