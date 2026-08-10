package cat.receptari.app.data.repository

import cat.receptari.app.data.local.dao.TagDao
import cat.receptari.app.data.local.entity.TagEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class TagRepositoryImplTest {

    private val dao = FakeTagDao()
    private val repository = TagRepositoryImpl(dao, UnconfinedTestDispatcher())

    @Test
    fun `creates tags that do not exist yet`() = runTest {
        val tags = repository.findOrCreate(listOf("Postres", "Ràpid"))

        assertEquals(2, tags.size)
        assertEquals(2, dao.stored.size)
    }

    @Test
    fun `reuses an existing tag instead of creating a duplicate`() = runTest {
        val existing = TagEntity(UUID.randomUUID().toString(), "Postres", "postres")
        dao.stored += existing

        val tags = repository.findOrCreate(listOf("postres"))

        assertEquals(1, tags.size)
        assertEquals(existing.id, tags.single().id)
        assertEquals(1, dao.stored.size)
    }

    @Test
    fun `case and accent variants collapse onto one tag`() = runTest {
        // "Ràpid" and "rapid" are the same tag typed two ways; storing both would clutter
        // the picker and split the filter.
        val tags = repository.findOrCreate(listOf("Ràpid", "rapid", "RÀPID"))

        assertEquals(1, tags.size)
        assertEquals(1, dao.stored.size)
    }

    @Test
    fun `blank names are ignored`() = runTest {
        val tags = repository.findOrCreate(listOf("  ", "", "Postres"))

        assertEquals(1, tags.size)
    }

    @Test
    fun `an empty request touches nothing`() = runTest {
        val tags = repository.findOrCreate(emptyList())

        assertEquals(0, tags.size)
        assertEquals(0, dao.stored.size)
    }

    private class FakeTagDao : TagDao {
        val stored = mutableListOf<TagEntity>()

        override fun observeAll(): Flow<List<TagEntity>> = flowOf(stored.toList())

        override suspend fun findByNormalizedNames(normalizedNames: List<String>): List<TagEntity> =
            stored.filter { it.normalizedName in normalizedNames }

        override suspend fun upsertAll(tags: List<TagEntity>) {
            tags.forEach { tag ->
                stored.removeAll { it.id == tag.id }
                stored += tag
            }
        }

        override suspend fun deleteOrphans() = Unit
    }
}
