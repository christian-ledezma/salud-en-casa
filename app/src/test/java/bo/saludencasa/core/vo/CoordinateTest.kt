package bo.saludencasa.core.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateTest {
    @Test
    fun acceptsAPointInsideTheCountry() {
        val coordinate = Coordinate.create(-16.4957, -68.1335).getOrNull()

        assertEquals(-16.4957, coordinate?.latitude)
        assertEquals(-68.1335, coordinate?.longitude)
    }

    @Test
    fun acceptsBothLatitudeBounds() {
        assertTrue(Coordinate.create(-90.0, 0.0).isSuccess)
        assertTrue(Coordinate.create(90.0, 0.0).isSuccess)
    }

    @Test
    fun acceptsBothLongitudeBounds() {
        assertTrue(Coordinate.create(0.0, -180.0).isSuccess)
        assertTrue(Coordinate.create(0.0, 180.0).isSuccess)
    }

    @Test
    fun rejectsLatitudeOutOfRange() {
        assertNull(Coordinate.create(-90.000001, 0.0).getOrNull())
        assertNull(Coordinate.create(90.000001, 0.0).getOrNull())
    }

    @Test
    fun rejectsLongitudeOutOfRange() {
        assertNull(Coordinate.create(0.0, -180.000001).getOrNull())
        assertNull(Coordinate.create(0.0, 180.000001).getOrNull())
    }

    // Every range check above answers false for a not-a-number, so without its
    // own branch such a pair would pass validation and reach the geography
    // column as a point PostGIS refuses to parse, with the save failing as an
    // unexplained server error.
    @Test
    fun rejectsANotANumberThatWouldSlipThroughEveryRangeCheck() {
        assertNull(Coordinate.create(Double.NaN, -68.1335).getOrNull())
        assertNull(Coordinate.create(-16.4957, Double.NaN).getOrNull())
    }

    @Test
    fun rejectsAnInfiniteValue() {
        assertNull(Coordinate.create(Double.POSITIVE_INFINITY, 0.0).getOrNull())
        assertNull(Coordinate.create(0.0, Double.NEGATIVE_INFINITY).getOrNull())
    }
}
