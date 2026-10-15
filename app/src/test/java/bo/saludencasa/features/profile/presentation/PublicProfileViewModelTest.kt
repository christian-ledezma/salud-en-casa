@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.profile.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.catalog.FakeCatalogRepository
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.usecase.GetProfessionalServicesUseCase
import bo.saludencasa.features.catalog.professionalService
import bo.saludencasa.features.catalog.venousLine
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.PublicProfileResult
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.CanScheduleAppointmentUseCase
import bo.saludencasa.features.profile.domain.usecase.GetPublicProfileUseCase
import bo.saludencasa.features.profile.loadedRoles
import bo.saludencasa.features.profile.publicProfile
import bo.saludencasa.features.reviews.FakeReviewRepository
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.usecase.GetProfessionalReviewsUseCase
import bo.saludencasa.features.reviews.professionalReview
import bo.saludencasa.features.reviews.professionalReviews
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
            val profiles = FakeProfileRepository()
            val viewModel = viewModel(profiles = profiles)
            advanceUntilIdle()

            val content = viewModel.uiState.value as PublicProfileUiState.Content
            assertEquals("Ana Quispe", content.profile.fullName)
            assertEquals(BigDecimal("120.00"), content.profile.baseRateBob)
            assertEquals(PROFESSIONAL, profiles.lastRequestedProfessionalId)
        }

    // INV-07 holding is not a failure. A professional whose verification is
    // still pending is simply absent from the view, and telling the person the
    // read failed would send them to check their connection over a working one.
    @Test
    fun `a professional the directory does not carry is reported as not published`() =
        runTest {
            val catalog = FakeCatalogRepository()
            val reviews = FakeReviewRepository()
            val viewModel =
                viewModel(
                    profiles = FakeProfileRepository(publicProfileResult = PublicProfileResult.NotPublished),
                    catalog = catalog,
                    reviews = reviews,
                )
            advanceUntilIdle()

            assertEquals(PublicProfileUiState.NotPublished, viewModel.uiState.value)
            // And neither section was asked for: an unpublished profile has no
            // services and no comments to draw.
            assertTrue(catalog.servicesOfRequests.isEmpty())
            assertTrue(reviews.requests.isEmpty())
        }

    @Test
    fun `a read that fails offers a retry that succeeds`() =
        runTest {
            val profiles =
                FakeProfileRepository(
                    publicProfileResult = PublicProfileResult.Failure(ProfileError.NetworkUnavailable),
                )
            val viewModel = viewModel(profiles = profiles)
            advanceUntilIdle()

            assertEquals(PublicProfileUiState.Failed(ProfileError.NetworkUnavailable), viewModel.uiState.value)

            profiles.publicProfileResult = PublicProfileResult.Success(publicProfile())
            viewModel.load()
            advanceUntilIdle()

            val content = viewModel.uiState.value as PublicProfileUiState.Content
            assertEquals("Ana Quispe", content.profile.fullName)
        }

    // The two extra reads are about the professional the card is about. Asking
    // with the caller's own id instead would quietly show the visitor their own
    // services on somebody else's profile.
    @Test
    fun `the services and the comments are asked for the professional on screen`() =
        runTest {
            val catalog = FakeCatalogRepository(services = listOf(professionalService(type = venousLine)))
            val reviews = FakeReviewRepository(professionalReviews(3))

            viewModel(catalog = catalog, reviews = reviews)
            advanceUntilIdle()

            assertEquals(PROFESSIONAL, catalog.servicesOfRequests.single())
            assertEquals(PROFESSIONAL, reviews.requests.single().professionalId)
        }

    @Test
    fun `a professional with no comments shows the empty state and not a failure`() =
        runTest {
            val viewModel = viewModel(reviews = FakeReviewRepository(emptyList()))
            advanceUntilIdle()

            assertEquals(ReviewsState.Empty, content(viewModel).reviews)
        }

    // The services section failing is not the profile failing. Taking the whole
    // screen down over a row of prices would hide the biography, the rate and
    // the comments, which read perfectly well.
    @Test
    fun `services that cannot be read leave the rest of the profile standing`() =
        runTest {
            val catalog = FakeCatalogRepository().apply { readFailure = CatalogError.NetworkUnavailable }
            val viewModel = viewModel(catalog = catalog, reviews = FakeReviewRepository(professionalReviews(2)))
            advanceUntilIdle()

            val content = content(viewModel)
            assertEquals(ServicesState.Failed(CatalogError.NetworkUnavailable), content.services)
            assertEquals("Ana Quispe", content.profile.fullName)
            assertTrue(content.reviews is ReviewsState.Content)
        }

    @Test
    fun `loading the next page of comments appends to the ones already read`() =
        runTest {
            val page = GetProfessionalReviewsUseCase.PAGE_SIZE
            val viewModel = viewModel(reviews = FakeReviewRepository(professionalReviews(page + 3)))
            advanceUntilIdle()

            viewModel.onReachedEnd()
            advanceUntilIdle()

            val reviews = content(viewModel).reviews as ReviewsState.Content
            assertEquals(page + 3, reviews.reviews.size)
            assertTrue(reviews.endReached)
            assertFalse(reviews.loadingMore)
        }

    // The one that makes the deduplication earn its place. Offset pagination
    // over a list that grew at the top hands back a row that is already on
    // screen, and a repeated key throws out of a LazyColumn. Static pages from
    // the fake never produce the overlap, so an assertion written over those
    // would hold with the deduplication deleted.
    @Test
    fun `a comment written while the list is open is not shown twice`() =
        runTest {
            val page = GetProfessionalReviewsUseCase.PAGE_SIZE
            val repository = FakeReviewRepository(professionalReviews(page + 2))
            val viewModel = viewModel(reviews = repository)
            advanceUntilIdle()

            // Somebody rates the professional: the newest comment goes first,
            // so every offset from here on points one row further back.
            repository.reviews = listOf(professionalReview(id = "review-0")) + repository.reviews
            viewModel.onReachedEnd()
            advanceUntilIdle()

            val reviews = (content(viewModel).reviews as ReviewsState.Content).reviews
            assertEquals(reviews.size, reviews.map { it.id }.distinct().size)
        }

    // The exact multiple is the border where a full page is followed by an
    // empty one. Reading that empty page as anything other than the end leaves
    // the footer asking for ever.
    @Test
    fun `a total that is an exact multiple of the page ends on the empty page`() =
        runTest {
            val page = GetProfessionalReviewsUseCase.PAGE_SIZE
            val viewModel = viewModel(reviews = FakeReviewRepository(professionalReviews(page)))
            advanceUntilIdle()

            val first = content(viewModel).reviews as ReviewsState.Content
            assertFalse("A full first page is not the end.", first.endReached)

            viewModel.onReachedEnd()
            advanceUntilIdle()

            val second = content(viewModel).reviews as ReviewsState.Content
            assertEquals(page, second.reviews.size)
            assertTrue(second.endReached)
            assertFalse(second.loadingMore)
        }

    @Test
    fun `comments that cannot be read at all offer a retry that succeeds`() =
        runTest {
            val repository = FakeReviewRepository(professionalReviews(2))
            repository.failure = ReviewError.NetworkUnavailable
            val viewModel = viewModel(reviews = repository)
            advanceUntilIdle()

            assertEquals(ReviewsState.Failed(ReviewError.NetworkUnavailable), content(viewModel).reviews)

            repository.failure = null
            viewModel.onRetryReviews()
            advanceUntilIdle()

            assertEquals(2, (content(viewModel).reviews as ReviewsState.Content).reviews.size)
        }

    @Test
    fun `services that cannot be read offer a retry that succeeds`() =
        runTest {
            val catalog = FakeCatalogRepository().apply { readFailure = CatalogError.NetworkUnavailable }
            val viewModel = viewModel(catalog = catalog)
            advanceUntilIdle()

            assertEquals(ServicesState.Failed(CatalogError.NetworkUnavailable), content(viewModel).services)

            catalog.readFailure = null
            viewModel.onRetryServices()
            advanceUntilIdle()

            assertEquals(ServicesState.Empty, content(viewModel).services)
        }

    // Tearing the list down because its last page failed would lose comments
    // the person is already reading.
    @Test
    fun `a failed next page keeps the comments already read and can be retried`() =
        runTest {
            val page = GetProfessionalReviewsUseCase.PAGE_SIZE
            val repository = FakeReviewRepository(professionalReviews(page + 2))
            val viewModel = viewModel(reviews = repository)
            advanceUntilIdle()

            repository.failure = ReviewError.NetworkUnavailable
            viewModel.onReachedEnd()
            advanceUntilIdle()

            val failed = content(viewModel).reviews as ReviewsState.Content
            assertEquals(page, failed.reviews.size)
            assertEquals(ReviewError.NetworkUnavailable, failed.notice)
            assertFalse(failed.loadingMore)

            repository.failure = null
            viewModel.onReachedEnd()
            advanceUntilIdle()

            val recovered = content(viewModel).reviews as ReviewsState.Content
            assertEquals(page + 2, recovered.reviews.size)
            assertEquals(null, recovered.notice)
        }

    // The footer asks again every time the list recomposes, so without the
    // guard one scroll would spend several requests on the same page.
    @Test
    fun `reaching the end twice while the page loads asks for it once`() =
        runTest {
            val page = GetProfessionalReviewsUseCase.PAGE_SIZE
            val repository = FakeReviewRepository(professionalReviews(page + 1))
            val viewModel = viewModel(reviews = repository)
            advanceUntilIdle()

            val gate = CompletableDeferred<Unit>()
            repository.gate = gate
            viewModel.onReachedEnd()
            viewModel.onReachedEnd()
            advanceUntilIdle()

            assertEquals(2, repository.requests.size)
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(2, repository.requests.size)
        }

    // HU-16. The action belongs to someone looking for care. A person holding
    // both roles who is acting as a professional is not booking anyone.
    @Test
    fun `scheduling is offered to an active patient viewing a professional who declares services`() =
        runTest {
            val viewModel =
                viewModel(
                    profiles = FakeProfileRepository(roleResult = loadedRoles()),
                    catalog = FakeCatalogRepository(services = listOf(professionalService(type = venousLine))),
                )
            advanceUntilIdle()

            assertTrue(content(viewModel).showsScheduleAction)
        }

    @Test
    fun `scheduling is not offered while the active role is professional`() =
        runTest {
            val profiles =
                FakeProfileRepository(
                    roleResult =
                        loadedRoles(
                            held = setOf(UserRole.PATIENT, UserRole.PROFESSIONAL),
                            active = UserRole.PROFESSIONAL,
                        ),
                )
            val viewModel =
                viewModel(
                    profiles = profiles,
                    catalog = FakeCatalogRepository(services = listOf(professionalService(type = venousLine))),
                )
            advanceUntilIdle()

            assertFalse(content(viewModel).showsScheduleAction)
        }

    // INV-16: the professional opening their own profile from "Mi cuenta" while
    // acting as a patient must not be offered to book themselves.
    @Test
    fun `scheduling is not offered on the caller's own profile`() =
        runTest {
            val viewModel =
                viewModel(
                    profiles = FakeProfileRepository(roleResult = loadedRoles(), signedInUserId = PROFESSIONAL),
                    catalog = FakeCatalogRepository(services = listOf(professionalService(type = venousLine))),
                )
            advanceUntilIdle()

            assertFalse(content(viewModel).showsScheduleAction)
        }

    @Test
    fun `scheduling is not offered by a professional who declares no services`() =
        runTest {
            val viewModel =
                viewModel(
                    profiles = FakeProfileRepository(roleResult = loadedRoles()),
                    catalog = FakeCatalogRepository(services = emptyList()),
                )
            advanceUntilIdle()

            assertEquals(ServicesState.Empty, content(viewModel).services)
            assertFalse(content(viewModel).showsScheduleAction)
        }

    // The roles failing to read hides the action and nothing else: the profile
    // reads perfectly well without it.
    @Test
    fun `roles that cannot be read hide the action and leave the profile standing`() =
        runTest {
            val viewModel =
                viewModel(
                    profiles = FakeProfileRepository(roleResult = RoleResult.Failure(ProfileError.NetworkUnavailable)),
                    catalog = FakeCatalogRepository(services = listOf(professionalService(type = venousLine))),
                )
            advanceUntilIdle()

            val content = content(viewModel)
            assertFalse(content.showsScheduleAction)
            assertTrue(content.services is ServicesState.Content)
        }

    @Test
    fun `the end of the comments is not asked for again`() =
        runTest {
            val repository = FakeReviewRepository(professionalReviews(2))
            val viewModel = viewModel(reviews = repository)
            advanceUntilIdle()

            viewModel.onReachedEnd()
            advanceUntilIdle()

            assertEquals(1, repository.requests.size)
        }
}

private const val PROFESSIONAL = "fcab94c0-0000-4000-8000-000000000000"

private fun content(viewModel: PublicProfileViewModel): PublicProfileUiState.Content =
    viewModel.uiState.value as PublicProfileUiState.Content

private fun viewModel(
    profiles: FakeProfileRepository = FakeProfileRepository(),
    catalog: FakeCatalogRepository = FakeCatalogRepository(),
    reviews: FakeReviewRepository = FakeReviewRepository(),
): PublicProfileViewModel =
    PublicProfileViewModel(
        professionalId = PROFESSIONAL,
        getPublicProfile = GetPublicProfileUseCase(profiles),
        getProfessionalServices = GetProfessionalServicesUseCase(catalog),
        getProfessionalReviews = GetProfessionalReviewsUseCase(reviews),
        canScheduleAppointment = CanScheduleAppointmentUseCase(profiles),
    )
