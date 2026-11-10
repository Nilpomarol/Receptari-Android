package cat.receptari.app.domain.repository

import cat.receptari.app.domain.model.RecipeListLayout
import kotlinx.coroutines.flow.Flow

/** How the reader prefers things shown. Device-local, like everything else. */
public interface DisplayPreferencesRepository {
    public fun observeListLayout(): Flow<RecipeListLayout>
    public suspend fun setListLayout(layout: RecipeListLayout)
}
