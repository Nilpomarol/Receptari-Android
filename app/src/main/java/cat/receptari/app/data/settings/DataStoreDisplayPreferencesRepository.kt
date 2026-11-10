package cat.receptari.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cat.receptari.app.domain.model.RecipeListLayout
import cat.receptari.app.domain.repository.DisplayPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.displayPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "display_preferences",
)

private val LIST_LAYOUT = stringPreferencesKey("recipe_list_layout")

class DataStoreDisplayPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DisplayPreferencesRepository {

    override fun observeListLayout(): Flow<RecipeListLayout> = context.displayPreferencesDataStore.data
        // An unreadable preferences file costs the reader their layout choice, nothing more.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            RecipeListLayout.entries.find { it.storedKey == prefs[LIST_LAYOUT] } ?: RecipeListLayout.Grid
        }
        .distinctUntilChanged()

    override suspend fun setListLayout(layout: RecipeListLayout) {
        context.displayPreferencesDataStore.edit { it[LIST_LAYOUT] = layout.storedKey }
    }
}

/** Fixed spellings on disk, so renaming a constant in code never loses the reader's choice. */
private val RecipeListLayout.storedKey: String
    get() = when (this) {
        RecipeListLayout.Grid -> "grid"
        RecipeListLayout.Index -> "index"
    }
