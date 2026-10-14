package com.rolesencia.diceroller.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.rolesencia.diceroller.domain.reglas.sistemas.SimboloLegendario
import com.rolesencia.diceroller.ui.mesa3d.SimbolosLegendarios
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.ui.mesa3d.colorCuerpo
import com.rolesencia.diceroller.ui.mesa3d.colorTinta
import com.rolesencia.diceroller.ui.theme.DoradoLegendario
import com.rolesencia.diceroller.ui.theme.Ivory

internal fun textoValor(tipo: TipoDado, valor: Int): String = when (tipo) {
    TipoDado.FATE -> when { valor > 0 -> "+"; valor < 0 -> "−"; else -> "0" }
    TipoDado.D100_DECENAS -> if (valor == 0) "00" else "$valor"
    else -> "$valor"
}

@Composable
internal fun FichaDado(
    dado: DadoEnMesa,
    texto: String,
    destacado: Boolean,
    simbolo: SimboloLegendario?,
    modifier: Modifier = Modifier
) {
    val tinta = colorTinta(dado)
    val fondo = colorCuerpo(dado)
    val borde = if (destacado) BorderStroke(3.dp, DoradoLegendario) else BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    Box(
        modifier = modifier
            .size(46.dp)
            .background(fondo, RoundedCornerShape(12.dp))
            .border(borde, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (simbolo != null) {
                // El Legendario no muestra número: muestra su símbolo, como el dado físico.
                Icon(
                    painterResource(SimbolosLegendarios.recurso(simbolo)),
                    contentDescription = simbolo.name,
                    tint = SimbolosLegendarios.color(simbolo),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(texto, color = tinta, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Text(dado.tipo.etiqueta, color = tinta.copy(alpha = 0.7f), fontSize = 10.sp)
        }
    }
}

/**
 * Versión de una sola línea para la interfaz a pantalla completa: las fichas se desplazan de costado
 * en vez de envolverse, así la barra inferior tiene siempre la misma altura y la mesa no "salta".
 * Pulsación larga sobre una ficha = quitar ese dado (antes de lanzar).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilaFichas(
    pool: List<DadoEnMesa>,
    resultados: List<ResultadoDado>,
    destacados: Set<Int>,
    estaRodando: Boolean,
    onQuitarDado: (Int) -> Unit,
    textoVacio: String,
    modifier: Modifier = Modifier
) {
    val fichas: List<Pair<DadoEnMesa, Int?>> =
        if (resultados.isNotEmpty()) resultados.map { it.dado to it.valor } else pool.map { it to null }
    Box(modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
        if (fichas.isEmpty()) {
            Text(textoVacio, color = Ivory.copy(alpha = 0.7f), fontSize = 13.sp)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Los dados explotados repiten id: la clave es la posición en la lista.
                itemsIndexed(fichas) { _, (dado, valor) ->
                    FichaDado(
                        dado = dado,
                        texto = when {
                            estaRodando -> "?"
                            valor != null -> textoValor(dado.tipo, valor)
                            else -> dado.tipo.etiqueta
                        },
                        destacado = dado.id in destacados,
                        simbolo = if (dado.tipo == TipoDado.LEGENDARIO && valor != null && !estaRodando) SimboloLegendario.de(valor) else null,
                        modifier = Modifier.combinedClickable(
                            enabled = !estaRodando && resultados.isEmpty(),
                            onClick = {},
                            onLongClick = { onQuitarDado(dado.id) }
                        )
                    )
                }
            }
        }
    }
}
