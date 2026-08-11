package cat.receptari.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cat.receptari.app.R

/**
 * Three families, each with one job (ADR-009):
 *
 * - **Pinyon Script** — the wordmark, and nothing else. A script face is unreadable at body
 *   size and the temptation to use it twice is exactly how this aesthetic goes wrong.
 * - **Playfair Display** — recipe titles and headings. High stroke contrast, which is what
 *   makes a title look printed rather than rendered.
 * - **Lora** — everything read rather than glanced at. A serif drawn for screen body text,
 *   so ingredient lines survive being read from across a kitchen.
 *
 * Lora and Playfair ship as variable fonts, so each weight below is the same file with a
 * different `wght` axis rather than another 200 KB on disk.
 */

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight, style: FontStyle = FontStyle.Normal) =
    Font(
        resId = resId,
        weight = weight,
        style = style,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

private fun lora(weight: FontWeight) = variableFont(R.font.lora_variable, weight)

private fun loraItalic(weight: FontWeight) =
    variableFont(R.font.lora_italic_variable, weight, FontStyle.Italic)

private fun playfair(weight: FontWeight) = variableFont(R.font.playfair_display_variable, weight)

internal val BodyFamily = FontFamily(
    lora(FontWeight.Normal),
    lora(FontWeight.Medium),
    lora(FontWeight.SemiBold),
    lora(FontWeight.Bold),
    loraItalic(FontWeight.Normal),
    loraItalic(FontWeight.SemiBold),
)

internal val DisplayFamily = FontFamily(
    playfair(FontWeight.Normal),
    playfair(FontWeight.Medium),
    playfair(FontWeight.SemiBold),
    playfair(FontWeight.Bold),
)

internal val ScriptFamily = FontFamily(Font(R.font.pinyon_script_regular))

private fun display(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.SemiBold) =
    TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
    )

private fun body(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = BodyFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)

internal val ReceptariTypography = Typography(
    displayLarge = display(44, 54),
    displayMedium = display(36, 46),
    displaySmall = display(30, 40),

    headlineLarge = display(30, 40),
    headlineMedium = display(26, 34),
    headlineSmall = display(22, 30),

    titleLarge = display(21, 28),
    titleMedium = display(18, 24),
    // Section names sit between a title and a label; the extra tracking is what makes them
    // read as a printed heading rather than a bolded sentence.
    titleSmall = body(15, 20, FontWeight.SemiBold, tracking = 0.6),

    // Ingredient lines and steps land on bodyLarge. It is deliberately larger than the
    // Material default: recipes are read at arm's length from a counter.
    bodyLarge = body(17, 28),
    bodyMedium = body(15, 23),
    bodySmall = body(13, 19),

    labelLarge = body(15, 20, FontWeight.Medium, tracking = 0.4),
    labelMedium = body(13, 17, FontWeight.Medium, tracking = 0.5),
    labelSmall = body(11, 15, FontWeight.Medium, tracking = 0.6),
)

/** Styles with no Material role to sit in. */
object ReceptariTextStyles {

    /** The app's name, set in script. Reserved for the library's masthead. */
    val Wordmark: TextStyle = TextStyle(
        fontFamily = ScriptFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 52.sp,
        // Script ascenders and descenders are long; a tight line height clips the swashes.
        lineHeight = 66.sp,
    )

    /** Source attributions, scaling notices, empty-state asides — anything in an aside voice. */
    val Aside: TextStyle = TextStyle(
        fontFamily = BodyFamily,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

}
