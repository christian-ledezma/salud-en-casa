package bo.saludencasa.features.verification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.UploadDocumentResult
import bo.saludencasa.features.verification.domain.model.VerificationChecklist
import bo.saludencasa.features.verification.domain.model.VerificationChecklistResult
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.usecase.GetMyVerificationChecklistUseCase
import bo.saludencasa.features.verification.domain.usecase.UploadVerificationDocumentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface VerificationUiState {
    data object Loading : VerificationUiState

    data class Failed(
        val error: VerificationError,
    ) : VerificationUiState

    data class Content(
        val checklist: VerificationChecklist,
        val otherCaption: String,
        val pendingType: DocumentType?,
        val stagedBytes: Map<DocumentType, ByteArray>,
        val notice: VerificationError?,
    ) : VerificationUiState
}

class VerificationViewModel(
    private val getChecklist: GetMyVerificationChecklistUseCase,
    private val uploadDocument: UploadVerificationDocumentUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<VerificationUiState>(VerificationUiState.Loading)

    val uiState: StateFlow<VerificationUiState> = state.asStateFlow()

    init {
        reload(preserveStagedBytes = emptyMap())
    }

    fun load() = reload(preserveStagedBytes = emptyMap())

    fun onCaptionChanged(value: String) {
        val content = currentContent() ?: return
        state.value = content.copy(otherCaption = value, notice = null)
    }

    fun onImageChosen(
        type: DocumentType,
        bytes: ByteArray,
    ) {
        val content = currentContent() ?: return
        if (content.pendingType != null) return

        state.value =
            content.copy(
                stagedBytes = content.stagedBytes + (type to bytes),
                pendingType = type,
                notice = null,
            )
        upload(type, bytes, content.otherCaption)
    }

    fun onRetry(type: DocumentType) {
        val content = currentContent() ?: return
        if (content.pendingType != null) return
        val bytes = content.stagedBytes[type] ?: return

        state.value = content.copy(pendingType = type, notice = null)
        upload(type, bytes, content.otherCaption)
    }

    fun onCompressionFailed(error: VerificationError) {
        val content = currentContent() ?: return
        state.value = content.copy(notice = error)
    }

    private fun upload(
        type: DocumentType,
        bytes: ByteArray,
        otherCaption: String,
    ) {
        val caption = if (type == DocumentType.OTHER) otherCaption else null
        viewModelScope.launch {
            when (val result = uploadDocument(type, bytes, caption)) {
                UploadDocumentResult.Success -> {
                    // Re-read the checklist after an upload, but keep the
                    // staged bytes for the slots that have not yet succeeded,
                    // so a failure in one slot does not force recapture when
                    // another one is sent (HU-07 criterion 5).
                    val content = currentContent() ?: return@launch
                    reload(preserveStagedBytes = content.stagedBytes - type)
                }

                is UploadDocumentResult.Failure -> {
                    val current = currentContent() ?: return@launch
                    state.value = current.copy(pendingType = null, notice = result.error)
                }
            }
        }
    }

    private fun reload(preserveStagedBytes: Map<DocumentType, ByteArray>) {
        state.value = VerificationUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val result = getChecklist()) {
                    is VerificationChecklistResult.Loaded -> {
                        VerificationUiState.Content(
                            checklist = result.checklist,
                            otherCaption =
                                result.checklist
                                    .documentFor(DocumentType.OTHER)
                                    ?.caption
                                    .orEmpty(),
                            pendingType = null,
                            stagedBytes = preserveStagedBytes,
                            notice = null,
                        )
                    }

                    is VerificationChecklistResult.Failure -> {
                        VerificationUiState.Failed(result.error)
                    }
                }
        }
    }

    private fun currentContent(): VerificationUiState.Content? = state.value as? VerificationUiState.Content
}
