@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.domain.usecase

import bo.saludencasa.features.requests.FakeOfferRepository
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.offerThread
import bo.saludencasa.features.requests.requestOffer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SubmitCounterOfferUseCaseTest {
    private val repository = FakeOfferRepository()
    private val useCase = SubmitCounterOfferUseCase(repository)

    // The professional offered 160 against a budget of 130; the patient answered
    // 135; the professional came down to 150. The patient's floor is 135.
    private fun patientThread(budget: String? = "130.00") =
        offerThread(
            professionalId = "p1",
            offers =
                listOf(
                    requestOffer(id = "o1", amountBob = BigDecimal("160.00"), issuer = OfferIssuer.PROFESSIONAL),
                    requestOffer(
                        id = "o2",
                        parentOfferId = "o1",
                        amountBob = BigDecimal("135.00"),
                        issuer = OfferIssuer.PATIENT,
                    ),
                    requestOffer(
                        id = "o3",
                        parentOfferId = "o2",
                        amountBob = BigDecimal("150.00"),
                        issuer = OfferIssuer.PROFESSIONAL,
                    ),
                ),
        ).copy(suggestedBudgetBob = budget?.let(::BigDecimal))

    private suspend fun counter(
        thread: OfferThread = patientThread(),
        amountRaw: String = "140",
        message: String = "",
    ) = useCase(requestId = "r1", thread = thread, amountRaw = amountRaw, message = message)

    // HT-24: the patient never goes below what they already proposed on this
    // thread, even when the professional came down.
    @Test
    fun thePatientCannotCounterBelowTheirLastOffer() =
        runTest {
            assertEquals(
                OfferSubmitResult.Failure(OfferError.BelowMinimum(BigDecimal("135.00"))),
                counter(amountRaw = "134,99"),
            )
            assertTrue(repository.submitted.isEmpty())
        }

    // The budget published with the request is the patient's first offer.
    @Test
    fun thePatientCannotCounterBelowTheirBudget() =
        runTest {
            val firstAnswer =
                offerThread(offers = listOf(requestOffer(id = "o1", amountBob = BigDecimal("160.00"))))
                    .copy(suggestedBudgetBob = BigDecimal("130.00"))

            assertEquals(
                OfferSubmitResult.Failure(OfferError.BelowMinimum(BigDecimal("130.00"))),
                counter(thread = firstAnswer, amountRaw = "120"),
            )
        }

    // "Not less" is not "more": the floor itself is a valid counter-offer.
    @Test
    fun exactlyTheFloorIsAccepted() =
        runTest {
            assertEquals(OfferSubmitResult.Success, counter(amountRaw = "135"))
        }

    // Without a budget and before any counter-offer there is no floor.
    @Test
    fun withoutABudgetTheFirstCounterIsFree() =
        runTest {
            val noBudget =
                offerThread(offers = listOf(requestOffer(id = "o1", amountBob = BigDecimal("160.00"))))

            assertEquals(OfferSubmitResult.Success, counter(thread = noBudget, amountRaw = "50"))
        }

    // The rule is the patient's only: the professional may answer with anything.
    @Test
    fun theProfessionalHasNoFloor() =
        runTest {
            val professionalSide =
                patientThread().copy(
                    iAmTheProfessional = true,
                    offers =
                        patientThread().offers.dropLast(1),
                )

            assertEquals(OfferSubmitResult.Success, counter(thread = professionalSide, amountRaw = "10"))
        }

    // RF-08.2: the answer references the offer on the table, on the thread of
    // that professional, and leaves without a message when none was written.
    @Test
    fun carriesTheOfferOnTheTableAndNoMessage() =
        runTest {
            assertEquals(OfferSubmitResult.Success, counter(amountRaw = "139,90", message = "   "))

            val submission = repository.submitted.single()
            assertEquals("r1", submission.requestId)
            assertEquals("p1", submission.professionalId)
            assertEquals("o3", submission.parentOfferId)
            assertEquals(BigDecimal("139.90"), submission.amount.value)
            assertNull(submission.message)
        }

    @Test
    fun rejectsAnAmountThatIsNotPositive() =
        runTest {
            assertEquals(OfferSubmitResult.Failure(OfferError.InvalidAmount), counter(amountRaw = "0"))
            assertTrue(repository.submitted.isEmpty())
        }
}
