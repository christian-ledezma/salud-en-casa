package bo.saludencasa.features.profile.presentation

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.PublicProfile
import bo.saludencasa.features.reviews.domain.model.ProfessionalReview
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant

// The footer is what pages the list, and the guard that keeps it from retrying
// a failed page by itself is a condition in the screen, not in the view model.
// Nothing below this file would catch its removal.
@RunWith(AndroidJUnit4::class)
class PublicProfileReviewsFooterTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theFooterAsksForTheNextPageWhenThereIsNothingToRead() {
        var asked = 0
        show(notice = null) { asked++ }

        scrollToFooter()

        assertEquals(1, asked)
    }

    // Without the guard this is an endless loop: the page fails, the footer
    // composes, the effect asks again, it fails again, and the screen spends
    // the person's data until they leave it.
    @Test
    fun aFooterCarryingAReasonWaitsForTheTap() {
        var asked = 0
        show(notice = ReviewError.NetworkUnavailable) { asked++ }

        scrollToFooter()

        assertEquals(0, asked)
    }

    private fun scrollToFooter() {
        composeRule.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(FOOTER)
        composeRule.waitForIdle()
    }

    private fun show(
        notice: ReviewError?,
        onReachedEnd: () -> Unit,
    ) {
        composeRule.setContent {
            SaludEnCasaTheme(darkTheme = false) {
                PublicProfileContent(
                    uiState =
                        PublicProfileUiState.Content(
                            profile = profile,
                            services = ServicesState.Empty,
                            reviews =
                                ReviewsState.Content(
                                    reviews = listOf(review),
                                    loadingMore = false,
                                    endReached = false,
                                    notice = notice,
                                ),
                        ),
                    actions = PublicProfileActions({}, {}, {}, {}, onReachedEnd, {}),
                )
            }
        }
    }

    private val profile =
        PublicProfile(
            professionalId = "fcab94c0-0000-4000-8000-000000000000",
            fullName = "Ana Quispe",
            photoUrl = null,
            averageRating = 4.8,
            totalReviews = 12,
            totalServices = 34,
            professionalType = ProfessionalType.NURSE,
            specialty = "Enfermería geriátrica",
            biography = null,
            yearsOfExperience = 10,
            baseRateBob = BigDecimal("120.00"),
            availableNow = true,
        )

    private val review =
        ProfessionalReview(
            id = "review-1",
            rating = 5,
            comment = "Muy puntual.",
            createdAt = Instant.parse("2026-09-01T12:00:00Z"),
        )
}

// header, identity, facts, the two section titles with the empty services under
// the first, the only comment, and the footer. There is no biography here.
private const val FOOTER = 7
