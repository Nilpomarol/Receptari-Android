package cat.receptari.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration harness.
 *
 * There is only schema version 1 so far, so this file exists to prove the machinery works —
 * the exported schema is on the device, the helper can create a database from it, and it
 * opens. Every future version adds a `migrate(N, N+1)` test here alongside its `Migration`
 * (Docs/DATA_MODEL.md §5); the database is built without a destructive fallback, so a
 * missing migration is a crash, not a silent wipe.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ReceptariDatabase::class.java,
    )

    @Test
    fun schemaVersionOneIsExportedAndOpenable() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            assertTrue(db.isOpen)

            val tables = mutableListOf<String>()
            db.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
                while (cursor.moveToNext()) tables += cursor.getString(0)
            }

            listOf(
                "recipes",
                "ingredient_sections",
                "ingredients",
                "instruction_sections",
                "steps",
                "tags",
                "recipe_tag_cross_ref",
                "cook_events",
                "recipe_fts",
            ).forEach { table ->
                assertTrue("missing table: $table", tables.contains(table))
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
