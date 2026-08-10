package cat.receptari.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = TerracottaLight,
    onPrimary = TerracottaLightOn,
    primaryContainer = TerracottaContainerLight,
    onPrimaryContainer = TerracottaContainerLightOn,
    secondary = OliveLight,
    onSecondary = OliveLightOn,
    secondaryContainer = OliveContainerLight,
    onSecondaryContainer = OliveContainerLightOn,
    background = BackgroundLight,
    onBackground = BackgroundLightOn,
    surface = BackgroundLight,
    onSurface = BackgroundLightOn,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = SurfaceVariantLightOn,
    outline = OutlineLight,
    error = ErrorLight,
    onError = ErrorLightOn,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorContainerLightOn,
)

private val DarkColors = darkColorScheme(
    primary = TerracottaDark,
    onPrimary = TerracottaDarkOn,
    primaryContainer = TerracottaContainerDark,
    onPrimaryContainer = TerracottaContainerDarkOn,
    secondary = OliveDark,
    onSecondary = OliveDarkOn,
    secondaryContainer = OliveContainerDark,
    onSecondaryContainer = OliveContainerDarkOn,
    background = BackgroundDark,
    onBackground = BackgroundDarkOn,
    surface = BackgroundDark,
    onSurface = BackgroundDarkOn,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = SurfaceVariantDarkOn,
    outline = OutlineDark,
    error = ErrorDark,
    onError = ErrorDarkOn,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainerDarkOn,
)

@Composable
fun ReceptariTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        // minSdk is 33, so dynamic colour is always available (ADR-006).
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReceptariTypography,
        content = content,
    )
}
