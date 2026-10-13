package com.rolesencia.diceroller.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rolesencia.diceroller.domain.reglas.CatalogoSistemas
import com.rolesencia.diceroller.domain.reglas.SistemaReglas

/** Lista de mecánicas de juego con su explicación. Elegir una reemplaza el pool por el sugerido. */
@Composable
fun SelectorSistema(
    actual: SistemaReglas,
    onElegir: (SistemaReglas) -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
        title = { Text("Sistema de juego") },
        text = {
            LazyColumn {
                items(CatalogoSistemas.todos, key = { it.id }) { sistema ->
                    ListItem(
                        headlineContent = {
                            Text(
                                sistema.nombre,
                                fontWeight = if (sistema.id == actual.id) FontWeight.Black else FontWeight.Normal,
                                color = if (sistema.id == actual.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = { Text(sistema.descripcion) },
                        modifier = Modifier
                            .clickable { onElegir(sistema); onCerrar() }
                            .padding(vertical = 2.dp)
                    )
                    HorizontalDivider()
                }
            }
        }
    )
}
