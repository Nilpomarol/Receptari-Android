package cat.receptari.app.di

import cat.receptari.app.data.timer.PreferencesCookingTimerRepository
import cat.receptari.app.domain.repository.CookingTimerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimerRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCookingTimerRepository(
        impl: PreferencesCookingTimerRepository,
    ): CookingTimerRepository
}
