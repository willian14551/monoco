package com.example.monoco.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val coresMonoco = darkColorScheme(
    primary = DouradoMonoco, onPrimary = Color(0xFF30230A),
    primaryContainer = Color(0xFF42341C), onPrimaryContainer = Color(0xFFFFE2A5),
    secondary = TurquesaMonoco, onSecondary = AzulMonoco,
    secondaryContainer = AzulMonoco, onSecondaryContainer = Color(0xFFB6EAE5),
    background = FundoMonoco, surface = SuperficieMonoco,
    onBackground = Color(0xFFF0EBE1), onSurface = Color(0xFFF0EBE1),
    onSurfaceVariant = Color(0xFFB9C3CA), outline = Color(0xFF6F7E89)
)

@Composable
fun MonocoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = coresMonoco, typography = TipografiaMonoco, content = content)
}
