package bo.saludencasa.i18n

import bo.saludencasa.ProjectSources
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainErrorModelingTest {
    @Test
    fun domainErrorsCarryTypesNotMessages() {
        val violations =
            ProjectSources.mainKotlinFiles().flatMap { file ->
                val text = file.readText()
                val fromConstructor = bareErrorConstructor.findAll(text).map { it.value }
                val fromField = errorClassWithMessageField.findAll(text).map { it.value }
                (fromConstructor + fromField).map { match -> "${file.path}: $match" }
            }

        assertTrue(
            "A domain error carries a user-facing message instead of a type " +
                "(.claude/rules/i18n.md, RequestError sealed type example):\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }
}

private val bareErrorConstructor = Regex("""\bError\(\s*"[^"]*"\s*\)""")
private val errorClassWithMessageField = Regex("""data class \w*Error\([^)]*\bmessage\s*:\s*String\b""")
