package com.rolesencia.diceroller.ui.skins

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.CategoriaSkin
import com.rolesencia.diceroller.domain.apariencia.EstiloSkin
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.apariencia.Tipografia
import com.rolesencia.diceroller.ui.mesa3d.Fuentes
import com.rolesencia.diceroller.ui.mesa3d.ImagenesSkins
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.ui.theme.DoradoLegendario
import com.rolesencia.diceroller.ui.theme.Ivory

/** Pincel que resume el estilo de una skin en un círculo (bicolor partido, degradado, gema, metal…). */
fun pincelMuestra(skin: SkinDado): Brush {
    val c1 = Color(skin.cuerpoPara(TipoDado.D6))
    val c2 = Color(skin.color2Para(TipoDado.D6))
    return when (skin.estilo) {
        EstiloSkin.BICOLOR -> Brush.horizontalGradient(0f to c1, 0.5f to c1, 0.5f to c2, 1f to c2)
        EstiloSkin.DEGRADADO -> Brush.verticalGradient(listOf(c1, c2))
        EstiloSkin.GEMA -> Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), c1, c1.copy(alpha = 0.6f)))
        EstiloSkin.METALICO -> Brush.linearGradient(listOf(c1, Color.White, c1, Color.Black.copy(alpha = 0.6f)))
        EstiloSkin.MARMOL -> Brush.linearGradient(0f to c1, 0.45f to c1, 0.5f to c2, 0.55f to c1, 1f to c1)
        EstiloSkin.PURPURINA -> Brush.radialGradient(listOf(c2, c1, c1), radius = 18f)
        EstiloSkin.BRILLO_NOCTURNO -> Brush.radialGradient(listOf(c1, c1, c2))
        else -> Brush.linearGradient(listOf(c1, c1))
    }
}

/** Muestra de una skin: círculo con su estilo y un "6" (o "VI") en la tinta y tipografía de la skin. */
@Composable
fun MuestraSkin(skin: SkinDado, tamano: Dp = 26.dp) {
    Box(
        Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(pincelMuestra(skin))
            .border(2.dp, Color(if (skin.estilo == EstiloSkin.BRILLO_NOCTURNO) skin.color2Para(TipoDado.D6) else skin.bordePara(TipoDado.D6)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (skin.estilo == EstiloSkin.IMAGEN && skin.imagenes.isNotEmpty()) {
            ImagenesSkins.bitmap(skin.imagenes.first())?.let {
                Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Text(
            if (skin.tipografia == Tipografia.ROMANA) "VI" else "6",
            color = Color(skin.tintaEfectiva(skin.cuerpoPara(TipoDado.D6))),
            fontSize = (tamano.value * if (skin.tipografia == Tipografia.ROMANA) 0.36f else 0.5f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Fuentes.de(skin.tipografia))
        )
    }
}

/**
 * Desplegable de la solapa: elige con qué skin se agregan los dados. Incluye las opciones de
 * marcar los próximos dados como especiales, pintar los que ya están en la mesa y crear una skin.
 */
@Composable
fun SelectorSkin(
    skinActual: SkinDado,
    personalizadas: List<SkinDado>,
    especial: Boolean,
    skinsDelSistema: Boolean,
    habilitado: Boolean,
    onElegir: (SkinDado) -> Unit,
    onToggleEspecial: () -> Unit,
    onToggleSkinsDelSistema: () -> Unit,
    onAplicarATodos: () -> Unit,
    onCrear: () -> Unit,
    onEditar: (SkinDado) -> Unit,
    onBorrar: (SkinDado) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        Column(
            Modifier
                .clickable(enabled = habilitado) { abierto = true }
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MuestraSkin(skinActual)
                Text(" ▾", color = DoradoLegendario)
            }
            Text(skinActual.nombre, fontSize = 11.sp, color = Ivory, textAlign = TextAlign.Center, maxLines = 2)
            if (especial) Text("especial", fontSize = 10.sp, color = Color(0xFFFF5252))
        }

        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(
                text = { Text("✦ Crear skin…", color = DoradoLegendario, fontWeight = FontWeight.Bold) },
                onClick = { abierto = false; onCrear() }
            )
            DropdownMenuItem(
                text = { Text("Pintar los dados de la mesa") },
                onClick = { abierto = false; onAplicarATodos() }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = especial, onCheckedChange = null)
                        Column {
                            Text("Agregar como especial")
                            Text("Hambre, Miedo, Estrés… (aristas rojas)", fontSize = 11.sp, color = Ivory.copy(alpha = 0.6f))
                        }
                    }
                },
                onClick = onToggleEspecial
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = skinsDelSistema, onCheckedChange = null)
                        Column {
                            Text("Skins del sistema de juego")
                            Text("V5: Hambre · Daggerheart: Esperanza y Miedo…", fontSize = 11.sp, color = Ivory.copy(alpha = 0.6f))
                        }
                    }
                },
                onClick = onToggleSkinsDelSistema
            )
            val grupos = listOf(
                CategoriaSkin.PERSONALIZADAS to personalizadas,
                CategoriaSkin.POR_TIPO to listOf(CatalogoSkins.POR_TIPO),
                CategoriaSkin.CLASICOS to CatalogoSkins.clasicos,
                CategoriaSkin.ESPECIALES to CatalogoSkins.especiales,
                CategoriaSkin.GEMAS to CatalogoSkins.gemas,
                CategoriaSkin.METALES to CatalogoSkins.metales,
                CategoriaSkin.TEMATICOS to CatalogoSkins.tematicos,
                CategoriaSkin.SISTEMA to CatalogoSkins.porSistema
            )
            grupos.filter { it.second.isNotEmpty() }.forEach { (categoria, skins) ->
                HorizontalDivider()
                Text(
                    categoria.titulo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DoradoLegendario,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
                )
                skins.forEach { skin ->
                    DropdownMenuItem(
                        leadingIcon = { MuestraSkin(skin, 24.dp) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    skin.nombre,
                                    fontWeight = if (skin.id == skinActual.id) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.width(120.dp)
                                )
                                if (skin.esPersonalizada) {
                                    Spacer(Modifier.width(4.dp))
                                    Text("✎", modifier = Modifier.clickable { abierto = false; onEditar(skin) }.padding(6.dp))
                                    Text("✕", color = Color(0xFFFF5252), modifier = Modifier.clickable { onBorrar(skin) }.padding(6.dp))
                                }
                            }
                        },
                        trailingIcon = { if (skin.id == skinActual.id) Text("✓", color = DoradoLegendario) },
                        onClick = { abierto = false; onElegir(skin) }
                    )
                }
            }
        }
    }
}
