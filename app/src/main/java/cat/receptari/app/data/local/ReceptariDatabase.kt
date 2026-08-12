package cat.receptari.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cat.receptari.app.data.local.dao.CookEventDao
import cat.receptari.app.data.local.dao.FolderDao
import cat.receptari.app.data.local.dao.RecipeDao
import cat.receptari.app.data.local.dao.TagDao
import cat.receptari.app.data.local.entity.CookEventEntity
import cat.receptari.app.data.local.entity.FolderEntity
import cat.receptari.app.data.local.entity.IngredientEntity
import cat.receptari.app.data.local.entity.IngredientSectionEntity
import cat.receptari.app.data.local.entity.InstructionSectionEntity
import cat.receptari.app.data.local.entity.RecipeEntity
import cat.receptari.app.data.local.entity.RecipeFtsEntity
import cat.receptari.app.data.local.entity.RecipeTagCrossRef
import cat.receptari.app.data.local.entity.RecipeTranslationEntity
import cat.receptari.app.data.local.entity.StepEntity
import cat.receptari.app.data.local.entity.TagEntity
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

private const val DATABASE_SCHEMA_VERSION = 5

/**
 * Schema version 5.
 *
 * `exportSchema = true` and the generated JSON under `app/schemas/` are committed, because
 * every future change ships an explicit migration plus a migration test — there is no
 * destructive fallback outside debug builds (see `Docs/DATA_MODEL.md` §5).
 */
