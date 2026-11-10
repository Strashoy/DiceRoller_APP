package com.rolesencia.diceroller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Todos los roles de Material definidos: lo que no se define cae en la paleta violeta de fábrica,
// que no tiene nada que ver con el azul noche de la app.
private val DarkColorScheme = darkColorScheme(
    primary = DeepSkyBlue,
    onPrimary = Color.Black,
    primaryContainer = AzulContenedor,
    onPrimaryContainer = CelesteSobreContenedor,
    inversePrimary = Color(0xFF006688),
    secondary = PurpuraLeyenda,
    onSecondary = Color.White,
    // Chips seleccionados, botones tonales y el segmento elegido.
    secondaryContainer = AzulContenedor,
    onSecondaryContainer = CelesteSobreContenedor,
    tertiary = OroAntiguo,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF5A3A00),
    onTertiaryContainer = Color(0xFFFFDDB0),
    background = DarkBlueFantasia,
    onBackground = Ivory,
    surface = DarkBlueFantasia,
    onSurface = Ivory,
    surfaceVariant = PiedraInactiva,
    onSurfaceVariant = Ivory.copy(alpha = 0.7f),
    surfaceTint = DeepSkyBlue,
    surfaceDim = AzulNoche,
    surfaceBright = AzulRealce,
    surfaceContainerLowest = AzulNoche,
    surfaceContainerLow = AzulProfundo,
    surfaceContainer = AzulMenu,          // menús desplegables
    surfaceContainerHigh = PiedraInactiva,
    surfaceContainerHighest = AzulRealce, // riel de los interruptores apagados
    inverseSurface = Ivory,               // avisos (snackbar)
    inverseOnSurface = DarkBlueFantasia,
    outline = GrisContorno,
    outlineVariant = GrisContornoSuave,
    error = RojoSobreOscuro,
    onError = Color(0xFF3B0008),
    errorContainer = Color(0xFF5C1018),
    onErrorContainer = Color(0xFFFFDAD9),
    scrim = Color.Black
)

/** Siempre "Dark Fantasy", igual que el Lanzador Legendario. */
@Composable
fun DiceRollerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
