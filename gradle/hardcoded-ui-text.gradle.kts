/**
 * Compose has no stock lint check for hardcoded user-facing strings — Android's
 * `HardcodedText` only inspects XML layouts, and this app has none. This task fills that
 * gap so the Catalan-first localization rule in ADR-004 is enforced by the build rather
 * than by memory.
 *
 * It flags string literals passed to the arguments that end up on screen. Anything that
 * legitimately must be a literal can carry a trailing `// i18n-exempt` comment.
 */

abstract class CheckHardcodedUiText : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val patterns = listOf(
            Regex("""\btext\s*=\s*""""),
            Regex("""\bText\s*\(\s*""""),
            Regex("""\bcontentDescription\s*=\s*""""),
        )

        val violations = mutableListOf<String>()

        sources.files.filter { it.isFile && it.extension == "kt" }.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (line.contains("i18n-exempt")) return@forEachIndexed
                if (patterns.any { it.containsMatchIn(line) }) {
                    violations += "${file.path}:${index + 1}: ${line.trim()}"
                }
            }
        }

        val output = report.get().asFile
        output.parentFile.mkdirs()
        output.writeText(violations.joinToString(System.lineSeparator()))

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Hardcoded user-facing text found in ${violations.size} place(s).")
                    appendLine("Use stringResource(R.string.…) and add the string to res/values/strings.xml (Catalan).")
                    appendLine()
                    violations.forEach { appendLine("  $it") }
                },
            )
        }
    }
}

val checkNoHardcodedUiText = tasks.register<CheckHardcodedUiText>("checkNoHardcodedUiText") {
    group = "verification"
    description = "Fails when user-facing text is hardcoded instead of read from strings.xml."
    sources.from(
        fileTree("src/main/java") {
            include("**/*.kt")
        },
    )
    report.set(layout.buildDirectory.file("reports/hardcoded-ui-text.txt"))
}

tasks.named("check") {
    dependsOn(checkNoHardcodedUiText)
}
