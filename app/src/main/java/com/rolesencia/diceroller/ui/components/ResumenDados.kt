package com.rolesencia.diceroller.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.rolesencia.diceroller.ui.theme.PiedraInactiva

/**
 * Fichas con cada dado del pool y, tras la tirada, su valor (incluye los dados que explotaron).
 * Complementa a la mesa 3D: se lee de un vistazo y permite quitar dados con una pulsación larga.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResumenDados(
    pool: List<DadoEnMesa>,
    resultados: List<ResultadoDado>,
    destacados: Set<Int>,
    estaRodando: Boolean,
    onQuitarDado: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Los resultados pueden traer dados extra (explosiones), así que se dibujan ellos si existen.
    val fichas: List<Pair<DadoEnMesa, Int?>> =
        if (resultados.isNotEmpty()) resultados.map { it.dado to it.valor } else pool.map { it to null }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PiedraInactiva.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (fichas.isEmpty()) {
            Text("Agregá dados a la mesa", color = Ivory.copy(alpha = 0.6f), modifier = Modifier.padding(8.dp))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fichas.forEach { (dado, valor) ->
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

private fun textoValor(tipo: TipoDado, valor: Int): String = when (tipo) {
    TipoDado.FATE -> when { valor > 0 -> "+"; valor < 0 -> "−"; else -> "0" }
    TipoDado.D100_DECENAS -> if (valor == 0) "00" else "$valor"
    else -> "$valor"
}

@Composable
private fun FichaDado(
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
