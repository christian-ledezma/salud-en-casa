package bo.saludencasa.features.catalog.data.mapper

import bo.saludencasa.features.catalog.data.model.ProfessionalServiceDto
import bo.saludencasa.features.catalog.data.model.ServiceTypeDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CatalogMapperTest {
    private val consultation =
        ServiceTypeDto(
            id = "type-consultation",
            name = "Consulta médica general",
            description = "Valoración médica en domicilio, anamnesis y examen físico.",
            referencePriceBob = BigDecimal("150.00"),
            estimatedDurationMin = 45,
        )

    private val physiotherapy =
        ServiceTypeDto(
            id = "type-physiotherapy",
            name = "Terapia física y rehabilitación",
            description = null,
            referencePriceBob = BigDecimal("130.00"),
            estimatedDurationMin = 60,
        )

    private fun declaredRow(
        id: String,
        typeId: String,
        price: String,
    ): ProfessionalServiceDto =
        ProfessionalServiceDto(
            id = id,
            serviceTypeId = typeId,
            referencePriceBob = BigDecimal(price),
        )

    // The professional's own price is the one that travels, not the catalog's:
    // taking the reference by mistake would publish a rate nobody declared.
    @Test
    fun aDeclaredServiceKeepsItsOwnPriceAndNotTheCatalogReference() {
        val declared =
            declaredServicesOf(
                types = listOf(consultation),
                services = listOf(declaredRow("declared-1", consultation.id, "180.00")),
            )

        val service = declared.services.single()
        assertEquals(BigDecimal("180.00"), service.referencePriceBob)
        assertEquals(BigDecimal("150.00"), service.type.referencePriceBob)
    }

    // service_types_select_all returns only the active entries, so a type the
    // administrator deactivated has no row to join against. Keeping the service
    // would mean drawing a card with no name and no duration on it.
    @Test
    fun aDeclaredServiceWhoseTypeLeftTheCatalogIsDropped() {
        val declared =
            declaredServicesOf(
                types = listOf(consultation),
                services =
                    listOf(
                        declaredRow("declared-1", consultation.id, "180.00"),
                        declaredRow("declared-2", "type-withdrawn", "90.00"),
                    ),
            )

        assertEquals(listOf("declared-1"), declared.services.map { it.id })
    }

    // Two reads arrive in whatever order the server sends them, and the list is
    // what the professional scans to find the one they want to reprice.
    @Test
    fun theDeclaredServicesAreOrderedByTheNameOfTheirType() {
        val declared =
            declaredServicesOf(
                types = listOf(consultation, physiotherapy),
                services =
                    listOf(
                        declaredRow("declared-physio", physiotherapy.id, "140.00"),
                        declaredRow("declared-consultation", consultation.id, "180.00"),
                    ),
            )

        assertEquals(
            listOf("Consulta médica general", "Terapia física y rehabilitación"),
            declared.services.map { it.type.name },
        )
    }
}
