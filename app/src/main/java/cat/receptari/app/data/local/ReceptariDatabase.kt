package cat.receptari.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import cat.receptari.app.data.local.dao.CookEventDao
import cat.receptari.app.data.local.dao.RecipeDao
import cat.receptari.app.data.local.dao.TagDao
import cat.receptari.app.data.local.entity.CookEventEntity
import cat.receptari.app.data.local.entity.IngredientEntity
import cat.receptari.app.data.local.entity.IngredientSectionEntity
import cat.receptari.app.data.local.entity.InstructionSectionEntity
import cat.receptari.app.data.local.entity.RecipeEntity
import cat.receptari.app.data.local.entity.RecipeFtsEntity
import cat.receptari.app.data.local.entity.RecipeTagCrossRef
import cat.receptari.app.data.local.entity.StepEntity
import cat.receptari.app.data.local.entity.TagEntity

/**
 * Schema version 1.
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
        CookEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ReceptariDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun tagDao(): TagDao
    abstract fun cookEventDao(): CookEventDao

    companion object {
        const val NAME = "receptari.db"
    }
}
