package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.domain.model.RequestError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class RequestErrorMapperTest {
    // The function raises bare labels and supabase-kt carries the body as the
    // message: a change to the label here is the regression this catches.
    @Test
    fun readsTheAddressOwnershipRefusalFromTheBody() {
        val error = RuntimeException("""{"message":"address_not_owned"}""").toRequestError()
        assertEquals(RequestError.AddressNotOwned, error)
    }

    @Test
    fun readsTheServiceTypeRefusalFromTheBody() {
        val error = RuntimeException("""{"message":"service_type_unavailable"}""").toRequestError()
        assertEquals(RequestError.ServiceTypeUnavailable, error)
    }

    // HU-16: one label per refusal of create_scheduled_request. None is a
    // substring of another, which the matching by contains depends on.
    @Test
    fun readsEachRefusalOfTheScheduledCreationFromTheBody() {
        val expected =
            mapOf(
                "cannot_schedule_with_yourself" to RequestError.CannotScheduleWithYourself,
                "professional_unavailable" to RequestError.ProfessionalUnavailable,
                "service_not_offered" to RequestError.ServiceNotOffered,
                "outside_coverage" to RequestError.OutsideCoverage,
                "time_not_bookable" to RequestError.TimeNotBookable,
                "service_type_unavailable" to RequestError.ServiceTypeUnavailable,
            )

        expected.forEach { (label, error) ->
            assertEquals(label, error, RuntimeException("""{"message":"$label"}""").toRequestError())
        }
    }

    @Test
    fun mapsAConnectionFailureToNetworkUnavailable() {
        assertEquals(RequestError.NetworkUnavailable, IOException("boom").toRequestError())
    }

    @Test
    fun mapsAnythingElseToUnexpected() {
        assertEquals(RequestError.Unexpected, RuntimeException("something odd").toRequestError())
    }
}
