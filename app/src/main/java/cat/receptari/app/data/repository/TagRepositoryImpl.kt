package cat.receptari.app.data.repository

import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.data.local.dao.TagDao
import cat.receptari.app.data.local.entity.TagEntity
import cat.receptari.app.data.local.mapper.toDomain
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.domain.repository.TagRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TagRepository {

    override fun observeAll(): Flow<List<Tag>> =
        tagDao.observeAll()
            .map { tags -> tags.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun findOrCreate(names: List<String>): List<Tag> = withContext(ioDispatcher) {
        // Deduplicate the request itself: "Postres" and "postres" typed on the same recipe
        // are one tag, and inserting both would violate the unique index.
        val requested = names
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .associateBy { Tag.normalize(it) }

        if (requested.isEmpty()) return@withContext emptyList()

        val existing = tagDao.findByNormalizedNames(requested.keys.toList())
            .associateBy { it.normalizedName }

        val created = requested
            .filterKeys { it !in existing }
            .map { (normalized, displayName) ->
                TagEntity(
                    id = UUID.randomUUID().toString(),
                    name = displayName,
                    normalizedName = normalized,
                )
            }

        if (created.isNotEmpty()) {
            tagDao.upsertAll(created)
        }

        (existing.values + created).map { it.toDomain() }
    }
}
