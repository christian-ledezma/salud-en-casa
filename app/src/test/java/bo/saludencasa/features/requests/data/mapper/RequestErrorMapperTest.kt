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

    @Test
    fun mapsAConnectionFailureToNetworkUnavailable() {
        assertEquals(RequestError.NetworkUnavailable, IOException("boom").toRequestError())
    }

    @Test
    fun mapsAnythingElseToUnexpected() {
        assertEquals(RequestError.Unexpected, RuntimeException("something odd").toRequestError())
    }
}
