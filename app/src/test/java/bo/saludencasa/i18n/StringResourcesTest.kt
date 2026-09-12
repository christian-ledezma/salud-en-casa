package bo.saludencasa.i18n

import bo.saludencasa.ProjectSources
import org.junit.Assert.assertTrue
import org.junit.Test

class StringResourcesTest {
    @Test
    fun everyStringKeyUsedInCodeExistsInAllLocales() {
        val usedKeys =
            ProjectSources
                .mainKotlinFiles()
                .flatMap { stringResourceUsage.findAll(it.readText()).map { match -> match.groupValues[1] } }
                .toSet()

        val localeFiles = ProjectSources.localeStringsXmlFiles()
        assertTrue("No strings.xml locale files were found under app/src/main/res.", localeFiles.isNotEmpty())

        val missingByLocale =
            localeFiles
                .associate { file ->
                    val declaredKeys = declaredStringKey.findAll(file.readText()).map { it.groupValues[1] }.toSet()
                    file.path to (usedKeys - declaredKeys)
                }.filterValues { it.isNotEmpty() }

        assertTrue(
            "Keys referenced from code but missing from a locale file:\n" +
                missingByLocale.entries.joinToString("\n") { (path, keys) -> "$path: $keys" },
            missingByLocale.isEmpty(),
        )
    }

    @Test
    fun noVisibleTextIsHardcodedInScreens() {
        val violations =
            ProjectSources.mainKotlinFiles().flatMap { file ->
                val withoutPreviews = stripPreviewFunctions(file.readText())
                hardcodedVisibleTextPatterns
                    .flatMap { pattern -> pattern.findAll(withoutPreviews).map { it.groupValues[1] } }
                    .map { literal -> "${file.path}: \"$literal\"" }
            }

        assertTrue(
            "User-visible text written directly in code instead of a string resource:\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }
}

private val stringResourceUsage = Regex("""R\.string\.([A-Za-z0-9_]+)""")
private val declaredStringKey = Regex("""<string\s+name="([A-Za-z0-9_]+)"""")

// Preview composables render only in tooling, never in the running app, so
// their sample literals are not user-visible text and are excluded before
// scanning for hardcoded strings.
private val previewFunctionStart = Regex("""@Preview[\s\S]{0,300}?fun\s+\w+\s*\([^)]*\)\s*\{""")

private fun stripPreviewFunctions(source: String): String {
    var result = source
    var match = previewFunctionStart.find(result)
    while (match != null) {
        var depth = 1
        var index = match.range.last + 1
        while (index < result.length && depth > 0) {
            when (result[index]) {
                '{' -> depth++
                '}' -> depth--
            }
            index++
        }
        result = result.removeRange(match.range.first, index)
        match = previewFunctionStart.find(result)
    }
    return result
}

private val hardcodedVisibleTextPatterns =
    listOf(
        Regex("""\bText\(\s*"([^"]+)""""),
        Regex("""\bText\(\s*text\s*=\s*"([^"]+)""""),
        Regex("""\bcontentDescription\s*=\s*"([^"]+)""""),
        Regex("""\.showSnackbar\(\s*"([^"]+)""""),
        Regex("""Toast\.makeText\([^,]*,\s*"([^"]+)""""),
    )
