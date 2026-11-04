package bo.saludencasa.core.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListRefresherTest {
    // The indicator has to close, also when the read fails, or it spins forever.
    @Test
    fun theIndicatorClosesWhenTheReadEnds() =
        runTest {
            val refresher = refresher()
            val gate = CompletableDeferred<Boolean>()

            refresher.pull { gate.await() }
            advanceUntilIdle()
            assertTrue(refresher.state.value.refreshing)

            gate.complete(false)
            advanceUntilIdle()
            assertEquals(ListRefresh(refreshing = false, failed = true), refresher.state.value)
        }

    @Test
    fun aSecondPullWhileReadingIsIgnored() =
        runTest {
            val refresher = refresher()
            val gate = CompletableDeferred<Boolean>()
            var reads = 0

            repeat(2) {
                refresher.pull {
                    reads++
                    gate.await()
                }
            }
            gate.complete(true)
            advanceUntilIdle()

            assertEquals(1, reads)
        }

    // The notice belongs to the pull that failed, not to the next one.
    @Test
    fun pullingAgainClearsTheFailure() =
        runTest {
            val refresher = refresher()
            refresher.pull { false }
            advanceUntilIdle()

            refresher.pull { true }
            advanceUntilIdle()

            assertFalse(refresher.state.value.failed)
        }

    private fun TestScope.refresher() = ListRefresher(this)
}
