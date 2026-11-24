package bo.saludencasa.features.services.domain.usecase

import bo.saludencasa.features.services.FakeServiceRepository
import bo.saludencasa.features.services.domain.model.ConfirmPaymentError
import bo.saludencasa.features.services.domain.model.ConfirmPaymentResult
import bo.saludencasa.features.services.domain.model.PaymentMethod
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmPaymentUseCaseTest {
    private val repository = FakeServiceRepository()
    private val confirmPayment = ConfirmPaymentUseCase(repository)

    // FA-02: there is no gateway. The engine refuses it too; the phone never sends it.
    @Test
    fun aGatewayPaymentIsRefusedBeforeReachingTheEngine() =
        runTest {
            val result = confirmPayment("s1", PaymentMethod.GATEWAY)

            assertEquals(ConfirmPaymentResult.Failure(ConfirmPaymentError.MethodNotAllowed), result)
            assertTrue(repository.payments.isEmpty())
        }

    @Test
    fun bothMethodsAPersonUsesReachTheEngine() =
        runTest {
            PaymentMethod.declarable.forEach { method ->
                assertEquals(ConfirmPaymentResult.Confirmed, confirmPayment("s1", method))
            }

            assertEquals(listOf(PaymentMethod.CASH, PaymentMethod.DIRECT_QR), repository.payments)
        }
}
