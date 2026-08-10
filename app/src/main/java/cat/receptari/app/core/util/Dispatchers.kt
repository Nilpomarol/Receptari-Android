package cat.receptari.app.core.util

import javax.inject.Qualifier

/**
 * Injected rather than referenced directly so tests can substitute a test dispatcher
 * without the production code knowing.
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class DefaultDispatcher
