package bo.saludencasa.features.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.ageOf
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.catalog.presentation.messageRes
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.PublicProfile
import bo.saludencasa.features.reviews.domain.model.ProfessionalReview
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.presentation.messageRes
import bo.saludencasa.features.reviews.presentation.relativeAgeText
import bo.saludencasa.ui.components.AvailabilityPill
import bo.saludencasa.ui.components.BottomActionBar
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ProfileAvatar
import bo.saludencasa.ui.components.RatingBadge
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant

data class PublicProfileActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit,
    val onRetryServices: () -> Unit,
    val onRetryReviews: () -> Unit,
    val onReachedEnd: () -> Unit,
    val onSchedule: () -> Unit,
)

@Composable
fun PublicProfileScreen(
    professionalId: String,
    onBack: () -> Unit,
    onSchedule: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PublicProfileViewModel = koinViewModel { parametersOf(professionalId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PublicProfileContent(
        uiState = uiState,
        actions =
            PublicProfileActions(
                onBack = onBack,
                onRetry = viewModel::load,
                onRetryServices = viewModel::onRetryServices,
                onRetryReviews = viewModel::onRetryReviews,
                onReachedEnd = viewModel::onReachedEnd,
                onSchedule = onSchedule,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun PublicProfileContent(
    uiState: PublicProfileUiState,
    actions: PublicProfileActions,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    when (uiState) {
        PublicProfileUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        PublicProfileUiState.NotPublished -> {
            Column(modifier = modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.public_profile_title),
                    onBack = actions.onBack,
                    modifier = Modifier.padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
                )
                CenteredMessage(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.public_profile_not_published_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.public_profile_not_published_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        is PublicProfileUiState.Failed -> {
            CenteredMessage(modifier = modifier) {
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
                OutlinedButton(onClick = actions.onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        }

        is PublicProfileUiState.Content -> {
            PublishedProfile(uiState = uiState, actions = actions, modifier = modifier)
        }
    }
}

@Composable
private fun PublishedProfile(
    uiState: PublicProfileUiState.Content,
    actions: PublicProfileActions,
    modifier: Modifier = Modifier,
) {
    val profile = uiState.profile

    Column(modifier = modifier.fillMaxSize()) {
        ProfileList(uiState = uiState, profile = profile, actions = actions, modifier = Modifier.weight(1f))

        if (uiState.showsScheduleAction) {
            BottomActionBar {
                PrimaryButton(
                    text = stringResource(R.string.public_profile_schedule_action),
                    onClick = actions.onSchedule,
                )
            }
        }
    }
}

@Composable
private fun ProfileList(
    uiState: PublicProfileUiState.Content,
    profile: PublicProfile,
    actions: PublicProfileActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        item(key = "header") {
            ScreenHeader(title = stringResource(R.string.public_profile_title), onBack = actions.onBack)
        }

        item(key = "identity") { Identity(profile = profile) }

        item(key = "facts") { Facts(profile = profile) }

        profile.biography?.let { biography ->
            item(key = "biography") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                    SectionTitle(text = stringResource(R.string.public_profile_about_title))
                    Text(
                        text = biography,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item(key = "services-title") {
            SectionTitle(text = stringResource(R.string.public_profile_services_title))
        }
        servicesSection(state = uiState.services, onRetry = actions.onRetryServices)

        item(key = "reviews-title") {
            SectionTitle(text = stringResource(R.string.public_profile_reviews_title))
        }
        reviewsSection(
            state = uiState.reviews,
            onRetry = actions.onRetryReviews,
            onReachedEnd = actions.onReachedEnd,
        )
    }
}

// The badge is drawn from being in the directory at all, with no field of its
// own: professional_directory only carries approved and active professionals,
// so a published profile is a verified one (INV-07). The document behind the
// verification has no column here and cannot reach the screen (RF-04.6).
@Composable
private fun Identity(
    profile: PublicProfile,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ExtraShapes.featuredCard,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileAvatar(
                photoUrl = profile.photoUrl,
                contentDescription = stringResource(R.string.cd_professional_photo),
                name = profile.fullName,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = profile.fullName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                occupationOf(profile)?.let { occupation ->
                    Text(
                        text = occupation,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            VerifiedBadge()

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Stats(profile = profile)
        }
    }
}

@Composable
private fun occupationOf(profile: PublicProfile): String? {
    val type = profile.professionalType?.let { stringResource(it.labelRes()) }
    val specialty = profile.specialty
    return if (type != null && specialty != null) {
        stringResource(R.string.public_profile_type_and_specialty, type, specialty)
    } else {
        type ?: specialty
    }
}

@Composable
private fun VerifiedBadge(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_verified_badge)

    Row(
        modifier =
            modifier
                .semantics(mergeDescendants = true) { contentDescription = description }
                .clip(ExtraShapes.pill)
                .background(SaludEnCasaTheme.statusColors.positiveContainer)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale4),
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = SaludEnCasaTheme.statusColors.onPositiveContainer,
            modifier = Modifier.size(Spacing.scale16),
        )
        Text(
            text = stringResource(R.string.profile_verified_badge),
            style = MaterialTheme.typography.labelLarge,
            color = SaludEnCasaTheme.statusColors.onPositiveContainer,
        )
    }
}

// Stacked at a large font scale (docs/decisions.md, 2026-10-15, the search and profile screens).
@Composable
private fun Stats(
    profile: PublicProfile,
    modifier: Modifier = Modifier,
) {
    val numbers = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0])
    val years = numbers.format(profile.yearsOfExperience.toLong())
    val services = numbers.format(profile.totalServices.toLong())
    val yearsLabel = pluralStringResource(R.plurals.public_profile_stat_years, profile.yearsOfExperience)
    val servicesLabel = pluralStringResource(R.plurals.public_profile_stat_services, profile.totalServices)

    if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
        ) {
            RatingStat(profile = profile, numbers = numbers, modifier = Modifier.fillMaxWidth())
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Stat(label = yearsLabel, modifier = Modifier.fillMaxWidth()) { StatNumber(text = years) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Stat(label = servicesLabel, modifier = Modifier.fillMaxWidth()) { StatNumber(text = services) }
        }
    } else {
        Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            RatingStat(profile = profile, numbers = numbers, modifier = Modifier.weight(1f))
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Stat(label = yearsLabel, modifier = Modifier.weight(1f)) { StatNumber(text = years) }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Stat(label = servicesLabel, modifier = Modifier.weight(1f)) { StatNumber(text = services) }
        }
    }
}

// An average of zero over no ratings is not a bad professional, it is a
// professional nobody has rated. Drawing the badge anyway would read as one
// star out of five.
@Composable
private fun RatingStat(
    profile: PublicProfile,
    numbers: NumberFormat,
    modifier: Modifier = Modifier,
) {
    if (profile.totalReviews == 0) {
        Stat(label = stringResource(R.string.public_profile_no_reviews), modifier = modifier) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = EMPTY_STAR_ALPHA),
                modifier = Modifier.size(Spacing.scale24),
            )
        }
        return
    }

    Stat(
        label =
            pluralStringResource(
                R.plurals.public_profile_stat_reviews,
                profile.totalReviews,
                numbers.format(profile.totalReviews.toLong()),
            ),
        modifier = modifier,
    ) {
        RatingBadge(rating = profile.averageRating)
    }
}

