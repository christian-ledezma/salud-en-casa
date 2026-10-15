package bo.saludencasa.core.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

// Robolectric, because the date skeletons come from the platform.
@RunWith(AndroidJUnit4::class)
class AppointmentFormatTest {
    private val spanish = Locale.forLanguageTag("es-BO")

    // 02:00 UTC on Saturday is 22:00 on Friday in La Paz. Read in the phone's
    // zone, or in UTC, the professional's inbox would show the visit a day
    // late, which is the error this catches.
    @Test
    fun aStoredAppointmentIsReadInLaPazTimeEvenAcrossMidnight() {
        val text = formatAppointment(Instant.parse("2026-10-17T02:00:00Z"), spanish)

        assertEquals(formatAppointment(LocalDate.of(2026, 10, 16), LocalTime.of(22, 0), spanish), text)
        assertTrue(text, text.contains("16"))
        assertFalse(text, text.contains("17"))
    }

    @Test
    fun theAppointmentNamesItsDayAndMonthInTheDevicesLanguage() {
        val text = formatAppointment(LocalDate.of(2026, 10, 16), LocalTime.of(9, 30), spanish)

        assertTrue(text, text.contains("oct"))
        assertTrue(text, text.contains("9:30"))
        assertFalse(text, text.contains("2026"))
    }
}
