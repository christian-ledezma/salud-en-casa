package bo.saludencasa.architecture

import bo.saludencasa.ProjectSources
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// The project is a single Gradle module, so no compiler enforces the dependency
// rule: these three checks are what replaces the module boundary that the
// abandoned multiplatform structure used to provide (docs/decisions.md,
// 2026-09-10).
class ArchitectureRulesTest {
    @Test
    fun domainLayerHasNoPlatformImports() {
        val violations =
            filesInLayer("domain").flatMap { file ->
                importsOf(file)
                    .filter { import -> platformPrefixes.any(import::startsWith) }
                    .map { import -> "${ProjectSources.pathWithinSourceRoot(file)} imports $import" }
            }

        assertTrue(
            "The domain layer must stay free of platform types so its tests run on the JVM without " +
                "an emulator (RNF-09):\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun presentationLayerDoesNotImportTheDataLayer() {
        val violations =
            filesInLayer("presentation").flatMap { file ->
                importsOf(file)
                    .filter(dataLayerImport::containsMatchIn)
                    .map { import -> "${ProjectSources.pathWithinSourceRoot(file)} imports $import" }
            }

        assertTrue(
            "A screen or view model reaches the data layer directly instead of going through a use case:\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun featureNeverImportsTheDataLayerOfAnotherFeature() {
        val violations =
            ProjectSources.mainKotlinFiles().flatMap { file ->
                val path = ProjectSources.pathWithinSourceRoot(file)
                val owningFeature = featureOf(path) ?: return@flatMap emptyList()
                importsOf(file)
                    .filter { import ->
                        val importedFeature = featureOfDataLayerImport(import)
                        importedFeature != null && importedFeature != owningFeature
                    }.map { import -> "$path imports $import" }
            }

        assertTrue(
            "A feature reaches into another feature's data layer instead of asking for its use case " +
                "or repository interface:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }
}

private val platformPrefixes = listOf("androidx.", "android.", "io.github.jan.supabase.")
private val importStatement = Regex("""^import\s+([A-Za-z0-9_.]+)""", RegexOption.MULTILINE)
private val dataLayerImport = Regex("""^bo\.saludencasa\.[A-Za-z0-9_.]*\bdata\.""")
private val featureDataLayerImport = Regex("""^bo\.saludencasa\.features\.([A-Za-z0-9_]+)\.data\.""")
private val featurePath = Regex("""(?:^|/)features/([^/]+)/""")

private fun importsOf(file: File): List<String> =
    importStatement.findAll(file.readText()).map { it.groupValues[1] }.toList()

private fun filesInLayer(layer: String): List<File> =
    ProjectSources.mainKotlinFiles().filter { file ->
        ProjectSources.pathWithinSourceRoot(file).split("/").contains(layer)
    }

private fun featureOf(pathWithinSourceRoot: String): String? {
    val match = featurePath.find(pathWithinSourceRoot) ?: return null
    return match.groupValues[1]
}

private fun featureOfDataLayerImport(import: String): String? {
    val match = featureDataLayerImport.find(import) ?: return null
    return match.groupValues[1]
}
