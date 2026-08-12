package cat.receptari.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration harness.
 *
 * Every schema version is exported and every step has a `migrate(N, N+1)` check alongside
 * its `Migration` (Docs/DATA_MODEL.md §5). The database is built without a destructive
 * fallback, so a missing migration is a crash, not a silent wipe.
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

    @Test
    fun migrate3To4AddsTranslationDisplayFields() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            ReceptariDatabase.MIGRATION_1_2,
            ReceptariDatabase.MIGRATION_2_3,
            ReceptariDatabase.MIGRATION_3_4,
        )

        assertColumnExists(db, "recipes", "displayLanguage")
        assertColumnExists(db, "ingredient_sections", "originalName")
        assertColumnExists(db, "ingredients", "displayText")
        assertColumnExists(db, "instruction_sections", "originalName")
    }

    @Test
    fun migrate4To5CachesTheExistingTranslatedDisplay() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            db.execSQL(
                """
                INSERT INTO recipes (
                    id, title, originalTitle, imagePath, prepTimeMinutes, cookTimeMinutes,
                    totalTimeMinutes, baseServings, isFavorite, rating, notes, sourceName,
                    sourceUrl, originalLanguage, displayLanguage, folderId, createdAt, updatedAt
                ) VALUES (
                    'recipe', 'Pa amb all', 'Garlic bread', NULL, NULL, NULL,
                    NULL, NULL, 0, NULL, NULL, NULL,
                    NULL, 'en', 'ca', NULL, 1, 2
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO ingredient_sections (id, recipeId, name, originalName, position)
                VALUES ('ingredients', 'recipe', 'Ingredients', 'Ingredients', 0)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO ingredients (
                    id, sectionId, position, quantity, quantityMax, unit, name, note,
                    originalText, displayText
                ) VALUES (
                    'garlic', 'ingredients', 0, 2, NULL, 'clove', 'all', NULL,
                    '2 cloves garlic', '2 grans d''all'
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO instruction_sections (id, recipeId, name, originalName, position)
                VALUES ('method', 'recipe', 'Elaboració', 'Method', 0)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO steps (id, sectionId, position, text, originalText)
                VALUES ('toast', 'method', 0, 'Torra el pa.', 'Toast the bread.')
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            ReceptariDatabase.MIGRATION_4_5,
        )

        db.query(
            "SELECT language, payloadJson FROM recipe_translations WHERE recipeId = 'recipe'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("ca", cursor.getString(0))
            val payload = JSONObject(cursor.getString(1))
            assertEquals("en", payload.getString("source_language"))
            assertEquals(
                EXPECTED_TRANSLATION_SOURCE_FINGERPRINT,
                payload.getString("source_fingerprint"),
            )
            assertEquals("Pa amb all", payload.getString("title"))
            assertEquals(
                "2 grans d'all",
                payload.getJSONArray("ingredients").getJSONObject(0).getString("text"),
            )
            assertEquals(
                "Torra el pa.",
                payload.getJSONArray("steps").getJSONObject(0).getString("text"),
            )
        }
    }

    private fun assertColumnExists(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        table: String,
        column: String,
    ) {
        val columns = mutableListOf<String>()
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
        }
        assertTrue("missing column: $table.$column", columns.contains(column))
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val EXPECTED_TRANSLATION_SOURCE_FINGERPRINT =
            "9524a9c483d09ae2a3b51e0b5a1d4eb2a383ba978c6ba13d5194788577a6ad9b"
    }
}
