package cat.receptari.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cat.receptari.app.domain.repository.ApiKeyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * One provider's API key, encrypted at rest.
 *
 * The DataStore file is named `secrets` to match the exclusions in `backup_rules.xml` and
 * `data_extraction_rules.xml` — keys are device-local and must not travel to cloud backup
 * or a device transfer (ADR-002). Each provider's key is a separate entry in that file
 * ([preferenceName]); `ApiKeyModule` creates one store per provider.
 */
private val Context.secretsDataStore: DataStore<Preferences> by preferencesDataStore(name = "secrets")

class ApiKeyStore(
    private val context: Context,
    private val cipher: ApiKeyCipher,
    private val ioDispatcher: CoroutineDispatcher,
    preferenceName: String,
) : ApiKeyRepository {

    private val API_KEY = stringPreferencesKey(preferenceName)

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
}
