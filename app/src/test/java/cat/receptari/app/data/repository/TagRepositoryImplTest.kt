package cat.receptari.app.data.repository

import cat.receptari.app.data.local.dao.TagCountRow
import cat.receptari.app.data.local.dao.TagDao
import cat.receptari.app.data.local.entity.TagEntity
import cat.receptari.app.domain.model.Tag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    @Test
    fun `renaming changes the name and its dedupe key`() = runTest {
        val tag = dao.add("Postrs")

        repository.rename(tag.id, "  Postres ")

        val renamed = dao.stored.single()
        assertEquals("Postres", renamed.name)
        assertEquals("postres", renamed.normalizedName)
    }

    @Test
    fun `a blank new name changes nothing`() = runTest {
        val tag = dao.add("Postres")

        repository.rename(tag.id, "   ")

        assertEquals("Postres", dao.stored.single().name)
    }

    @Test
    fun `renaming to another tags name merges the two`() = runTest {
        val sweets = dao.add("Dolços")
        val desserts = dao.add("Postres")
        dao.link("r1", sweets.id)
        dao.link("r2", sweets.id)
        dao.link("r2", desserts.id)

        repository.rename(sweets.id, "postres")

        // One tag is left, it keeps its own name, and r2 holds it once rather than twice.
        assertEquals(listOf(desserts), dao.stored)
        assertEquals(setOf("r1" to desserts.id, "r2" to desserts.id), dao.links)
    }

    @Test
    fun `deleting a tag unlinks its recipes and removes it`() = runTest {
        val keep = dao.add("Postres")
        val drop = dao.add("Ràpid")
        dao.link("r1", keep.id)
        dao.link("r1", drop.id)

        repository.delete(drop.id)

        assertEquals(listOf(keep), dao.stored)
        assertEquals(setOf("r1" to keep.id), dao.links)
    }

    @Test
    fun `counts are the number of recipes carrying each tag`() = runTest {
        val tag = dao.add("Postres")
        dao.add("Sense receptes")
        dao.link("r1", tag.id)
        dao.link("r2", tag.id)

        val counts = repository.observeAllWithCounts().first().associate { it.tag.name to it.recipeCount }

        assertEquals(mapOf("Postres" to 2, "Sense receptes" to 0), counts)
    }

    private class FakeTagDao : TagDao {
        val stored = mutableListOf<TagEntity>()
        val links = mutableSetOf<Pair<String, String>>()

        fun add(name: String): TagEntity =
            TagEntity(UUID.randomUUID().toString(), name, Tag.normalize(name)).also { stored += it }

        fun link(recipeId: String, tagId: String) {
            links += recipeId to tagId
        }

        override fun observeAllWithCounts(): Flow<List<TagCountRow>> =
            flowOf(stored.map { tag -> TagCountRow(tag, links.count { it.second == tag.id }) })

        override suspend fun recipeIdsWithTag(tagId: String): List<String> =
            links.filter { it.second == tagId }.map { it.first }

        override suspend fun updateName(id: String, name: String, normalizedName: String) {
            val index = stored.indexOfFirst { it.id == id }
            stored[index] = stored[index].copy(name = name, normalizedName = normalizedName)
        }

        override suspend fun relink(sourceId: String, targetId: String) {
            links += links.filter { it.second == sourceId }.map { it.first to targetId }
        }

        override suspend fun deleteById(id: String) {
            stored.removeAll { it.id == id }
            links.removeAll { it.second == id }
        }

        override suspend fun refreshSearchTagNames(recipeIds: List<String>) = Unit

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
