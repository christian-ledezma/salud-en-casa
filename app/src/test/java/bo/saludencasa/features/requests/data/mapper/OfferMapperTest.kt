package bo.saludencasa.features.requests.data.mapper

import bo.saludencasa.features.requests.data.model.RequestOfferDto
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class OfferMapperTest {
    private fun dto(
        id: String,
        parentOfferId: String? = null,
        professionalId: String = "p1",
        issuer: String = "PROFESSIONAL",
        status: String = "PROPOSED",
        amount: String = "160.00",
        createdAt: String = "2026-10-27T18:00:00+00:00",
    ) = RequestOfferDto(
        id = id,
        parentOfferId = parentOfferId,
        professionalId = professionalId,
        professionalName = "Ana Pérez",
        professionalPhotoUrl = null,
        professionalRating = BigDecimal("4.80"),
        amountBob = BigDecimal(amount),
        message = null,
        issuer = issuer,
        status = status,
        createdAt = createdAt,
    )

    // HT-24: each thread carries its professional's distance, rounded to whole
    // metres, and the budget the patient published, which is their floor.
    @Test
    fun eachThreadCarriesItsDistanceAndTheBudget() {
        val threads =
            listOf(
                dto("a1", professionalId = "p1").copy(distanceM = 1200.0, suggestedBudgetBob = BigDecimal("130.00")),
                dto("b1", professionalId = "p2").copy(distanceM = 3400.0, suggestedBudgetBob = BigDecimal("130.00")),
            ).toOfferThreads("patient-1")!!

        val first = threads.first { it.professionalId == "p1" }
        assertEquals(1200, first.distanceM)
        assertEquals(3400, threads.first { it.professionalId == "p2" }.distanceM)
        assertEquals(BigDecimal("130.00"), first.counterFloorBob)
    }

    // The floor is the greater of the budget and the patient's own highest offer,
    // and the professional has none.
    @Test
    fun theFloorIsTheGreaterOfTheBudgetAndThePatientsOwnOffer() {
        val rows =
            listOf(
                dto("o1").copy(suggestedBudgetBob = BigDecimal("130.00")),
                dto("o2", parentOfferId = "o1", issuer = "PATIENT", amount = "140.00")
                    .copy(suggestedBudgetBob = BigDecimal("130.00")),
                dto("o3", parentOfferId = "o2", amount = "150.00").copy(suggestedBudgetBob = BigDecimal("130.00")),
            )

        assertEquals(BigDecimal("140.00"), rows.toOfferThreads("patient-1")!!.single().counterFloorBob)
        assertNull(rows.toOfferThreads("p1")!!.single().counterFloorBob)
    }

    @Test
    fun groupsTheOffersIntoOneThreadPerProfessional() {
        val threads =
            listOf(
                dto("a1", professionalId = "p1"),
                dto("b1", professionalId = "p2"),
                dto("a2", parentOfferId = "a1", professionalId = "p1", issuer = "PATIENT"),
            ).toOfferThreads("patient-1")!!

        assertEquals(2, threads.size)
        assertEquals(setOf("p1", "p2"), threads.map { it.professionalId }.toSet())
        assertEquals(2, threads.first { it.professionalId == "p1" }.offers.size)
    }

    // Two offers written in the same transaction carry the same created_at, so
    // time does not order a chain. The parent reference does: with the rows
    // handed over backwards, the chain still comes out first-to-last.
    @Test
    fun ordersTheChainByItsParentAndNotByTheTimestamp() {
        val threads =
            listOf(
                dto("o3", parentOfferId = "o2"),
                dto("o1"),
                dto("o2", parentOfferId = "o1", issuer = "PATIENT"),
            ).toOfferThreads("patient-1")!!

        assertEquals(listOf("o1", "o2", "o3"), threads.single().offers.map { it.id })
    }

    // A chain missing its root cannot be rebuilt, and re-sorting it here would
    // invent an order: the rows keep the one the engine sent, and none is lost.
    @Test
    fun aChainWithoutItsRootKeepsTheOrderTheEngineSent() {
        val thread =
            listOf(
                dto("o2", parentOfferId = "o1", issuer = "PATIENT"),
                dto("o3", parentOfferId = "o2"),
            ).toOfferThreads("patient-1")!!
                .single()

        assertEquals(listOf("o2", "o3"), thread.offers.map { it.id })
        assertEquals("o3", thread.lastOffer?.id)
    }

    // Which side the reader is on comes from the identifier, not from a role.
    @Test
    fun theProfessionalOfTheThreadIsTheSignedInUser() {
        val mine = listOf(dto("o1", professionalId = "p1")).toOfferThreads("p1")!!.single()
        val theirs = listOf(dto("o1", professionalId = "p1")).toOfferThreads("patient-1")!!.single()

        assertTrue(mine.iAmTheProfessional)
        assertEquals(OfferIssuer.PROFESSIONAL, mine.myIssuer)
        assertFalse(theirs.iAmTheProfessional)
        assertEquals(OfferIssuer.PATIENT, theirs.myIssuer)
    }

    // A rejected last offer closes the thread for both sides, so neither is
    // offered a control that the engine would refuse.
    @Test
    fun aRejectedLastOfferClosesTheThread() {
        val thread =
            listOf(
                dto("o1"),
                dto("o2", parentOfferId = "o1", issuer = "PATIENT", status = "REJECTED"),
            ).toOfferThreads("patient-1")!!
                .single()

        assertTrue(thread.isClosed)
        assertFalse(thread.isMyTurn)
        assertEquals(OfferStatus.REJECTED, thread.lastOffer?.status)
    }

    // The turn belongs to whoever did not write the offer on the table.
    @Test
    fun theTurnBelongsToTheOneWhoDidNotWriteTheLastOffer() {
        val rows = listOf(dto("o1"), dto("o2", parentOfferId = "o1", issuer = "PATIENT"))

        assertFalse(rows.toOfferThreads("patient-1")!!.single().isMyTurn)
        assertTrue(rows.toOfferThreads("p1")!!.single().isMyTurn)
    }

    // An enum value the client does not know is a contract break, not a row to
    // drop silently: the whole read fails and the screen says so.
    @Test
    fun anUnknownStatusFailsTheWholeRead() {
        assertNull(listOf(dto("o1", status = "SETTLED")).toOfferThreads("patient-1"))
    }

    @Test
    fun anUnknownIssuerFailsTheWholeRead() {
        assertNull(listOf(dto("o1", issuer = "ADMIN")).toOfferThreads("patient-1"))
    }
}
