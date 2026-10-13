package com.rolesencia.diceroller.ui.theme

import androidx.compose.ui.graphics.Color
import com.rolesencia.diceroller.domain.reglas.NivelResultado

// --- PALETA DARK FANTASY (heredada del Lanzador Legendario) ---
val DarkBlueFantasia = Color(0xFF0D1B2A)
val PiedraInactiva = Color(0xFF1C2A3A)
val Ivory = Color(0xFFFFFFF0)
val DeepSkyBlue = Color(0xFF00BFFF)
val CyanNeon = Color(0xFF00E5FF)
val OroAntiguo = Color(0xFFFFA000)
val PurpuraLeyenda = Color(0xFF6A1B9A)

// --- PALETA SEMÁNTICA DE RESULTADOS ---
val RojoFallo = Color(0xFFC20114)
val NaranjaCostoso = Color(0xFFFFB020)
val VerdeExito = Color(0xFF35CE8D)
val DoradoLegendario = Color(0xFFFFD700)
val PurpuraMistico = Color(0xFF9C27B0)

/** Color de los dados con [com.rolesencia.diceroller.domain.dados.RolDado.ESPECIAL] (Hambre, Miedo, Legendario…). */
val ColorDadoEspecial = Color(0xFFB71C1C)

fun colorDe(nivel: NivelResultado): Color = when (nivel) {
    NivelResultado.FALLO_CRITICO -> PurpuraMistico
    NivelResultado.FALLO -> RojoFallo
    NivelResultado.EXITO_PARCIAL -> NaranjaCostoso
    NivelResultado.EXITO -> VerdeExito
    NivelResultado.EXITO_CRITICO -> DoradoLegendario
    NivelResultado.NEUTRO -> Ivory
}
