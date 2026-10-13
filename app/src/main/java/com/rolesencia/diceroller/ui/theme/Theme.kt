package com.rolesencia.diceroller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DeepSkyBlue,
    secondary = PurpuraLeyenda,
    tertiary = OroAntiguo,
    background = DarkBlueFantasia,
    surface = DarkBlueFantasia,
    surfaceVariant = PiedraInactiva,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Ivory,
    onSurface = Ivory,
    onSurfaceVariant = Ivory.copy(alpha = 0.7f)
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
