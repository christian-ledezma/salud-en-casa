package bo.saludencasa.features.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import java.time.Instant

data class PublicProfileActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit,
    val onRetryServices: () -> Unit,
    val onRetryReviews: () -> Unit,
    val onReachedEnd: () -> Unit,
)

@Composable
fun PublicProfileScreen(
    professionalId: String,
    onBack: () -> Unit,
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
                Text(
                    text = biography,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                )
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileAvatar(
            photoUrl = profile.photoUrl,
            contentDescription = stringResource(R.string.cd_professional_photo),
        )

        Text(
            text = profile.fullName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        VerifiedBadge()

        profile.professionalType?.let { type ->
            Text(
                text = stringResource(type.labelRes()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        profile.specialty?.let { specialty ->
            Text(
                text = specialty,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Reputation(rating = profile.averageRating, totalReviews = profile.totalReviews)
    }
}

@Composable
private fun VerifiedBadge(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_verified_badge)

    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale4),
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = SaludEnCasaTheme.statusColors.positive,
            modifier = Modifier.size(Spacing.scale16),
        )
        Text(
            text = stringResource(R.string.profile_verified_badge),
            style = MaterialTheme.typography.labelLarge,
            color = SaludEnCasaTheme.statusColors.positive,
        )
    }
}

@Composable
private fun Facts(
    profile: PublicProfile,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text =
                pluralStringResource(
                    R.plurals.public_profile_years_of_experience,
                    profile.yearsOfExperience,
                    profile.yearsOfExperience,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text =
                profile.baseRateBob
                    ?.let { stringResource(R.string.public_profile_base_rate, formatBob(it, locale)) }
                    ?: stringResource(R.string.public_profile_base_rate_unset),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text =
                stringResource(
                    if (profile.availableNow) {
                        R.string.public_profile_available_now
                    } else {
                        R.string.public_profile_not_available_now
                    },
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text =
                pluralStringResource(
                    R.plurals.public_profile_total_services,
                    profile.totalServices,
                    profile.totalServices,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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

@Composable
private fun ServiceRow(
    service: ProfessionalService,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
        Text(
            text = service.type.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = formatBob(service.referencePriceBob, locale),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text =
                pluralStringResource(
                    R.plurals.catalog_estimated_duration,
                    service.type.estimatedDurationMin,
                    service.type.estimatedDurationMin,
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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

// An average of zero over no ratings is not a bad professional, it is a
// professional nobody has rated. Drawing the badge anyway would read as one
// star out of five.
@Composable
private fun Reputation(
    rating: Double,
    totalReviews: Int,
    modifier: Modifier = Modifier,
) {
    if (totalReviews == 0) {
        Text(
            text = stringResource(R.string.public_profile_no_reviews),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
    ) {
        RatingBadge(rating = rating)
        Text(
            text = pluralStringResource(R.plurals.public_profile_total_reviews, totalReviews, totalReviews),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private val previewActions =
    PublicProfileActions(
        onBack = {},
        onRetry = {},
        onRetryServices = {},
        onRetryReviews = {},
        onReachedEnd = {},
    )

private fun previewProfile(
    baseRateBob: BigDecimal? = BigDecimal("120.00"),
    totalReviews: Int = 12,
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
        availableNow = true,
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
): PublicProfileUiState.Content = PublicProfileUiState.Content(profile, services, reviews)

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
                profile = previewProfile(baseRateBob = null, totalReviews = 0),
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
