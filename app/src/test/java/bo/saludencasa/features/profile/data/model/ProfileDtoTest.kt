package bo.saludencasa.features.profile.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

// PostgREST is the other half of this contract and it is not in the test, so
// what these two pin is the shape the client expects and produces. Both sides
// of a money column go through the literal text of the number.
class ProfileDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    // A numeric column arrives as a bare JSON number. Reading it as a Double
    // first and building the BigDecimal from that loses the stored scale, and
    // the rate would come back as 120.0 for a column that holds 120.00.
    @Test
    fun aNumericColumnIsReadWithoutLosingItsScale() {
        val row =
            """
            {
              "id": "fcab94c0-c5e2-4c34-80a5-3735dce40dbd",
              "professional_type": "NURSE",
              "specialty": "Enfermería geriátrica",
              "biography": null,
              "years_of_experience": 10,
              "base_rate_bob": 120.00,
              "coverage_radius_km": 8.00,
              "available_now": true,
              "verification_status": "PENDING",
              "total_services": 0
            }
            """.trimIndent()

        val dto = json.decodeFromString<ProfessionalDto>(row)

        assertEquals(BigDecimal("120.00"), dto.baseRateBob)
        assertEquals(BigDecimal("8.00"), dto.coverageRadiusKm)
    }

    // The amount leaves as the exact text of the number, which the server casts
    // to the numeric argument. Letting kotlinx encode it as a JSON number sends
    // it through Double and 120.00 arrives as 120.0: harmless for two decimals,
    // and the wrong habit to build into the path that offers and payments will
    // reuse.
    @Test
    fun anAmountLeavesAsItsExactText() {
        val encoded =
            json.encodeToString(
                SaveProfileParams(
                    fullName = "Ana Quispe",
                    baseRateBob = BigDecimal("120.00"),
                    coverageRadiusKm = BigDecimal("8"),
                ),
            )

        assertTrue(encoded, encoded.contains(""""p_base_rate_bob":"120.00""""))
        assertTrue(encoded, encoded.contains(""""p_coverage_radius_km":"8""""))
    }
}