// The value sits in a box of one common height, so the labels of the three
// columns start on the same line whether the value is a number or the badge.
@Composable
private fun Stat(
    label: String,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.heightIn(min = Spacing.scale32), contentAlignment = Alignment.Center) {
            value()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatNumber(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
private fun Facts(
    profile: PublicProfile,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
    ) {
        if (profile.availableNow) {
            AvailabilityPill(text = stringResource(R.string.public_profile_available_now))
        } else {
            FactPill(
                text = stringResource(R.string.public_profile_not_available_now),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FactPill(
            text =
                profile.baseRateBob
                    ?.let { stringResource(R.string.public_profile_base_rate, formatBob(it, locale)) }
                    ?: stringResource(R.string.public_profile_base_rate_unset),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun FactPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = contentColor,
        modifier =
            modifier
                .clip(ExtraShapes.pill)
                .background(containerColor)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale4),
    )
}

private fun LazyListScope.servicesSection(
    state: ServicesState,
    onRetry: () -> Unit,
) {
    when (state) {
        ServicesState.Loading -> {
            item(key = "services-loading") { CenteredProgress() }
        }

        ServicesState.Empty -> {
            item(key = "services-empty") {
                SectionMessage(text = stringResource(R.string.public_profile_services_empty))
            }
        }

        is ServicesState.Failed -> {
            item(key = "services-failed") {
                SectionFailure(message = stringResource(state.error.messageRes()), onRetry = onRetry)
            }
        }

        is ServicesState.Content -> {
            items(state.services, key = { "service-" + it.id }) { service ->
                ServiceRow(service = service)
            }
        }
    }
}

// Stacked at a large font scale (docs/decisions.md, 2026-10-15, the search and profile screens).
@Composable
private fun ServiceRow(
    service: ProfessionalService,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val price = formatBob(service.referencePriceBob, locale)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) {
            Column(
                modifier = Modifier.padding(Spacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
            ) {
                ServiceSummary(service = service)
                ServicePrice(text = price)
            }
        } else {
            Row(
                modifier = Modifier.padding(Spacing.cardPadding),
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ServiceSummary(service = service, modifier = Modifier.weight(1f))
                ServicePrice(text = price)
            }
        }
    }
}

@Composable
private fun ServiceSummary(
    service: ProfessionalService,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
        Text(
            text = service.type.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = durationText(service.type.estimatedDurationMin),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ServicePrice(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
private fun durationText(minutes: Int): String {
    val hours = minutes / MINUTES_IN_AN_HOUR
    val remainder = minutes % MINUTES_IN_AN_HOUR
    return when {
        hours == 0 -> stringResource(R.string.public_profile_duration_minutes, minutes)
        remainder == 0 -> stringResource(R.string.public_profile_duration_hours, hours)
        else -> stringResource(R.string.public_profile_duration_hours_minutes, hours, remainder)
    }
}

private fun LazyListScope.reviewsSection(
    state: ReviewsState,
    onRetry: () -> Unit,
    onReachedEnd: () -> Unit,
) {
    when (state) {
        ReviewsState.Loading -> {
            item(key = "reviews-loading") { CenteredProgress() }
        }

        ReviewsState.Empty -> {
            item(key = "reviews-empty") {
                SectionMessage(text = stringResource(R.string.public_profile_reviews_empty))
            }
        }

        is ReviewsState.Failed -> {
            item(key = "reviews-failed") {
                SectionFailure(message = stringResource(state.error.messageRes()), onRetry = onRetry)
            }
        }

        is ReviewsState.Content -> {
            items(state.reviews, key = { "review-" + it.id }) { review ->
                ReviewCard(review = review)
            }

            if (!state.endReached) {
                item(key = "reviews-footer") {
                    if (state.notice != null && !state.loadingMore) {
                        SectionFailure(
                            message = stringResource(state.notice.messageRes()),
                            onRetry = onReachedEnd,
                        )
                    } else {
                        // Asking from the footer is what pages a lazy list: the
                        // effect only runs while there is no notice to read, or
                        // a failed page would retry itself for ever.
                        LaunchedEffect(state.reviews.size) { onReachedEnd() }
                        CenteredProgress()
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: ProfessionalReview,
    modifier: Modifier = Modifier,
) {
    val now = remember { Instant.now() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ExtraShapes.featuredCard,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Spacing.scale4,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RatingStars(rating = review.rating)
                Text(
                    text = relativeAgeText(ageOf(review.createdAt, now)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = review.comment ?: stringResource(R.string.public_profile_review_without_comment),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (review.comment == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}

// The five glyphs sit on the amber fill and not on the page: amber reaches
// 1,96:1 against a light surface, so docs/design-system.md only allows it as a
// fill with dark content on top.
@Composable
private fun RatingStars(
    rating: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.cd_review_rating, rating)

    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = ExtraShapes.pill,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.scale8, vertical = Spacing.scale4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            repeat(MAX_RATING) { index ->
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint =
                        if (index < rating) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = EMPTY_STAR_ALPHA)
                        },
                    modifier = Modifier.size(Spacing.scale16),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionFailure(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.common_retry))
        }
    }
}

@Composable
private fun CenteredProgress(modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.cd_loading)

    Box(modifier = modifier.fillMaxWidth().padding(Spacing.scale16), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = loadingDescription },
        )
    }
}

@Composable
private fun CenteredMessage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

private const val MAX_RATING = 5
private const val EMPTY_STAR_ALPHA = 0.3f
private const val MINUTES_IN_AN_HOUR = 60
private const val LARGE_FONT_SCALE = 1.5f

private val previewActions =
    PublicProfileActions(
        onBack = {},
        onRetry = {},
        onRetryServices = {},
        onRetryReviews = {},
        onReachedEnd = {},
        onSchedule = {},
    )

private fun previewProfile(
    baseRateBob: BigDecimal? = BigDecimal("120.00"),
    totalReviews: Int = 12,
    availableNow: Boolean = true,
): PublicProfile =
    PublicProfile(
        professionalId = "fcab94c0-0000-4000-8000-000000000000",
        fullName = "Ana Quispe",
        photoUrl = null,
        averageRating = 4.8,
        totalReviews = totalReviews,
        totalServices = 34,
        professionalType = ProfessionalType.NURSE,
        specialty = "Enfermería geriátrica",
        biography = "Diez años atendiendo a domicilio en La Paz.",
        yearsOfExperience = 10,
        baseRateBob = baseRateBob,
        availableNow = availableNow,
    )

private fun previewService(
    id: String,
    name: String,
    price: String,
    minutes: Int,
): ProfessionalService =
    ProfessionalService(
        id = id,
        type =
            ServiceType(
                id = "type-" + id,
                name = name,
                description = null,
                referencePriceBob = BigDecimal(price),
                estimatedDurationMin = minutes,
            ),
        referencePriceBob = BigDecimal(price),
    )

private fun previewServices(): List<ProfessionalService> =
    listOf(
        previewService("1", "Colocación y control de vía venosa", "120.00", 45),
        previewService("2", "Cuidado de adulto mayor", "200.00", 240),
        previewService("3", "Curación de heridas", "100.00", 30),
    )

private fun previewReview(
    id: String,
    rating: Int,
    comment: String?,
    daysAgo: Long,
): ProfessionalReview =
    ProfessionalReview(
        id = id,
        rating = rating,
        comment = comment,
        createdAt = Instant.parse("2026-10-11T12:00:00Z").minusSeconds(daysAgo * SECONDS_IN_A_DAY),
    )

private fun previewReviews(): List<ProfessionalReview> =
    listOf(
        previewReview("1", 5, "Muy puntual y explicó todo con paciencia. Volvería a llamarla.", 12),
        previewReview("2", 4, "Buen trato, llegó un poco tarde.", 40),
        previewReview("3", 5, null, 90),
    )

private fun previewContent(
    services: ServicesState = ServicesState.Content(previewServices()),
    reviews: ReviewsState =
        ReviewsState.Content(previewReviews(), loadingMore = false, endReached = true, notice = null),
    profile: PublicProfile = previewProfile(),
    canSchedule: Boolean = true,
): PublicProfileUiState.Content = PublicProfileUiState.Content(profile, services, reviews, canSchedule)

@Preview(showBackground = true, heightDp = 1500, name = "Perfil publico, claro")
@Composable
private fun PublicProfileLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PublicProfileContent(previewContent(), previewActions)
    }
}

@Preview(showBackground = true, heightDp = 1500, name = "Perfil publico, oscuro")
@Composable
private fun PublicProfileDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        PublicProfileContent(previewContent(), previewActions)
    }
}

@Preview(
    showBackground = true,
    heightDp = 2600,
    widthDp = 320,
    fontScale = 2f,
    name = "Perfil publico al 200 por ciento, LargeFont",
)
@Composable
private fun PublicProfileLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PublicProfileContent(previewContent(), previewActions)
    }
}

