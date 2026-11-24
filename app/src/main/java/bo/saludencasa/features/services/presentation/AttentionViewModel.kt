package bo.saludencasa.features.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.services.domain.model.AdvanceAttentionResult
import bo.saludencasa.features.services.domain.model.AnswerPaymentResult
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.CancelAttentionError
import bo.saludencasa.features.services.domain.model.CancelAttentionResult
import bo.saludencasa.features.services.domain.model.ConfirmPaymentError
import bo.saludencasa.features.services.domain.model.ConfirmPaymentResult
import bo.saludencasa.features.services.domain.model.PaymentAnswer
import bo.saludencasa.features.services.domain.model.PaymentMethod
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.usecase.AcceptReceiptUseCase
import bo.saludencasa.features.services.domain.usecase.AdvanceAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.AnswerPaymentUseCase
import bo.saludencasa.features.services.domain.usecase.CancelAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.ConfirmPaymentUseCase
import bo.saludencasa.features.services.domain.usecase.ConfirmReceiptUseCase
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.ObserveAttentionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AttentionUiState {
    data object Loading : AttentionUiState

    data class Content(
        val attention: Attention,
        val confirming: AttentionStep? = null,
        val advancing: Boolean = false,
        val notice: ServiceError? = null,
        val cancelDraft: CancelDraft? = null,
        val paymentDraft: PaymentDraft? = null,
        val answerDraft: AnswerDraft? = null,
        val accepting: Boolean = false,
    ) : AttentionUiState

    // The service is not there for this caller: the acceptance did not land, or
    // they are not one of its two parties.
    data object NotFound : AttentionUiState

    data class Failed(
        val error: ServiceError,
    ) : AttentionUiState
}

data class CancelDraft(
    val reason: String = "",
    val error: CancelAttentionError? = null,
    val sending: Boolean = false,
)

// No method preselected: RF-11.2 asks the patient to say which one they used, and
// the professional who declares first names it too.
data class PaymentDraft(
    val method: PaymentMethod? = null,
    val error: ConfirmPaymentError? = null,
    val sending: Boolean = false,
) {
    val canConfirm: Boolean get() = method != null && !sending
}

// The answer is chosen by the button the professional pressed; the dialog only
// asks them to confirm it, because it cannot be undone.
data class AnswerDraft(
    val answer: PaymentAnswer,
    val error: ServiceError? = null,
    val sending: Boolean = false,
)

