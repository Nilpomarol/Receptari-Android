package cat.receptari.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Stores the user's Claude API key (ADR-002).
 *
 * The key is deliberately **not** observable. The UI can learn whether one is configured,
 * never what it is — nothing outside `data/remote/claude/` has a reason to read it, and a
 * value that never reaches the UI layer cannot be logged, screenshotted, or put in a
 * savedInstanceState by accident.
 */
interface ApiKeyRepository {

    /** Whether a key is configured. The only key-related state the UI sees. */
    fun observeHasKey(): Flow<Boolean>

    /** Reads the key for an outgoing API call. Callers must not retain or log it. */
    suspend fun getKey(): String?

    suspend fun setKey(key: String)

    suspend fun clearKey()
}
