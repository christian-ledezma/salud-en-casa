package bo.saludencasa.schema

import bo.saludencasa.ProjectSources
import org.junit.Assert.assertTrue
import org.junit.Test

// Row level security policies and triggers run inside PostgreSQL, so the JVM
// cannot exercise them (.claude/rules/testing.md). What it can do is read the
// migrations and refuse the two regressions that no compiler and no screen would
// ever notice.
class SchemaRulesTest {
    // INV-12 and RN-11 hold by absence: there is no administrator policy on
    // messages, and the day someone adds one nothing fails except this.
    @Test
    fun noMigrationGrantsTheAdministratorAccessToMessages() {
        val violations =
            ProjectSources.migrationFiles().flatMap { file ->
                messagesPolicy
                    .findAll(file.readText())
                    .map { it.value }
                    .filter { it.contains("is_admin", ignoreCase = true) }
                    .map { "${file.name}: ${it.lines().first()}" }
            }

        assertTrue(
            "An administrator policy on messages breaks INV-12:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    // A definer function without a pinned search_path can be hijacked by an
    // object the caller puts earlier in the path.
    @Test
    fun everySecurityDefinerFunctionPinsItsSearchPath() {
        val violations =
            ProjectSources.migrationFiles().flatMap { file ->
                functionHeader
                    .findAll(file.readText())
                    .filter { it.value.contains("security definer", ignoreCase = true) }
                    .filterNot { it.value.contains("set search_path", ignoreCase = true) }
                    .map { "${file.name}: ${it.groupValues[1]}" }
            }

        assertTrue(
            "A security definer function does not pin search_path:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }
}

private val messagesPolicy =
    Regex("""create\s+policy\s+\w+\s+on\s+public\.messages[^;]*;""", RegexOption.IGNORE_CASE)
private val functionHeader =
    Regex("""create\s+(?:or\s+replace\s+)?function\s+([\w.]+)[\s\S]*?\bas\s+\$\$""", RegexOption.IGNORE_CASE)
