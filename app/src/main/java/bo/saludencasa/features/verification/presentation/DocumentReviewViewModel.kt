package bo.saludencasa.features.verification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.verification.domain.model.DocumentReviewDossier
import bo.saludencasa.features.verification.domain.model.DocumentReviewDossierResult
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.DocumentUrlResult
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.usecase.ApproveDocumentUseCase
import bo.saludencasa.features.verification.domain.usecase.ApproveProfessionalVerificationUseCase
import bo.saludencasa.features.verification.domain.usecase.GetDocumentReviewDossierUseCase
import bo.saludencasa.features.verification.domain.usecase.GetSignedDocumentUrlUseCase
import bo.saludencasa.features.verification.domain.usecase.RejectDocumentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DocumentImageState {
    data object Missing : DocumentImageState

    data object Loading : DocumentImageState

    data class Ready(
        val url: String,
    ) : DocumentImageState

    data class Failed(
        val error: VerificationError,
    ) : DocumentImageState
}

sealed interface DocumentReviewUiState {
    data object Loading : DocumentReviewUiState

    data class Failed(
        val error: VerificationError,
    ) : DocumentReviewUiState

    data class Content(
        val dossier: DocumentReviewDossier,
        val selectedType: DocumentType,
        val image: DocumentImageState,
        val busy: Boolean,
        val rejectionDraft: String?,
        val confirmingProfessionalApproval: Boolean,
        val notice: VerificationError?,
    ) : DocumentReviewUiState
}

class DocumentReviewViewModel(
    private val profileId: String,
    private val initialType: DocumentType,
    private val getDossier: GetDocumentReviewDossierUseCase,
    private val getSignedUrl: GetSignedDocumentUrlUseCase,
    private val approveDocument: ApproveDocumentUseCase,
    private val rejectDocument: RejectDocumentUseCase,
    private val approveProfessional: ApproveProfessionalVerificationUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<DocumentReviewUiState>(DocumentReviewUiState.Loading)

    val uiState: StateFlow<DocumentReviewUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = DocumentReviewUiState.Loading
        viewModelScope.launch {
            when (val result = getDossier(profileId)) {
                is DocumentReviewDossierResult.Loaded -> {
                    state.value =
                        DocumentReviewUiState.Content(
                            dossier = result.dossier,
                            selectedType = initialType,
                            image = DocumentImageState.Loading,
                            busy = false,
                            rejectionDraft = null,
                            confirmingProfessionalApproval = false,
                            notice = null,
                        )
                    loadImage(initialType)
                }

                is DocumentReviewDossierResult.Failure -> {
                    state.value = DocumentReviewUiState.Failed(result.error)
                }
            }
        }
    }

    fun onSelect(type: DocumentType) {
        val content = idleContent() ?: return
        state.value = content.copy(selectedType = type, image = DocumentImageState.Loading, notice = null)
        loadImage(type)
    }

    fun onRetryImage() {
        val content = idleContent() ?: return
        state.value = content.copy(image = DocumentImageState.Loading)
        loadImage(content.selectedType)
    }

    fun onApprove() {
        val content = idleContent() ?: return
        state.value = content.copy(busy = true, notice = null)
        viewModelScope.launch {
            settle(approveDocument(profileId, content.selectedType))
        }
    }

    fun onRejectClick() {
        val content = idleContent() ?: return
        state.value = content.copy(rejectionDraft = "", notice = null)
    }

    fun onRejectReasonChanged(value: String) {
        val content = state.value as? DocumentReviewUiState.Content ?: return
        if (content.rejectionDraft == null) return
        state.value = content.copy(rejectionDraft = value, notice = null)
    }

    fun onRejectDismiss() {
        val content = state.value as? DocumentReviewUiState.Content ?: return
        if (content.busy) return
        state.value = content.copy(rejectionDraft = null, notice = null)
    }

    fun onRejectConfirm() {
        val content = idleContent() ?: return
        val reason = content.rejectionDraft ?: return
        state.value = content.copy(busy = true, notice = null)
        viewModelScope.launch {
            settle(rejectDocument(profileId, content.selectedType, reason))
        }
    }

    fun onApproveProfessionalClick() {
        val content = idleContent() ?: return
        state.value = content.copy(confirmingProfessionalApproval = true, notice = null)
    }

    fun onApproveProfessionalDismiss() {
        val content = state.value as? DocumentReviewUiState.Content ?: return
        if (content.busy) return
        state.value = content.copy(confirmingProfessionalApproval = false)
    }

    fun onApproveProfessionalConfirm() {
        val content = idleContent() ?: return
        state.value = content.copy(busy = true, notice = null)
        viewModelScope.launch {
            settle(approveProfessional(profileId))
        }
    }

    // docs/decisions.md, 2026-10-06, reread after every write.
    private suspend fun settle(result: ReviewActionResult) {
        when (result) {
            ReviewActionResult.Success -> {
                val reread = getDossier(profileId)
                // Taken after the suspension: an image reply that landed meanwhile
                // would be overwritten by a copy of the state from before it.
                val content = state.value as? DocumentReviewUiState.Content ?: return
                // The write succeeded either way, so both dialogs close and a
                // failed reread is reported outside them.
                val settled = content.copy(busy = false, rejectionDraft = null, confirmingProfessionalApproval = false)
                state.value =
                    when (reread) {
                        is DocumentReviewDossierResult.Loaded -> settled.copy(dossier = reread.dossier, notice = null)
                        is DocumentReviewDossierResult.Failure -> settled.copy(notice = reread.error)
                    }
            }

            is ReviewActionResult.Failure -> {
                val content = state.value as? DocumentReviewUiState.Content ?: return
                // A rejected rejection keeps its dialog open so the text is not lost.
                state.value = content.copy(busy = false, confirmingProfessionalApproval = false, notice = result.error)
            }
        }
    }

    private fun loadImage(type: DocumentType) {
        val path =
            (state.value as? DocumentReviewUiState.Content)
                ?.dossier
                ?.checklist
                ?.documentFor(type)
                ?.storagePath
        if (path == null) {
            updateImageIfStillSelected(type, DocumentImageState.Missing)
            return
        }
        viewModelScope.launch {
            updateImageIfStillSelected(
                type,
                when (val result = getSignedUrl(path)) {
                    is DocumentUrlResult.Loaded -> DocumentImageState.Ready(result.url)
                    is DocumentUrlResult.Failure -> DocumentImageState.Failed(result.error)
                },
            )
        }
    }

    private fun updateImageIfStillSelected(
        type: DocumentType,
        image: DocumentImageState,
    ) {
        val content = state.value as? DocumentReviewUiState.Content ?: return
        if (content.selectedType != type) return
        state.value = content.copy(image = image)
    }

    private fun idleContent(): DocumentReviewUiState.Content? =
        (state.value as? DocumentReviewUiState.Content)?.takeUnless { it.busy }
}
