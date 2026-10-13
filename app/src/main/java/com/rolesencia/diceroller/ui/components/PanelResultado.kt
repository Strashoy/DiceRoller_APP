package com.rolesencia.diceroller.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.reglas.Veredicto
import com.rolesencia.diceroller.ui.theme.Ivory
import com.rolesencia.diceroller.ui.theme.colorDe

@Composable
fun PanelResultado(veredicto: Veredicto?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible = veredicto != null, enter = fadeIn(), exit = fadeOut()) {
            if (veredicto != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        veredicto.titulo,
                        color = colorDe(veredicto.nivel),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    if (veredicto.detalle.isNotBlank()) {
                        Text(veredicto.detalle, color = Ivory.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
