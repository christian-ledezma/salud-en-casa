package bo.saludencasa.features.reviews.domain.usecase

import bo.saludencasa.features.reviews.FakeReviewRepository
import bo.saludencasa.features.reviews.domain.model.ProfessionalReviewsPageResult
import bo.saludencasa.features.reviews.professionalReviews
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetProfessionalReviewsUseCaseTest {
    // A full page is not the end of the list. Reading it as one would hide
    // every comment past the first page.
    @Test
    fun `a full page is not the end of the comments`() =
        runTest {
            val repository = FakeReviewRepository(professionalReviews(GetProfessionalReviewsUseCase.PAGE_SIZE * 2))

            val result = GetProfessionalReviewsUseCase(repository)(PROFESSIONAL, offset = 0)

            val loaded = result as ProfessionalReviewsPageResult.Loaded
            assertEquals(GetProfessionalReviewsUseCase.PAGE_SIZE, loaded.reviews.size)
            assertFalse(loaded.endReached)
        }

    @Test
    fun `a page shorter than the page size is the end of the comments`() =
        runTest {
            val repository = FakeReviewRepository(professionalReviews(GetProfessionalReviewsUseCase.PAGE_SIZE - 1))

            val result = GetProfessionalReviewsUseCase(repository)(PROFESSIONAL, offset = 0)

            assertTrue((result as ProfessionalReviewsPageResult.Loaded).endReached)
        }

    // The offset is what the repository is told, not something the use case
    // recomputes: a page asked for twice from the wrong offset repeats rows.
    @Test
    fun `the offset travels to the repository with the page size`() =
        runTest {
            val repository = FakeReviewRepository(professionalReviews(30))

            GetProfessionalReviewsUseCase(repository)(PROFESSIONAL, offset = 10)

            assertEquals(
                FakeReviewRepository.Request(PROFESSIONAL, 10, GetProfessionalReviewsUseCase.PAGE_SIZE),
                repository.requests.single(),
            )
        }
}

private const val PROFESSIONAL = "fcab94c0-0000-4000-8000-000000000000"
