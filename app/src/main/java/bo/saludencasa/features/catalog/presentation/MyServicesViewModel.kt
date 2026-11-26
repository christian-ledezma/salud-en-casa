package bo.saludencasa.features.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.DeclaredServices
import bo.saludencasa.features.catalog.domain.model.DeclaredServicesResult
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.catalog.domain.model.matchingName
import bo.saludencasa.features.catalog.domain.usecase.DeclareServicesUseCase
import bo.saludencasa.features.catalog.domain.usecase.GetMyDeclaredServicesUseCase
import bo.saludencasa.features.catalog.domain.usecase.RemoveServiceUseCase
import bo.saludencasa.features.catalog.domain.usecase.UpdateServicePriceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed interface ServiceEditor {
    data class Repricing(
        val service: ProfessionalService,
        val price: String,
        val error: CatalogError?,
    ) : ServiceEditor

    data class Removing(
        val service: ProfessionalService,
    ) : ServiceEditor
}

// The selector of the catalog, open over the list. It lives in the same state
// as the list so a declaration reloads one thing (docs/decisions.md,
// 2026-11-25, HT-32).
data class ServicePicker(
    val query: String = "",
    val selected: Set<String> = emptySet(),
    val error: CatalogError? = null,
)

// docs/decisions.md, 2026-11-25, HT-32: the list of what is declared grows its
// own search past this many rows.
internal const val SEARCH_THRESHOLD = 8

sealed interface MyServicesUiState {
    data object Loading : MyServicesUiState

    data class Failed(
        val error: CatalogError,
    ) : MyServicesUiState

    // docs/decisions.md, 2026-10-09, the empty state is a block and not a
    // variant of its own.
    data class Content(
        val declaredServices: DeclaredServices,
        val editor: ServiceEditor? = null,
        val picker: ServicePicker? = null,
        val query: String = "",
        val newTypeIds: Set<String> = emptySet(),
        val busy: Boolean = false,
        val notice: CatalogError? = null,
        val declarationNotice: Int? = null,
    ) : MyServicesUiState {
        val showsSearch: Boolean = declaredServices.services.size > SEARCH_THRESHOLD

        val visibleServices: List<ProfessionalService> =
            if (showsSearch) {
                declaredServices.services.matchingName(query) { it.type.name }
            } else {
                declaredServices.services
            }

        val pickerTypes: List<ServiceType> =
            declaredServices.undeclaredTypes.matchingName(picker?.query.orEmpty(), ServiceType::name)
    }
}

