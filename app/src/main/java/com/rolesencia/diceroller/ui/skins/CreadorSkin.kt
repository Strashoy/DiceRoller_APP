package com.rolesencia.diceroller.ui.skins

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.CategoriaSkin
import com.rolesencia.diceroller.domain.apariencia.ColorArgb
import com.rolesencia.diceroller.domain.apariencia.EstiloSkin
import com.rolesencia.diceroller.domain.apariencia.IdeaSkin
import com.rolesencia.diceroller.domain.apariencia.Tipografia
import com.rolesencia.diceroller.ui.mesa3d.Fuentes
import androidx.compose.ui.text.font.FontFamily
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.apariencia.coloresDesdeImagen
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.ui.mesa3d.IconoDado3D
import com.rolesencia.diceroller.ui.theme.DoradoLegendario
import com.rolesencia.diceroller.ui.theme.Ivory
import kotlinx.coroutines.launch

/** Qué color está editando el selector HSL. */
private enum class Objetivo(val titulo: String) { CUERPO("Cuerpo"), TINTA("Números"), BORDE("Aristas"), COLOR2("Color 2") }

/**
 * Creador de skins. Herramientas, de la más directa a la más creativa:
 *  1. Editor de color (tono, saturación, luz y código hex) para cuerpo, números y aristas.
 *  2. Números y aristas automáticos: negro/blanco legible y cuerpo oscurecido.
 *  3. Ideas de armonía (complementario, tríada, pastel, neón, metálico…) a partir del color elegido.
 *  4. Colores tomados de una imagen (selector de fotos): tocar un color de la paleta o usarla entera.
 * Siempre muestra el contraste de los números, para que el dado se pueda leer en la mesa.
 */
