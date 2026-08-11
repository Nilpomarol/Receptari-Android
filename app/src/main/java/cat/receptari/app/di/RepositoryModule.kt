package cat.receptari.app.di

import cat.receptari.app.data.image.FileImageStore
import cat.receptari.app.data.remote.claude.ClaudeAiClient
import cat.receptari.app.data.remote.web.OkHttpWebPageSource
import cat.receptari.app.data.repository.CookHistoryRepositoryImpl
import cat.receptari.app.data.repository.RecipeRepositoryImpl
import cat.receptari.app.data.repository.TagRepositoryImpl
import cat.receptari.app.data.settings.ApiKeyStore
import cat.receptari.app.domain.ai.AiClient
import cat.receptari.app.domain.importer.WebPageSource
import cat.receptari.app.domain.repository.ApiKeyRepository
import cat.receptari.app.domain.repository.CookHistoryRepository
import cat.receptari.app.domain.repository.ImageStore
import cat.receptari.app.domain.repository.RecipeRepository
import cat.receptari.app.domain.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    @Singleton
    abstract fun bindCookHistoryRepository(impl: CookHistoryRepositoryImpl): CookHistoryRepository

    @Binds
    @Singleton
    abstract fun bindImageStore(impl: FileImageStore): ImageStore

    @Binds
    @Singleton
    abstract fun bindApiKeyRepository(impl: ApiKeyStore): ApiKeyRepository

    @Binds
    @Singleton
    abstract fun bindAiClient(impl: ClaudeAiClient): AiClient

    @Binds
    @Singleton
    abstract fun bindWebPageSource(impl: OkHttpWebPageSource): WebPageSource
}
