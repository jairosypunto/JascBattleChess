package com.jasc.jascbattlechess.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ===================================================
// 🎨 ENUM COMPLETO CON COLORES DE TABLERO Y FONDO EXTERIOR
// ===================================================
enum class TemaAjedrez(
    val clara: Color,
    val oscura: Color,
    val principal: Color,
    val fondoTablero: Color // 🆕 ¡Agregado en el constructor!
) {
    AZUL_HERMOSO(
        Color(0xFFE3F2FD),
        Color(0xFF1976D2),
        Color(0xFF1976D2),
        Color(0xFF0D47A1) // Fondo azul rey profundo
    ),
    MADERA(
        Color(0xFFF0D9B5),
        Color(0xFFB58863),
        Color(0xFF8B5A2B),
        Color(0xFF2E7D32) // Tu fondo verde de tapete clásico actual
    ),
    NEON(
        Color(0xFF263238),
        Color(0xFF00E676),
        Color(0xFF00E676),
        Color(0xFF121212) // Fondo oscuro Cyberpunk
    ),
    CARBON(
        Color(0xFFE0E0E0),
        Color(0xFF424242),
        Color(0xFF212121),
        Color(0xFF303030) // Fondo gris carbón plano
    ),
    MORADO_MISTICO(
        Color(0xFFF3E5F5),
        Color(0xFF7B1FA2),
        Color(0xFF4A148C),
        Color(0xFF1A0A2A) // Fondo morado místico oscuro
    )
}

// Colores por defecto del sistema de Material
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650a4),
    secondary = Color(0xFF625b71),
    tertiary = Color(0xFF7D5260)
)

@Composable
fun JascbattlechessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}