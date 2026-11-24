package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.domain.model.ConfirmPaymentError
import bo.saludencasa.features.services.domain.model.ConfirmPaymentResult
import bo.saludencasa.features.services.domain.model.PaymentMethod
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmReceiptUseCaseTest {
    private val repository = FakeServiceRepository()
    private val confirmReceipt = ConfirmReceiptUseCase(repository)

    // FA-02 holds for the professional who declares first as for the patient.
    @Test
    fun aGatewayReceiptIsRefusedBeforeReachingTheEngine() =
        runTest {
            val result = confirmReceipt("s1", PaymentMethod.GATEWAY)

            assertEquals(ConfirmPaymentResult.Failure(ConfirmPaymentError.MethodNotAllowed), result)
            assertTrue(repository.receipts.isEmpty())
        }

    @Test
    fun aReceiptIsNeverSentAsThePatientsConfirmation() =
        runTest {
            assertEquals(ConfirmPaymentResult.Confirmed, confirmReceipt("s1", PaymentMethod.CASH))

            assertEquals(listOf(PaymentMethod.CASH), repository.receipts)
            assertTrue(repository.payments.isEmpty())
        }
}
