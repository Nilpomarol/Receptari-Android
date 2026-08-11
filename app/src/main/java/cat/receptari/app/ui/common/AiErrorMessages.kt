package cat.receptari.app.ui.common

import androidx.annotation.StringRes
import cat.receptari.app.R
import cat.receptari.app.domain.ai.AiError
import cat.receptari.app.domain.importer.ImportError

/**
 * The single place where an import failure becomes something a person reads.
 *
 * Every import path shares it, so "the key was rejected" reads the same whether it happened
 * in Settings or halfway through a website import. Note that [AiError.Unexpected.detail] is
 * deliberately *not* shown: it is a diagnostic string of unknown provenance, and the user
 * can do nothing with it.
 */
@StringRes
fun AiError.messageRes(): Int = when (this) {
    AiError.NoApiKey -> R.string.ai_error_no_key
    AiError.InvalidApiKey -> R.string.ai_error_invalid_key
    AiError.RateLimited -> R.string.ai_error_rate_limited
    AiError.QuotaExceeded -> R.string.ai_error_quota
    AiError.Offline -> R.string.ai_error_offline
    AiError.Refused -> R.string.ai_error_refused
    AiError.UnreadableResponse -> R.string.ai_error_unreadable
    is AiError.Unexpected -> R.string.ai_error_unexpected
}

@StringRes
fun ImportError.messageRes(): Int = when (this) {
    ImportError.Offline -> R.string.ai_error_offline
    ImportError.PageUnreachable -> R.string.import_error_unreachable
    ImportError.InvalidUrl -> R.string.import_error_invalid_url
    ImportError.NoRecipeFound -> R.string.import_nothing_found
}

/** Failures of neither kind should not reach the UI, but must not crash it if they do. */
@StringRes
fun Throwable.aiMessageRes(): Int = when (this) {
    is AiError -> messageRes()
    is ImportError -> messageRes()
    else -> R.string.ai_error_unexpected
}
