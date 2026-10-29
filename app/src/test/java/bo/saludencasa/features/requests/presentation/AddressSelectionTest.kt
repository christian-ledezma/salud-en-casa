package bo.saludencasa.features.requests.presentation

import bo.saludencasa.features.location.address
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressSelectionTest {
    private val office = address(id = "a1", alias = "Consultorio", isPrimary = true)
    private val work = address(id = "a2", alias = "Trabajo", isPrimary = false)
    private val home = address(id = "a3", alias = "Casa", isPrimary = false)

    // HT-24: the person left the form to add an address, so the one they added is
    // the one they mean to be cared for at.
    @Test
    fun theAddressJustAddedIsTheOneChosen() {
        val refreshed = listOf(office, work, home)

        assertEquals("a3", refreshed.selectionAfterRefresh(previous = listOf(office, work), previousSelectedId = "a2"))
    }

    // Coming back without adding anything keeps the choice already made.
    @Test
    fun withNothingAddedTheChoiceStays() {
        val same = listOf(office, work)

        assertEquals("a2", same.selectionAfterRefresh(previous = same, previousSelectedId = "a2"))
    }

    // An address deleted meanwhile cannot stay chosen: the primary takes its place.
    @Test
    fun aChoiceThatWasDeletedFallsBackToThePrimary() {
        assertEquals(
            "a1",
            listOf(office).selectionAfterRefresh(previous = listOf(office, work), previousSelectedId = "a2"),
        )
    }

    // More than one new address says nothing about which one is meant.
    @Test
    fun severalNewAddressesKeepTheChoiceAlreadyMade() {
        val refreshed = listOf(office, work, home)

        assertEquals("a1", refreshed.selectionAfterRefresh(previous = listOf(office), previousSelectedId = "a1"))
    }
}
