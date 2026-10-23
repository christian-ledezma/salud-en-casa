package bo.saludencasa.features.search.presentation

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.search.domain.model.SearchCriteria
import bo.saludencasa.features.search.domain.model.SearchError
import bo.saludencasa.features.search.domain.model.SearchOrigin
import bo.saludencasa.features.search.domain.model.SearchRadius
import bo.saludencasa.features.search.nearbyProfessional
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class ProfessionalSearchScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val origin =
        SearchOrigin(
            addressId = "home",
            alias = "Mi casa",
            addressText = "Avenida América 1234, Cochabamba",
            coordinate = Coordinate.create(-17.3835, -66.1568).getOrThrow(),
        )

    // "Colocación y control de vía venosa" is the longest of the twelve catalog
    // entries, so it is the one that decides whether the chip row can hold them.
    private val venousLine =
        ServiceType("t1", "Colocación y control de vía venosa", null, BigDecimal("120.00"), 60)

    // The short one is the measuring stick of the test below, and "Todos"
    // cannot be it: that word appears twice once the list is open, as the folded
    // value and as the first option.
    private val vaccination = ServiceType("t2", "Vacunación", null, BigDecimal("90.00"), 20)

    private fun show(
        results: SearchResultsState,
        criteria: SearchCriteria = SearchCriteria(),
        withOrigin: Boolean = true,
        typeFilterExpanded: Boolean = false,
    ) {
        composeRule.setContentAtDoubleFontScale {
            ProfessionalSearchContent(
                uiState =
                    ProfessionalSearchUiState(
                        criteria = criteria,
                        origin = if (withOrigin) origin else null,
                        serviceTypes = listOf(venousLine, vaccination),
                        typeFilterExpanded = typeFilterExpanded,
                        results = results,
                    ),
                onBack = {},
                actions = SearchActions(),
            )
        }
    }

    private fun content(vararg distances: Int) =
        SearchResultsState.Content(
            professionals =
                distances.mapIndexed { index, distance ->
                    nearbyProfessional(
                        id = "p$index",
                        fullName = "Carla Villarroel Antezana",
                        distanceM = distance,
                    )
                },
            loadingMore = false,
            endReached = true,
            notice = null,
        )

    // A LazyColumn composes nothing below the fold, so a row the criteria pushed
    // out of sight is absent from the semantics tree and cannot be reached by
    // its text. The list is moved to its index first; the page is the outer
    // scrollable, the first in tree order.
    private fun scrollTo(index: Int) {
        composeRule.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(index)
    }

    @Test
    fun theWayBackStaysOnScreen() {
        show(content(1200))

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)),
        )
    }

    // RF-06.1. Three chips sharing the width of a 320 dp phone at a 200 % font
    // scale is where a segmented control would have clipped them, which is why
    // they are a FlowRow.
    @Test
    fun everyRadiusOptionStaysOnScreen() {
        show(content(1200))
        scrollTo(2)

        SearchRadius.entries.forEach { radius ->
            composeRule.assertFitsTheScreen(
                composeRule.onNodeWithText(string(R.string.search_radius_option, radius.km)),
            )
        }
    }

    // Folded, the filter says what it is filtering by and nothing else.
    @Test
    fun theFoldedTypeFilterSaysWhatIsSelected() {
        show(content(1200))
        scrollTo(2)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.search_type_all)))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(string(R.string.cd_open_type_filter)),
        )
    }

    // Open, the longest name of the catalog has to be readable whole. As a chip
    // it was not: 470 dp of label on a 320 dp screen, and capping the chip only
    // moved the overflow inside it.
    @Test
    fun theLongestServiceTypeFitsTheScreenWhenTheListIsOpen() {
        show(content(1200), typeFilterExpanded = true)
        scrollTo(2)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(venousLine.name))
    }

    // And it fits by wrapping, not by hiding the rest of itself. Measuring the
    // row against its own resting height does not catch that -- at a 200 % font
    // scale one line already clears 48 dp -- so the measure is the short option
    // beside it: if the long label wrapped, its row is taller than the short
    // one's; if it was pinned to one line, both are the same height.
    // This is as close as an assertion gets to the clipping inside a container
    // that the previews exist for (docs/decisions.md, 2026-10-08).
    @Test
    fun theLongestServiceTypeWrapsInsteadOfHidingItsTail() {
        show(content(1200), typeFilterExpanded = true)
        scrollTo(2)

        val shortRow = composeRule.onNodeWithText(vaccination.name).getUnclippedBoundsInRoot()
        val longRow = composeRule.onNodeWithText(venousLine.name).getUnclippedBoundsInRoot()
        val shortHeight = shortRow.bottom - shortRow.top
        val longHeight = longRow.bottom - longRow.top

        assertTrue(
            "Both rows are $longHeight tall, so the long name did not wrap and its tail is hidden.",
            longHeight > shortHeight,
        )
    }

    // The second criterion of HU-11: distance, base rate and reputation, on the
    // card of the longest name the fixtures have. And the action that saves the
    // trip through the profile (docs/decisions.md, 2026-10-23).
    @Test
    fun aResultCardKeepsItsNameDistanceRateAndActionsOnScreen() {
        show(content(0))
        scrollTo(4)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("Carla Villarroel Antezana"))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.common_distance_near)),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText("180", substring = true))
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithContentDescription(
                string(R.string.cd_open_professional_profile, "Carla Villarroel Antezana"),
            ),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.search_schedule_action)))
    }

    // The button books the professional of its own card, not whichever card
    // the profile tap belongs to.
    @Test
    fun theScheduleActionNamesTheProfessionalOfItsCard() {
        var scheduled: String? = null
        var opened: String? = null
        composeRule.setContentAtDoubleFontScale {
            ProfessionalSearchContent(
                uiState =
                    ProfessionalSearchUiState(
                        origin = origin,
                        results = content(400),
                    ),
                onBack = {},
                actions = SearchActions(onSchedule = { scheduled = it }, onOpenProfessional = { opened = it }),
            )
        }
        scrollTo(4)

        composeRule.onNodeWithText(string(R.string.search_schedule_action)).performClick()

        assertEquals("p0", scheduled)
        assertNull(opened)
    }

    // An average of zero and no average at all are the same number, and the
    // card used to draw the badge for both: every professional with no ratings
    // appeared with the worst possible score.
    @Test
    fun aProfessionalWithoutRatingsIsNotShownAsRatedZero() {
        composeRule.setContentAtDoubleFontScale {
            ProfessionalSearchContent(
                uiState =
                    ProfessionalSearchUiState(
                        origin = origin,
                        results =
                            SearchResultsState.Content(
                                professionals =
                                    listOf(
                                        nearbyProfessional(
                                            id = "p0",
                                            averageRating = 0.0,
                                            totalReviews = 0,
                                        ),
                                    ),
                                loadingMore = false,
                                endReached = true,
                                notice = null,
                            ),
                    ),
                onBack = {},
                actions = SearchActions(),
            )
        }
        scrollTo(4)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.search_no_rating_yet)),
        )
        composeRule.onNodeWithText("0,0").assertDoesNotExist()
        composeRule.onNodeWithText("0.0").assertDoesNotExist()
    }

    // The row that carries the rating also carries the distance, and without a
    // rating the label that takes the badge's place is a sentence rather than a
    // pill. In a plain Row the second child only gets what the first one left
    // over, so the distance was squeezed into a column one character wide.
    //
    // The measure is the other card's distance, which is a LONGER string: a
    // shorter line cannot honestly be taller than a longer one of the same
    // style, so if it is, it was crushed.
    @Test
    fun theDistanceOfAnUnratedProfessionalIsNotSqueezedOutOfItsRow() {
        composeRule.setContentAtDoubleFontScale {
            ProfessionalSearchContent(
                uiState =
                    ProfessionalSearchUiState(
                        origin = origin,
                        results =
                            SearchResultsState.Content(
                                professionals =
                                    listOf(
                                        nearbyProfessional(id = "rated", fullName = "Ana Pérez", distanceM = 0),
                                        nearbyProfessional(
                                            id = "unrated",
                                            fullName = "Luis Mamani",
                                            totalReviews = 0,
                                            averageRating = 0.0,
                                            distanceM = 900,
                                        ),
                                    ),
                                loadingMore = false,
                                endReached = true,
                                notice = null,
                            ),
                    ),
                onBack = {},
                actions = SearchActions(),
            )
        }
        scrollTo(5)

        // The unmerged tree: the whole card is one button, so in the merged one
        // a text answers with the bounds of its card and not its own.
        val withBadge = composeRule.onNodeWithText(string(R.string.common_distance_near), useUnmergedTree = true)
        val withLabel = composeRule.onNodeWithText(string(R.string.common_distance_m, 900), useUnmergedTree = true)
        composeRule.assertFitsTheScreen(withLabel)

        val rated = withBadge.getUnclippedBoundsInRoot()
        val unrated = withLabel.getUnclippedBoundsInRoot()
        val ratedHeight = rated.bottom - rated.top
        val unratedHeight = unrated.bottom - unrated.top

        assertTrue(
            "The shorter distance is $unratedHeight tall against $ratedHeight for a longer one: it was crushed.",
            unratedHeight <= ratedHeight,
        )
    }

    // Someone whose first visit is this screen has to be able to leave it: the
    // search has nothing to measure from until they register an address.
    @Test
    fun theBlockWithoutAnAddressKeepsItsWayOutOnScreen() {
        show(SearchResultsState.NoPrimaryAddress, withOrigin = false)
        scrollTo(1)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.search_no_address_title)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.search_no_address_action)),
        )
    }

    // Two ways out of an empty result, and both have to be reachable: the
    // filters that match nobody, and the radius that holds nobody.
    @Test
    fun anEmptyResultKeepsBothWaysOutOnScreen() {
        show(
            SearchResultsState.Empty,
            criteria = SearchCriteria(radius = SearchRadius.KM_2, serviceTypeId = venousLine.id),
        )
        scrollTo(3)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.search_clear_filters)),
        )
        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.search_widen_radius, SearchRadius.KM_5.km)),
        )
    }

    // The footer after a page that failed: the reason and the way to ask again.
    // Without both on screen the list simply stops, with no explanation and no
    // handle.
    @Test
    fun aFailedNextPageKeepsItsReasonAndItsRetryOnScreen() {
        show(
            SearchResultsState.Content(
                professionals = listOf(nearbyProfessional(id = "p0", distanceM = 1200)),
                loadingMore = false,
                endReached = false,
                notice = SearchError.NetworkUnavailable,
            ),
        )
        scrollTo(5)

        composeRule.assertFitsTheScreen(
            composeRule.onNodeWithText(string(R.string.error_network_unavailable)),
        )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.search_load_more)))
    }

    @Test
    fun aFailedSearchKeepsItsRetryOnScreen() {
        show(SearchResultsState.Failed(SearchError.NetworkUnavailable))
        scrollTo(3)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }
}
