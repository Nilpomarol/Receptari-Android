package cat.receptari.app.ui.importer

import androidx.annotation.StringRes

/**
 * What any import source can tell its screen. Shared by all three because they all end the
 * same way: either a draft is waiting in [ImportDraftHandoff], or there is something to say.
 */
sealed interface ImportEffect {
    /** Extraction succeeded; open the editor to review the draft. */
    data object DraftReady : ImportEffect

    data class ShowMessage(@param:StringRes val messageRes: Int) : ImportEffect
}
