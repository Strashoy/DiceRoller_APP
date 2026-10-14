package com.rolesencia.diceroller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.reglas.CampoParametro
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import com.rolesencia.diceroller.sensorial.MaterialMesa
import com.rolesencia.diceroller.tirada.ModoDecision
import com.rolesencia.diceroller.ui.theme.DoradoLegendario
import com.rolesencia.diceroller.ui.viewmodel.MetricasTirada

/** Encabezado común de las hojas inferiores. */
@Composable
internal fun TituloHoja(texto: String) {
    Text(
        texto,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = DoradoLegendario,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun Seccion(titulo: String) {
    Text(
        titulo.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

/**
 * Lo que cambia de una tirada a otra (modificador, dificultad, ventaja). Va en su propia hoja, a un
 * toque del botón de lanzar: es lo que el jugador toca entre tirada y tirada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaParametros(
    parametros: ParametrosTirada,
    usaModificador: Boolean,
    campoObjetivo: CampoParametro?,
    habilitado: Boolean,
    onModificador: (Int) -> Unit,
    onObjetivo: (Int) -> Unit,
    onConservar: (Conservar) -> Unit,
    onTiradaSugerida: () -> Unit,
    onCerrar: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        TituloHoja("Tirada")
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ControlesParametros(
                parametros = parametros,
                usaModificador = usaModificador,
                campoObjetivo = campoObjetivo,
                onModificador = onModificador,
                onObjetivo = onObjetivo,
                onConservar = onConservar,
                habilitado = habilitado
            )
            OutlinedButton(onClick = { onTiradaSugerida(); onCerrar() }, enabled = habilitado) {
                Text("Volver a la tirada sugerida del sistema")
            }
        }
    }
}

/** Resumen de una línea de los parámetros, para el botón de la barra inferior. */
fun resumenParametros(parametros: ParametrosTirada, usaModificador: Boolean, campoObjetivo: CampoParametro?): String {
    val partes = buildList {
        if (usaModificador) add(if (parametros.modificador >= 0) "+${parametros.modificador}" else "${parametros.modificador}")
        if (campoObjetivo != null) add("${campoObjetivo.etiqueta} ${parametros.objetivo ?: campoObjetivo.porDefecto}")
        when (parametros.conservar) {
            Conservar.MAYOR -> add("Mayor")
            Conservar.MENOR -> add("Menor")
            Conservar.TODOS -> Unit
        }
    }
    return if (partes.isEmpty()) "Tirada" else partes.joinToString(" · ")
}

/**
 * Ajustes que se tocan poco: modo de decisión, mesa, sonido y pantalla. Antes estaban todos
 * apilados debajo de la mesa; ahora la mesa ocupa la pantalla y estos viven detrás del engranaje.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaAjustes(
    modo: ModoDecision,
    habilitado: Boolean,
    onModo: (ModoDecision) -> Unit,
    pool: List<DadoEnMesa>,
    elegirValores: Boolean,
    valoresElegidos: Map<Int, Int>,
    onToggleElegir: () -> Unit,
    onCiclarValor: (Int) -> Unit,
    metricas: MetricasTirada?,
    hayTorre: Boolean,
    materialMesa: MaterialMesa,
    onMaterialMesa: (MaterialMesa) -> Unit,
    audio: Boolean,
    onAudio: () -> Unit,
    vibracion: Boolean,
    onVibracion: () -> Unit,
    pantallaEncendida: Boolean,
    onPantallaEncendida: () -> Unit,
    pantallaCompleta: Boolean,
    onPantallaCompleta: () -> Unit,
    onCerrar: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        TituloHoja("Ajustes")
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Seccion("Quién decide el número")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ModoDecision.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = modo == m,
                        onClick = { onModo(m) },
                        enabled = habilitado,
                        shape = SegmentedButtonDefaults.itemShape(i, ModoDecision.entries.size),
                        label = { Text(m.titulo, maxLines = 1) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when (modo) {
                ModoDecision.FISICA_REAL -> ApartadoFisicaReal(metricas)
                ModoDecision.PREDETERMINADO -> ApartadoPredeterminado(
                    pool = pool,
                    elegirValores = elegirValores,
                    valoresElegidos = valoresElegidos,
                    habilitado = habilitado,
                    onToggleElegir = onToggleElegir,
                    onCiclarValor = onCiclarValor,
                    metricas = metricas
                )
            }

            if (!hayTorre) {
                Seccion("Superficie de la bandeja")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MaterialMesa.entries.forEach { material ->
                        FilterChip(
                            selected = materialMesa == material,
                            onClick = { onMaterialMesa(material) },
                            label = { Text(material.titulo) }
                        )
                    }
                }
            }

            Seccion("Sonido y vibración")
            Interruptor("Sonido de los dados", "Foley grabado: cada choque suena distinto", audio, onAudio)
            Interruptor("Vibración", "Proporcional a la fuerza de cada golpe", vibracion, onVibracion)

            Seccion("Pantalla")
            Interruptor("Pantalla completa", "Oculta las barras del sistema; deslizá desde el borde para verlas", pantallaCompleta, onPantallaCompleta)
            Interruptor("Mantener encendida", "Que no se apague en medio de la partida", pantallaEncendida, onPantallaEncendida)
        }
    }
}

@Composable
private fun Interruptor(titulo: String, detalle: String, valor: Boolean, onCambio: () -> Unit) {
    ListItem(
        headlineContent = { Text(titulo) },
        supportingContent = { Text(detalle, fontSize = 12.sp) },
        trailingContent = { Switch(checked = valor, onCheckedChange = { onCambio() }) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    )
}
