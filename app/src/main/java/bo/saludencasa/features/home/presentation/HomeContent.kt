package bo.saludencasa.features.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import bo.saludencasa.R
import bo.saludencasa.core.util.formatAppointment
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.presentation.messageRes
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.presentation.inboxDistanceText
import bo.saludencasa.features.requests.presentation.labelRes
import bo.saludencasa.features.requests.presentation.messageRes
import bo.saludencasa.ui.animations.FloatingCircles
import bo.saludencasa.ui.animations.RadarPulse
import bo.saludencasa.ui.animations.StaggeredEntrance
import bo.saludencasa.ui.components.AvailabilityDot
import bo.saludencasa.ui.theme.Spacing

// The same threshold the welcome screen and the public profile use to move a
// figure under its label instead of beside it.
private const val LARGE_FONT_SCALE = 1.5f
private const val LEADING_TINT_ALPHA = 0.16f
private const val HALO_ALPHA = 0.18f

internal fun firstNameOf(fullName: String): String? = fullName.trim().split(" ").firstOrNull { it.isNotBlank() }

@Composable
internal fun HomeGreeting(
    firstName: String?,
    roleLabel: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
        // The role sits on the line of the greeting, under the avatar that
        // changes it (docs/decisions.md, 2026-10-30, HT-25).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(if (firstName != null) R.string.home_greeting else R.string.home_greeting_alone),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            roleLabel?.let { StatusPill(text = it, tone = RowTone.NEUTRAL) }
        }
        firstName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
