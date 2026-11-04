package bo.saludencasa.di

import bo.saludencasa.features.conversation.data.datasource.SupabaseMessageDataSource
import bo.saludencasa.features.conversation.data.repository.MessageRepository
import bo.saludencasa.features.conversation.domain.repository.IMessageRepository
import bo.saludencasa.features.conversation.domain.usecase.CatchUpMessagesUseCase
import bo.saludencasa.features.conversation.domain.usecase.GetConversationHeaderUseCase
import bo.saludencasa.features.conversation.domain.usecase.GetMessagesUseCase
import bo.saludencasa.features.conversation.domain.usecase.MarkConversationReadUseCase
import bo.saludencasa.features.conversation.domain.usecase.ObserveConversationUseCase
import bo.saludencasa.features.conversation.domain.usecase.SendMessageUseCase
import bo.saludencasa.features.conversation.presentation.ConversationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val conversationModule =
    module {
        single { SupabaseMessageDataSource(supabase = get()) }
        single<IMessageRepository> { MessageRepository(get()) }

        factory { GetConversationHeaderUseCase(get()) }
        factory { GetMessagesUseCase(get()) }
        factory { CatchUpMessagesUseCase(get()) }
        factory { SendMessageUseCase(get()) }
        factory { MarkConversationReadUseCase(get()) }
        factory { ObserveConversationUseCase(get()) }

        viewModel { (requestId: String) ->
            ConversationViewModel(
                requestId = requestId,
                getHeader = get(),
                getMessages = get(),
                catchUpMessages = get(),
                sendMessage = get(),
                markConversationRead = get(),
                observeConversation = get(),
                enterConversation = get(),
                leaveConversation = get(),
            )
        }
    }
