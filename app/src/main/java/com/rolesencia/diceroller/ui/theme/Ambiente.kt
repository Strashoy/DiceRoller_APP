package com.rolesencia.diceroller.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.rolesencia.diceroller.domain.apariencia.PaletasAmbiente

/**
 * Ambiente donde se lanza (bandeja o torre). Quien dibuja dados "Por tipo" lo lee para volver a
 * pintarse cuando cambia (los colores salen de [PaletasAmbiente]).
 */
val LocalAmbiente = staticCompositionLocalOf { PaletasAmbiente.BANDEJA_LONA }

/** Color de resaltado del ambiente: dados que deciden la tirada, dados elegidos del carrusel. */
val LocalAcento = staticCompositionLocalOf { Color(PaletasAmbiente.acento(PaletasAmbiente.BANDEJA_LONA)) }
