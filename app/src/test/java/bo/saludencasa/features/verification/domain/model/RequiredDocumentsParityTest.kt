package bo.saludencasa.features.verification.domain.model

import bo.saludencasa.ProjectSources
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test

// The set of documents a person must have approved lives twice: here, to decide
// which button to show, and in required_document_types, which is the copy that
// decides. If they drift the engine never approves anyone it should not, but the
// screen starts offering a button the engine refuses, or hides one it would
// accept. This reads the migration so changing one side alone fails the build.
class RequiredDocumentsParityTest {
    @Test
    fun theSetsTheEngineRequiresMatchTheOnesTheChecklistAsks() {
        val (studentProfessional, otherProfessional, patient) = requiredArraysFromTheMigration()

        assertEquals(studentProfessional, required(setOf(UserRole.PROFESSIONAL), ProfessionalType.STUDENT))
        listOf(ProfessionalType.DOCTOR, ProfessionalType.NURSE, ProfessionalType.PHYSIOTHERAPIST).forEach { type ->
            assertEquals("professional type $type", otherProfessional, required(setOf(UserRole.PROFESSIONAL), type))
        }
        assertEquals(patient, required(setOf(UserRole.PATIENT), null))
    }

    @Test
    fun anAdministratorOwesNoDocumentInEitherCopy() {
        assertEquals(emptySet<String>(), required(setOf(UserRole.ADMIN), null))
        assertEquals(true, functionBody().contains("else '{}'::public.document_type[]"))
    }

    private fun required(
        roles: Set<UserRole>,
        type: ProfessionalType?,
    ): Set<String> =
        VerificationChecklist
            .create(roles, type, emptyList())
            .required
            .map { it.name }
            .toSet()

    private fun functionBody(): String {
        val defining =
            ProjectSources
                .migrationFiles()
                .map { it.readText() }
                .lastOrNull { functionStart.containsMatchIn(it) }
        checkNotNull(defining) { "No migration defines required_document_types." }
        val start = functionStart.find(defining)!!.range.first
        return defining.substring(start, defining.indexOf("\$\$;", defining.indexOf("as \$\$", start)))
    }

    // Order matters and is asserted by size: student professional, any other
    // professional, patient. Reordering the branches fails loudly, not silently.
    private fun requiredArraysFromTheMigration(): List<Set<String>> {
        val arrays =
            arrayLiteral
                .findAll(functionBody())
                .map { match ->
                    match.groupValues[1]
                        .split(",")
                        .map { it.trim().trim('\'') }
                        .toSet()
                }.toList()
        assertEquals("required_document_types must declare three arrays", 3, arrays.size)
        return arrays
    }
}

private val functionStart = Regex("""create\s+or\s+replace\s+function\s+public\.required_document_types""")
private val arrayLiteral = Regex("""array\[([^\]]*)]::public\.document_type\[]""")
