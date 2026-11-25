package bo.saludencasa.features.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R
import bo.saludencasa.core.util.formatDate
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.subscription.domain.model.Subscription
import bo.saludencasa.ui.theme.Spacing

// What the person set up once and comes back to change: verification, services,
// availability and addresses (docs/decisions.md, 2026-10-30, HT-25). verified is
// null when it is not known, which is always the case for a patient. subscription
// is null unless the platform requires one and the professional has a period (HU-38).
@Composable
internal fun SettingsContent(
    role: UserRole,
    verified: Boolean?,
    navigation: HomeNavigation,
    modifier: Modifier = Modifier,
    subscription: Subscription? = null,
) {
    val professional = role == UserRole.PROFESSIONAL

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        HomeRowGroup {
            HomeRow(
                title = stringResource(R.string.home_settings_verification),
                details = listOf(stringResource(R.string.home_settings_verification_body)),
                icon = painterResource(R.drawable.ic_verified_user),
                tone = if (verified == false) RowTone.PENDING else RowTone.NEUTRAL,
                onClick = navigation.onOpenVerification,
                trailing =
                    when (verified) {
                        true -> {
                            { StatusPill(stringResource(R.string.home_settings_verified), RowTone.NEUTRAL) }
                        }

                        false -> {
                            { StatusPill(stringResource(R.string.home_settings_pending), RowTone.PENDING) }
                        }

                        null -> {
                            null
                        }
                    },
            )
            if (professional && subscription != null) {
                HomeRowDivider()
                // The state goes in the detail and not in a pill: at 200 % a pill
                // leaves the title too narrow for one word.
                val end = formatDate(subscription.endsAt, LocalConfiguration.current.locales[0])
                HomeRow(
                    title = stringResource(R.string.home_settings_subscription),
                    details =
                        listOf(
                            stringResource(
                                if (subscription.isValid) {
                                    R.string.home_settings_subscription_trial
                                } else {
                                    R.string.home_settings_subscription_expired
                                },
                                end,
                            ),
                        ),
                    icon = painterResource(R.drawable.ic_visibility),
                    tone = if (subscription.isValid) RowTone.NEUTRAL else RowTone.PENDING,
                    onClick = navigation.onOpenSubscription,
                )
            }
            if (professional) {
                HomeRowDivider()
                HomeRow(
                    title = stringResource(R.string.home_settings_services),
                    details = listOf(stringResource(R.string.home_settings_services_body)),
                    icon = rememberVectorPainter(Icons.AutoMirrored.Filled.List),
                    onClick = navigation.onOpenMyServices,
                )
                HomeRowDivider()
                HomeRow(
                    title = stringResource(R.string.home_settings_availability),
                    details = listOf(stringResource(R.string.home_settings_availability_body)),
                    icon = rememberVectorPainter(Icons.Filled.DateRange),
                    onClick = navigation.onOpenAvailability,
                )
            }
            HomeRowDivider()
            HomeRow(
                title = stringResource(R.string.home_settings_addresses),
                details =
                    listOf(
                        stringResource(
                            if (professional) {
                                R.string.home_settings_addresses_body_professional
                            } else {
                                R.string.home_settings_addresses_body_patient
                            },
                        ),
                    ),
                icon = rememberVectorPainter(Icons.Filled.Place),
                onClick = navigation.onOpenAddresses,
            )
        }
    }
}
