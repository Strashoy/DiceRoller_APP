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

/**
 * Rojo para texto y marcas chicas sobre fondos oscuros: [RojoFallo] da 2,7:1 contra el fondo de la
 * app y no llega al mínimo de 3:1; este da 6,3:1.
 */
val RojoSobreOscuro = Color(0xFFFF6B6B)

/** Color de los dados con [com.rolesencia.diceroller.domain.dados.RolDado.ESPECIAL] (Hambre, Miedo, Legendario…). */
val ColorDadoEspecial = Color(0xFFB71C1C)

/** Violeta para texto sobre fondos oscuros: [PurpuraMistico] da 2,8:1; este, 7,3:1. */
val VioletaSobreOscuro = Color(0xFFCE93D8)

// --- SUPERFICIES Y CONTENEDORES (el esquema completo de Material, en la gama de la app) ---
// Sin esto, chips seleccionados, botones tonales, menús e interruptores salían en el gris
// violáceo que trae Material por defecto.
val AzulNoche = Color(0xFF08121D)
val AzulProfundo = Color(0xFF101F30)
val AzulMenu = Color(0xFF152536)
val AzulRealce = Color(0xFF243447)
val AzulContenedor = Color(0xFF1A5173)
val CelesteSobreContenedor = Color(0xFFD6EEFF)
val GrisContorno = Color(0xFF7C8EA3)
val GrisContornoSuave = Color(0xFF33455A)

/**
 * Color del veredicto **como texto** sobre el fondo oscuro de la app. Fallo y pifia usan sus
 * versiones claras: el rojo y el violeta saturados no llegan al contraste mínimo.
 */
fun colorDe(nivel: NivelResultado): Color = when (nivel) {
    NivelResultado.FALLO_CRITICO -> VioletaSobreOscuro
    NivelResultado.FALLO -> RojoSobreOscuro
    NivelResultado.EXITO_PARCIAL -> NaranjaCostoso
    NivelResultado.EXITO -> VerdeExito
    NivelResultado.EXITO_CRITICO -> DoradoLegendario
    NivelResultado.NEUTRO -> Ivory
}
