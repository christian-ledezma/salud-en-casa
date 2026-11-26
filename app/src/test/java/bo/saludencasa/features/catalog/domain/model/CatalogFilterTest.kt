package bo.saludencasa.features.catalog.domain.model

import bo.saludencasa.features.catalog.generalConsultation
import bo.saludencasa.features.catalog.physiotherapy
import bo.saludencasa.features.catalog.serviceType
import bo.saludencasa.features.catalog.venousLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogFilterTest {
    private val prenatal = serviceType(id = "type-prenatal", name = "Control prenatal y posparto")
    private val elderCare = serviceType(id = "type-elder-care", name = "Cuidado de adulto mayor")
    private val postoperative = serviceType(id = "type-postoperative", name = "Cuidado postoperatorio")
    private val catalog = listOf(generalConsultation, prenatal, elderCare, postoperative, venousLine, physiotherapy)

    private fun search(query: String): List<ServiceType> = catalog.matchingName(query, ServiceType::name)

    // Spanish accents are hard to type on the keyboards the professionals use.
    // A search that needed them would answer "no results" for a type that is
    // in the catalog under its own name.
    @Test
    fun findsATypeWhoseNameHasAnAccentWithoutTypingIt() {
        assertEquals(listOf(generalConsultation), search("medica"))
        assertEquals(listOf(physiotherapy), search("fisica"))
    }

    @Test
    fun ignoresCapitalsAndTheSpacesAroundTheQuery() {
        assertEquals(listOf(venousLine), search("  VÍA venosa "))
    }

    // The words of a name are not the only place a person remembers it from.
    @Test
    fun findsAQueryInTheMiddleOfAWord() {
        assertEquals(listOf(prenatal), search("natal"))
    }

    @Test
    fun keepsTheCatalogOrderOfEveryMatch() {
        assertEquals(listOf(elderCare, postoperative), search("cuidado"))
    }

    // A blank field means "show everything". Answering with nothing would
    // empty the selector the moment the person clears what they typed.
    @Test
    fun aBlankQueryReturnsTheWholeListUntouched() {
        assertEquals(catalog, search(""))
        assertEquals(catalog, search("   "))
    }

    @Test
    fun aQueryThatMatchesNothingReturnsAnEmptyList() {
        assertTrue(search("dentista").isEmpty())
    }

    // Declared services are searched through the name of their type, and the
    // same rule has to hold for any list the caller can name a title for.
    @Test
    fun filtersAnyListByTheNameTheCallerPicks() {
        val declared = listOf(professionalOf(venousLine), professionalOf(physiotherapy))

        assertEquals(listOf(declared[1]), declared.matchingName("terapia") { it.type.name })
    }

    private fun professionalOf(type: ServiceType): ProfessionalService =
        ProfessionalService(id = "declared-${type.id}", type = type, referencePriceBob = type.referencePriceBob)
}
