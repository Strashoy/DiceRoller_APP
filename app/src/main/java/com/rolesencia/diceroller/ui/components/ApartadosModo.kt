package com.rolesencia.diceroller.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.tirada.ModoDecision
import com.rolesencia.diceroller.ui.mesa3d.colorCuerpo
import com.rolesencia.diceroller.ui.mesa3d.colorTinta
import com.rolesencia.diceroller.ui.theme.DoradoLegendario
import com.rolesencia.diceroller.ui.theme.Ivory
import com.rolesencia.diceroller.ui.viewmodel.MetricasTirada

@Composable
private fun Tarjeta(titulo: String, contenido: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DoradoLegendario)
            contenido()
        }
    }
}

@Composable
private fun Pasos(vararg pasos: String) {
    pasos.forEachIndexed { i, p -> Text("${i + 1}. $p", fontSize = 13.sp) }
}

/** Apartado del MODO 1. */
@Composable
fun ApartadoFisicaReal(metricas: MetricasTirada?) {
    Tarjeta("Física real · el número lo decide la simulación") {
        Pasos(
            "Al lanzar se crean los cuerpos rígidos con un impulso al azar (o el de tu gesto).",
            "Cada cuadro avanza la física en sub-pasos fijos de 1/240 s: gravedad, choques, rebotes y fricción.",
            "Lo que ves es exactamente lo que calcula el motor: no hay animación preparada.",
            "Garantías: si un dado queda montado se lo empuja; si no se apoya en 8 s, se lo asienta.",
            "Cuando todos duermen, se LEE la cara superior de cada dado. Ese es el resultado."
        )
        TarjetaMetricas(metricas?.takeIf { it.sesion.modo == ModoDecision.FISICA_REAL })
    }
}

/** Apartado del MODO 2. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ApartadoPredeterminado(
    pool: List<DadoEnMesa>,
    elegirValores: Boolean,
    valoresElegidos: Map<Int, Int>,
    habilitado: Boolean,
    onToggleElegir: () -> Unit,
    onCiclarValor: (Int) -> Unit,
    metricas: MetricasTirada?
) {
    Tarjeta("Resultado predeterminado · el número se decide antes") {
        Pasos(
            "Se decide el número de cada dado: al azar o elegido por vos.",
            "Se simula TODA la tirada sin dibujarla (pre-simulación). Esto es la demora antes de ver movimiento.",
            "Se mira qué cara dejó la física y se busca la rotación de simetría del dado que lleva tu número a esa posición.",
            "Se reproduce la grabación con esa rotación aplicada desde el primer cuadro: mismos choques y sonidos, otro número impreso arriba."
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = elegirValores, onCheckedChange = { onToggleElegir() }, enabled = habilitado)
            Text(if (elegirValores) "Valores elegidos (tocá un dado para cambiarlo)" else "Valores sorteados al azar", fontSize = 13.sp)
        }
        if (elegirValores) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pool.forEach { dado ->
                    val valor = valoresElegidos[dado.id] ?: dado.tipo.maximo
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(colorCuerpo(dado), RoundedCornerShape(10.dp))
                            .combinedClickable(enabled = habilitado, onClick = { onCiclarValor(dado.id) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$valor", color = colorTinta(dado), fontWeight = FontWeight.Bold)
                            Text(dado.tipo.etiqueta, color = colorTinta(dado).copy(alpha = 0.7f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        TarjetaMetricas(metricas?.takeIf { it.sesion.modo == ModoDecision.PREDETERMINADO })
    }
}

@Composable
private fun TarjetaMetricas(m: MetricasTirada?) {
    if (m == null) {
        Text("Lanzá para medir la demora y ver el detalle.", fontSize = 12.sp, color = Ivory.copy(alpha = 0.6f))
        return
    }
    val s = m.sesion
    Column(
        Modifier.fillMaxWidth().background(Ivory.copy(alpha = 0.06f), RoundedCornerShape(8.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("Medición de la última tirada", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        s.preSimulacionMs?.let { Text("Pre-simulación: $it ms de cálculo", fontSize = 13.sp) }
        Text("Preparación (del toque a la sesión lista): ${m.preparacionMs ?: "—"} ms", fontSize = 13.sp)
        Text("Del toque al primer cuadro en movimiento: ${m.latenciaPrimerMovimientoMs ?: "—"} ms", fontSize = 13.sp)
        Text("Tirada completa: ${m.duracionTotalMs} ms · ${"%.2f".format(s.segundosSimulados)} s simulados en ${s.pasos} pasos", fontSize = 13.sp)
        if (m.rondas > 1) Text("Rondas (dados que explotaron): ${m.rondas}", fontSize = 13.sp)
        Text(
            "Garantías aplicadas: ${s.empujones} empujón(es), " +
                (if (s.golpecitos > 0) "${s.golpecitos} golpecito(s) a la torre, " else "") +
                "${s.forzados} asentado(s) a la fuerza",
            fontSize = 13.sp
        )
        Text(
            "Sonido: ${m.sonido.eventos} choques → ${m.sonido.disparos} reproducciones, " +
                "${m.sonido.tomasDistintas}/${m.sonido.tomasTotales} tomas distintas",
            fontSize = 13.sp
        )
        s.detalle.forEach { Text("• $it", fontSize = 12.sp, color = Ivory.copy(alpha = 0.8f)) }
    }
}