class MyServicesViewModel(
    private val getMyDeclaredServices: GetMyDeclaredServicesUseCase,
    private val declareServices: DeclareServicesUseCase,
    private val updateServicePrice: UpdateServicePriceUseCase,
    private val removeService: RemoveServiceUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<MyServicesUiState>(MyServicesUiState.Loading)

    val uiState: StateFlow<MyServicesUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        state.value = MyServicesUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val result = getMyDeclaredServices()) {
                    is DeclaredServicesResult.Loaded -> MyServicesUiState.Content(result.declaredServices)
                    is DeclaredServicesResult.Failure -> MyServicesUiState.Failed(result.error)
                }
        }
    }

    fun onQueryChanged(value: String) {
        val content = currentContent() ?: return

        state.value = content.copy(query = value)
    }

    fun onAddServicesClick() {
        val content = currentContent() ?: return
        if (content.busy) return

        state.value = content.copy(picker = ServicePicker(), editor = null, notice = null, declarationNotice = null)
    }

    fun onPickerQueryChanged(value: String) {
        val content = currentContent() ?: return
        val picker = content.picker ?: return

        state.value = content.copy(picker = picker.copy(query = value, error = null))
    }

    fun onPickerToggle(serviceTypeId: String) {
        val content = currentContent() ?: return
        val picker = content.picker ?: return
        if (content.busy) return

        val selected =
            if (serviceTypeId in picker.selected) picker.selected - serviceTypeId else picker.selected + serviceTypeId
        state.value = content.copy(picker = picker.copy(selected = selected, error = null))
    }

    fun onPickerDismiss() {
        val content = currentContent() ?: return
        if (content.busy) return

        state.value = content.copy(picker = null)
    }

    fun onConfirmDeclaration() {
        val content = currentContent() ?: return
        val picker = content.picker ?: return
        if (content.busy || picker.selected.isEmpty()) return

        state.value = content.copy(busy = true, picker = picker.copy(error = null))
        viewModelScope.launch {
            when (val result = declareServices(picker.selected)) {
                ServiceDeclarationResult.Success -> {
                    reread { latest, declared ->
                        latest
                            .reloaded(declared)
                            .copy(
                                picker = null,
                                newTypeIds = latest.newTypeIds + picker.selected,
                                declarationNotice = picker.selected.size,
                            )
                    }
                }

                is ServiceDeclarationResult.Failure -> {
                    onDeclarationFailed(result.error)
                }
            }
        }
    }

    fun onDeclarationNoticeShown() {
        val content = currentContent() ?: return

        state.value = content.copy(declarationNotice = null)
    }

    fun onRepriceClick(service: ProfessionalService) {
        val content = currentContent() ?: return
        if (content.busy) return

        state.value =
            content.copy(
                editor = ServiceEditor.Repricing(service, service.referencePriceBob.toEditableText(), error = null),
                notice = null,
            )
    }

    fun onRemoveClick(service: ProfessionalService) {
        val content = currentContent() ?: return
        if (content.busy) return

        state.value = content.copy(editor = ServiceEditor.Removing(service), notice = null)
    }

    fun onPriceChanged(value: String) {
        val content = currentContent() ?: return
        val editor = content.editor as? ServiceEditor.Repricing ?: return

        state.value = content.copy(editor = editor.copy(price = value, error = null))
    }

    fun onDismissEditor() {
        val content = currentContent() ?: return
        if (content.busy) return

        state.value = content.copy(editor = null)
    }

    fun onConfirmPrice() {
        val content = currentContent() ?: return
        if (content.busy) return
        val editor = content.editor as? ServiceEditor.Repricing ?: return

        state.value = content.copy(busy = true, editor = editor.copy(error = null))
        viewModelScope.launch {
            when (val result = updateServicePrice(editor.service.id, editor.price)) {
                ServiceDeclarationResult.Success -> {
                    reread { latest, declared -> latest.reloaded(declared).copy(editor = null) }
                }

                // A refused price stays in its dialog, beside the field it
                // belongs to, so the number the person typed is still there
                // to correct.
                is ServiceDeclarationResult.Failure -> {
                    val current = currentContent() ?: return@launch
                    val open = current.editor as? ServiceEditor.Repricing ?: return@launch
                    state.value = current.copy(busy = false, editor = open.copy(error = result.error))
                }
            }
        }
    }

    fun onConfirmRemoval() {
        val content = currentContent() ?: return
        if (content.busy) return
        val editor = content.editor as? ServiceEditor.Removing ?: return

        state.value = content.copy(busy = true)
        viewModelScope.launch {
            when (val result = removeService(editor.service.id)) {
                ServiceDeclarationResult.Success -> {
                    reread { latest, declared -> latest.reloaded(declared).copy(editor = null) }
                }

                // A failed removal has no field to correct, so its
                // confirmation closes and the reason goes to the screen.
                is ServiceDeclarationResult.Failure -> {
                    val current = currentContent() ?: return@launch
                    state.value = current.copy(busy = false, editor = null, notice = result.error)
                }
            }
        }
    }

    // A refusal that says the catalog moved under the selector is answered by
    // reading it again, with the selector still open: what the person chose
    // and is still offered stays chosen, what is not is dropped, and a second
    // tap on the same button does not fail the same way
    // (docs/decisions.md, 2026-11-25, HT-32).
    private suspend fun onDeclarationFailed(error: CatalogError) {
        val movedUnderTheSelector =
            error == CatalogError.ServiceAlreadyDeclared || error == CatalogError.ServiceTypeUnavailable

        if (movedUnderTheSelector) {
            reread { latest, declared ->
                val picker = latest.picker ?: return@reread latest.reloaded(declared)
                val reloaded = latest.reloaded(declared)
                val stillOffered = reloaded.declaredServices.undeclaredTypes.mapTo(HashSet()) { it.id }
                reloaded.copy(
                    picker =
                        picker.copy(
                            selected = picker.selected.filterTo(HashSet()) { it in stillOffered },
                            error = error,
                        ),
                )
            }
            return
        }

        val current = currentContent() ?: return
        val picker = current.picker ?: return
        state.value = current.copy(busy = false, picker = picker.copy(error = error))
    }

    // The list is read again rather than patched: what is still undeclared is
    // derived from both sides, and reading is what keeps the two from
    // disagreeing. It is replaced in place, with no Loading in between, so the
    // scroll position of a long list survives a write.
    private suspend fun reread(build: (MyServicesUiState.Content, DeclaredServices) -> MyServicesUiState.Content) {
        when (val result = getMyDeclaredServices()) {
            is DeclaredServicesResult.Loaded -> {
                val latest = currentContent() ?: return
                state.value = build(latest, result.declaredServices)
            }

            is DeclaredServicesResult.Failure -> {
                state.value = MyServicesUiState.Failed(result.error)
            }
        }
    }

    private fun MyServicesUiState.Content.reloaded(declared: DeclaredServices): MyServicesUiState.Content =
        copy(
            declaredServices = declared,
            newTypeIds = newTypeIds.filterTo(HashSet()) { declared.declares(it) },
            busy = false,
            notice = null,
        )

    private fun currentContent(): MyServicesUiState.Content? = state.value as? MyServicesUiState.Content
}

private fun BigDecimal.toEditableText(): String = stripTrailingZeros().toPlainString()
