package bo.saludencasa.features.location.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.location.domain.model.AddressDraft
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.GeocodingResult
import bo.saludencasa.features.location.domain.model.MapDefaults
import bo.saludencasa.features.location.domain.model.MyAddressResult
import bo.saludencasa.features.location.domain.model.Place
import bo.saludencasa.features.location.domain.model.PositionResult
import bo.saludencasa.features.location.domain.model.SaveAddressResult
import bo.saludencasa.features.location.domain.usecase.DescribePointUseCase
import bo.saludencasa.features.location.domain.usecase.FindPlaceUseCase
import bo.saludencasa.features.location.domain.usecase.GetCurrentPositionUseCase
import bo.saludencasa.features.location.domain.usecase.GetMyAddressUseCase
import bo.saludencasa.features.location.domain.usecase.SaveAddressUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddressForm(
    val alias: String,
    val addressText: String,
    val reference: String,
    val city: String,
)

enum class LocationPermissionState {
    NotRequested,
    Granted,
    Denied,
}

// The camera is moved by the application only when it relocates the marker
// itself, which is why it carries a counter. Following the marker instead would
// slide the map out from under the finger every time it is dropped.
data class MapCamera(
    val target: Coordinate,
    val moves: Int,
)

// A notice is what the screen says beside the map without stopping anything: a
// search that found nothing is not a failure, and neither is a device with no
// geocoder.
sealed interface AddressNotice {
    data object PlaceNotFound : AddressNotice

    data object PointNotNamed : AddressNotice

    data class Problem(
        val error: AddressError,
    ) : AddressNotice
}

sealed interface SaveStatus {
    data object Idle : SaveStatus

    data object Saving : SaveStatus

    data object Saved : SaveStatus

    data class Failed(
        val error: AddressError,
    ) : SaveStatus
}

sealed interface AddressUiState {
    data object Loading : AddressUiState

    data class Failed(
        val error: AddressError,
    ) : AddressUiState

    data class Content(
        val addressId: String?,
        val form: AddressForm,
        val point: Coordinate?,
        val camera: MapCamera,
        val permission: LocationPermissionState,
        val status: SaveStatus,
        val notice: AddressNotice?,
        val isLocating: Boolean,
        val isSearching: Boolean,
        val isPrimary: Boolean,
        val query: String,
    ) : AddressUiState
}