@Preview(showBackground = true, heightDp = 1100, name = "Perfil publico sin servicios ni comentarios")
@Composable
private fun PublicProfileEmptySectionsPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PublicProfileContent(
            previewContent(
                services = ServicesState.Empty,
                reviews = ReviewsState.Empty,
                profile = previewProfile(baseRateBob = null, totalReviews = 0, availableNow = false),
            ),
            previewActions,
        )
    }
}

@Preview(showBackground = true, heightDp = 1500, name = "Perfil publico con la pagina de comentarios en error")
@Composable
private fun PublicProfileReviewsNoticePreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PublicProfileContent(
            previewContent(
                reviews =
                    ReviewsState.Content(
                        previewReviews(),
                        loadingMore = false,
                        endReached = false,
                        notice = ReviewError.NetworkUnavailable,
                    ),
            ),
            previewActions,
        )
    }
}

@Preview(showBackground = true, heightDp = 1200, name = "Perfil publico con los servicios en error")
@Composable
private fun PublicProfileServicesFailedPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        PublicProfileContent(
            previewContent(
                services = ServicesState.Failed(CatalogError.NetworkUnavailable),
                reviews = ReviewsState.Loading,
            ),
            previewActions,
        )
    }
}

@Preview(showBackground = true, heightDp = 1100, name = "Perfil publico con los comentarios en error")
@Composable
private fun PublicProfileReviewsFailedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PublicProfileContent(
            previewContent(reviews = ReviewsState.Failed(ReviewError.NetworkUnavailable)),
            previewActions,
        )
    }
}

@Preview(showBackground = true, name = "Perfil publico no publicado")
@Composable
private fun PublicProfileNotPublishedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PublicProfileContent(PublicProfileUiState.NotPublished, previewActions)
    }
}

@Preview(showBackground = true, name = "Perfil publico cargando")
@Composable
private fun PublicProfileLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        PublicProfileContent(PublicProfileUiState.Loading, previewActions)
    }
}

@Preview(showBackground = true, name = "Perfil publico que no se pudo cargar")
@Composable
private fun PublicProfileFailedPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        PublicProfileContent(PublicProfileUiState.Failed(ProfileError.NetworkUnavailable), previewActions)
    }
}

private const val SECONDS_IN_A_DAY = 86_400L
