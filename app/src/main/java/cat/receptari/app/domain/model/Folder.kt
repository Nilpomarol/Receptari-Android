package cat.receptari.app.domain.model

import java.text.Normalizer
import java.util.Locale

/**
 * A user-created folder. Flat — no nesting — and a recipe files under at most one (PRD §5).
 *
 * [normalizedName] is the dedupe key, so "Postres", "postres", and "POSTRES" are one folder.
 */
data class Folder(
    val id: String,
    val name: String,
    val normalizedName: String = normalize(name),
    val color: FolderColor = FolderColor.OLIVE,
    val icon: FolderIcon = FolderIcon.FOLDER,
) {
    companion object {
        private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")

        fun normalize(name: String): String =
            Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replace(DIACRITICS, "")
                .lowercase(Locale.ROOT)
    }
}
