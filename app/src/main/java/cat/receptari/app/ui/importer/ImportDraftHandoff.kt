package cat.receptari.app.ui.importer

import cat.receptari.app.domain.ai.DraftRecipe
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries an extracted [DraftRecipe] from an import screen to the editor.
 *
 * The draft is deliberately *not* a navigation argument and *not* a database row. Not an
 * argument because it is a few kilobytes of structured data that would have to be serialised
 * into a route and would then sit in the back stack; not a row because an unreviewed model
 * response must never reach the database (PRD §14, AGENTS.md §7).
 *
 * [consume] returns the draft exactly once, so a configuration change or a second visit to
 * the editor cannot resurrect a stale import.
 */
@Singleton
class ImportDraftHandoff @Inject constructor() {

    @Volatile
    private var pending: DraftRecipe? = null

    fun offer(draft: DraftRecipe) {
        pending = draft
    }

    @Synchronized
    fun consume(): DraftRecipe? = pending.also { pending = null }
}
