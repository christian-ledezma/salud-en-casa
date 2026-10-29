package bo.saludencasa.features.requests.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.requests.domain.model.OfferActionResult
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferThread
import bo.saludencasa.features.requests.domain.model.OfferThreadResult
import bo.saludencasa.features.requests.domain.model.OfferableRequest
import bo.saludencasa.features.requests.domain.model.OfferableRequestResult
import bo.saludencasa.features.requests.domain.usecase.AcceptOfferUseCase
import bo.saludencasa.features.requests.domain.usecase.GetOfferThreadUseCase
import bo.saludencasa.features.requests.domain.usecase.GetOfferableRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.ObserveOfferSignalsUseCase
import bo.saludencasa.features.requests.domain.usecase.RejectOfferUseCase
import bo.saludencasa.features.requests.domain.usecase.SubmitCounterOfferUseCase
import bo.saludencasa.features.requests.domain.usecase.SubmitOfferUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OfferThreadUiState {
    data object Loading : OfferThreadUiState

    // The professional has not offered yet: the request and the form of the first
    // offer. Once it is sent, the same screen turns into the negotiation
    // (docs/decisions.md, 2026-10-27, HT-24).
    data class Opening(
        val request: OfferableRequest,
        val amount: String = "",
        val message: String = "",
        val submitting: Boolean = false,
        val error: OfferError? = null,
    ) : OfferThreadUiState

    data class Content(
        val thread: OfferThread,
        // Only the professional reads the request's detail here: for the patient
        // it is their own request, which they already know.
        val request: OfferableRequest? = null,
        val countering: Boolean = false,
        val amount: String = "",
        val message: String = "",
        val submitting: Boolean = false,
        val confirmingRejection: Boolean = false,
        val confirmingAcceptance: Boolean = false,
        val error: OfferError? = null,
    ) : OfferThreadUiState {
        // Whoever did not write the offer on the table is the one who answers it,
        // which is also who may accept it (HU-19).
        val canAnswer: Boolean get() = thread.isMyTurn && !submitting

        val canSubmit: Boolean get() = canAnswer && countering
    }

    // Nothing to negotiate here: the request is gone, or this thread does not
    // exist for this caller.
    data object Gone : OfferThreadUiState

    // The offer was accepted: the service exists and the screen moves on to the
    // attention (HU-19).
    data object Accepted : OfferThreadUiState

    data class Failed(
        val error: OfferError,
    ) : OfferThreadUiState
}

