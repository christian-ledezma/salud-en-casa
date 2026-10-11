package bo.saludencasa.features.search.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FitZoomTest {
    private val cochabamba = -17.3835

    // The shorter side of the map box on a 360 dp phone: 280 dp tall beats
    // 320 dp wide.
    private val mapSideDp = 280f

    // The zoom level is logarithmic in base two: twice the ground covered is
    // exactly one level out. Getting the base wrong, or dividing where a
    // multiplication belongs, shows as a camera that frames a circle of the
    // wrong size, which on a map nobody reads as an error.
    @Test
    fun doublingTheRadiusCostsExactlyOneZoomLevel() {
        val near = fitZoom(radiusKm = 2, latitude = cochabamba, sideDp = mapSideDp)
        val far = fitZoom(radiusKm = 4, latitude = cochabamba, sideDp = mapSideDp)

        assertEquals(1f, near - far, 0.001f)
    }

    @Test
    fun aWiderRadiusAlwaysZoomsFurtherOut() {
        val two = fitZoom(radiusKm = 2, latitude = cochabamba, sideDp = mapSideDp)
        val five = fitZoom(radiusKm = 5, latitude = cochabamba, sideDp = mapSideDp)
        val ten = fitZoom(radiusKm = 10, latitude = cochabamba, sideDp = mapSideDp)

        assertTrue("$two should be closer in than $five", two > five)
        assertTrue("$five should be closer in than $ten", five > ten)
    }

    // Mercator stretches away from the equator, so a pixel covers less ground
    // the further from it one is and the same circle needs one more step out.
    // Dropping the cosine would frame a circle noticeably too small in Bolivia.
    @Test
    fun theSameRadiusNeedsLessZoomFurtherFromTheEquator() {
        val equator = fitZoom(radiusKm = 5, latitude = 0.0, sideDp = mapSideDp)
        val bolivia = fitZoom(radiusKm = 5, latitude = cochabamba, sideDp = mapSideDp)

        assertTrue("$bolivia should be further out than $equator", bolivia < equator)
    }

    // A circle of five kilometres across a phone lands between the zoom of a
    // district and that of a block. Outside that range the number is a defect
    // even when the arithmetic above holds.
    @Test
    fun theDefaultRadiusFramesAtACityZoom() {
        val zoom = fitZoom(radiusKm = 5, latitude = cochabamba, sideDp = mapSideDp)

        assertTrue("$zoom is not a city zoom", zoom in 10f..14f)
    }
}
