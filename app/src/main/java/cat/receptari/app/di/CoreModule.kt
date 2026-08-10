package cat.receptari.app.di

import cat.receptari.app.core.util.DefaultDispatcher
import cat.receptari.app.core.util.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * Injected rather than calling `Instant.now()` directly, so tests can freeze time and
     * assert on `createdAt` / `updatedAt` / cooked timestamps.
     */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