class AddressViewModel(
    private val getMyAddress: GetMyAddressUseCase,
    private val saveAddress: SaveAddressUseCase,
    private val describePoint: DescribePointUseCase,
    private val findPlace: FindPlaceUseCase,
    private val getCurrentPosition: GetCurrentPositionUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<AddressUiState>(AddressUiState.Loading)

    val uiState: StateFlow<AddressUiState> = state.asStateFlow()

    init {
        load()
    }

    // An address already registered opens from the row: its written form was
    // resolved once, when it was saved, and asking the geocoder again would be
    // the repeated lookup RF-03.3 exists to avoid.
    fun load() {
        state.value = AddressUiState.Loading
        viewModelScope.launch {
            state.value =
                when (val result = getMyAddress()) {
                    is MyAddressResult.Registered -> result.address.toContent(SaveStatus.Idle)
                    MyAddressResult.NotRegistered -> emptyContent()
                    is MyAddressResult.Failure -> AddressUiState.Failed(result.error)
                }
        }
    }

    fun onFormChange(form: AddressForm) {
        val content = currentContent() ?: return
        if (content.status is SaveStatus.Saving) return
        state.value = content.copy(form = form, status = SaveStatus.Idle)
    }

    fun onQueryChange(query: String) {
        val content = currentContent() ?: return
        state.value = content.copy(query = query, notice = null)
    }

    fun onPermissionResult(granted: Boolean) {
        val content = currentContent() ?: return
        if (!granted) {
            state.value =
                content.copy(
                    permission = LocationPermissionState.Denied,
                    notice = AddressNotice.Problem(AddressError.LocationPermissionDenied),
                )
            return
        }

        state.value = content.copy(permission = LocationPermissionState.Granted, isLocating = true, notice = null)
        viewModelScope.launch {
            when (val result = getCurrentPosition()) {
                is PositionResult.Located -> {
                    val located = currentContent() ?: return@launch
                    state.value = located.moveTo(result.coordinate, recenter = true).copy(isLocating = false)
                    describe(result.coordinate)
                }

                is PositionResult.Failure -> {
                    val failed = currentContent() ?: return@launch
                    state.value =
                        failed.copy(isLocating = false, notice = AddressNotice.Problem(result.error))
                }
            }
        }
    }

    // RF-03.6: declining is a way of using the screen, not an error in it. The
    // map stays where it opened and the address is searched or marked by hand.
    fun onPermissionDeclined() {
        val content = currentContent() ?: return
        state.value = content.copy(permission = LocationPermissionState.Denied, notice = null)
    }

    fun onPointPicked(
        latitude: Double,
        longitude: Double,
    ) {
        val content = currentContent() ?: return
        // El guardado ya se llevó el punto que había cuando empezó. Mover el
        // marcador mientras la petición viaja haría que la fila que vuelve del
        // servidor lo devolviera a su sitio sin explicación, descartando lo que
        // la persona acaba de elegir (revisión del pull request #4).
        if (content.status is SaveStatus.Saving) return
        val coordinate = Coordinate.create(latitude, longitude).getOrNull() ?: return

        state.value =
            content
                .moveTo(coordinate, recenter = false)
                .copy(status = SaveStatus.Idle, notice = null)
        viewModelScope.launch { describe(coordinate) }
    }

    fun onSearch() {
        val content = currentContent() ?: return
        if (content.query.isBlank() || content.isSearching) return
        // Por la misma razón que el toque en el mapa: una búsqueda que acierta
        // mueve el marcador, y el guardado en curso lo desharía.
        if (content.status is SaveStatus.Saving) return

        state.value = content.copy(isSearching = true, notice = null)
        viewModelScope.launch {
            val result = findPlace(content.query)
            val current = currentContent() ?: return@launch
            state.value =
                when (result) {
                    is GeocodingResult.Found -> {
                        current
                            .moveTo(result.place.coordinate, recenter = true)
                            .withPlace(result.place)
                            .copy(isSearching = false)
                    }

                    GeocodingResult.NotFound -> {
                        current.copy(isSearching = false, notice = AddressNotice.PlaceNotFound)
                    }

                    is GeocodingResult.Failure -> {
                        current.copy(isSearching = false, notice = AddressNotice.Problem(result.error))
                    }
                }
        }
    }

    fun save() {
        val content = currentContent() ?: return
        if (content.status is SaveStatus.Saving) return

        state.value = content.copy(status = SaveStatus.Saving)
        viewModelScope.launch {
            val draft =
                AddressDraft(
                    id = content.addressId,
                    alias = content.form.alias,
                    addressText = content.form.addressText,
                    reference = content.form.reference,
                    city = content.form.city,
                    coordinate = content.point,
                )
            val current = currentContent() ?: return@launch
            state.value =
                when (val result = saveAddress(draft)) {
                    is SaveAddressResult.Success -> {
                        result.address.toContent(
                            status = SaveStatus.Saved,
                            camera = current.camera,
                            permission = current.permission,
                        )
                    }

                    is SaveAddressResult.Failure -> {
                        current.copy(status = SaveStatus.Failed(result.error))
                    }
                }
        }
    }

    private suspend fun describe(coordinate: Coordinate) {
        when (val result = describePoint(coordinate)) {
            is GeocodingResult.Found -> {
                val current = currentContent() ?: return
                state.value = current.withPlace(result.place)
            }

            // A point the geocoder cannot name is still a point, so the written
            // address stays as it is: erasing it would cost the person the line
            // they typed by hand. Staying quiet would be worse, though, because
            // the marker would sit on one place and the text would describe
            // another, which is the opposite of what this story asks for.
            GeocodingResult.NotFound -> {
                val current = currentContent() ?: return
                state.value = current.copy(notice = AddressNotice.PointNotNamed)
            }

            is GeocodingResult.Failure -> {
                val current = currentContent() ?: return
                state.value = current.copy(notice = AddressNotice.Problem(result.error))
            }
        }
    }

    private fun currentContent(): AddressUiState.Content? = state.value as? AddressUiState.Content
}

private fun AddressUiState.Content.moveTo(
    coordinate: Coordinate,
    recenter: Boolean,
): AddressUiState.Content =
    copy(
        point = coordinate,
        camera = if (recenter) MapCamera(coordinate, camera.moves + 1) else camera,
    )

// What the geocoder answers replaces the written address, which is the third
// criterion of the story. It never replaces it with nothing: a geocoder that
// returns a point with no street would otherwise empty a line the person typed.
// It says so instead, because the line it kept describes the previous point.
private fun AddressUiState.Content.withPlace(place: Place): AddressUiState.Content =
    copy(
        form =
            form.copy(
                addressText = place.addressText.ifBlank { form.addressText },
                city = place.city.ifBlank { form.city },
            ),
        notice = if (place.addressText.isBlank()) AddressNotice.PointNotNamed else null,
        // Limpiar el estado es para retirar el error de un guardado anterior,
        // no para tapar uno en curso: una descripción que llega tarde apagaría
        // el indicador de guardando mientras la petición sigue viajando.
        status = if (status is SaveStatus.Saving) status else SaveStatus.Idle,
    )

private fun Address.toContent(
    status: SaveStatus,
    camera: MapCamera? = null,
    permission: LocationPermissionState = LocationPermissionState.NotRequested,
): AddressUiState.Content =
    AddressUiState.Content(
        addressId = id,
        form =
            AddressForm(
                alias = alias,
                addressText = addressText,
                reference = reference.orEmpty(),
                city = city,
            ),
        point = coordinate,
        camera = camera ?: MapCamera(coordinate, 1),
        permission = permission,
        status = status,
        notice = null,
        isLocating = false,
        isSearching = false,
        isPrimary = isPrimary,
        query = "",
    )

private fun emptyContent(): AddressUiState.Content =
    AddressUiState.Content(
        addressId = null,
        form = AddressForm(alias = "", addressText = "", reference = "", city = ""),
        point = null,
        camera = MapCamera(MapDefaults.initialPosition, 0),
        permission = LocationPermissionState.NotRequested,
        status = SaveStatus.Idle,
        notice = null,
        isLocating = false,
        isSearching = false,
        isPrimary = false,
        query = "",
    )
