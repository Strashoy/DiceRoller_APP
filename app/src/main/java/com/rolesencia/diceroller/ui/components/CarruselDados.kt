package com.rolesencia.diceroller.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.sistemas.SimboloLegendario
import com.rolesencia.diceroller.ui.mesa3d.IconoDado3D
import com.rolesencia.diceroller.ui.mesa3d.SimbolosLegendarios
import com.rolesencia.diceroller.ui.mesa3d.colorCuerpo
import com.rolesencia.diceroller.ui.mesa3d.colorTinta
import com.rolesencia.diceroller.ui.theme.LocalAcento
import com.rolesencia.diceroller.ui.theme.LocalAmbiente
import com.rolesencia.diceroller.ui.theme.Ivory

/**
 * Fila de dados en la zona del pulgar, como la barra de dados de las apps de rol más usadas:
 * **tocar un dado suma uno, mantenerlo quita uno**.
 *
 * - Antes de tirar, cada tipo se ve como su **dado en 3D** (el mismo de la solapa y de la mesa) con
 *   un contador dorado de cuántos hay.
 * - Después de tirar, el número ocupa el lugar del dado: cada dado de ese tipo es una **ficha con su
 *   valor grande**, en el color de su skin, y las fichas del mismo tipo van agrupadas bajo su
 *   nombre. La que decide el resultado lleva borde dorado.
 *
 * La fila se desplaza de costado: el elemento cortado en el borde avisa que hay más.
 */
@Composable
fun CarruselDados(
    pool: List<DadoEnMesa>,
    resultados: List<ResultadoDado>,
    destacados: Set<Int>,
    estaRodando: Boolean,
    skinActual: SkinDado,
    onAgregar: (TipoDado) -> Unit,
    onQuitar: (TipoDado) -> Unit,
    modifier: Modifier = Modifier
) {
    LocalAmbiente.current   // los colores "Por tipo" dependen del ambiente: repintar si cambia
    LazyRow(
        modifier.fillMaxWidth().height(ALTO_FILA),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(TipoDado.entries, key = { it.name }) { tipo ->
            val enMesa = pool.filter { it.tipo == tipo }
            val valores = if (estaRodando) emptyList() else resultados.filter { it.dado.tipo == tipo }
            GrupoTipo(
                tipo = tipo,
                // El ícono toma la skin del dado de ese tipo que ya está en la mesa, o la elegida.
                muestra = enMesa.firstOrNull() ?: DadoEnMesa(-1, tipo, skin = skinActual),
                cantidad = enMesa.size,
                resultados = valores,
                destacados = destacados,
                habilitado = !estaRodando,
                onAgregar = { onAgregar(tipo) },
                onQuitar = { onQuitar(tipo) }
            )
        }
    }
}

