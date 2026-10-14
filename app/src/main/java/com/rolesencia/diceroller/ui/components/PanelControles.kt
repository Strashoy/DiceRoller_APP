package com.rolesencia.diceroller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.reglas.CampoParametro
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada

@Composable
fun ControlesParametros(
    parametros: ParametrosTirada,
    usaModificador: Boolean,
    campoObjetivo: CampoParametro?,
    onModificador: (Int) -> Unit,
    onObjetivo: (Int) -> Unit,
    onConservar: (Conservar) -> Unit,
    habilitado: Boolean
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (usaModificador) {
            Stepper("Modificador", conSignoUi(parametros.modificador), onModificador, habilitado)
        }
        if (campoObjetivo != null) {
            Stepper(campoObjetivo.etiqueta, "${parametros.objetivo ?: campoObjetivo.porDefecto}", onObjetivo, habilitado)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Conservar.TODOS to "Todos", Conservar.MAYOR to "Mayor", Conservar.MENOR to "Menor").forEach { (modo, texto) ->
                FilterChip(
                    selected = parametros.conservar == modo,
                    onClick = { onConservar(modo) },
                    enabled = habilitado,
                    label = { Text(texto) }
                )
            }
        }
    }
}

private fun conSignoUi(n: Int) = if (n >= 0) "+$n" else "$n"

@Composable
private fun Stepper(etiqueta: String, valor: String, onDelta: (Int) -> Unit, habilitado: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(etiqueta)
        OutlinedButton(onClick = { onDelta(-1) }, enabled = habilitado) { Text("−") }
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        OutlinedButton(onClick = { onDelta(+1) }, enabled = habilitado) { Text("+") }
    }
}
