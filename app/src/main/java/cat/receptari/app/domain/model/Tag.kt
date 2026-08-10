package cat.receptari.app.domain.model

import java.text.Normalizer
import java.util.Locale

/**
 * A user-defined tag. Flat — no hierarchy, no colours, no categories (PRD §5).
 *
 * [normalizedName] is the dedupe key, so "Postres", "postres", and "POSTRES" are one tag.
 */
data class Tag(
    val id: String,
    val name: String,
    val normalizedName: String = normalize(name),
) {
    companion object {
        private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")

        /**
         * Lowercased and accent-stripped, so "Iaia" and "iaia" collapse, and so do
         * "Postres" and "postres". Accent-stripping matters here because the same tag gets
         * typed with and without accents in Catalan and Spanish.
         */
        fun normalize(name: String): String =
            Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replace(DIACRITICS, "")
                .lowercase(Locale.ROOT)
    }
}
