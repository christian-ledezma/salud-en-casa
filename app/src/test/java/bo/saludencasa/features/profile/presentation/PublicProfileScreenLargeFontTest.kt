package bo.saludencasa.features.profile.presentation

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.PublicProfile
import bo.saludencasa.features.reviews.domain.model.ProfessionalReview
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PublicProfileScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    // "Colocación y control de vía venosa" is the longest of the twelve catalog
    // entries, so it is the one that decides whether a service row can hold a
    // name at this font scale.
    private val venousLine =
        ServiceType("t1", "Colocación y control de vía venosa", null, BigDecimal("120.00"), 60)

    private val vaccination =
        ServiceType("t2", "Vacunación", null, BigDecimal("90.00"), 20)

    private fun show(
        services: ServicesState = ServicesState.Content(listOf(service(venousLine), service(vaccination))),
        reviews: ReviewsState = reviewsContent(),
        profile: PublicProfile = profile(),
    ) {
        composeRule.setContentAtDoubleFontScale {
            PublicProfileContent(
                uiState = PublicProfileUiState.Content(profile, services, reviews),
                actions = PublicProfileActions({}, {}, {}, {}, {}),
            )
        }
    }

    // A LazyColumn composes nothing below the fold, so a row the sections
    // pushed out of sight is absent from the semantics tree and cannot be
    // reached by its text. The list is moved to its index first.
    private fun scrollTo(index: Int) {
        composeRule.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(index)
    }

    @Test
    fun theWayBackStaysOnScreen() {
        show()

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)),
        )
    }

    @Test
    fun theVerifiedBadgeIsReachableAndFitsTheScreen() {
        show()

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_verified_badge)),
        )
    }

    @Test
    fun theNameAndTheRateFitTheScreen() {
        show()

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Ana Quispe"))
        // Matched on the label and not on the whole line: the amount is
        // rendered by the platform currency formatter, whose output depends on
        // the locale the test environment happens to carry. What this test is
        // about is that the row is composed, reachable and within the edges.
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText("Tarifa base", substring = true),
        )
    }

    @Test
    fun theSectionTitlesAreReachable() {
        show()

        scrollTo(SERVICES_TITLE)
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.public_profile_services_title)),
        )

        scrollTo(REVIEWS_TITLE)
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.public_profile_reviews_title)),
        )
    }

    // The relative comparison and not an absolute height: at a 200 % font scale
    // a single line already clears any resting height, so measuring against a
    // constant passes without holding anything (docs/decisions.md, 2026-10-09).
    // The long service name has to wrap, which makes its row taller than the
    // short one's. If it were clipped to one line, both would measure the same.
    @Test
    fun theLongestServiceNameWrapsInsteadOfBeingClipped() {
        show()

        scrollTo(FIRST_SERVICE)
        val long = composeRule.onNodeWithText(venousLine.name).getUnclippedBoundsInRoot()
        val short = composeRule.onNodeWithText(vaccination.name).getUnclippedBoundsInRoot()

        val longHeight = long.bottom - long.top
        val shortHeight = short.bottom - short.top
        // Twice and not merely taller. Measured on 2026-10-11: clipped to one
        // line the long name still comes out 35 dp against the short one's 33,
        // because the two strings do not carry the same glyphs, so "taller than"
        // passes in both worlds. Wrapping means at least two lines, and the real
        // figure is 75 dp.
        assertTrue(
            "The long name is $longHeight against the short one's $shortHeight, so it did not wrap.",
            longHeight > shortHeight * 2,
        )
    }

    @Test
    fun aCommentAndItsRatingStayWithinTheScreen() {
        show()

        scrollTo(FIRST_REVIEW)
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(LONG_COMMENT))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_review_rating, 5)),
        )
    }

    @Test
    fun theEmptyStateOfEachSectionIsReachable() {
        show(services = ServicesState.Empty, reviews = ReviewsState.Empty)

        scrollTo(SERVICES_TITLE)
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.public_profile_services_empty)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.public_profile_reviews_empty)),
        )
    }

    // The footer of a failed page carries its reason and its retry, and both
    // have to be reachable or the list is stuck with no way forward.
    @Test
    fun aFailedPageOfCommentsKeepsItsReasonAndItsRetryOnScreen() {
        show(
            reviews =
                ReviewsState.Content(
                    reviews = listOf(review("r1", 5, LONG_COMMENT)),
                    loadingMore = false,
                    endReached = false,
                    notice = ReviewError.NetworkUnavailable,
                ),
        )

        scrollTo(FIRST_REVIEW + 1)
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.error_network_unavailable)),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }

    @Test
    fun theServicesFailureOffersItsRetryWithoutTakingTheProfileDown() {
        show(services = ServicesState.Failed(CatalogError.NetworkUnavailable), reviews = ReviewsState.Empty)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Ana Quispe"))
        scrollTo(SERVICES_TITLE)
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }

    @Test
    fun aCommentWithoutWordsSaysSoInsteadOfLeavingABlankLine() {
        show(
            reviews =
                ReviewsState.Content(
                    reviews = listOf(review("r1", 4, null)),
                    loadingMore = false,
                    endReached = true,
                    notice = null,
                ),
        )

        scrollTo(FIRST_REVIEW)
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.public_profile_review_without_comment)),
        )
    }

    private fun profile(): PublicProfile =
        PublicProfile(
            professionalId = "fcab94c0-0000-4000-8000-000000000000",
            fullName = "Ana Quispe",
            photoUrl = null,
            averageRating = 4.8,
            totalReviews = 12,
            totalServices = 34,
            professionalType = ProfessionalType.NURSE,
            specialty = "Enfermería geriátrica",
            biography = "Diez años atendiendo a domicilio en La Paz.",
            yearsOfExperience = 10,
            baseRateBob = BigDecimal("120.00"),
            availableNow = true,
        )

    private fun service(type: ServiceType): ProfessionalService =
        ProfessionalService(id = "s-" + type.id, type = type, referencePriceBob = type.referencePriceBob)

    private fun review(
        id: String,
        rating: Int,
        comment: String?,
    ): ProfessionalReview =
        ProfessionalReview(
            id = id,
            rating = rating,
            comment = comment,
            createdAt = Instant.parse("2026-09-01T12:00:00Z"),
        )

    private fun reviewsContent(): ReviewsState.Content =
        ReviewsState.Content(
            reviews = listOf(review("r1", 5, LONG_COMMENT), review("r2", 3, "Buen trato.")),
            loadingMore = false,
            endReached = true,
            notice = null,
        )
}

private const val LONG_COMMENT =
    "Muy puntual y explicó todo con paciencia a mi madre, que es mayor y pregunta mucho. Volvería a llamarla."

// The indices of the lazy list: header, identity, facts, biography, the two
// section titles and what hangs under each.
private const val SERVICES_TITLE = 4
private const val FIRST_SERVICE = 5
private const val REVIEWS_TITLE = 7
private const val FIRST_REVIEW = 8
