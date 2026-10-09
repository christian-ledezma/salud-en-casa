package bo.saludencasa.features.catalog.domain.model

import bo.saludencasa.features.catalog.declaredServices
import bo.saludencasa.features.catalog.generalConsultation
import bo.saludencasa.features.catalog.physiotherapy
import bo.saludencasa.features.catalog.professionalService
import bo.saludencasa.features.catalog.venousLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeclaredServicesTest {
    // HU-10's third criterion is a refusal, and the screen's half of it is not
    // offering the type twice in the first place. Were this to return the whole
    // catalog, the professional would be invited into a write the unique index
    // is going to reject.
    @Test
    fun theUndeclaredTypesExcludeEveryTypeAlreadyDeclared() {
        val declared =
            declaredServices(
                services = listOf(professionalService(type = generalConsultation)),
                catalog = listOf(generalConsultation, venousLine, physiotherapy),
            )

        assertEquals(listOf(venousLine, physiotherapy), declared.undeclaredTypes)
    }

    @Test
    fun aProfessionalWhoDeclaredEveryTypeHasNothingLeftToAdd() {
        val catalog = listOf(generalConsultation, venousLine)
        val declared =
            declaredServices(
                services =
                    catalog.mapIndexed { index, type ->
                        professionalService(id = "declared-$index", type = type)
                    },
                catalog = catalog,
            )

        assertTrue(declared.undeclaredTypes.isEmpty())
    }

    // What the use case asks before writing. A type the person does not offer
    // has to come back false, or declaring anything at all becomes impossible.
    @Test
    fun declaresAnswersForTheTypesOfferedAndOnlyForThose() {
        val declared =
            declaredServices(
                services = listOf(professionalService(type = venousLine)),
                catalog = listOf(generalConsultation, venousLine),
            )

        assertTrue(declared.declares(venousLine.id))
        assertFalse(declared.declares(generalConsultation.id))
    }
}