class AttentionViewModel(
    private val requestId: String,
    private val getAttention: GetAttentionUseCase,
    private val advanceAttention: AdvanceAttentionUseCase,
    private val observeAttention: ObserveAttentionUseCase,
    private val cancelAttention: CancelAttentionUseCase,
    private val confirmPayment: ConfirmPaymentUseCase,
    private val answerPayment: AnswerPaymentUseCase,
    private val confirmReceipt: ConfirmReceiptUseCase,
    private val acceptReceipt: AcceptReceiptUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<AttentionUiState>(AttentionUiState.Loading)
    val uiState: StateFlow<AttentionUiState> = state.asStateFlow()

    init {
        load()
        viewModelScope.launch { observeAttention(requestId).collect { reread() } }
    }

    fun refresh() = load()

    fun onResumed() {
        viewModelScope.launch { reread() }
    }

    fun onMarkRequested() {
        updateContent { it.copy(confirming = it.attention.nextStepForMe, notice = null) }
    }

    fun onMarkDismissed() {
        updateContent { it.copy(confirming = null) }
    }

    fun onMarkConfirmed() {
        val content = state.value as? AttentionUiState.Content ?: return
        val step = content.confirming ?: return
        if (content.advancing) return

        updateContent { it.copy(confirming = null, advancing = true) }
        viewModelScope.launch {
            val notice =
                when (val result = advanceAttention(requestId, step)) {
                    AdvanceAttentionResult.Advanced -> {
                        reread()
                        null
                    }

                    is AdvanceAttentionResult.Failure -> {
                        // docs/decisions.md, 2026-11-04, HU-23.
                        val reread = result.error == ServiceError.AttentionChanged && reread()
                        result.error.takeUnless { reread }
                    }
                }
            updateContent { it.copy(advancing = false, notice = notice) }
        }
    }

    fun onCancelRequested() {
        updateContent { if (it.attention.canCancel) it.copy(cancelDraft = CancelDraft(), notice = null) else it }
    }

    fun onCancelReasonChanged(reason: String) {
        updateDraft { it.copy(reason = reason, error = null) }
    }

    fun onCancelDismissed() {
        updateContent { if (it.cancelDraft?.sending == true) it else it.copy(cancelDraft = null) }
    }

    fun onCancelConfirmed() {
        val content = state.value as? AttentionUiState.Content ?: return
        val draft = content.cancelDraft ?: return
        if (draft.sending || content.advancing) return

        updateDraft { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            when (val result = cancelAttention(requestId, draft.reason)) {
                CancelAttentionResult.Cancelled -> {
                    updateContent { it.copy(cancelDraft = null) }
                    reread()
                }

                is CancelAttentionResult.Failure -> {
                    // docs/decisions.md, 2026-11-04, HU-23: reread closes the dialog
                    // when the attention stopped being cancellable.
                    if (result.error == CancelAttentionError.Service(ServiceError.AttentionChanged)) reread()
                    updateDraft { it.copy(sending = false, error = result.error) }
                }
            }
        }
    }

    fun onPayRequested() {
        updateContent {
            if (it.attention.canDeclarePayment) {
                it.copy(
                    paymentDraft = PaymentDraft(),
                    notice = null,
                )
            } else {
                it
            }
        }
    }

    fun onPaymentMethodChosen(method: PaymentMethod) {
        updatePaymentDraft { if (it.sending) it else it.copy(method = method, error = null) }
    }

    fun onPayDismissed() {
        updateContent { if (it.paymentDraft?.sending == true) it else it.copy(paymentDraft = null) }
    }

    fun onPayConfirmed() {
        val content = state.value as? AttentionUiState.Content ?: return
        val draft = content.paymentDraft ?: return
        val method = draft.method ?: return
        if (draft.sending) return

        updatePaymentDraft { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val serviceId = content.attention.serviceId
            val result =
                if (content.attention.iAmThePatient) {
                    confirmPayment(serviceId, method)
                } else {
                    confirmReceipt(serviceId, method)
                }
            when (result) {
                ConfirmPaymentResult.Confirmed -> {
                    updateContent { it.copy(paymentDraft = null) }
                    reread()
                }

                is ConfirmPaymentResult.Failure -> {
                    // docs/decisions.md, 2026-11-04, HU-23: reread closes the dialog
                    // when the other party declared first.
                    if (result.error == ConfirmPaymentError.Service(ServiceError.AttentionChanged)) reread()
                    updatePaymentDraft { it.copy(sending = false, error = result.error) }
                }
            }
        }
    }

    fun onAnswerRequested(answer: PaymentAnswer) {
        updateContent {
            if (it.attention.canAnswerPayment) it.copy(answerDraft = AnswerDraft(answer), notice = null) else it
        }
    }

    fun onAnswerDismissed() {
        updateContent { if (it.answerDraft?.sending == true) it else it.copy(answerDraft = null) }
    }

    fun onAnswerConfirmed() {
        val content = state.value as? AttentionUiState.Content ?: return
        val draft = content.answerDraft ?: return
        if (draft.sending) return

        updateAnswerDraft { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            when (val result = answerPayment(content.attention.serviceId, draft.answer)) {
                AnswerPaymentResult.Answered -> {
                    updateContent { it.copy(answerDraft = null) }
                    reread()
                }

                is AnswerPaymentResult.Failure -> {
                    // docs/decisions.md, 2026-11-04, HU-23: reread closes the dialog
                    // when the payment stopped being answerable.
                    if (result.error == ServiceError.AttentionChanged) reread()
                    updateAnswerDraft { it.copy(sending = false, error = result.error) }
                }
            }
        }
    }

    fun onAcceptReceipt() {
        val content = state.value as? AttentionUiState.Content ?: return
        if (!content.attention.canAcceptReceipt || content.accepting) return

        updateContent { it.copy(accepting = true, notice = null) }
        viewModelScope.launch {
            val notice =
                when (val result = acceptReceipt(content.attention.serviceId)) {
                    AnswerPaymentResult.Answered -> {
                        reread()
                        null
                    }

                    is AnswerPaymentResult.Failure -> {
                        // docs/decisions.md, 2026-11-04, HU-23.
                        val reread = result.error == ServiceError.AttentionChanged && reread()
                        result.error.takeUnless { reread }
                    }
                }
            updateContent { it.copy(accepting = false, notice = notice) }
        }
    }

    private fun load() {
        state.value = AttentionUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val result = getAttention(requestId)) {
                    is AttentionResult.Loaded -> AttentionUiState.Content(result.attention)
                    AttentionResult.NotFound -> AttentionUiState.NotFound
                    is AttentionResult.Failure -> AttentionUiState.Failed(result.error)
                }
        }
    }

    // docs/decisions.md, 2026-10-30, HT-25: a failed read leaves the screen as it was.
    private suspend fun reread(): Boolean {
        val result = getAttention(requestId) as? AttentionResult.Loaded ?: return false
        state.update { current ->
            if (current is AttentionUiState.Content) {
                // A step that is no longer the next one must not be confirmed: the
                // answer given was to a question the screen stopped asking.
                current.copy(
                    attention = result.attention,
                    confirming = current.confirming?.takeIf { it == result.attention.nextStepForMe },
                    cancelDraft = current.cancelDraft?.takeIf { result.attention.canCancel },
                    paymentDraft = current.paymentDraft?.takeIf { result.attention.canDeclarePayment },
                    answerDraft = current.answerDraft?.takeIf { result.attention.canAnswerPayment },
                )
            } else {
                AttentionUiState.Content(result.attention)
            }
        }
        return true
    }

    private fun updateDraft(transform: (CancelDraft) -> CancelDraft) {
        updateContent { content -> content.copy(cancelDraft = content.cancelDraft?.let(transform)) }
    }

    private fun updatePaymentDraft(transform: (PaymentDraft) -> PaymentDraft) {
        updateContent { content -> content.copy(paymentDraft = content.paymentDraft?.let(transform)) }
    }

    private fun updateAnswerDraft(transform: (AnswerDraft) -> AnswerDraft) {
        updateContent { content -> content.copy(answerDraft = content.answerDraft?.let(transform)) }
    }

    private fun updateContent(transform: (AttentionUiState.Content) -> AttentionUiState.Content) {
        state.update { current -> if (current is AttentionUiState.Content) transform(current) else current }
    }
}
