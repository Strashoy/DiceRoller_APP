package com.rolesencia.diceroller.ui.components

import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolesencia.diceroller.domain.discord.ConfigDiscord
import com.rolesencia.diceroller.domain.discord.WebhookDiscord
import com.rolesencia.diceroller.ui.theme.Ivory
import com.rolesencia.diceroller.ui.theme.NaranjaCostoso
import com.rolesencia.diceroller.ui.theme.RojoSobreOscuro
import com.rolesencia.diceroller.ui.theme.VerdeExito
import com.rolesencia.diceroller.ui.viewmodel.PruebaDiscord

/**
 * Ajustes de Discord: encender el envío, pegar el webhook del canal, elegir con qué nombre se figura
 * y mandar un mensaje de prueba.
 *
 * El webhook es un secreto (quien lo tenga puede escribir en el canal), así que una vez guardado
 * sólo se muestra enmascarado. El camino corto es el botón "Pegar": la URL se copia en Discord y se
 * pega acá con un toque, sin teclado.
 */
@Composable
fun ApartadoDiscord(
    config: ConfigDiscord,
    prueba: PruebaDiscord?,
    onCambio: (ConfigDiscord) -> Unit,
    onProbar: () -> Unit,
    /** Un campo de texto tomó el foco: la hoja tiene que abrirse entera para que el teclado no lo tape. */
    alEnfocarCampo: () -> Unit = {}
) {
    val contexto = LocalContext.current
    val foco = LocalFocusManager.current
    val hayWebhook = WebhookDiscord.esValido(config.webhook)
    var editando by remember { mutableStateOf(false) }
    var borrador by remember { mutableStateOf("") }
    val borradorValido = WebhookDiscord.esValido(borrador)

    fun guardarWebhook(url: String) {
        onCambio(config.copy(webhook = url.trim()))
        borrador = ""
        editando = false
        foco.clearFocus()
    }

    Interruptor(
        titulo = "Enviar las tiradas a Discord",
        detalle = "Cada resultado aparece en el canal de la mesa",
        valor = config.habilitado,
        onCambio = { onCambio(config.copy(habilitado = !config.habilitado)) }
    )
    if (!config.habilitado) return

    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (hayWebhook && !editando) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Webhook del canal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(WebhookDiscord.enmascarar(config.webhook), fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { editando = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cambiar") }
            }
        } else {
            OutlinedTextField(
                value = borrador,
                onValueChange = { borrador = it.trim() },
                label = { Text("Webhook del canal") },
                placeholder = { Text("https://discord.com/api/webhooks/…") },
                singleLine = true,
                isError = borrador.isNotEmpty() && !borradorValido,
                supportingText = {
                    Text(
                        if (borrador.isNotEmpty() && !borradorValido) "No parece un webhook de Discord"
                        else "En Discord: Editar canal → Integraciones → Webhooks → Copiar URL"
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { if (borradorValido) guardarWebhook(borrador) else foco.clearFocus() }),
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) alEnfocarCampo() }
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        val pegado = contexto.getSystemService(ClipboardManager::class.java)
                            ?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()?.trim().orEmpty()
                        if (WebhookDiscord.esValido(pegado)) guardarWebhook(pegado) else borrador = pegado
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Pegar") }
                if (borradorValido) {
                    FilledTonalButton(onClick = { guardarWebhook(borrador) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Guardar") }
                }
                if (hayWebhook) {
                    TextButton(onClick = { borrador = ""; editando = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") }
                    // Sacar el secreto del teléfono: el webhook guardado se borra y Discord queda sin destino.
                    TextButton(
                        onClick = { borrador = ""; editando = false; onCambio(config.copy(webhook = "")) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Quitar el webhook", color = RojoSobreOscuro) }
                }
            }
        }

        val nombreValido = WebhookDiscord.nombreValido(config.jugador)
        OutlinedTextField(
            value = config.jugador,
            onValueChange = { onCambio(config.copy(jugador = it.take(WebhookDiscord.MAX_NOMBRE))) },
            label = { Text("Tu nombre en la mesa") },
            placeholder = { Text("Ej.: Thorin") },
            singleLine = true,
            isError = !nombreValido,
            supportingText = {
                Text(
                    if (nombreValido) "Tu personaje o tu nombre: así figuran tus tiradas en el canal"
                    else "Discord no acepta nombres con «discord» ni «clyde»"
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { foco.clearFocus() }),
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) alEnfocarCampo() }
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = onProbar,
                enabled = config.activo && prueba != PruebaDiscord.EN_CURSO,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Enviar prueba") }
            if (prueba != null) {
                Text(
                    prueba.texto,
                    fontSize = 13.sp,
                    color = when (prueba) {
                        PruebaDiscord.LLEGO -> VerdeExito
                        PruebaDiscord.EN_CURSO -> Ivory.copy(alpha = 0.7f)
                        else -> NaranjaCostoso
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(
            "Las tiradas se guardan siempre en el teléfono. Sin conexión esperan hasta 10 minutos para " +
                "publicarse; pasado ese tiempo no se envían. Las tiradas ocultas (mantené LANZAR) y las de " +
                "valores elegidos a mano nunca se publican.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
