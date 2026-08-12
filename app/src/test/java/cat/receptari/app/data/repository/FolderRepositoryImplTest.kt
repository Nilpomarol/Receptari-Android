package cat.receptari.app.data.repository

import cat.receptari.app.data.local.dao.FolderDao
import cat.receptari.app.data.local.entity.FolderEntity
import cat.receptari.app.data.local.entity.FolderWithCount
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class FolderRepositoryImplTest {

    private val dao = FakeFolderDao()
    private val repository = FolderRepositoryImpl(dao, UnconfinedTestDispatcher())

    @Test
    fun `creates a folder that does not exist yet`() = runTest {
        val folder = repository.create("Postres de Nadal", FolderColor.OLIVE, FolderIcon.CAKE)

        assertEquals("Postres de Nadal", folder.name)
        assertEquals(FolderColor.OLIVE, folder.color)
        assertEquals(FolderIcon.CAKE, folder.icon)
        assertEquals(1, dao.stored.size)
    }

    @Test
    fun `reuses an existing folder instead of creating a duplicate`() = runTest {
        val existing = existingFolder(name = "Postres")
        dao.stored += existing

        val folder = repository.create("postres", FolderColor.GOLD, FolderIcon.COOKIE)

        assertEquals(existing.id, folder.id)
        assertEquals(1, dao.stored.size)
    }

    @Test
    fun `update changes the name, normalized name, colour and icon`() = runTest {
        val existing = existingFolder(name = "Postres")
        dao.stored += existing

        repository.update(existing.id, "Àpats ràpids", FolderColor.TERRACOTTA, FolderIcon.PIZZA)

        val updated = dao.stored.single { it.id == existing.id }
        assertEquals("Àpats ràpids", updated.name)
        assertEquals("apats rapids", updated.normalizedName)
        assertEquals(FolderColor.TERRACOTTA.name, updated.color)
        assertEquals(FolderIcon.PIZZA.name, updated.icon)
    }

    @Test
    fun `delete removes the folder`() = runTest {
        val existing = existingFolder(name = "Postres")
        dao.stored += existing

        repository.delete(existing.id)

        assertEquals(0, dao.stored.size)
    }

    private fun existingFolder(name: String) = FolderEntity(
        id = UUID.randomUUID().toString(),
        name = name,
        normalizedName = name.lowercase(),
        color = FolderColor.OLIVE.name,
        icon = FolderIcon.FOLDER.name,
    )

    private class FakeFolderDao : FolderDao {
        val stored = mutableListOf<FolderEntity>()

        override fun observeAll(): Flow<List<FolderEntity>> = flowOf(stored.toList())

        override fun observeAllWithCounts(): Flow<List<FolderWithCount>> = flowOf(
            stored.map {
                FolderWithCount(it.id, it.name, it.normalizedName, it.color, it.icon, recipeCount = 0)
            },
        )

        override suspend fun findByNormalizedName(normalizedName: String): FolderEntity? =
            stored.find { it.normalizedName == normalizedName }

        override suspend fun upsert(folder: FolderEntity) {
            stored.removeAll { it.id == folder.id }
            stored += folder
        }

        override suspend fun delete(id: String) {
            stored.removeAll { it.id == id }
        }
    }
}
