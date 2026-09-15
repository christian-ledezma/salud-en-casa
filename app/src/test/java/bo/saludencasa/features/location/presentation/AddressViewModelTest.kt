@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.location.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.location.FakeAddressRepository
import bo.saludencasa.features.location.FakeDeviceLocationRepository
import bo.saludencasa.features.location.FakeGeocodingRepository
import bo.saludencasa.features.location.address
import bo.saludencasa.features.location.coordinate
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.GeocodingResult
import bo.saludencasa.features.location.domain.model.MapDefaults
import bo.saludencasa.features.location.domain.model.MyAddressResult
import bo.saludencasa.features.location.domain.model.PositionResult
import bo.saludencasa.features.location.domain.model.SaveAddressResult
import bo.saludencasa.features.location.domain.usecase.DescribePointUseCase
import bo.saludencasa.features.location.domain.usecase.FindPlaceUseCase
import bo.saludencasa.features.location.domain.usecase.GetCurrentPositionUseCase
import bo.saludencasa.features.location.domain.usecase.GetMyAddressUseCase
import bo.saludencasa.features.location.domain.usecase.SaveAddressUseCase
import bo.saludencasa.features.location.place
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AddressViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // The fourth criterion of the story, and the reason address_text is a
    // column. An address already registered was resolved once, when it was
    // saved; asking the geocoder again on every visit would pay for a lookup
    // whose answer is already stored.
    @Test
    fun `a registered address opens without asking the geocoder anything`() =
        runTest {
            val geocoder = FakeGeocodingRepository()
            val viewModel =
                viewModel(addresses = FakeAddressRepository(MyAddressResult.Registered(address())), geocoder = geocoder)

            viewModel.uiState.test {
                assertEquals(AddressUiState.Loading, awaitItem())

                val content = awaitItem() as AddressUiState.Content
                assertEquals("Avenida Arce 2081, La Paz", content.form.addressText)
                assertEquals(coordinate(), content.point)
            }

            assertEquals(0, geocoder.describeCalls)
        }

    // The third criterion. Dropping the marker somewhere else means the written
    // address no longer describes the point, and the person is the one who
    // would be blamed for it later.
    @Test
    fun `dropping the marker rewrites the address from the new point`() =
        runTest {
            val geocoder =
                FakeGeocodingRepository(
                    describeResult =
                        GeocodingResult.Found(
                            place(addressText = "Calle Jaén 711, La Paz", city = "La Paz"),
                        ),
                )
            val viewModel = viewModel(geocoder = geocoder)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onPointPicked(-16.4960, -68.1340)
                assertEquals(coordinate(-16.4960, -68.1340), (awaitItem() as AddressUiState.Content).point)

                assertEquals("Calle Jaén 711, La Paz", (awaitItem() as AddressUiState.Content).form.addressText)
            }

            assertEquals(1, geocoder.describeCalls)
        }

    // A geocoder that cannot name a point must not empty a line the person
    // typed: on a device without one, that line is the whole address.
    @Test
    fun `a point the geocoder cannot name leaves the written address alone`() =
        runTest {
            val geocoder = FakeGeocodingRepository(describeResult = GeocodingResult.NotFound)
            val viewModel = viewModel(geocoder = geocoder)

            viewModel.uiState.test {
                awaitItem()
                val content = awaitItem() as AddressUiState.Content

                viewModel.onFormChange(content.form.copy(addressText = "Calle sin nombre, casa azul"))
                awaitItem()

                viewModel.onPointPicked(-16.4960, -68.1340)
                assertEquals(
                    "Calle sin nombre, casa azul",
                    (awaitItem() as AddressUiState.Content).form.addressText,
                )
            }
        }

    // The other half of the same rule, and a defect found on the device: keeping
    // the line quietly leaves the marker on one place and the text describing
    // another, and the person saves an address that does not match its point.
    @Test
    fun `a point the geocoder cannot name says so instead of leaving the text quietly wrong`() =
        runTest {
            val geocoder = FakeGeocodingRepository(describeResult = GeocodingResult.NotFound)
            val viewModel = viewModel(geocoder = geocoder)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onPointPicked(-16.4960, -68.1340)
                awaitItem()

                assertEquals(AddressNotice.PointNotNamed, (awaitItem() as AddressUiState.Content).notice)
            }
        }

    // A geocoder that answers with a city but no street line is the same
    // situation wearing a different shape: the line that stays behind is the one
    // that described the previous point.
    @Test
    fun `a place with no street line is reported the same way`() =
        runTest {
            val geocoder =
                FakeGeocodingRepository(describeResult = GeocodingResult.Found(place(addressText = "")))
            val viewModel = viewModel(geocoder = geocoder)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onPointPicked(-16.4960, -68.1340)
                awaitItem()

                assertEquals(AddressNotice.PointNotNamed, (awaitItem() as AddressUiState.Content).notice)
            }
        }

    // The first criterion: granting the permission centres the map on the
    // person. The counter is what the screen watches, because following the
    // marker instead would slide the map every time it is dropped.
    @Test
    fun `granting the permission centres the map on the current position`() =
        runTest {
            val device = FakeDeviceLocationRepository(PositionResult.Located(coordinate(-17.7833, -63.1821)))
            val viewModel = viewModel(device = device)

            viewModel.uiState.test {
                awaitItem()
                val before = awaitItem() as AddressUiState.Content
                assertEquals(MapDefaults.initialPosition, before.camera.target)

                viewModel.onPermissionResult(granted = true)
                awaitItem()

                val located = awaitItem() as AddressUiState.Content
                assertEquals(coordinate(-17.7833, -63.1821), located.camera.target)
                assertNotEquals(before.camera.moves, located.camera.moves)

                // The point is described afterwards, and that is the subject
                // of the test about dropping the marker, not of this one.
                cancelAndIgnoreRemainingEvents()
            }
            advanceUntilIdle()

            assertEquals(1, device.positionReads)
        }

    // The second criterion. A denied permission is a way of using the screen,
    // not a failure of it: the map stays where it opened and the address is
    // searched or marked by hand.
    @Test
    fun `denying the permission leaves the map usable and asks the device for nothing`() =
        runTest {
            val device = FakeDeviceLocationRepository()
            val viewModel = viewModel(device = device)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onPermissionResult(granted = false)

                val denied = awaitItem() as AddressUiState.Content
                assertEquals(LocationPermissionState.Denied, denied.permission)
                assertEquals(MapDefaults.initialPosition, denied.camera.target)
                assertEquals(AddressNotice.Problem(AddressError.LocationPermissionDenied), denied.notice)
            }
            advanceUntilIdle()

            assertEquals(0, device.positionReads)
        }

    // The other half of the second criterion: with no permission the address is
    // still reachable by writing it.
    @Test
    fun `searching an address without any permission moves the marker to it`() =
        runTest {
            val geocoder =
                FakeGeocodingRepository(
                    findResult =
                        GeocodingResult.Found(
                            place(
                                coordinate = coordinate(-17.7833, -63.1821),
                                addressText = "Plaza 24 de Septiembre",
                                city = "Santa Cruz",
                            ),
                        ),
                )
            val viewModel = viewModel(geocoder = geocoder)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onQueryChange("Plaza 24 de Septiembre")
                awaitItem()
                viewModel.onSearch()
                awaitItem()

                val found = awaitItem() as AddressUiState.Content
                assertEquals(coordinate(-17.7833, -63.1821), found.point)
                assertEquals("Plaza 24 de Septiembre", found.form.addressText)
                assertEquals("Santa Cruz", found.form.city)
            }
            advanceUntilIdle()

            assertEquals("Plaza 24 de Septiembre", geocoder.lastQuery)
        }

    // A search that matched nothing is not a failure to report as one. Saying
    // "unexpected error" would send the person to check a connection that works.
    @Test
    fun `a search that matches nothing says so without moving the marker`() =
        runTest {
            val geocoder = FakeGeocodingRepository(findResult = GeocodingResult.NotFound)
            val viewModel = viewModel(geocoder = geocoder)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onQueryChange("Calle que no existe")
                awaitItem()
                viewModel.onSearch()
                awaitItem()

                val answered = awaitItem() as AddressUiState.Content
                assertEquals(AddressNotice.PlaceNotFound, answered.notice)
                assertNull(answered.point)
            }
        }

    // The seventh criterion, as the screen shows it. The flag comes from the
    // row the server stored and not from anything this side decided.
    @Test
    fun `a saved first address comes back marked as the primary one`() =
        runTest {
            val repository =
                FakeAddressRepository(saveResult = SaveAddressResult.Success(address(isPrimary = true)))
            val viewModel = viewModel(addresses = repository)

            viewModel.uiState.test {
                awaitItem()
                val content = awaitItem() as AddressUiState.Content

                viewModel.onPointPicked(-16.4957, -68.1335)
                awaitItem()
                awaitItem()
                viewModel.onFormChange(
                    content.form.copy(alias = "Casa", addressText = "Avenida Arce 2081", city = "La Paz"),
                )
                awaitItem()

                viewModel.save()
                assertEquals(SaveStatus.Saving, (awaitItem() as AddressUiState.Content).status)

                val saved = awaitItem() as AddressUiState.Content
                assertEquals(SaveStatus.Saved, saved.status)
                assertEquals(true, saved.isPrimary)
                assertEquals("6b1f1f2e-0000-4000-8000-000000000000", saved.addressId)
            }
        }

    // Losing what was typed while showing the error would cost the person the
    // whole form to correct one field.
    @Test
    fun `a refused save keeps what the person typed`() =
        runTest {
            val repository = FakeAddressRepository()
            val viewModel = viewModel(addresses = repository)

            viewModel.uiState.test {
                awaitItem()
                val content = awaitItem() as AddressUiState.Content

                viewModel.onFormChange(
                    content.form.copy(alias = "Casa de mi madre", addressText = "Avenida Arce 2081", city = "La Paz"),
                )
                awaitItem()

                viewModel.save()
                awaitItem()

                val failed = awaitItem() as AddressUiState.Content
                assertEquals(SaveStatus.Failed(AddressError.PointNotChosen), failed.status)
                assertEquals("Casa de mi madre", failed.form.alias)
            }

            assertEquals(0, repository.saveAttempts)
        }

    // A profile that cannot be read is not a profile with no address: showing an
    // empty form after a failed read invites saving a second address over the
    // one that already exists.
    @Test
    fun `an address that cannot be read stops on an error the person can retry`() =
        runTest {
            val repository = FakeAddressRepository(MyAddressResult.Failure(AddressError.NetworkUnavailable))
            val viewModel = viewModel(addresses = repository)

            viewModel.uiState.test {
                assertEquals(AddressUiState.Loading, awaitItem())
                assertEquals(AddressUiState.Failed(AddressError.NetworkUnavailable), awaitItem())

                repository.myAddressResult = MyAddressResult.Registered(address())
                viewModel.load()

                assertEquals(AddressUiState.Loading, awaitItem())
                assertEquals("Casa", (awaitItem() as AddressUiState.Content).form.alias)
            }

            assertEquals(2, repository.reads)
        }
}

private fun viewModel(
    addresses: FakeAddressRepository = FakeAddressRepository(),
    geocoder: FakeGeocodingRepository = FakeGeocodingRepository(),
    device: FakeDeviceLocationRepository = FakeDeviceLocationRepository(),
): AddressViewModel =
    AddressViewModel(
        getMyAddress = GetMyAddressUseCase(addresses),
        saveAddress = SaveAddressUseCase(addresses),
        describePoint = DescribePointUseCase(geocoder),
        findPlace = FindPlaceUseCase(geocoder),
        getCurrentPosition = GetCurrentPositionUseCase(device),
    )