/** Alto de la fila: el recuadro del dado más la franja del "−" encima. */
private val ALTO_FILA = 108.dp
private val ALTO_QUITAR = 32.dp
/** Espacio libre entre el "−" y el recuadro: un toque en el borde no cae en "agregar". */
private val SEPARACION_QUITAR = 6.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GrupoTipo(
    tipo: TipoDado,
    muestra: DadoEnMesa,
    cantidad: Int,
    resultados: List<ResultadoDado>,
    destacados: Set<Int>,
    habilitado: Boolean,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit
) {
    val nombre = if (tipo == TipoDado.LEGENDARIO) "Legendario" else tipo.etiqueta
    val conResultado = resultados.isNotEmpty()
    val acento = LocalAcento.current
    // La columna toma el ancho de la ficha (que crece con varios resultados) y la franja del "−" lo llena.
    Column(Modifier.width(IntrinsicSize.Max), horizontalAlignment = Alignment.CenterHorizontally) {
    // "−" encima del recuadro: la pulsación larga no se descubre sola (todo gesto necesita un
    // control a la vista). Toda la franja, a lo ancho de la ficha, es zona de "quitar", y un espacio
    // libre la separa del recuadro: tocar cerca del "−" nunca agrega un dado por error. Los tipos sin
    // dados reservan el lugar para que la fila quede alineada.
    Box(
        Modifier
            .height(ALTO_QUITAR)
            .fillMaxWidth()
            .then(if (cantidad > 0 && habilitado) Modifier.clickable(onClickLabel = "quitar un $nombre", onClick = onQuitar) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (cantidad > 0 && habilitado) Box(
            Modifier.size(24.dp).background(Color(0xFF8E2430), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("−", color = Ivory, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
    Spacer(Modifier.height(SEPARACION_QUITAR))
    Box(
        Modifier
            .heightIn(min = 64.dp)
            .widthIn(min = 56.dp)
            // Los tipos que no están en la mesa se ven apagados: así se lee de un vistazo qué hay.
            .alpha(if (cantidad > 0 || conResultado) 1f else 0.45f)
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            // El borde va por detrás del contenido: el contador queda encima, sin la línea cruzándolo.
            .drawBehind {
                val ancho = (if (cantidad > 0) 1.5.dp else 1.dp).toPx()
                drawRoundRect(
                    color = if (cantidad > 0) acento.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f),
                    topLeft = Offset(ancho / 2f, ancho / 2f),
                    size = Size(size.width - ancho, size.height - ancho),
                    cornerRadius = CornerRadius(14.dp.toPx() - ancho / 2f),
                    style = Stroke(width = ancho)
                )
            }
            .semantics {
                contentDescription = buildString {
                    append(nombre)
                    append(if (cantidad == 1) ", 1 en la mesa" else ", $cantidad en la mesa")
                    if (conResultado) append(", salió ${resultados.joinToString(", ") { textoValor(tipo, it.valor) }}")
                    append(". Tocá para agregar")
                    if (cantidad > 0) append(", mantené para quitar")
                }
            }
            .combinedClickable(
                enabled = habilitado,
                onClickLabel = "agregar un $nombre",
                onClick = onAgregar,
                onLongClickLabel = if (cantidad > 0) "quitar un $nombre" else null,
                onLongClick = if (cantidad > 0) onQuitar else null
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (conResultado) {
                // El número toma el lugar del dado: una ficha por dado, en su color.
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    resultados.forEach { r ->
                        FichaValor(r, destacado = r.dado.id in destacados)
                    }
                }
            } else {
                IconoDado3D(muestra.tipo, muestra.rol, Modifier.size(44.dp), muestra.skin)
            }
            Text(
                if (tipo == TipoDado.LEGENDARIO) "dL" else tipo.etiqueta,
                color = Ivory.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold
            )
        }
        if (cantidad > 0 && !conResultado) Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(20.dp)
                .background(acento, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$cantidad", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
    }
}

/** Un dado ya tirado: su valor grande sobre el color de su skin (el Legendario, con su símbolo). */
@Composable
private fun FichaValor(resultado: ResultadoDado, destacado: Boolean) {
    val dado = resultado.dado
    val borde = if (destacado) BorderStroke(3.dp, LocalAcento.current) else BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    Box(
        Modifier
            .size(width = 38.dp, height = 42.dp)
            .background(colorCuerpo(dado), RoundedCornerShape(10.dp))
            .border(borde, RoundedCornerShape(10.dp))
            // Anillo oscuro por dentro del acento: el resaltado se lee aun sobre dados claros
            // (un acento crema sobre un dado marfil, por ejemplo).
            .then(if (destacado) Modifier.padding(3.dp).border(1.dp, Color.Black.copy(alpha = 0.55f), RoundedCornerShape(7.dp)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (dado.tipo == TipoDado.LEGENDARIO) {
            val simbolo = SimboloLegendario.de(resultado.valor)
            Icon(
                painterResource(SimbolosLegendarios.recurso(simbolo)),
                contentDescription = simbolo.name,
                tint = SimbolosLegendarios.color(simbolo),
                modifier = Modifier.size(26.dp)
            )
        } else {
            val texto = textoValor(dado.tipo, resultado.valor)
            Text(
                texto,
                color = colorTinta(dado),
                fontSize = if (texto.length >= 3) 15.sp else 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}
