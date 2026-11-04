package bo.saludencasa.features.conversation.domain.usecase

import bo.saludencasa.features.conversation.FakeMessageRepository
import bo.saludencasa.features.conversation.REQUEST_ID
import bo.saludencasa.features.conversation.domain.model.Message
import bo.saludencasa.features.conversation.domain.model.MessageError
import bo.saludencasa.features.conversation.domain.model.MessagesResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CatchUpMessagesUseCaseTest {
    private val repository = FakeMessageRepository()
    private val catchUp = CatchUpMessagesUseCase(GetMessagesUseCase(repository))

    private suspend fun loaded(result: MessagesResult): List<Message> = (result as MessagesResult.Loaded).messages

    // The phone slept while more than a page arrived: a single page would leave
    // the middle out, and nothing would ever load it.
    @Test
    fun catchesUpMoreThanAPageWithoutLeavingAGap() =
        runTest {
            val onScreen = listOf(repository.receive("hola"))
            val arrived = (1..GetMessagesUseCase.PAGE_SIZE * 2 + 5).map { repository.receive("m$it") }

            val caughtUp = loaded(catchUp(REQUEST_ID, onScreen))

            assertEquals(arrived.map(Message::id), caughtUp.map(Message::id))
        }

    // readReceiptIsVisibleToTheSender: the signal of a read carries no data, so
    // the walk starts at my unread message and brings it back read.
    @Test
    fun bringsMyLastMessageBackReadOnceTheCounterpartReadsIt() =
        runTest {
            val mine = repository.store("Tengo gasas", sentByMe = true)
            repository.counterpartReadsEverything()

            val caughtUp = loaded(catchUp(REQUEST_ID, listOf(mine)))

            assertNotNull(caughtUp.single { it.id == mine.id }.readAt)
        }

    // Once it is read there is nothing to refresh: only what is newer comes back.
    @Test
    fun aReadMessageIsNotFetchedAgain() =
        runTest {
            repository.store("Tengo gasas", sentByMe = true)
            repository.counterpartReadsEverything()
            val onScreen = loaded(catchUp(REQUEST_ID, emptyList()))
            val reply = repository.receive("Llevo suero")

            assertEquals(listOf(reply.id), loaded(catchUp(REQUEST_ID, onScreen)).map(Message::id))
        }

    @Test
    fun aFailureReturnsTheErrorAndNoHalfPage() =
        runTest {
            val onScreen = listOf(repository.receive("hola"))
            repository.fetchError = MessageError.NetworkUnavailable

            assertEquals(MessagesResult.Failure(MessageError.NetworkUnavailable), catchUp(REQUEST_ID, onScreen))
        }
}
