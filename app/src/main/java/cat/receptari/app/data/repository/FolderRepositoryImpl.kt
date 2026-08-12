package cat.receptari.app.data.repository

import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.data.local.dao.FolderDao
import cat.receptari.app.data.local.entity.FolderEntity
import cat.receptari.app.data.local.mapper.toDomain
import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.FolderSummary
import cat.receptari.app.domain.repository.FolderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FolderRepository {

    override fun observeAll(): Flow<List<Folder>> =
        folderDao.observeAll()
            .map { folders -> folders.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeAllWithCounts(): Flow<List<FolderSummary>> =
        folderDao.observeAllWithCounts()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun create(name: String, color: FolderColor, icon: FolderIcon): Folder =
        withContext(ioDispatcher) {
            val trimmed = name.trim()
            val normalized = Folder.normalize(trimmed)

            folderDao.findByNormalizedName(normalized)?.let { return@withContext it.toDomain() }

            val entity = FolderEntity(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                normalizedName = normalized,
                color = color.name,
                icon = icon.name,
            )
            folderDao.upsert(entity)
            entity.toDomain()
        }

    override suspend fun update(
        id: String,
        name: String,
        color: FolderColor,
        icon: FolderIcon,
    ): Unit = withContext(ioDispatcher) {
        val trimmed = name.trim()
        folderDao.upsert(
            FolderEntity(
                id = id,
                name = trimmed,
                normalizedName = Folder.normalize(trimmed),
                color = color.name,
                icon = icon.name,
            ),
        )
    }

    override suspend fun delete(id: String): Unit = withContext(ioDispatcher) {
        folderDao.delete(id)
    }
}
