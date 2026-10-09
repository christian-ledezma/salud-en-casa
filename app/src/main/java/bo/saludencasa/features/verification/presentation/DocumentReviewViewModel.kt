package bo.saludencasa.features.verification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.verification.domain.model.DocumentReviewDossier
import bo.saludencasa.features.verification.domain.model.DocumentReviewDossierResult
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.DocumentUrlResult
import bo.saludencasa.features.verification.domain.model.ReviewActionResult
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.usecase.ApproveDocumentsUseCase
import bo.saludencasa.features.verification.domain.usecase.ApproveProfessionalVerificationUseCase
import bo.saludencasa.features.verification.domain.usecase.GetDocumentReviewDossierUseCase
import bo.saludencasa.features.verification.domain.usecase.GetSignedDocumentUrlUseCase
import bo.saludencasa.features.verification.domain.usecase.RejectDocumentsUseCase
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
        val checkedTypes: Set<DocumentType>,
        val image: DocumentImageState,
        val busy: Boolean,
        val expandedImage: Boolean,
        val rejectionDraft: String?,
        val confirmingProfessionalApproval: Boolean,
        val notice: VerificationError?,
    ) : DocumentReviewUiState {
        val allChecked: Boolean
            get() = dossier.selectableTypes.isNotEmpty() && checkedTypes.size == dossier.selectableTypes.size
    }
}

class DocumentReviewViewModel(
    private val profileId: String,
    private val getDossier: GetDocumentReviewDossierUseCase,
    private val getSignedUrl: GetSignedDocumentUrlUseCase,
    private val approveDocuments: ApproveDocumentsUseCase,
    private val rejectDocuments: RejectDocumentsUseCase,
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
                    // The queue card names a person, not a document, so which
                    // one opens is decided here.
                    val first = result.dossier.firstTypeToShow
                    state.value =
                        DocumentReviewUiState.Content(
                            dossier = result.dossier,
                            selectedType = first,
                            checkedTypes = emptySet(),
                            image = DocumentImageState.Loading,
                            busy = false,
                            expandedImage = false,
                            rejectionDraft = null,
                            confirmingProfessionalApproval = false,
                            notice = null,
                        )
                    loadImage(first)
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

    fun onToggleChecked(type: DocumentType) {
        val content = idleContent() ?: return
        val checked = if (type in content.checkedTypes) content.checkedTypes - type else content.checkedTypes + type
        state.value = content.copy(checkedTypes = checked, notice = null)
    }

    fun onToggleAllChecked() {
        val content = idleContent() ?: return
        val checked = if (content.allChecked) emptySet() else content.dossier.selectableTypes.toSet()
        state.value = content.copy(checkedTypes = checked, notice = null)
    }

    fun onExpandImage() {
        val content = idleContent() ?: return
        state.value = content.copy(expandedImage = true)
    }

    fun onCollapseImage() {
        val content = state.value as? DocumentReviewUiState.Content ?: return
        state.value = content.copy(expandedImage = false)
    }

    fun onRetryImage() {
        val content = idleContent() ?: return
        state.value = content.copy(image = DocumentImageState.Loading)
        loadImage(content.selectedType)
    }

    fun onApproveSelected() {
        val content = idleContent()?.takeIf { it.checkedTypes.isNotEmpty() } ?: return
        state.value = content.copy(busy = true, notice = null)
        viewModelScope.launch {
            settle(approveDocuments(profileId, content.checkedTypes))
        }
    }

    fun onRejectClick() {
        val content = idleContent()?.takeIf { it.checkedTypes.isNotEmpty() } ?: return
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
        if (content.checkedTypes.isEmpty()) return
        state.value = content.copy(busy = true, notice = null)
        viewModelScope.launch {
            // docs/decisions.md, 2026-10-08, one reason for the whole selection.
            settle(rejectDocuments(profileId, content.checkedTypes, reason))
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
                // The write succeeded either way, so both dialogs close, the
                // marks on documents already decided go away, and a failed
                // reread is reported outside the dialogs.
                val settled =
                    content.copy(
                        busy = false,
                        checkedTypes = emptySet(),
                        rejectionDraft = null,
                        confirmingProfessionalApproval = false,
                    )
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
