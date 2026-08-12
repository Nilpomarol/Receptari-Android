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

    @Test
    fun migrate1To2AddsFolders() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            ReceptariDatabase.MIGRATION_1_2,
        )

        val tables = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            while (cursor.moveToNext()) tables += cursor.getString(0)
        }
        assertTrue("missing table: folders", tables.contains("folders"))

        val recipeColumns = mutableListOf<String>()
        db.query("PRAGMA table_info(recipes)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) recipeColumns += cursor.getString(nameIndex)
        }
        assertTrue("missing column: recipes.folderId", recipeColumns.contains("folderId"))
    }

    @Test
    fun migrate2To3AddsFolderAppearance() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            ReceptariDatabase.MIGRATION_1_2,
            ReceptariDatabase.MIGRATION_2_3,
        )

        val folderColumns = mutableListOf<String>()
        db.query("PRAGMA table_info(folders)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) folderColumns += cursor.getString(nameIndex)
        }
        assertTrue("missing column: folders.color", folderColumns.contains("color"))
        assertTrue("missing column: folders.icon", folderColumns.contains("icon"))
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
