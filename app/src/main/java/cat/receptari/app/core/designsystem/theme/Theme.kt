package cat.receptari.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Tokens the aesthetic needs and Material has no role for.
 *
 * Everything that maps cleanly onto a Material role stays a Material role — this is only
 * for the handful of things a recipe book has and a Material app does not.
 */
@Immutable
data class ReceptariPalette(
    /** Aging at the edge of the page. Vignette and grain only; never behind text. */
    val paperEdge: Color,
    /** Hairline frames and rules. Lighter than [ReceptariPalette.ink], heavier than nothing. */
    val rule: Color,
    /** Ratings. Gold leaf, not a warning yellow. */
    val gold: Color,
    /** Favourites. Sealing-wax red, deliberately distinct from the error red. */
    val heart: Color,
    /** Secondary inks used only for user-selected folder stamps. */
    val folderForest: Color,
    val folderIndigo: Color,
    val folderCocoa: Color,
    val folderSlate: Color,
    /** How strongly the paper texture prints over the base colour. Zero is a flat fill. */
    val paperIntensity: Float,
)

private val LightPalette = ReceptariPalette(
    paperEdge = PaperEdge,
    rule = Rule,
    gold = Gold,
    heart = Claret,
    folderForest = Forest,
    folderIndigo = Indigo,
    folderCocoa = Cocoa,
    folderSlate = Slate,
    paperIntensity = 0.42f,
)

private val DarkPalette = ReceptariPalette(
    paperEdge = LeatherEdge,
    rule = RuleDark,
    gold = GoldDark,
    heart = ClaretDark,
    folderForest = ForestDark,
    folderIndigo = IndigoDark,
    folderCocoa = CocoaDark,
    folderSlate = SlateDark,
    // Texture on a dark ground reads as sensor noise long before it reads as paper.
    paperIntensity = 0.18f,
)

private val LightColors = lightColorScheme(
    primary = Olive,
    onPrimary = OliveOn,
    primaryContainer = OliveTint,
    onPrimaryContainer = OliveTintOn,
    secondary = Terracotta,
    onSecondary = TerracottaOn,
    secondaryContainer = TerracottaTint,
    onSecondaryContainer = TerracottaTintOn,
    tertiary = Gold,
    onTertiary = OliveOn,
    tertiaryContainer = Gold.copy(alpha = 0.22f).compositeOver(Parchment),
    onTertiaryContainer = GoldOn,
    background = Parchment,
    onBackground = Ink,
    surface = Parchment,
    onSurface = Ink,
    surfaceVariant = ParchmentSunk,
    onSurfaceVariant = InkSoft,
    surfaceContainerLowest = ParchmentRaised,
    surfaceContainerLow = ParchmentRaised,
    surfaceContainer = ParchmentRaised,
    surfaceContainerHigh = ParchmentRaised,
    surfaceContainerHighest = ParchmentSunk,
    inverseSurface = Ink,
    inverseOnSurface = Parchment,
    outline = Rule,
    outlineVariant = Rule.copy(alpha = 0.45f).compositeOver(Parchment),
    scrim = Ink,
    error = ErrorInk,
    onError = ErrorInkOn,
    errorContainer = ErrorTint,
    onErrorContainer = ErrorTintOn,
)

private val DarkColors = darkColorScheme(
    primary = OliveDark,
    onPrimary = OliveDarkOn,
    primaryContainer = OliveTintDark,
    onPrimaryContainer = OliveTintDarkOn,
    secondary = TerracottaDark,
    onSecondary = TerracottaDarkOn,
    secondaryContainer = TerracottaTintDark,
    onSecondaryContainer = TerracottaTintDarkOn,
    tertiary = GoldDark,
    onTertiary = GoldDarkOn,
    tertiaryContainer = GoldDark.copy(alpha = 0.22f).compositeOver(Leather),
    onTertiaryContainer = GoldDark,
    background = Leather,
    onBackground = InkDark,
    surface = Leather,
    onSurface = InkDark,
    surfaceVariant = LeatherSunk,
    onSurfaceVariant = InkSoftDark,
    surfaceContainerLowest = LeatherSunk,
    surfaceContainerLow = LeatherRaised,
    surfaceContainer = LeatherRaised,
    surfaceContainerHigh = LeatherRaised,
    surfaceContainerHighest = LeatherRaised,
    inverseSurface = InkDark,
    inverseOnSurface = Leather,
    outline = RuleDark,
    outlineVariant = RuleDark.copy(alpha = 0.5f).compositeOver(Leather),
    scrim = Color.Black,
    error = ErrorInkDark,
    onError = ErrorInkDarkOn,
    errorContainer = ErrorTintDark,
    onErrorContainer = ErrorTintDarkOn,
)

private val LocalReceptariPalette = staticCompositionLocalOf { LightPalette }

/**
 * @param darkTheme kept as a parameter, not read from the system.
 *
 * A printed book is paper, so the app is paper — following the phone into dark mode would
 * hand the look back to a setting the design does not agree with. The lamplight palette is
 * still here and still correct, waiting for the theme preference in Settings to drive it.
 */
@Composable
fun ReceptariTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val palette = if (darkTheme) DarkPalette else LightPalette

    CompositionLocalProvider(LocalReceptariPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ReceptariTypography,
            shapes = ReceptariShapes,
            content = content,
        )
    }
}

/** Accessor for the tokens Material does not carry, in the shape Material uses for its own. */
object ReceptariTheme {
    val palette: ReceptariPalette
        @Composable @ReadOnlyComposable get() = LocalReceptariPalette.current
}
