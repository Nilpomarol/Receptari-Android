package cat.receptari.app.di

import android.content.Context
import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.core.util.TranslationKey
import cat.receptari.app.data.settings.ApiKeyCipher
import cat.receptari.app.data.settings.ApiKeyStore
import cat.receptari.app.domain.repository.ApiKeyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {

    @Provides
    @Singleton
    fun provideClaudeKey(
        @ApplicationContext context: Context,
        cipher: ApiKeyCipher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ApiKeyRepository = ApiKeyStore(context, cipher, ioDispatcher, "claude_api_key")

    @Provides
    @Singleton
    @TranslationKey
    fun provideTranslationKey(
        @ApplicationContext context: Context,
        cipher: ApiKeyCipher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ApiKeyRepository = ApiKeyStore(context, cipher, ioDispatcher, "openai_api_key")
}
