package cat.receptari.app.di

import android.content.Context
import androidx.room.Room
import cat.receptari.app.data.local.ReceptariDatabase
import cat.receptari.app.data.local.dao.CookEventDao
import cat.receptari.app.data.local.dao.FolderDao
import cat.receptari.app.data.local.dao.RecipeDao
import cat.receptari.app.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReceptariDatabase =
        Room.databaseBuilder(context, ReceptariDatabase::class.java, ReceptariDatabase.NAME)
            // No destructive fallback: every schema change ships a real migration
            // (Docs/DATA_MODEL.md §5).
            .addMigrations(
                ReceptariDatabase.MIGRATION_1_2,
                ReceptariDatabase.MIGRATION_2_3,
                ReceptariDatabase.MIGRATION_3_4,
                ReceptariDatabase.MIGRATION_4_5,
            )
            .build()

    @Provides
    fun provideRecipeDao(database: ReceptariDatabase): RecipeDao = database.recipeDao()

    @Provides
    fun provideTagDao(database: ReceptariDatabase): TagDao = database.tagDao()

    @Provides
    fun provideFolderDao(database: ReceptariDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideCookEventDao(database: ReceptariDatabase): CookEventDao = database.cookEventDao()
}
