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
import cat.receptari.app.data.local.entity.StepEntity
import cat.receptari.app.data.local.entity.TagEntity

/**
 * Schema version 3.
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
    ],
    version = 3,
    exportSchema = true,
)
abstract class ReceptariDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun tagDao(): TagDao
    abstract fun folderDao(): FolderDao
    abstract fun cookEventDao(): CookEventDao

    companion object {
        const val NAME = "receptari.db"

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
    }
}
