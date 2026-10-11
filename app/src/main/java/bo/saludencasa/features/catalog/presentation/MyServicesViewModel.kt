package bo.saludencasa.features.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.DeclaredServices
import bo.saludencasa.features.catalog.domain.model.DeclaredServicesResult
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.catalog.domain.usecase.DeclareServiceUseCase
import bo.saludencasa.features.catalog.domain.usecase.GetMyDeclaredServicesUseCase
import bo.saludencasa.features.catalog.domain.usecase.RemoveServiceUseCase
import bo.saludencasa.features.catalog.domain.usecase.UpdateServicePriceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed interface ServiceEditor {
    data class Declaring(
        val type: ServiceType,
        val price: String,
        val error: CatalogError?,
    ) : ServiceEditor

    data class Repricing(
        val service: ProfessionalService,
        val price: String,
        val error: CatalogError?,
    ) : ServiceEditor

    data class Removing(
        val service: ProfessionalService,
    ) : ServiceEditor
}

sealed interface MyServicesUiState {
    data object Loading : MyServicesUiState

    data class Failed(
        val error: CatalogError,
    ) : MyServicesUiState

    // docs/decisions.md, 2026-10-09, the empty state is a block and not a
    // variant of its own.
    data class Content(
        val declaredServices: DeclaredServices,
        val editor: ServiceEditor?,
        val busy: Boolean,
        val notice: CatalogError?,
    ) : MyServicesUiState
}

class MyServicesViewModel(
    private val getMyDeclaredServices: GetMyDeclaredServicesUseCase,
    private val declareService: DeclareServiceUseCase,
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
                    is DeclaredServicesResult.Loaded -> {
                        MyServicesUiState.Content(
                            declaredServices = result.declaredServices,
                            editor = null,
                            busy = false,
                            notice = null,
                        )
                    }

                    is DeclaredServicesResult.Failure -> {
                        MyServicesUiState.Failed(result.error)
                    }
                }
        }
    }

    // The catalog's reference price opens the field already filled, so the
    // professional confirms a number instead of inventing one (RF-05.1).
    fun onDeclareClick(type: ServiceType) {
        val content = currentContent() ?: return
        if (content.busy) return

        state.value =
            content.copy(
                editor = ServiceEditor.Declaring(type, type.referencePriceBob.toEditableText(), error = null),
                notice = null,
            )
    }

    fun onRepriceClick(service: ProfessionalService) {
        val content = currentContent() ?: return
        if (content.busy) return

        state.value =
            content.copy(
                editor =
                    ServiceEditor.Repricing(
                        service,
                        service.referencePriceBob.toEditableText(),
                        error = null,
                    ),
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
        val editor =
            when (val current = content.editor) {
                is ServiceEditor.Declaring -> current.copy(price = value, error = null)
                is ServiceEditor.Repricing -> current.copy(price = value, error = null)
                is ServiceEditor.Removing, null -> return
            }

        state.value = content.copy(editor = editor)
    }

    fun onDismissEditor() {
        val content = currentContent() ?: return
        if (content.busy) return

        state.value = content.copy(editor = null)
    }

    fun onConfirmPrice() {
        val content = currentContent() ?: return
        if (content.busy) return

        when (val editor = content.editor) {
            is ServiceEditor.Declaring -> {
                state.value = content.copy(busy = true, editor = editor.copy(error = null))
                write { declareService(editor.type.id, editor.price) }
            }

            is ServiceEditor.Repricing -> {
                state.value = content.copy(busy = true, editor = editor.copy(error = null))
                write { updateServicePrice(editor.service.id, editor.price) }
            }

            is ServiceEditor.Removing, null -> {
                return
            }
        }
    }

    fun onConfirmRemoval() {
        val content = currentContent() ?: return
        if (content.busy) return
        val editor = content.editor as? ServiceEditor.Removing ?: return

        state.value = content.copy(busy = true)
        write { removeService(editor.service.id) }
    }

    private fun write(block: suspend () -> ServiceDeclarationResult) {
        viewModelScope.launch {
            when (val result = block()) {
                // The list is read again rather than patched: what is still
                // undeclared is derived from both sides, and reloading is what
                // keeps the two sections from disagreeing.
                ServiceDeclarationResult.Success -> {
                    load()
                }

                is ServiceDeclarationResult.Failure -> {
                    val current = currentContent() ?: return@launch
                    state.value =
                        when (val editor = current.editor) {
                            // A refused price stays in its dialog, beside the
                            // field it belongs to, so the number the person
                            // typed is still there to correct.
                            is ServiceEditor.Declaring -> {
                                current.copy(busy = false, editor = editor.copy(error = result.error))
                            }

                            is ServiceEditor.Repricing -> {
                                current.copy(busy = false, editor = editor.copy(error = result.error))
                            }

                            // A failed removal has no field to correct, so its
                            // confirmation closes and the reason goes to the
                            // screen.
                            is ServiceEditor.Removing, null -> {
                                current.copy(busy = false, editor = null, notice = result.error)
                            }
                        }
                }
            }
        }
    }

    private fun currentContent(): MyServicesUiState.Content? = state.value as? MyServicesUiState.Content
}

private fun BigDecimal.toEditableText(): String = stripTrailingZeros().toPlainString()
