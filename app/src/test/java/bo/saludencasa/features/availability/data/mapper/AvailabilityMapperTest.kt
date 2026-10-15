package bo.saludencasa.features.availability.data.mapper

import bo.saludencasa.features.availability.data.model.AvailabilitySlotDto
import bo.saludencasa.features.availability.domain.model.AvailabilityError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class AvailabilityMapperTest {
    @Test
    fun dtoIsMappedToDomainWithTheRightIsoDayAndTimes() {
        val dto =
            AvailabilitySlotDto(
                id = "x",
                dayOfWeek = 3,
                startTime = "08:30:00",
                endTime = "10:00:00",
            )

        val slot = dto.toDomain()

        assertEquals("x", slot.id)
        assertEquals(DayOfWeek.WEDNESDAY, slot.dayOfWeek)
        assertEquals(LocalTime.of(8, 30), slot.startTime)
        assertEquals(LocalTime.of(10, 0), slot.endTime)
    }

    @Test
    fun localTimeIsFormattedWithSeconds() {
        assertEquals("09:00:00", LocalTime.of(9, 0).toColumnText())
        assertEquals("09:15:00", LocalTime.of(9, 15).toColumnText())
    }

    // Both signatures hit the same unique-violation errcode (23505) and should
    // present the same way, so the dialog does not need to tell apart "exact
    // duplicate" from "partial overlap".
    @Test
    fun overlapSignatureMapsToOverlapError() {
        val error = RuntimeException("code 23505: availability_slot_overlaps")

        assertEquals(AvailabilityError.OverlapsExistingSlot, error.toAvailabilityError())
    }

    @Test
    fun duplicateKeyMapsToOverlapError() {
        val error =
            RuntimeException(
                "duplicate key value violates unique constraint " +
                    "\"availability_slots_professional_id_day_of_week_start_time_end_time_key\"",
            )

        assertEquals(AvailabilityError.OverlapsExistingSlot, error.toAvailabilityError())
    }

    @Test
    fun networkFailureMapsToNetworkError() {
        val request = HttpRequestBuilder().apply { url("https://example.supabase.co/rest/v1/availability_slots") }
        val error = HttpRequestException("connection reset", request)

        assertEquals(AvailabilityError.NetworkUnavailable, error.toAvailabilityError())
    }

    @Test
    fun unknownErrorMapsToUnexpected() {
        val error = RuntimeException("something else")

        assertEquals(AvailabilityError.Unexpected, error.toAvailabilityError())
    }
}
