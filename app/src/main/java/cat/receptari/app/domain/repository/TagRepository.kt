package cat.receptari.app.domain.repository

import cat.receptari.app.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {

    fun observeAll(): Flow<List<Tag>>

    /**
     * Resolves names to tags, creating the ones that do not exist yet.
     *
     * Matching is by normalized name, so "Postres", "postres" and "POSTRES" all resolve to
     * the same tag rather than three.
     */
    suspend fun findOrCreate(names: List<String>): List<Tag>
}