@Database(
    entities = [
        RecipeEntity::class,
        RecipeFtsEntity::class,
        IngredientSectionEntity::class,
        IngredientEntity::class,
        InstructionSectionEntity::class,
        StepEntity::class,
        TagEntity::class,
        RecipeTagCrossRef::class,
        FolderEntity::class,
        CookEventEntity::class,
        RecipeTranslationEntity::class,
    ],
    version = DATABASE_SCHEMA_VERSION,
    exportSchema = true,
)
abstract class ReceptariDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun tagDao(): TagDao
    abstract fun folderDao(): FolderDao
    abstract fun cookEventDao(): CookEventDao

    companion object {
        const val NAME = "receptari.db"
        const val SCHEMA_VERSION = DATABASE_SCHEMA_VERSION

        /** Adds folders: a flat, user-managed table a recipe optionally files under. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `folders` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `normalizedName` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_folders_normalizedName` " +
                        "ON `folders` (`normalizedName`)",
                )
                db.execSQL(
                    "ALTER TABLE `recipes` ADD COLUMN `folderId` TEXT DEFAULT NULL " +
                        "REFERENCES `folders`(`id`) ON DELETE SET NULL",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recipes_folderId` ON `recipes` (`folderId`)",
                )
            }
        }

        /**
         * Adds a colour and an icon to folders. Existing folders get the same defaults new
         * ones start with — [cat.receptari.app.domain.model.FolderColor.OLIVE] and
         * [cat.receptari.app.domain.model.FolderIcon.FOLDER] — so nothing appears unset.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `folders` ADD COLUMN `color` TEXT NOT NULL DEFAULT 'OLIVE'",
                )
                db.execSQL(
                    "ALTER TABLE `folders` ADD COLUMN `icon` TEXT NOT NULL DEFAULT 'FOLDER'",
                )
            }
        }

        /** Adds translated display fields while leaving approved source text untouched. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `recipes` ADD COLUMN `displayLanguage` TEXT DEFAULT NULL")
                db.execSQL(
                    "ALTER TABLE `ingredient_sections` ADD COLUMN `originalName` TEXT DEFAULT NULL",
                )
                db.execSQL("ALTER TABLE `ingredients` ADD COLUMN `displayText` TEXT DEFAULT NULL")
                db.execSQL(
                    "ALTER TABLE `instruction_sections` ADD COLUMN `originalName` TEXT DEFAULT NULL",
                )
            }
        }

        /** Caches every display language instead of overwriting the previous one. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recipe_translations` (
                        `recipeId` TEXT NOT NULL,
                        `language` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`recipeId`, `language`),
                        FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                // Version 4 already stored one translated display in the normal recipe
                // fields. Preserve that work as the first cached variant.
                db.query(
                    """
                    SELECT id, title, COALESCE(originalTitle, title),
                           originalLanguage, displayLanguage, updatedAt
                    FROM recipes
                    WHERE displayLanguage IS NOT NULL
                    """.trimIndent(),
                ).use { recipes ->
                    while (recipes.moveToNext()) {
                        val recipeId = recipes.getString(0)
                        val payload = JSONObject()
                            .putNullable("source_language", recipes.getString(3))
                            .put(
                                "source_fingerprint",
                                sourceFingerprint(db, recipeId, recipes.getString(2)),
                            )
                            .put("title", recipes.getString(1))
                            .put(
                                "ingredient_sections",
                                cachedFields(
                                    db = db,
                                    sql = """
                                        SELECT id, name FROM ingredient_sections
                                        WHERE recipeId = ? ORDER BY position
                                    """.trimIndent(),
                                    recipeId = recipeId,
                                ),
                            )
                            .put(
                                "ingredients",
                                cachedFields(
                                    db = db,
                                    sql = """
                                        SELECT i.id, COALESCE(i.displayText, i.originalText)
                                        FROM ingredients i
                                        JOIN ingredient_sections s ON s.id = i.sectionId
                                        WHERE s.recipeId = ? ORDER BY s.position, i.position
                                    """.trimIndent(),
                                    recipeId = recipeId,
                                ),
                            )
                            .put(
                                "instruction_sections",
                                cachedFields(
                                    db = db,
                                    sql = """
                                        SELECT id, name FROM instruction_sections
                                        WHERE recipeId = ? ORDER BY position
                                    """.trimIndent(),
                                    recipeId = recipeId,
                                ),
                            )
                            .put(
                                "steps",
                                cachedFields(
                                    db = db,
                                    sql = """
                                        SELECT st.id, st.text
                                        FROM steps st
                                        JOIN instruction_sections s ON s.id = st.sectionId
                                        WHERE s.recipeId = ? ORDER BY s.position, st.position
                                    """.trimIndent(),
                                    recipeId = recipeId,
                                ),
                            )

                        db.execSQL(
                            """
                            INSERT OR REPLACE INTO recipe_translations
                                (recipeId, language, payloadJson, updatedAt)
                            VALUES (?, ?, ?, ?)
                            """.trimIndent(),
                            arrayOf(
                                recipeId,
                                recipes.getString(4),
                                payload.toString(),
                                recipes.getLong(5),
                            ),
                        )
                    }
                }
            }
        }

        private fun cachedFields(
            db: SupportSQLiteDatabase,
            sql: String,
            recipeId: String,
        ): JSONArray = JSONArray().apply {
            db.query(sql, arrayOf(recipeId)).use { cursor ->
                while (cursor.moveToNext()) {
                    put(
                        JSONObject()
                            .put("id", cursor.getString(0))
                            .putNullable("text", cursor.getString(1)),
                    )
                }
            }
        }

        private fun JSONObject.putNullable(key: String, value: String?): JSONObject =
            put(key, value ?: JSONObject.NULL)

        private fun sourceFingerprint(
            db: SupportSQLiteDatabase,
            recipeId: String,
            title: String,
        ): String {
            val source = buildString {
                appendFingerprintField("title", title)
                db.query(
                    """
                    SELECT id, COALESCE(originalName, name)
                    FROM ingredient_sections WHERE recipeId = ? ORDER BY position
                    """.trimIndent(),
                    arrayOf(recipeId),
                ).use { sections ->
                    while (sections.moveToNext()) {
                        val sectionId = sections.getString(0)
                        appendFingerprintField(
                            "ingredient-section:$sectionId",
                            sections.getString(1),
                        )
                        db.query(
                            """
                            SELECT id, originalText FROM ingredients
                            WHERE sectionId = ? ORDER BY position
                            """.trimIndent(),
                            arrayOf(sectionId),
                        ).use { ingredients ->
                            while (ingredients.moveToNext()) {
                                appendFingerprintField(
                                    "ingredient:${ingredients.getString(0)}",
                                    ingredients.getString(1),
                                )
                            }
                        }
                    }
                }
                db.query(
                    """
                    SELECT id, COALESCE(originalName, name)
                    FROM instruction_sections WHERE recipeId = ? ORDER BY position
                    """.trimIndent(),
                    arrayOf(recipeId),
                ).use { sections ->
                    while (sections.moveToNext()) {
                        val sectionId = sections.getString(0)
                        appendFingerprintField(
                            "instruction-section:$sectionId",
                            sections.getString(1),
                        )
                        db.query(
                            """
                            SELECT id, COALESCE(originalText, text) FROM steps
                            WHERE sectionId = ? ORDER BY position
                            """.trimIndent(),
                            arrayOf(sectionId),
                        ).use { steps ->
                            while (steps.moveToNext()) {
                                appendFingerprintField(
                                    "step:${steps.getString(0)}",
                                    steps.getString(1),
                                )
                            }
                        }
                    }
                }
            }
            return MessageDigest.getInstance("SHA-256")
                .digest(source.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        }

        private fun StringBuilder.appendFingerprintField(key: String, value: String?) {
            append(key.length).append(':').append(key)
            append(value?.length ?: -1).append(':').append(value.orEmpty()).append(';')
        }
    }
}
