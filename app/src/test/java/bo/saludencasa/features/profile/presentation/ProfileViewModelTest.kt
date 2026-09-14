package bo.saludencasa.features.profile.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.PatientDetails
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.profile.domain.usecase.SaveProfileUseCase
import bo.saludencasa.features.profile.userProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // The criterion asks to see what Google brought, already filled in. An empty
    // form would invite the person to retype a name and a phone the server
    // already holds, and to overwrite them with whatever they remember.
    @Test
    fun `fills the form with what the server already holds`() =
        runTest {
            val viewModel = viewModel(FakeProfileRepository())

            viewModel.uiState.test {
                assertEquals(ProfileUiState.Loading, awaitItem())

                val content = awaitItem() as ProfileUiState.Content
                assertEquals("Ana Quispe", content.form.fullName)
                assertEquals("+59171234567", content.form.phone)
                assertEquals(UserRole.PATIENT, content.header.role)
            }
        }

    // A profile that cannot be read is not an empty profile. Showing a blank
    // form after a failed read invites the person to save it over the real one.
    @Test
    fun `a profile that cannot be read stops on an error the person can retry`() =
        runTest {
            val repository =
                FakeProfileRepository(profileResult = ProfileResult.Failure(ProfileError.NetworkUnavailable))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                assertEquals(ProfileUiState.Loading, awaitItem())
                assertEquals(ProfileUiState.Failed(ProfileError.NetworkUnavailable), awaitItem())

                repository.profileResult = ProfileResult.Success(userProfile())
                viewModel.load()

                assertEquals(ProfileUiState.Loading, awaitItem())
                assertEquals("Ana Quispe", (awaitItem() as ProfileUiState.Content).form.fullName)
            }

            assertEquals(2, repository.profileReads)
        }

    // The criterion says the error is shown and nothing is saved. Losing what
    // the person typed while showing them the error would be worse than the
    // error: they would have to type the whole form again to correct one field.
    @Test
    fun `an invalid phone is reported without discarding what the person typed`() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                val content = awaitItem() as ProfileUiState.Content

                viewModel.onFormChange(content.form.copy(phone = "123", fullName = "Ana Maria Quispe"))
                awaitItem()

                viewModel.save()
                awaitItem()

                val failed = awaitItem() as ProfileUiState.Content
                assertEquals(SaveStatus.Failed(ProfileError.InvalidPhone), failed.status)
                assertEquals("123", failed.form.phone)
                assertEquals("Ana Maria Quispe", failed.form.fullName)
            }

            assertEquals(0, repository.saveAttempts)
        }

    // What the screen shows after saving is the row the server sends back, not
    // the text that was typed. It is the only way the screen can claim the
    // change persisted rather than merely that the request left the device.
    @Test
    fun `a saved profile is redrawn from what the server returned`() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                val content = awaitItem() as ProfileUiState.Content

                repository.saveResult =
                    ProfileResult.Success(
                        userProfile(
                            fullName = "Ana Maria Quispe",
                            phone = "+59176543210",
                            patient = PatientDetails(null, "Luis Quispe", null),
                        ),
                    )
                viewModel.onFormChange(content.form.copy(phone = "76543210", fullName = "Ana Maria Quispe"))
                awaitItem()

                viewModel.save()
                assertEquals(SaveStatus.Saving, (awaitItem() as ProfileUiState.Content).status)

                val saved = awaitItem() as ProfileUiState.Content
                assertEquals(SaveStatus.Saved, saved.status)
                assertEquals("+59176543210", saved.form.phone)
                assertEquals("Luis Quispe", saved.form.emergencyContact)
            }

            assertEquals("+59176543210", repository.lastUpdate?.phone?.value)
        }

    // A professional has no patients row. Sending the patient section anyway
    // would write columns that are not theirs and that no policy lets them fill.
    @Test
    fun `a professional saves no patient data`() =
        runTest {
            val repository = FakeProfileRepository(profileResult = ProfileResult.Success(professionalProfile()))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.save()
                awaitItem()
                awaitItem()
            }

            assertEquals(null, repository.lastUpdate?.patient)
        }
}

private fun professionalProfile() = userProfile(role = UserRole.PROFESSIONAL, patient = null)

private fun viewModel(repository: FakeProfileRepository): ProfileViewModel =
    ProfileViewModel(
        getProfile = GetProfileUseCase(repository),
        saveProfile = SaveProfileUseCase(repository),
    )
