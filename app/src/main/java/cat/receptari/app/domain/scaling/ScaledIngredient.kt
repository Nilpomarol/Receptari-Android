package cat.receptari.app.domain.scaling

import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.parser.IngredientParser
import java.util.Locale

/**
 * An ingredient as it should appear on screen for the currently selected serving count.
 *
 * This is deliberately a *different type* from [Ingredient] rather than a copy of it. The
 * serving multiplier must never touch stored data (PRD §3.3, §14), and having no stored
 * type in hand makes that a compile-time property instead of a rule someone has to
 * remember.
 */
data class ScaledIngredient(
    val id: String,
    val quantity: Double?,
    val quantityMax: Double?,
    val unit: String?,
    val name: String?,
    val note: String?,
    val originalText: String,
    val wasScaled: Boolean,
) {
    /** True when nothing was recovered by parsing and the raw text is all there is. */
    val isUnstructured: Boolean
        get() = quantity == null && unit == null && name == null

    /** The numeric part ready for display, or null when this line has no quantity. */
    fun displayQuantity(locale: Locale = Locale.getDefault()): String? =
        quantity?.let { QuantityFormatter.formatRange(it, quantityMax, unit, locale) }

    /**
     * The whole line as it should appear on screen.
     *
     * Unscaled, that is the source text verbatim. Scaled, it is the source text with only
     * the leading quantity swapped out.
     *
     * Rebuilding the line from `quantity + unit + name` looks correct in English and wrong
     * everywhere else: the parser strips the connector to isolate the name, so "2 grans
     * d'all" comes back as "2 grans all" and "200 ml de nata" as "200 ml nata". Restoring
     * the connector would mean handling Catalan elision (d'all vs de nata) and noun plural
     * agreement — grammar the source text already got right. So the source text is what
     * gets shown.
     */
    fun displayText(locale: Locale = Locale.getDefault()): String {
        if (!wasScaled || quantity == null) return originalText

        val range = IngredientParser.leadingQuantityRange(originalText) ?: return originalText
        val scaledQuantity = QuantityFormatter.formatRange(quantity, quantityMax, unit, locale)

        return scaledQuantity + originalText.substring(range.last + 1)
    }
}

/** A section of ingredients scaled for display. */
data class ScaledIngredientSection(
    val id: String,
    val name: String?,
    val ingredients: List<ScaledIngredient>,
)
