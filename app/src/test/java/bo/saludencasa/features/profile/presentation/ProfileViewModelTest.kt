@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.profile.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.AvailabilityResult
import bo.saludencasa.features.profile.domain.model.PatientDetails
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.profile.domain.usecase.SaveProfileUseCase
import bo.saludencasa.features.profile.domain.usecase.SetAvailabilityUseCase
import bo.saludencasa.features.profile.dualRoleProfile
import bo.saludencasa.features.profile.professionalDetails
import bo.saludencasa.features.profile.professionalProfile
import bo.saludencasa.features.profile.userProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
                assertEquals(UserRole.PATIENT, content.header.activeRole)
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

    // Someone who only holds the professional role has no patients row. Sending
    // the patient section anyway would write columns that are not theirs and
    // that no policy lets them fill.
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

            assertNull(repository.lastUpdate?.patient)
        }

    // Someone who only holds the patient role has no professionals row, so the
    // screen must not hand the use case a professional section built out of the
    // empty half of the form.
    @Test
    fun `a patient saves no professional data`() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.save()
                awaitItem()
                awaitItem()
            }

            assertNull(repository.lastUpdate?.professional)
        }

    // numeric(10, 2) stores 120.00 for a rate typed as 120. Showing the stored
    // scale back would make every visit to the screen look like a pending edit,
    // and the radius would read 8.00 for a number nobody wrote that way.
    @Test
    fun `a stored rate is shown without the scale the column added`() =
        runTest {
            val viewModel =
                viewModel(FakeProfileRepository(profileResult = ProfileResult.Success(professionalProfile())))

            viewModel.uiState.test {
                awaitItem()
                val content = awaitItem() as ProfileUiState.Content

                assertEquals("120", content.form.baseRateBob)
                assertEquals("8", content.form.coverageRadiusKm)
            }
        }

    // The criterion says the switch reflects the change at once. Waiting for the
    // server would leave the switch sitting on the old value while the request
    // travels, which reads as a switch that does not work.
    @Test
    fun `the availability switch shows the new state before the server answers`() =
        runTest {
            val repository = FakeProfileRepository(profileResult = ProfileResult.Success(professionalProfile()))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                assertEquals(false, (awaitItem() as ProfileUiState.Content).availability?.availableNow)

                viewModel.onAvailabilityChange(true)

                assertEquals(true, (awaitItem() as ProfileUiState.Content).availability?.availableNow)
            }
            advanceUntilIdle()

            assertEquals(1, repository.availabilityWrites)
            assertEquals(true, repository.lastAvailability)
        }

    // The other half of showing it at once: if the write fails, the switch has
    // to go back. A switch left on while the server holds the opposite value
    // would tell the professional they are taking work when they are not.
    @Test
    fun `a failed availability write puts the switch back and says why`() =
        runTest {
            val repository =
                FakeProfileRepository(
                    profileResult = ProfileResult.Success(professionalProfile()),
                    availabilityResult = AvailabilityResult.Failure(ProfileError.NetworkUnavailable),
                )
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onAvailabilityChange(true)
                assertEquals(true, (awaitItem() as ProfileUiState.Content).availability?.availableNow)

                val reverted = (awaitItem() as ProfileUiState.Content).availability
                assertEquals(false, reverted?.availableNow)
                assertEquals(ProfileError.NetworkUnavailable, reverted?.error)
            }
        }

    // The switch writes on its own while the form is still being edited, so it
    // must not take the typed text with it.
    @Test
    fun `toggling availability keeps what the person was typing`() =
        runTest {
            val repository = FakeProfileRepository(profileResult = ProfileResult.Success(professionalProfile()))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                val content = awaitItem() as ProfileUiState.Content

                viewModel.onFormChange(content.form.copy(baseRateBob = "150"))
                awaitItem()

                viewModel.onAvailabilityChange(true)

                assertEquals("150", (awaitItem() as ProfileUiState.Content).form.baseRateBob)
            }
        }

    // Someone who only holds the patient role has no professionals row, so there
    // is no switch to draw.
    @Test
    fun `a patient has no availability switch at all`() =
        runTest {
            val viewModel = viewModel(FakeProfileRepository())

            viewModel.uiState.test {
                awaitItem()
                assertNull((awaitItem() as ProfileUiState.Content).availability)
            }
        }

    // The one this sprint exists for. Someone who holds both roles carries both
    // sections, so deciding what to write by which section loaded would send
    // professional columns on an edit made as a patient. The active role decides,
    // and save_my_profile reads the same active role on the server.
    @Test
    fun aPersonWithBothRolesEditingAsPatientWritesNoProfessionalData() =
        runTest {
            val profile = dualRoleProfile(activeRole = UserRole.PATIENT)
            val repository = FakeProfileRepository(profileResult = ProfileResult.Success(profile))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.save()
                awaitItem()
                awaitItem()
            }

            assertNull(repository.lastUpdate?.professional)
            assertEquals(null, repository.lastUpdate?.patient?.birthDate)
        }

    @Test
    fun `a person with both roles editing as a professional writes no patient data`() =
        runTest {
            val profile = dualRoleProfile(activeRole = UserRole.PROFESSIONAL)
            val repository = FakeProfileRepository(profileResult = ProfileResult.Success(profile))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.save()
                awaitItem()
                awaitItem()
            }

            assertNull(repository.lastUpdate?.patient)
        }

    // The professionals row exists for this person while they work as a patient,
    // so a switch drawn from the row existing would appear in patient mode.
    // RF-02.5 is a statement a patient has no way to make.
    @Test
    fun `a person with both roles sees no availability switch while working as a patient`() =
        runTest {
            val profile = dualRoleProfile(activeRole = UserRole.PATIENT)
            val viewModel = viewModel(FakeProfileRepository(profileResult = ProfileResult.Success(profile)))

            viewModel.uiState.test {
                awaitItem()
                assertNull((awaitItem() as ProfileUiState.Content).availability)
            }
        }

    @Test
    fun `the same person sees the availability switch once they switch to professional`() =
        runTest {
            val profile = dualRoleProfile(activeRole = UserRole.PROFESSIONAL)
            val viewModel = viewModel(FakeProfileRepository(profileResult = ProfileResult.Success(profile)))

            viewModel.uiState.test {
                awaitItem()
                assertEquals(
                    AvailabilityState(availableNow = false),
                    (awaitItem() as ProfileUiState.Content).availability,
                )
            }
        }

    // A professional who has not filled the form yet still opens the screen. The
    // empty rate has to stay empty rather than become a zero the column refuses.
    @Test
    fun `a professional with nothing declared yet opens on an empty rate`() =
        runTest {
            val profile = professionalProfile(professionalDetails(baseRateBob = null, professionalType = null))
            val viewModel = viewModel(FakeProfileRepository(profileResult = ProfileResult.Success(profile)))

            viewModel.uiState.test {
                awaitItem()
                val content = awaitItem() as ProfileUiState.Content

                assertEquals("", content.form.baseRateBob)
                assertNull(content.form.professionalType)
            }
        }
}

private fun viewModel(repository: FakeProfileRepository): ProfileViewModel =
    ProfileViewModel(
        getProfile = GetProfileUseCase(repository),
        saveProfile = SaveProfileUseCase(repository),
        setAvailability = SetAvailabilityUseCase(repository),
    )