internal fun PatientHome(
    uiState: PatientHomeUiState,
    greeting: @Composable () -> Unit,
    onOpenImmediateCare: () -> Unit,
    onOpenScheduling: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeColumn(modifier = modifier) {
        StaggeredEntrance(index = 0, modifier = Modifier.fillMaxWidth()) { greeting() }
        StaggeredEntrance(index = 1) { SectionTitle(text = stringResource(R.string.home_what_do_you_need)) }
        StaggeredEntrance(index = 2, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap)) {
                ActionRow(
                    title = stringResource(R.string.home_immediate_care_title),
                    body = stringResource(R.string.home_immediate_care_body),
                    emphasized = true,
                    onClick = onOpenImmediateCare,
                ) {
                    // The rings alone fade to nothing at rest; the dot keeps the
                    // figure readable as the patient's point.
                    RadarPulse(color = LocalContentColor.current, modifier = Modifier.size(Spacing.scale40))
                    Box(
                        modifier =
                            Modifier
                                .size(Spacing.scale8)
                                .clip(CircleShape)
                                .background(LocalContentColor.current),
                    )
                }
                ActionRow(
                    title = stringResource(R.string.home_schedule_title),
                    body = stringResource(R.string.home_schedule_body),
                    emphasized = false,
                    onClick = onOpenScheduling,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.scale24),
                    )
                }
            }
        }
        if (uiState is PatientHomeUiState.InProgress) {
            StaggeredEntrance(index = 3) { SectionTitle(text = stringResource(R.string.home_in_progress_title)) }
            StaggeredEntrance(index = 4, modifier = Modifier.fillMaxWidth()) {
                HomeRowGroup {
                    HomeRow(
                        title = uiState.request.serviceTypeName,
                        details = listOf(stringResource(uiState.request.status.labelRes())),
                        icon = rememberVectorPainter(Icons.Filled.Notifications),
                        tone = RowTone.PENDING,
                        onClick = onOpenImmediateCare,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProfessionalHome(
    uiState: ProfessionalHomeUiState,
    greeting: @Composable () -> Unit,
    onAvailabilityChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onOpenVerification: () -> Unit,
    onOpenRequest: (String) -> Unit,
    onSeeAllRequests: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeColumn(modifier = modifier) {
        StaggeredEntrance(index = 0, modifier = Modifier.fillMaxWidth()) { greeting() }
        when (uiState) {
            ProfessionalHomeUiState.Loading -> {
                CenteredProgress(modifier = Modifier.fillMaxWidth())
            }

            is ProfessionalHomeUiState.Failed -> {
                Failure(message = stringResource(uiState.error.messageRes()), onRetry = onRetry)
            }

            is ProfessionalHomeUiState.Content -> {
                StaggeredEntrance(index = 1, modifier = Modifier.fillMaxWidth()) {
                    AvailabilityRow(
                        availableNow = uiState.availableNow,
                        error = uiState.availabilityError,
                        onChange = onAvailabilityChange,
                    )
                }
                if (!uiState.verified) {
                    StaggeredEntrance(index = 2, modifier = Modifier.fillMaxWidth()) {
                        HomeRowGroup {
                            HomeRow(
                                title = stringResource(R.string.home_verification_pending_title),
                                details = listOf(stringResource(R.string.home_verification_pending_body)),
                                icon = painterResource(R.drawable.ic_verified_user),
                                tone = RowTone.PENDING,
                                onClick = onOpenVerification,
                            )
                        }
                    }
                }
                StaggeredEntrance(index = 3) {
                    SectionTitle(text = stringResource(R.string.home_nearby_requests_title))
                }
                StaggeredEntrance(index = 4, modifier = Modifier.fillMaxWidth()) {
                    NearbyRequestsBlock(
                        nearby = uiState.nearby,
                        onOpenRequest = onOpenRequest,
                        onSeeAll = onSeeAllRequests,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AdminHome(
    greeting: @Composable () -> Unit,
    onOpenDocumentReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeColumn(modifier = modifier) {
        StaggeredEntrance(index = 0, modifier = Modifier.fillMaxWidth()) { greeting() }
        StaggeredEntrance(index = 1) { SectionTitle(text = stringResource(R.string.home_review_section_title)) }
        StaggeredEntrance(index = 2, modifier = Modifier.fillMaxWidth()) {
            HomeRowGroup {
                HomeRow(
                    title = stringResource(R.string.home_review_documents_title),
                    details = listOf(stringResource(R.string.home_review_documents_body)),
                    icon = painterResource(R.drawable.ic_verified_user),
                    onClick = onOpenDocumentReview,
                )
            }
        }
    }
}

@Composable
private fun HomeColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        FloatingCircles(modifier = Modifier.matchParentSize())
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
            content = content,
        )
    }
}

// The whole row is the button, and its title says exactly where it goes.
@Composable
private fun ActionRow(
    title: String,
    body: String,
    emphasized: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    val container = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val leadingContainer =
        if (emphasized) content.copy(alpha = LEADING_TINT_ALPHA) else MaterialTheme.colorScheme.primaryContainer
    val leadingContent = if (emphasized) content else MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(Spacing.scale48),
                shape = MaterialTheme.shapes.medium,
                color = leadingContainer,
                contentColor = leadingContent,
            ) {
                Box(contentAlignment = Alignment.Center) { leading() }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = body, style = MaterialTheme.typography.labelMedium)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(Spacing.scale20),
            )
        }
    }
}

// docs/decisions.md, 2026-10-30, HT-25: discreet and in the theme's teal, not
// the green the status palette keeps for what is approved.
@Composable
private fun AvailabilityRow(
    availableNow: Boolean,
    error: ProfileError?,
    onChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = availableNow, role = Role.Switch, onValueChange = onChange)
                        .heightIn(min = Spacing.minTouchTarget)
                        .padding(horizontal = Spacing.cardPadding, vertical = Spacing.scale8),
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(Spacing.scale16)
                            .clip(CircleShape)
                            .background(
                                if (availableNow) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = HALO_ALPHA)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (availableNow) {
                        AvailabilityDot(color = MaterialTheme.colorScheme.primary)
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .size(Spacing.scale8)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.home_available_now),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text =
                        stringResource(
                            if (availableNow) R.string.home_available_now_on else R.string.home_available_now_off,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        if (availableNow) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                Switch(checked = availableNow, onCheckedChange = null)
            }
            error?.let {
                Text(
                    text = stringResource(it.messageRes()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier =
                        Modifier.padding(
                            start = Spacing.cardPadding,
                            end = Spacing.cardPadding,
                            bottom = Spacing.scale8,
                        ),
                )
            }
        }
    }
}

@Composable
private fun NearbyRequestsBlock(
    nearby: NearbyRequests,
    onOpenRequest: (String) -> Unit,
    onSeeAll: () -> Unit,
    onRetry: () -> Unit,
) {
    when (nearby) {
        NearbyRequests.None -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.sectionGap),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
                ) {
                    Text(
                        text = stringResource(R.string.home_nearby_requests_empty_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.home_nearby_requests_empty_body),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        is NearbyRequests.Failed -> {
            Failure(message = stringResource(nearby.error.messageRes()), onRetry = onRetry)
        }

        is NearbyRequests.Some -> {
            HomeRowGroup {
                nearby.requests.forEach { request ->
                    NearbyRequestRow(request = request, onClick = { onOpenRequest(request.id) })
                    HomeRowDivider()
                }
                SeeAllRow(onClick = onSeeAll)
            }
        }
    }
}

@Composable
private fun NearbyRequestRow(
    request: InboxRequest,
    onClick: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val place = stringResource(R.string.home_nearby_request_place, request.city, inboxDistanceText(request.distanceM))
    val schedule =
        request.scheduledAt?.let { stringResource(R.string.request_scheduled_for, formatAppointment(it, locale)) }
    val budget = request.suggestedBudgetBob?.let { formatBob(it, locale) }
    // At a large font the amount moves under the title, or the title keeps a
    // sliver of the row to wrap in.
    val stacked = LocalDensity.current.fontScale >= LARGE_FONT_SCALE

    HomeRow(
        title = request.serviceTypeName,
        details = listOfNotNull(place, schedule, budget?.takeIf { stacked }),
        icon =
            rememberVectorPainter(
                if (request.modality == RequestModality.SCHEDULED) Icons.Filled.DateRange else Icons.Filled.LocationOn,
            ),
        onClick = onClick,
        showChevron = false,
        trailing =
            if (budget != null && !stacked) {
                {
                    Text(
                        text = budget,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                null
            },
    )
}

@Composable
private fun SeeAllRow(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .heightIn(min = Spacing.minTouchTarget)
                .padding(horizontal = Spacing.cardPadding, vertical = Spacing.scale12),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale4, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_nearby_requests_see_all),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Spacing.scale20),
        )
    }
}

@Composable
private fun Failure(
    message: String,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.common_retry))
        }
    }
}

@Composable
internal fun CenteredProgress(modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.cd_loading)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
    }
}