@Composable
fun CreadorSkin(
    inicial: SkinDado?,
    base: SkinDado,
    cantidadPersonalizadas: Int,
    onGuardar: (SkinDado) -> Unit,
    onCerrar: () -> Unit
) {
    val partida = inicial ?: base
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "Mi skin ${cantidadPersonalizadas + 1}") }
    var cuerpo by remember { mutableStateOf(partida.cuerpoPara(TipoDado.D6)) }
    var tintaManual by remember { mutableStateOf(partida.tinta) }
    var bordeManual by remember { mutableStateOf(partida.bordePara(TipoDado.D6)) }
    var tintaAuto by remember { mutableStateOf(inicial == null) }
    var bordeAuto by remember { mutableStateOf(inicial?.borde == null) }
    var objetivo by remember { mutableStateOf(Objetivo.CUERPO) }
    var estilo by remember { mutableStateOf(partida.estilo) }
    var color2 by remember { mutableStateOf(partida.color2Para(TipoDado.D6)) }
    var tipografia by remember { mutableStateOf(partida.tipografia) }
    var imagenes by remember { mutableStateOf(partida.imagenes) }
    val idSkin = remember { inicial?.id ?: "mia_${System.currentTimeMillis()}" }
    var paleta by remember { mutableStateOf<ResultadoPaleta?>(null) }
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()

    val tinta = if (tintaAuto) ColorArgb.tintaLegible(cuerpo) else tintaManual
    val borde = if (bordeAuto) ColorArgb.mezclar(cuerpo, 0xFF000000L, 0.45f) else bordeManual
    val borrador = SkinDado(
        id = idSkin,
        nombre = nombre.ifBlank { "Mi skin" },
        categoria = CategoriaSkin.PERSONALIZADAS,
        cuerpo = cuerpo, tinta = tinta, borde = if (bordeAuto) null else borde,
        estilo = estilo, color2 = if (estilo.etiquetaColor2 != null) color2 else null,
        tipografia = tipografia, imagenes = if (estilo == EstiloSkin.IMAGEN) imagenes else emptyList()
    )

    fun asignar(color: Long) {
        when (objetivo) {
            Objetivo.CUERPO -> cuerpo = color
            Objetivo.TINTA -> { tintaManual = color; tintaAuto = false }
            Objetivo.BORDE -> { bordeManual = color; bordeAuto = false }
            Objetivo.COLOR2 -> color2 = color
        }
    }
    val colorObjetivo = when (objetivo) { Objetivo.CUERPO -> cuerpo; Objetivo.TINTA -> tinta; Objetivo.BORDE -> borde; Objetivo.COLOR2 -> color2 }
    val selectorImagenesCara = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) scope.launch { imagenes = guardarImagenesSkin(contexto, uris, idSkin) }
    }

    val selectorImagen = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { paleta = extraerPaleta(contexto, uri) }
    }

    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Creador de skins", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DoradoLegendario)
                OutlinedTextField(nombre, { nombre = it.take(24) }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                // Vista previa en 3D con la misma malla y pintor que la mesa.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(TipoDado.D6, TipoDado.D20, TipoDado.D10, TipoDado.D12).forEach {
                        IconoDado3D(it, RolDado.NORMAL, Modifier.size(72.dp), borrador)
                    }
                }
                val contraste = ColorArgb.contraste(cuerpo, tinta)
                Text(
                    "Contraste de los números: ${"%.1f".format(contraste)} · " +
                        if (contraste >= 4.5) "se leen bien" else if (contraste >= 3) "justo" else "poco legible",
                    color = if (contraste >= 4.5) Color(0xFF35CE8D) else Color(0xFFFFB020),
                    fontSize = 13.sp
                )

                Seccion("1 · Estilo")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EstiloSkin.entries.forEach { e ->
                        FilterChip(selected = estilo == e, onClick = {
                            estilo = e
                            if (e.etiquetaColor2 == null && objetivo == Objetivo.COLOR2) objetivo = Objetivo.CUERPO
                        }, label = { Text(e.titulo) })
                    }
                }
                if (estilo == EstiloSkin.IMAGEN) {
                    Text(
                        "Elegí una imagen para todas las caras, o varias: se reparten en orden por las caras " +
                            "(con 20 imágenes, cada cara del d20 tiene la suya). Los números van encima con contorno.",
                        fontSize = 13.sp
                    )
                    OutlinedButton(onClick = {
                        selectorImagenesCara.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(if (imagenes.isEmpty()) "Elegir imágenes" else "Cambiar imágenes (${imagenes.size})") }
                }

                Seccion("2 · Tipografía")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tipografia.entries.forEach { t ->
                        FilterChip(
                            selected = tipografia == t, onClick = { tipografia = t },
                            label = { Text(t.titulo, fontFamily = FontFamily(Fuentes.de(t))) }
                        )
                    }
                }

                Seccion("3 · Color")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Objetivo.entries.forEach { o ->
                        if (o == Objetivo.COLOR2 && estilo.etiquetaColor2 == null) return@forEach
                        FilterChip(
                            selected = objetivo == o, onClick = { objetivo = o },
                            label = { Text(if (o == Objetivo.COLOR2) estilo.etiquetaColor2!! else o.titulo) }
                        )
                    }
                }
                EditorHsl(colorObjetivo) { asignar(it) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (CatalogoSkins.clasicos.mapNotNull { it.cuerpo } + listOf(ColorArgb.NEGRO, ColorArgb.BLANCO)).distinct().forEach { c ->
                        Circulo(c, seleccionado = c == colorObjetivo) { asignar(c) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(tintaAuto, { tintaAuto = it })
                    Text("  Números automáticos (negro o blanco)", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(bordeAuto, { bordeAuto = it })
                    Text("  Aristas automáticas (cuerpo oscurecido)", fontSize = 13.sp)
                }

                Seccion("4 · Ideas a partir del cuerpo")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IdeaSkin.entries.forEach { idea ->
                        AssistChip(
                            onClick = {
                                val c = idea.aplicar(cuerpo)
                                cuerpo = c.cuerpo; tintaManual = c.tinta; bordeManual = c.borde
                                tintaAuto = false; bordeAuto = false
                            },
                            label = { Text(idea.titulo) }
                        )
                    }
                }
                Text(IdeaSkin.entries.joinToString(" · ") { "${it.titulo}: ${it.explicacion}" }, fontSize = 11.sp, color = Ivory.copy(alpha = 0.6f))

                Seccion("5 · Colores desde una imagen")
                Text(
                    "Elegí una foto o ilustración (un personaje, un paisaje, tu dado favorito) y tocá sus colores " +
                        "para aplicarlos a lo que estés editando, o usá la paleta completa.",
                    fontSize = 13.sp
                )
                OutlinedButton(onClick = {
                    selectorImagen.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Elegir imagen") }
                paleta?.let { p -> PanelPaleta(p, colorObjetivo, onElegir = { asignar(it) }) {
                    val c = coloresDesdeImagen(p.dominante, p.muestras.map { it.color })
                    cuerpo = c.cuerpo; tintaManual = c.tinta; bordeManual = c.borde
                    tintaAuto = false; bordeAuto = false
                } }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    OutlinedButton(onClick = onCerrar) { Text("Cancelar") }
                    Button(onClick = { onGuardar(borrador) }) { Text("Guardar skin") }
                }
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String) {
    Text(titulo, fontWeight = FontWeight.Bold, color = DoradoLegendario, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Circulo(color: Long, seleccionado: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .background(Color(color), CircleShape)
            .border(if (seleccionado) 3.dp else 1.dp, if (seleccionado) DoradoLegendario else Ivory.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick)
    )
}

/** Tono, saturación y luz con sus pistas de color, más el código hex. */
@Composable
private fun EditorHsl(color: Long, onCambio: (Long) -> Unit) {
    val inicial = ColorArgb.aHsl(color)
    var h by remember { mutableFloatStateOf(inicial.h) }
    var s by remember { mutableFloatStateOf(inicial.s) }
    var l by remember { mutableFloatStateOf(inicial.l) }
    var ultimo by remember { mutableStateOf(color) }
    var textoHex by remember { mutableStateOf(ColorArgb.hex(color)) }
    if (color != ultimo) {
        // El color cambió desde afuera (otra pestaña, una idea, la imagen): se resincroniza.
        val nuevo = ColorArgb.aHsl(color)
        h = nuevo.h; s = nuevo.s; l = nuevo.l
        ultimo = color
        textoHex = ColorArgb.hex(color)
    }
    fun emitir() {
        val c = ColorArgb.desdeHsl(h, s, l)
        ultimo = c
        textoHex = ColorArgb.hex(c)
        onCambio(c)
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Pista(Brush.horizontalGradient((0..6).map { Color(ColorArgb.desdeHsl(it * 60f, 1f, 0.5f)) }))
        Slider(h, { h = it; emitir() }, valueRange = 0f..360f)
        Pista(Brush.horizontalGradient(listOf(Color(ColorArgb.desdeHsl(h, 0f, l)), Color(ColorArgb.desdeHsl(h, 1f, l)))))
        Slider(s, { s = it; emitir() })
        Pista(Brush.horizontalGradient(listOf(Color.Black, Color(ColorArgb.desdeHsl(h, s, 0.5f)), Color.White)))
        Slider(l, { l = it; emitir() })
        OutlinedTextField(
            value = textoHex,
            onValueChange = { t ->
                textoHex = t.take(7)
                ColorArgb.desdeHex(t)?.let { c -> ultimo = c; val n = ColorArgb.aHsl(c); h = n.h; s = n.s; l = n.l; onCambio(c) }
            },
            label = { Text("Código hex") },
            singleLine = true
        )
    }
}

@Composable
private fun Pista(brocha: Brush) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(brocha)
    )
}

@Composable
private fun PanelPaleta(paleta: ResultadoPaleta, actual: Long, onElegir: (Long) -> Unit, onUsarTodo: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Image(
            paleta.miniatura, contentDescription = "Imagen de referencia",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp))
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                paleta.muestras.forEach { m -> Circulo(m.color, seleccionado = m.color == actual) { onElegir(m.color) } }
            }
            Button(onClick = onUsarTodo) { Text("Usar la paleta completa") }
        }
    }
}
