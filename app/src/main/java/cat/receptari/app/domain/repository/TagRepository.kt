package cat.receptari.app.domain.repository

import cat.receptari.app.domain.model.Tag
import cat.receptari.app.domain.model.TagSummary
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

    /** Every tag with its recipe count, for managing the library's tags. */
    fun observeAllWithCounts(): Flow<List<TagSummary>>

    /**
     * Renames a tag everywhere it is used. If [newName] is another tag's name (matched as in
     * [findOrCreate]) the two are merged into that tag. A blank name changes nothing.
     */
    suspend fun rename(id: String, newName: String)

    /** Removes a tag from every recipe and from the library. The recipes are kept. */
    suspend fun delete(id: String)
}
