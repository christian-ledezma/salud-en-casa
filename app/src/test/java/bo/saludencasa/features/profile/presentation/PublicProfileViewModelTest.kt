package bo.saludencasa.features.profile.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.PublicProfileResult
import bo.saludencasa.features.profile.domain.usecase.GetPublicProfileUseCase
import bo.saludencasa.features.profile.publicProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class PublicProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // The card is read from professional_directory, so what reaches the screen
    // is what any patient reads and nothing else: the view carries no phone and
    // no email at all (RN-06).
    @Test
    fun `shows the professional as the directory publishes them`() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                assertEquals(PublicProfileUiState.Loading, awaitItem())

                val content = awaitItem() as PublicProfileUiState.Content
                assertEquals("Ana Quispe", content.profile.fullName)
                assertEquals(BigDecimal("120.00"), content.profile.baseRateBob)
            }

            assertEquals("fcab94c0-0000-4000-8000-000000000000", repository.lastRequestedProfessionalId)
        }

    // INV-07 holding is not a failure. A professional whose verification is
    // still pending is simply absent from the view, and telling the person the
    // read failed would send them to check their connection over a working one.
    @Test
    fun `a professional the directory does not carry is reported as not published`() =
        runTest {
            val repository = FakeProfileRepository(publicProfileResult = PublicProfileResult.NotPublished)

            viewModel(repository).uiState.test {
                assertEquals(PublicProfileUiState.Loading, awaitItem())
                assertEquals(PublicProfileUiState.NotPublished, awaitItem())
            }
        }

    @Test
    fun `a read that fails offers a retry that succeeds`() =
        runTest {
            val repository =
                FakeProfileRepository(
                    publicProfileResult = PublicProfileResult.Failure(ProfileError.NetworkUnavailable),
                )
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                assertEquals(PublicProfileUiState.Loading, awaitItem())
                assertEquals(PublicProfileUiState.Failed(ProfileError.NetworkUnavailable), awaitItem())

                repository.publicProfileResult = PublicProfileResult.Success(publicProfile())
                viewModel.load()

                assertEquals(PublicProfileUiState.Loading, awaitItem())
                assertEquals(
                    "Ana Quispe",
                    (awaitItem() as PublicProfileUiState.Content).profile.fullName,
                )
            }
        }
}

private fun viewModel(repository: FakeProfileRepository): PublicProfileViewModel =
    PublicProfileViewModel(
        professionalId = "fcab94c0-0000-4000-8000-000000000000",
        getPublicProfile = GetPublicProfileUseCase(repository),
    )
