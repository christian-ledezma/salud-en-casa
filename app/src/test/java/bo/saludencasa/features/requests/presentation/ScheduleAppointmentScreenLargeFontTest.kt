package bo.saludencasa.features.requests.presentation

import android.content.Context
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.core.util.formatFullDate
import bo.saludencasa.core.util.formatTime
import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.PublicProfile
import bo.saludencasa.features.requests.bookableStart
import bo.saludencasa.features.requests.domain.model.BookableStart
import bo.saludencasa.features.requests.domain.model.BookableTimes
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.ui.assertFitsTheScreen
import bo.saludencasa.ui.assertFitsTheScreenWithoutScrolling
import bo.saludencasa.ui.setContentAtDoubleFontScale
import bo.saludencasa.ui.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ScheduleAppointmentScreenLargeFontTest {
    @get:Rule
    val composeRule = createComposeRule()

    // The locale the screen formats with, so the expected labels match whatever
    // Robolectric configures.
    private val locale: Locale by lazy {
        ApplicationProvider
            .getApplicationContext<Context>()
            .resources.configuration.locales[0]
    }

    private val monday = LocalDate.of(2026, 10, 19)
    private val tuesday = monday.plusDays(1)
    private val saturday = monday.minusDays(2)

    // The longest of the twelve catalog entries.
    private val venousLine =
        ProfessionalService(
            id = "d1",
            type = ServiceType("t1", "Colocación y control de vía venosa", null, BigDecimal("120.00"), 60),
            referencePriceBob = BigDecimal("120.00"),
        )

    private val home =
        Address(
            id = "a1",
            alias = "Mi casa",
            addressText = "Avenida América 1234, Cochabamba",
            reference = null,
            city = "Cochabamba",
            coordinate = Coordinate.create(-17.3835, -66.1568).getOrThrow(),
            isPrimary = true,
            isProfessionalBase = false,
        )

    private val professional =
        PublicProfile(
            professionalId = "fcab94c0-0000-4000-8000-000000000000",
            fullName = "Ana Quispe Mamani",
            photoUrl = null,
            averageRating = 4.8,
            totalReviews = 12,
            totalServices = 34,
            professionalType = ProfessionalType.NURSE,
            specialty = null,
            biography = null,
            yearsOfExperience = 10,
            baseRateBob = BigDecimal("120.00"),
            availableNow = true,
        )

    // Friday then a weekend without times, then Monday and Tuesday.
    private val times =
        BookableTimes(
            listOf(
                bookableStart(saturday.minusDays(1), 15),
                bookableStart(monday, 9),
                bookableStart(monday, 9, 30),
                bookableStart(monday, 14),
                bookableStart(monday, 19),
                bookableStart(tuesday, 10),
            ),
        )

    private fun show(
        selectedStart: BookableStart? = null,
        submitError: RequestError? = null,
        timesState: TimesState = TimesState.Loaded(times),
    ) {
        composeRule.setContentAtDoubleFontScale {
            ScheduleAppointmentContent(
                uiState =
                    ScheduleAppointmentUiState.Content(
                        professional = professional,
                        services = listOf(venousLine),
                        addresses = listOf(home),
                        selectedServiceTypeId = venousLine.type.id,
                        times = timesState,
                        selectedDate = monday.takeIf { timesState is TimesState.Loaded },
                        selectedStart = selectedStart,
                        selectedAddressId = home.id,
                        description = "",
                        budget = "",
                        submitting = false,
                        submitError = submitError,
                    ),
                onBack = {},
                actions = ScheduleAppointmentActions(),
            )
        }
    }

    // performScrollTo moves only the closest scrolling parent, which for a day
    // or an hour is its carousel and not the form around it. The carousel is
    // brought into the form's view first, as the patient's thumb would.
    private fun bringIntoView(carousel: Int) {
        composeRule.onAllNodes(hasScrollToIndexAction())[carousel].performScrollTo()
    }

    @Test
    fun theWayBackStaysOnScreen() {
        show()

        composeRule.assertFitsTheScreen(composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)))
    }

    @Test
    fun theLongestServiceOptionStaysOnScreen() {
        show()

        val label =
            string(
                R.string.schedule_service_option,
                venousLine.type.name,
                formatBob(venousLine.referencePriceBob, locale),
            )
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(label))
    }

    @Test
    fun theChosenDayStaysOnScreenAndIsMarkedChosen() {
        show()
        bringIntoView(DAYS)

        val day = composeRule.onNodeWithContentDescription(formatFullDate(monday, locale))
        composeRule.assertFitsTheScreen(day)
        day.assertIsSelected()
    }

    // A dimmed day that still answered a tap would select a day with no hours
    // and leave the patient in front of an empty row.
    @Test
    fun aDayWithoutTimesDoesNotRespond() {
        show()

        composeRule
            .onNodeWithContentDescription(formatFullDate(saturday, locale))
            .assertIsNotEnabled()
    }

    // The last day sits past the fold of the carousel, where a lazy row has
    // not composed it yet; it is reached by scrolling the row, which is what
    // the patient does.
    @Test
    fun theLastDayIsReachedByScrollingTheCarousel() {
        show()
        bringIntoView(DAYS)

        composeRule.onAllNodes(hasScrollToIndexAction())[DAYS].performScrollToIndex(times.days.lastIndex)
        composeRule.assertFitsTheScreen(composeRule.onNodeWithContentDescription(formatFullDate(tuesday, locale)))
    }

    @Test
    fun theFirstHourAndItsPartOfTheDayStayOnScreen() {
        show()
        bringIntoView(HOURS)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_part_morning)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(formatTime(LocalTime.of(9, 0), locale)))
    }

    @Test
    fun theAddressAndTheTwoFieldsStayOnScreen() {
        show()

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(home.alias))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.request_create_description_label)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.request_create_budget_label)))
    }

    // The button is fixed to the foot, outside the scrolling form: it has to be
    // on screen with nothing scrolled, with the whole appointment in its label.
    @Test
    fun theButtonWithTheAppointmentStaysOnScreenWithoutScrolling() {
        val start = bookableStart(monday, 9, 30)
        show(selectedStart = start)

        val label = string(R.string.schedule_submit_at, formatAppointment(start.date, start.time, locale))
        composeRule.assertFitsTheScreenWithoutScrolling(composeRule.onNodeWithText(label))
    }

    @Test
    fun aRefusalIsSaidNextToTheButton() {
        show(submitError = RequestError.TimeNotBookable)

        composeRule.assertFitsTheScreenWithoutScrolling(
            composeRule.onNodeWithText(string(R.string.error_request_time_not_bookable)),
        )
    }

    @Test
    fun aProfessionalWhoCannotBeBookedLeavesAWayBack() {
        composeRule.setContentAtDoubleFontScale {
            ScheduleAppointmentContent(
                ScheduleAppointmentUiState.NotBookable,
                onBack = {},
                actions = ScheduleAppointmentActions(),
            )
        }

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.schedule_not_bookable_title)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithContentDescription(string(R.string.cd_back_button)))
    }

    @Test
    fun timesThatCouldNotBeReadLeaveTheirRetryOnScreen() {
        show(timesState = TimesState.Failed(RequestError.NetworkUnavailable))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.error_network_unavailable)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }

    @Test
    fun twoWeeksWithoutTimesSaySoOnScreen() {
        show(timesState = TimesState.Loaded(BookableTimes(emptyList())))

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.schedule_no_times_title)))
        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.schedule_no_times_description)))
    }

    @Test
    fun aDescriptionTooLongIsSaidUnderItsField() {
        show(submitError = RequestError.InvalidDescription)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.error_request_invalid_description)))
    }

    @Test
    fun anInvalidBudgetIsSaidUnderItsField() {
        show(submitError = RequestError.InvalidBudget)

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.error_request_invalid_budget)))
    }

    @Test
    fun aFailedLoadLeavesItsRetryOnScreen() {
        composeRule.setContentAtDoubleFontScale {
            ScheduleAppointmentContent(
                ScheduleAppointmentUiState.Failed(RequestError.NetworkUnavailable),
                onBack = {},
                actions = ScheduleAppointmentActions(),
            )
        }

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.common_retry)))
    }

    @Test
    fun withoutAnAddressTheWayToRegisterOneStaysOnScreen() {
        composeRule.setContentAtDoubleFontScale {
            ScheduleAppointmentContent(
                ScheduleAppointmentUiState.NoAddress,
                onBack = {},
                actions = ScheduleAppointmentActions(),
            )
        }

        composeRule.assertFitsTheScreen(composeRule.onNodeWithText(string(R.string.request_create_no_address_action)))
    }
}

private const val DAYS = 0
private const val HOURS = 1
