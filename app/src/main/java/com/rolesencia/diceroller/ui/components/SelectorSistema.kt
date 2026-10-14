package com.rolesencia.diceroller.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.rolesencia.diceroller.domain.reglas.CatalogoSistemas
import com.rolesencia.diceroller.domain.reglas.SistemaReglas

/**
 * Lista de mecánicas de juego con su explicación. Elegir una reemplaza el pool por el sugerido.
 *
 * Es una hoja inferior (no un diálogo): en el teléfono queda al alcance del pulgar, se cierra
 * deslizando hacia abajo o con el gesto "atrás" predictivo, y deja ver la mesa por arriba.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorSistema(
    actual: SistemaReglas,
    onElegir: (SistemaReglas) -> Unit,
    onCerrar: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        TituloHoja("Sistema de juego")
        LazyColumn {
            items(CatalogoSistemas.todos, key = { it.id }) { sistema ->
                val elegido = sistema.id == actual.id
                ListItem(
                    headlineContent = {
                        Text(
                            sistema.nombre,
                            fontWeight = if (elegido) FontWeight.Black else FontWeight.Normal,
                            color = if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = { Text(sistema.descripcion) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.clickable { onElegir(sistema); onCerrar() }
                )
                HorizontalDivider()
            }
        }
    }
}
