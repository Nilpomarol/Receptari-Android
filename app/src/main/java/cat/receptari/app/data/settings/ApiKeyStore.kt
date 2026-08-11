package cat.receptari.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.domain.repository.ApiKeyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The API key, encrypted at rest.
 *
 * The DataStore file is named `secrets` to match the exclusions in `backup_rules.xml` and
 * `data_extraction_rules.xml` — the key is device-local and must not travel to cloud backup
 * or a device transfer (ADR-002).
 */
private val Context.secretsDataStore: DataStore<Preferences> by preferencesDataStore(name = "secrets")

@Singleton
class ApiKeyStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cipher: ApiKeyCipher,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ApiKeyRepository {

    override fun observeHasKey(): Flow<Boolean> =
        context.secretsDataStore.data
            .map { it[API_KEY]?.isNotBlank() == true }
            .flowOn(ioDispatcher)

    override suspend fun getKey(): String? = withContext(ioDispatcher) {
        val stored = context.secretsDataStore.data.first()[API_KEY] ?: return@withContext null

        // Decryption fails if the Keystore key was invalidated — a restore to a new device,
        // or the user clearing the lock screen. The stored ciphertext is unrecoverable at
        // that point, so drop it rather than leaving the app stuck reporting "key
        // configured" while every request fails.
        cipher.decrypt(stored) ?: run {
            clearKey()
            null
        }
    }

    override suspend fun setKey(key: String) {
        withContext(ioDispatcher) {
            val encrypted = cipher.encrypt(key.trim())
            context.secretsDataStore.edit { it[API_KEY] = encrypted }
        }
    }

    override suspend fun clearKey() {
        withContext(ioDispatcher) {
            context.secretsDataStore.edit { it.remove(API_KEY) }
        }
    }

    private companion object {
        val API_KEY = stringPreferencesKey("claude_api_key")
    }
}