// One screen per negotiation, for both sides: the professional's first offer, the
// chain in order, the counter-offer and the rejection (docs/decisions.md,
// 2026-10-27). A null professionalId means the caller is the professional of this
// thread: every way in that a professional takes -- the inbox, a request's
// notification, a counter-offer's notification -- arrives without one, and every
// way in that a patient takes arrives with the professional they negotiate with.
class OfferThreadViewModel(
    private val requestId: String,
    private val professionalId: String?,
    private val getOfferThread: GetOfferThreadUseCase,
    private val getOfferableRequest: GetOfferableRequestUseCase,
    private val submitOffer: SubmitOfferUseCase,
    private val submitCounterOffer: SubmitCounterOfferUseCase,
    private val rejectOffer: RejectOfferUseCase,
    private val acceptOffer: AcceptOfferUseCase,
    private val observeOfferSignals: ObserveOfferSignalsUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<OfferThreadUiState>(OfferThreadUiState.Loading)
    val uiState: StateFlow<OfferThreadUiState> = state.asStateFlow()

    // Only one read of the thread is ever in flight, so a slow one cannot land
    // after a newer one and put an older thread back on the screen.
    private var loadJob: Job? = null

    init {
        // Listening starts before the first read, so an answer landing while it
        // travels is not missed; the route already says which side this is. A
        // signal never shows the spinner again.
        viewModelScope.launch {
            observeOfferSignals(asProfessional = professionalId == null).collect { reload() }
        }
        relaunchLoad(showLoading = true)
    }

    fun refresh() = relaunchLoad(showLoading = true)

    fun onCounterClicked() {
        state.updateContent { it.copy(countering = true, error = null) }
    }

    fun onCounterDismissed() {
        state.updateContent { it.copy(countering = false, amount = "", message = "", error = null) }
    }

    fun onAmountChanged(value: String) {
        state.value =
            when (val current = state.value) {
                is OfferThreadUiState.Opening -> current.copy(amount = value, error = null)
                is OfferThreadUiState.Content -> current.copy(amount = value, error = null)
                else -> current
            }
    }

    fun onMessageChanged(value: String) {
        state.value =
            when (val current = state.value) {
                is OfferThreadUiState.Opening -> current.copy(message = value, error = null)
                is OfferThreadUiState.Content -> current.copy(message = value, error = null)
                else -> current
            }
    }

    fun onSubmit() {
        when (val current = state.value) {
            is OfferThreadUiState.Opening -> submitFirstOffer(current)
            is OfferThreadUiState.Content -> submitCounter(current)
            else -> Unit
        }
    }

    fun onAcceptClicked() {
        state.updateContent { it.copy(confirmingAcceptance = true, error = null) }
    }

    fun onAcceptDismissed() {
        state.updateContent { it.copy(confirmingAcceptance = false) }
    }

    fun onAcceptConfirmed() {
        val content = state.value as? OfferThreadUiState.Content ?: return
        val target = content.thread.lastOffer ?: return
        if (!content.canAnswer) return

        state.updateContent { it.copy(confirmingAcceptance = false, submitting = true, error = null) }
        viewModelScope.launch {
            when (val result = acceptOffer(target.id)) {
                // The screen moves on to the attention; a late signal must not put
                // the thread back on top, so load() leaves this state alone.
                OfferActionResult.Success -> {
                    state.value = OfferThreadUiState.Accepted
                }

                is OfferActionResult.Failure -> {
                    state.updateContent { it.copy(submitting = false, error = result.error) }
                    reloadIfStale(result.error)
                }
            }
        }
    }

    fun onRejectClicked() {
        state.updateContent { it.copy(confirmingRejection = true, error = null) }
    }

    fun onRejectDismissed() {
        state.updateContent { it.copy(confirmingRejection = false) }
    }

    fun onRejectConfirmed() {
        val content = state.value as? OfferThreadUiState.Content ?: return
        val target = content.thread.lastOffer ?: return
        if (!content.canAnswer) return

        state.updateContent { it.copy(confirmingRejection = false, submitting = true, error = null) }
        viewModelScope.launch {
            when (val result = rejectOffer(target.id)) {
                OfferActionResult.Success -> {
                    state.updateContent { it.copy(submitting = false, countering = false) }
                    relaunchLoad(showLoading = false)
                }

                is OfferActionResult.Failure -> {
                    state.updateContent { it.copy(submitting = false, error = result.error) }
                    reloadIfStale(result.error)
                }
            }
        }
    }

    private fun submitFirstOffer(opening: OfferThreadUiState.Opening) {
        if (opening.submitting) return

        state.value = opening.copy(submitting = true, error = null)
        viewModelScope.launch {
            when (val result = submitOffer(requestId, opening.amount, opening.message)) {
                // The thread that now exists replaces the form on this same screen.
                // A full load and not a quiet one: if it fails, the form must not
                // stay up waiting with its button off for an offer already made.
                OfferSubmitResult.Success -> {
                    relaunchLoad(showLoading = true)
                }

                is OfferSubmitResult.Failure -> {
                    (state.value as? OfferThreadUiState.Opening)?.let {
                        state.value = it.copy(submitting = false, error = result.error)
                    }
                }
            }
        }
    }

    private fun submitCounter(content: OfferThreadUiState.Content) {
        if (!content.canSubmit) return

        state.updateContent { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result =
                submitCounterOffer(
                    requestId = requestId,
                    thread = content.thread,
                    amountRaw = content.amount,
                    message = content.message,
                )
            when (result) {
                OfferSubmitResult.Success -> {
                    state.updateContent {
                        it.copy(submitting = false, countering = false, amount = "", message = "")
                    }
                    relaunchLoad(showLoading = false)
                }

                is OfferSubmitResult.Failure -> {
                    state.updateContent { it.copy(submitting = false, error = result.error) }
                    reloadIfStale(result.error)
                }
            }
        }
    }

    // A refusal by the engine means the screen was behind it: the other side
    // answered, rejected, or raised the floor from another phone. Reading the
    // thread again shows what changed, with the error still on screen.
    private fun reloadIfStale(error: OfferError) {
        if (error == OfferError.NotAllowed || error is OfferError.BelowMinimum) reload()
    }

    private fun reload() = relaunchLoad(showLoading = false)

    private fun relaunchLoad(showLoading: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { load(showLoading) }
    }

    private suspend fun load(showLoading: Boolean) {
        // Once accepted, the screen is on its way to the attention: a signal that
        // lands in between must not pull the thread back onto it.
        if (state.value is OfferThreadUiState.Accepted) return
        if (showLoading) state.value = OfferThreadUiState.Loading

        when (val result = getOfferThread(requestId, professionalId)) {
            is OfferThreadResult.Loaded -> {
                state.value = content(result.thread)
            }

            // A professional with no thread yet is about to make the first offer;
            // for the patient there is nothing to show.
            OfferThreadResult.NotFound -> {
                state.value =
                    if (professionalId == null) opening() else OfferThreadUiState.Gone
            }

            // A read nobody asked for does not take the screen away: what is on
            // it is still the last thread the engine gave, and the form on top
            // of it is what the person is writing.
            is OfferThreadResult.Failure -> {
                if (showLoading || !state.value.isOnScreen()) {
                    state.value = OfferThreadUiState.Failed(result.error)
                }
            }
        }
    }

    // The form as it is after the read, not before it: what the person typed
    // while the request travelled stays.
    private suspend fun opening(): OfferThreadUiState {
        val result = getOfferableRequest(requestId)
        val current = state.value as? OfferThreadUiState.Opening

        return when (result) {
            is OfferableRequestResult.Loaded -> {
                current?.copy(request = result.request)
                    ?: OfferThreadUiState.Opening(result.request)
            }

            OfferableRequestResult.NotFound -> {
                OfferThreadUiState.Gone
            }

            is OfferableRequestResult.Failure -> {
                current ?: OfferThreadUiState.Failed(result.error)
            }
        }
    }

    // A reload keeps everything the person was doing: what they typed, the form,
    // the confirmation and the error on screen. Only the thread is replaced.
    private suspend fun content(thread: OfferThread): OfferThreadUiState.Content {
        val fetched = if (thread.iAmTheProfessional && requestOnScreen() == null) requestDetail() else null

        val previous = state.value as? OfferThreadUiState.Content
        return (previous ?: OfferThreadUiState.Content(thread)).copy(
            thread = thread,
            request = previous?.request ?: requestOnScreen() ?: fetched,
            countering = previous?.countering == true && thread.isMyTurn,
        )
    }

    // The request the professional is already looking at, from the thread or from
    // the first-offer form that the thread is replacing.
    private fun requestOnScreen(): OfferableRequest? =
        when (val current = state.value) {
            is OfferThreadUiState.Content -> current.request
            is OfferThreadUiState.Opening -> current.request
            else -> null
        }

    private suspend fun requestDetail(): OfferableRequest? =
        (getOfferableRequest(requestId) as? OfferableRequestResult.Loaded)?.request

    private fun OfferThreadUiState.isOnScreen(): Boolean =
        this is OfferThreadUiState.Content || this is OfferThreadUiState.Opening

    private fun MutableStateFlow<OfferThreadUiState>.updateContent(
        transform: (OfferThreadUiState.Content) -> OfferThreadUiState.Content,
    ) {
        val content = value as? OfferThreadUiState.Content ?: return
        value = transform(content)
    }
}
