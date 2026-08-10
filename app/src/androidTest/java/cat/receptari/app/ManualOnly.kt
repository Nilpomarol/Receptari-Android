package cat.receptari.app

/**
 * Marks an instrumented entry point as a development utility rather than a test.
 *
 * The suite is configured with `notAnnotation=cat.receptari.app.ManualOnly`, so anything
 * carrying this never runs in a normal `connectedAndroidTest` or in CI. Run one explicitly
 * with:
 *
 * ```
 * ./gradlew connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.annotation=cat.receptari.app.ManualOnly
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class ManualOnly
