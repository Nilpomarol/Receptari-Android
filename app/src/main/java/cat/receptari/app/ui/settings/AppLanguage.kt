package cat.receptari.app.ui.settings

import androidx.annotation.StringRes
import cat.receptari.app.R

/**
 * UI languages offered by the in-app picker.
 *
 * [languageTag] is empty for [System], which is what `LocaleManager` expects to mean
 * "follow the system". Adding a language here must stay a one-line change alongside a new
 * `values-XX/strings.xml` and a `<locale>` entry in `res/xml/locales_config.xml` (ADR-004).
 */
enum class AppLanguage(
    val languageTag: String,
    @param:StringRes val labelRes: Int,
) {
    System(languageTag = "", labelRes = R.string.settings_language_system),
    Catalan(languageTag = "ca", labelRes = R.string.settings_language_ca),
    Spanish(languageTag = "es", labelRes = R.string.settings_language_es),
    English(languageTag = "en", labelRes = R.string.settings_language_en),
    ;

    companion object {
        fun fromLanguageTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.languageTag.isNotEmpty() && tag?.startsWith(it.languageTag) == true }
                ?: System
    }
}
