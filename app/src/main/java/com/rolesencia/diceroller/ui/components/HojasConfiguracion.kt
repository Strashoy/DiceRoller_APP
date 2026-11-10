package com.rolesencia.diceroller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.discord.ConfigDiscord
import com.rolesencia.diceroller.domain.reglas.CampoParametro
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import com.rolesencia.diceroller.sensorial.MaterialMesa
import com.rolesencia.diceroller.tirada.ModoDecision
import com.rolesencia.diceroller.ui.theme.DoradoLegendario
import com.rolesencia.diceroller.ui.viewmodel.MetricasTirada
import com.rolesencia.diceroller.ui.viewmodel.PruebaDiscord

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
    onQuitarObjetivo: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ControlesParametros(
                parametros = parametros,
                usaModificador = usaModificador,
                campoObjetivo = campoObjetivo,
                onModificador = onModificador,
                onObjetivo = onObjetivo,
                onQuitarObjetivo = onQuitarObjetivo,
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
        if (campoObjetivo != null) {
            val objetivo = parametros.objetivo ?: campoObjetivo.inicial
            add(if (objetivo != null) "${campoObjetivo.etiqueta} $objetivo" else "sin ${campoObjetivo.etiqueta}")
        }
        when (parametros.conservar) {
            Conservar.MAYOR -> add("con ventaja")
            Conservar.MENOR -> add("con desventaja")
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
    voz: Boolean,
    onVoz: () -> Unit,
    agitar: Boolean,
    onAgitar: () -> Unit,
    zurdo: Boolean,
    onZurdo: () -> Unit,
    discord: ConfigDiscord,
    pruebaDiscord: PruebaDiscord?,
    onDiscord: (ConfigDiscord) -> Unit,
    onProbarDiscord: () -> Unit,
    onCerrar: () -> Unit
) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val alcance = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = estadoHoja,
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
            Seccion("Discord")
            // A media altura el teclado taparía el campo que se está escribiendo: al enfocarlo, la
            // hoja se abre entera y el campo queda a la vista.
            ApartadoDiscord(discord, pruebaDiscord, onDiscord, onProbarDiscord, alEnfocarCampo = { alcance.launch { estadoHoja.expand() } })

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

            Seccion("Lanzar")
            Interruptor("Agitar para lanzar", "Sacudí el teléfono como un cubilete", agitar, onAgitar)
            Interruptor("Controles para zurdos", "«Tirada» y «Dados» cambian de lado", zurdo, onZurdo)

            Seccion("Sonido, vibración y voz")
            Interruptor("Sonido de los dados", "Foley grabado: cada choque suena distinto", audio, onAudio)
            Interruptor("Vibración", "Proporcional a la fuerza de cada golpe", vibracion, onVibracion)
            Interruptor("Leer el resultado en voz alta", "Para tirar sin mirar la pantalla", voz, onVoz)

            Seccion("Pantalla")
            Interruptor("Pantalla completa", "Oculta las barras del sistema; deslizá desde el borde para verlas", pantallaCompleta, onPantallaCompleta)
            Interruptor("Mantener encendida", "Que no se apague en medio de la partida", pantallaEncendida, onPantallaEncendida)

            // Lo técnico va al final: se toca una vez, si se toca. Antes abría la hoja y empujaba
            // zurdos, agitar y sonido fuera de la vista.
            Seccion("Avanzado · quién decide el número")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ModoDecision.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = modo == m,
                        onClick = { onModo(m) },
                        enabled = habilitado,
                        shape = SegmentedButtonDefaults.itemShape(i, ModoDecision.entries.size),
                        // Sin el tilde: con él, "Resultado predeterminado" se cortaba en "Resultado".
                        icon = {},
                        label = { Text(m.titulo, maxLines = 2, fontSize = 13.sp, lineHeight = 15.sp, textAlign = TextAlign.Center) }
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
        }
    }
}

@Composable
internal fun Interruptor(titulo: String, detalle: String, valor: Boolean, onCambio: () -> Unit) {
    // Toda la fila es el interruptor (antes sólo respondía el control de la derecha): un objetivo
    // grande que alcanza cualquiera de los dos pulgares, y que TalkBack anuncia una sola vez.
    ListItem(
        headlineContent = { Text(titulo) },
        supportingContent = { Text(detalle, fontSize = 12.sp) },
        trailingContent = { Switch(checked = valor, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = valor, role = Role.Switch, onValueChange = { onCambio() })
    )
}
