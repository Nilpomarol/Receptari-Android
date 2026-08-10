package cat.receptari.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import cat.receptari.app.domain.scaling.ScaledIngredient
import java.util.Locale

/**
 * Renders one ingredient line, in the reader's locale.
 *
 * The line itself comes from the source text — see [ScaledIngredient.displayText] for why
 * that beats rebuilding it from the parsed fields.
 */
@Composable
@ReadOnlyComposable
fun ingredientLine(ingredient: ScaledIngredient): String {
    val locale: Locale = LocalConfiguration.current.locales[0]
    return ingredient.displayText(locale)
}
