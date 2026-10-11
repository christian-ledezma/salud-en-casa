package bo.saludencasa.features.search.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.presentation.labelRes
import bo.saludencasa.features.search.domain.model.NearbyProfessional
import bo.saludencasa.features.search.domain.model.SearchCriteria
import bo.saludencasa.features.search.domain.model.SearchError
import bo.saludencasa.features.search.domain.model.SearchOrigin
import bo.saludencasa.features.search.domain.model.SearchRadius
import bo.saludencasa.ui.animations.RadarPulse
import bo.saludencasa.ui.components.AvailabilityDot
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ProfessionalCard
import bo.saludencasa.ui.components.RadioOptionGroup
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.components.SegmentedControl
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal

@Composable
fun ProfessionalSearchScreen(
    onBack: () -> Unit,
    onOpenProfessional: (String) -> Unit,
    onRegisterAddress: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfessionalSearchViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // The origin is edited on another screen, so the search is resolved again on
    // every resume: coming back from registering an address has to search, and
    // coming back from a profile must not blank the list that was there.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    ProfessionalSearchContent(
        uiState = uiState,
        onBack = onBack,
        actions =
            SearchActions(
                onModeChange = viewModel::onModeChange,
                onRadiusChange = viewModel::onRadiusChange,
                onServiceTypeChange = viewModel::onServiceTypeChange,
                onTypeFilterToggle = viewModel::onTypeFilterToggle,
                onAvailabilityChange = viewModel::onAvailabilityChange,
                onClearFilters = viewModel::onClearFilters,
                onWidenRadius = viewModel::onWidenRadius,
                onProfessionalSelected = viewModel::onProfessionalSelected,
                onOpenProfessional = onOpenProfessional,
                onRegisterAddress = onRegisterAddress,
                onRetry = viewModel::onRetry,
                onReachedEnd = viewModel::onReachedEnd,
            ),
        map = { model, mapModifier ->
            ProfessionalSearchMap(
                model = model,
                onProfessionalClick = viewModel::onProfessionalSelected,
                modifier = mapModifier,
            )
        },
        modifier = modifier,
    )
}

internal data class SearchActions(
    val onModeChange: (ResultsMode) -> Unit = {},
    val onRadiusChange: (SearchRadius) -> Unit = {},
    val onServiceTypeChange: (String?) -> Unit = {},
    val onTypeFilterToggle: () -> Unit = {},
    val onAvailabilityChange: (Boolean) -> Unit = {},
    val onClearFilters: () -> Unit = {},
    val onWidenRadius: () -> Unit = {},
    val onProfessionalSelected: (String?) -> Unit = {},
    val onOpenProfessional: (String) -> Unit = {},
    val onRegisterAddress: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onReachedEnd: () -> Unit = {},
)

@Composable
internal fun ProfessionalSearchContent(
    uiState: ProfessionalSearchUiState,
    onBack: () -> Unit,
    actions: SearchActions,
    map: @Composable (SearchMapModel, Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val origin = uiState.origin
    val results = uiState.results

    // docs/decisions.md, 2026-10-09, the map is a card of the page and not the
    // background of the screen.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
    ) {
        item(key = "header") {
            ScreenHeader(title = stringResource(R.string.search_title), onBack = onBack)
        }

        origin?.let {
            item(key = "origin") {
                OriginBadge(alias = it.alias, addressText = it.addressText)
            }
        }

        if (results !is SearchResultsState.NoPrimaryAddress) {
            item(key = "mode") { ModeToggle(mode = uiState.mode, onModeChange = actions.onModeChange) }
            item(key = "criteria") { SearchCriteriaPanel(uiState = uiState, actions = actions) }
        }

        if (uiState.mode == ResultsMode.MAP && origin != null) {
            item(key = "map") {
                map(
                    SearchMapModel(
                        origin = origin.coordinate,
                        radius = uiState.criteria.radius,
                        professionals = results.professionals(),
                        searching = results is SearchResultsState.Searching,
                        selectedId = uiState.selectedId,
                    ),
                    Modifier
                        .fillMaxWidth()
                        .height(Spacing.mapHeight)
                        .clip(MaterialTheme.shapes.large)
                        .animateItem(),
                )
            }
        }

        when (results) {
            SearchResultsState.NoPrimaryAddress -> {
                item(key = "no-address") { NoAddressBlock(onRegisterAddress = actions.onRegisterAddress) }
            }

            SearchResultsState.Searching -> {
                item(key = "searching") { SearchingBlock() }
            }

            SearchResultsState.Empty -> {
                item(key = "empty") { EmptyBlock(uiState = uiState, actions = actions) }
            }

            is SearchResultsState.Failed -> {
                item(key = "failed") {
                    FailedBlock(messageRes = results.error.messageRes(), onRetry = actions.onRetry)
                }
            }

            is SearchResultsState.Content -> {
                item(key = "count") { ResultCountLine(count = results.professionals.size) }

                if (uiState.mode == ResultsMode.MAP) {
                    // Over the map the results are the pins, and a tap on one
                    // brings up its card here.
                    uiState.selected(results)?.let { selected ->
                        item(key = "selected") {
                            ResultCard(
                                professional = selected,
                                onClick = { actions.onOpenProfessional(selected.professionalId) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                } else {
                    resultRows(results = results, actions = actions)
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(
    mode: ResultsMode,
    onModeChange: (ResultsMode) -> Unit,
) {
    // The labels are read before the control, because its optionLabel is a
    // plain lambda and not a composable one.
    val labels = ResultsMode.entries.associateWith { stringResource(it.labelRes()) }

    SegmentedControl(
        options = ResultsMode.entries,
        selected = mode,
        onSelectedChange = onModeChange,
        optionLabel = { labels.getValue(it) },
    )
}

// The floating badge of the welcome header, reused: it is where the screen says
// what "near" is measured from, which is the one thing a distance means nothing
// without.
@Composable
private fun OriginBadge(
    alias: String,
    addressText: String,
) {
    Row(
        modifier =
            Modifier
                .clip(ExtraShapes.featuredCard)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp),
        )
        Column {
            Text(
                text = stringResource(R.string.search_origin, alias),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = addressText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SearchCriteriaPanel(
    uiState: ProfessionalSearchUiState,
    actions: SearchActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
        if (uiState.serviceTypes.isNotEmpty()) {
            ServiceTypeFilter(uiState = uiState, actions = actions)
        }

        CriteriaLabel(text = stringResource(R.string.search_radius_label))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            SearchRadius.entries.forEach { radius ->
                FilterChip(
                    selected = uiState.criteria.radius == radius,
                    onClick = { actions.onRadiusChange(radius) },
                    label = { Text(text = stringResource(R.string.search_radius_option, radius.km)) },
                )
            }
            FilterChip(
                selected = uiState.criteria.availableNowOnly,
                onClick = { actions.onAvailabilityChange(!uiState.criteria.availableNowOnly) },
                label = { Text(text = stringResource(R.string.search_available_now_filter)) },
            )
        }
    }
}

// docs/decisions.md, 2026-10-09, a Material chip cannot carry the name of a
// service type.
@Composable
private fun ServiceTypeFilter(
    uiState: ProfessionalSearchUiState,
    actions: SearchActions,
) {
    val all = stringResource(R.string.search_type_all)
    val selected = uiState.serviceTypes.firstOrNull { it.id == uiState.criteria.serviceTypeId }
    val options: List<ServiceType?> = listOf(null) + uiState.serviceTypes
    val labels = options.associateWith { type -> type?.name ?: all }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.minTouchTarget)
                    .clickable(onClick = actions.onTypeFilterToggle)
                    .padding(vertical = Spacing.scale4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                CriteriaLabel(text = stringResource(R.string.search_type_filter_label))
                Text(
                    text = selected?.name ?: all,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            // One icon turned over rather than two: the core icon set is the
            // only one this project ships, because RNF-03 caps the package at
            // 25 MB, and it has no arrow pointing up.
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = stringResource(R.string.cd_open_type_filter),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (uiState.typeFilterExpanded) 180f else 0f),
            )
        }

        if (uiState.typeFilterExpanded) {
            RadioOptionGroup(
                label = stringResource(R.string.search_type_filter_label),
                options = options,
                selected = selected,
                onSelectedChange = { type -> actions.onServiceTypeChange(type?.id) },
                optionLabel = { type -> labels.getValue(type) },
            )
        }
    }
}

@Composable
private fun CriteriaLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun LazyListScope.resultRows(
    results: SearchResultsState.Content,
    actions: SearchActions,
) {
    items(results.professionals, key = NearbyProfessional::professionalId) { professional ->
        ResultCard(
            professional = professional,
            onClick = { actions.onOpenProfessional(professional.professionalId) },
            modifier = Modifier.animateItem(),
        )
    }

    if (!results.endReached) {
        item(key = "footer") {
            if (results.notice != null && !results.loadingMore) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                    Text(
                        text = stringResource(results.notice.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = actions.onReachedEnd) {
                        Text(text = stringResource(R.string.search_load_more))
                    }
                }
            } else {
                // Asking for the page from the footer, and only while there is
                // no notice to read: otherwise the retry would fire again the
                // moment it failed.
                LaunchedEffect(results.professionals.size) { actions.onReachedEnd() }
                CenteredProgress()
            }
        }
    }
}

@Composable
private fun ResultCard(
    professional: NearbyProfessional,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val supporting =
        professional.specialty
            ?: professional.professionalType?.let { stringResource(it.labelRes()) }
            ?: ""

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
        ProfessionalCard(
            name = professional.fullName,
            specialty = supporting,
            // The badge only when votes hold it up: an average of zero and no
            // average at all are the same number (docs/decisions.md, 2026-10-09).
            rating = professional.averageRating.takeIf { professional.totalReviews > 0 },
            noRatingLabel = stringResource(R.string.search_no_rating_yet),
            distanceText = distanceText(professional.distanceM),
            rateText = stringResource(R.string.public_profile_base_rate, formatBob(professional.baseRateBob, locale)),
            photoUrl = professional.photoUrl,
            actionIcon = Icons.AutoMirrored.Filled.ArrowForward,
            actionContentDescription =
                stringResource(R.string.cd_open_professional_profile, professional.fullName),
            onActionClick = onClick,
        )

        if (professional.availableNow) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvailabilityDot()
                Text(
                    text = stringResource(R.string.search_available_now_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = SaludEnCasaTheme.statusColors.availableNow,
                )
            }
        }
    }
}

// RF-06.5 made visible without a badge per professional: nothing that is not
// approved and active can reach this list, so the count is the place to say it.
@Composable
private fun ResultCountLine(count: Int) {
    Text(
        text = pluralStringResource(R.plurals.search_results_count, count, count),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SearchingBlock() {
    val loadingDescription = stringResource(R.string.cd_loading)

    Row(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = loadingDescription },
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadarPulse(modifier = Modifier.size(Spacing.scale40))
        Text(
            text = stringResource(R.string.search_searching),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Two ways out, and they are not the same: a radius with nobody in it widens,
// and filters that match nobody clear. With ten kilometres and no filters there
// is nothing to offer and the screen says so plainly.
@Composable
private fun EmptyBlock(
    uiState: ProfessionalSearchUiState,
    actions: SearchActions,
) {
    val narrowed = uiState.criteria.isNarrowed
    val canWiden = uiState.criteria.radius != SearchRadius.entries.last()

    Text(
        text =
            stringResource(
                if (narrowed) R.string.search_empty_narrowed_title else R.string.search_empty_title,
            ),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text =
            stringResource(
                if (narrowed) {
                    R.string.search_empty_narrowed_description
                } else {
                    R.string.search_empty_description
                },
            ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (narrowed) {
        OutlinedButton(onClick = actions.onClearFilters, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.search_clear_filters))
        }
    }
    if (canWiden) {
        val wider = SearchRadius.entries.first { it.km > uiState.criteria.radius.km }
        OutlinedButton(onClick = actions.onWidenRadius, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.search_widen_radius, wider.km))
        }
    }
}

@Composable
private fun NoAddressBlock(onRegisterAddress: () -> Unit) {
    Text(
        text = stringResource(R.string.search_no_address_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = stringResource(R.string.search_no_address_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PrimaryButton(
        text = stringResource(R.string.search_no_address_action),
        onClick = onRegisterAddress,
    )
}

@Composable
private fun FailedBlock(
    messageRes: Int,
    onRetry: () -> Unit,
) {
    Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
    )
    PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetry)
}

@Composable
private fun CenteredProgress() {
    val loadingDescription = stringResource(R.string.cd_loading)

    Box(modifier = Modifier.fillMaxWidth().padding(Spacing.scale16), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
    }
}

@StringRes
private fun ResultsMode.labelRes(): Int =
    when (this) {
        ResultsMode.MAP -> R.string.search_mode_map
        ResultsMode.LIST -> R.string.search_mode_list
    }

private fun SearchResultsState.professionals(): List<NearbyProfessional> =
    (this as? SearchResultsState.Content)?.professionals ?: emptyList()

private fun ProfessionalSearchUiState.selected(results: SearchResultsState.Content): NearbyProfessional? =
    results.professionals.firstOrNull { it.professionalId == selectedId }

// The map never reaches a preview or a test: without a key and Play Services the
// SDK draws a blank grid, and the slot is what keeps it out (the AddressScreen
// precedent, HU-05).
@Composable
private fun PreviewMap(
    model: SearchMapModel,
    modifier: Modifier,
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
}

private fun previewOrigin() =
    SearchOrigin(
        addressId = "home",
        alias = "Mi casa",
        addressText = "Avenida América 1234, Cochabamba",
        coordinate = Coordinate.create(-17.3835, -66.1568).getOrThrow(),
    )

private fun previewProfessional(
    id: String,
    name: String,
    distanceM: Int,
    totalReviews: Int = 12,
    availableNow: Boolean = true,
) = NearbyProfessional(
    professionalId = id,
    fullName = name,
    photoUrl = null,
    professionalType = ProfessionalType.NURSE,
    specialty = "Enfermería geriátrica",
    baseRateBob = BigDecimal("180.00"),
    averageRating = 4.8,
    totalReviews = totalReviews,
    availableNow = availableNow,
    distanceM = distanceM,
    basePoint = Coordinate.create(-17.368, -66.174).getOrThrow(),
)

private fun previewTypes() =
    listOf(
        ServiceType("t1", "Consulta médica general", null, BigDecimal("150.00"), 45),
        ServiceType("t2", "Colocación y control de vía venosa", null, BigDecimal("120.00"), 60),
    )

private fun previewState(
    results: SearchResultsState,
    mode: ResultsMode = ResultsMode.MAP,
    criteria: SearchCriteria = SearchCriteria(),
    origin: SearchOrigin? = previewOrigin(),
    selectedId: String? = null,
    typeFilterExpanded: Boolean = false,
) = ProfessionalSearchUiState(
    criteria = criteria,
    mode = mode,
    origin = origin,
    serviceTypes = previewTypes(),
    selectedId = selectedId,
    typeFilterExpanded = typeFilterExpanded,
    results = results,
)

private fun previewContent() =
    SearchResultsState.Content(
        professionals =
            listOf(
                previewProfessional("p1", "Ana Pérez Quiroga", distanceM = 0),
                previewProfessional("p2", "Luis Mamani", distanceM = 1200, totalReviews = 0, availableNow = false),
                previewProfessional("p3", "Carla Villarroel Antezana", distanceM = 4800),
            ),
        loadingMore = false,
        endReached = true,
        notice = null,
    )

@Composable
private fun PreviewScreen(
    uiState: ProfessionalSearchUiState,
    darkTheme: Boolean = false,
) {
    SaludEnCasaTheme(darkTheme = darkTheme) {
        ProfessionalSearchContent(
            uiState = uiState,
            onBack = {},
            actions = SearchActions(),
            map = { model, modifier -> PreviewMap(model, modifier) },
        )
    }
}

@Preview(showBackground = true, name = "Busqueda en el mapa, claro", heightDp = 760)
@Composable
private fun SearchMapLightPreview() {
    PreviewScreen(previewState(previewContent(), selectedId = "p1"))
}

@Preview(showBackground = true, name = "Busqueda en el mapa, oscuro", heightDp = 760)
@Composable
private fun SearchMapDarkPreview() {
    PreviewScreen(previewState(previewContent(), selectedId = "p1"), darkTheme = true)
}

@Preview(showBackground = true, name = "Busqueda en lista, claro", heightDp = 900)
@Composable
private fun SearchListLightPreview() {
    PreviewScreen(previewState(previewContent(), mode = ResultsMode.LIST))
}

@Preview(showBackground = true, name = "Busqueda en lista, oscuro", heightDp = 900)
@Composable
private fun SearchListDarkPreview() {
    PreviewScreen(previewState(previewContent(), mode = ResultsMode.LIST), darkTheme = true)
}

@Preview(showBackground = true, name = "Busqueda en lista al 200 %", fontScale = 2f, heightDp = 1800)
@Composable
private fun SearchListLargeFontPreview() {
    PreviewScreen(previewState(previewContent(), mode = ResultsMode.LIST))
}

@Preview(showBackground = true, name = "Busqueda en el mapa al 200 %", fontScale = 2f, heightDp = 1200)
@Composable
private fun SearchMapLargeFontPreview() {
    PreviewScreen(previewState(previewContent(), selectedId = "p1"))
}

@Preview(showBackground = true, name = "Lista de tipos abierta", heightDp = 900)
@Composable
private fun SearchTypeListOpenPreview() {
    PreviewScreen(previewState(previewContent(), typeFilterExpanded = true))
}

@Preview(showBackground = true, name = "Lista de tipos abierta al 200 %", fontScale = 2f, heightDp = 1800)
@Composable
private fun SearchTypeListOpenLargeFontPreview() {
    PreviewScreen(previewState(previewContent(), typeFilterExpanded = true))
}

@Preview(showBackground = true, name = "Buscando", heightDp = 760)
@Composable
private fun SearchSearchingPreview() {
    PreviewScreen(previewState(SearchResultsState.Searching))
}

@Preview(showBackground = true, name = "Sin resultados en el radio", heightDp = 760)
@Composable
private fun SearchEmptyPreview() {
    PreviewScreen(previewState(SearchResultsState.Empty, mode = ResultsMode.LIST))
}

@Preview(showBackground = true, name = "Sin coincidencias con los filtros", heightDp = 760)
@Composable
private fun SearchEmptyNarrowedPreview() {
    PreviewScreen(
        previewState(
            SearchResultsState.Empty,
            mode = ResultsMode.LIST,
            criteria = SearchCriteria(radius = SearchRadius.KM_10, serviceTypeId = "t1", availableNowOnly = true),
        ),
    )
}

@Preview(showBackground = true, name = "Sin direccion registrada", heightDp = 760)
@Composable
private fun SearchNoAddressPreview() {
    PreviewScreen(previewState(SearchResultsState.NoPrimaryAddress, origin = null))
}

@Preview(showBackground = true, name = "Busqueda fallida, oscuro", heightDp = 760)
@Composable
private fun SearchFailedPreview() {
    PreviewScreen(
        previewState(SearchResultsState.Failed(SearchError.NetworkUnavailable)),
        darkTheme = true,
    )
}

@Preview(showBackground = true, name = "Pagina siguiente fallida", heightDp = 900)
@Composable
private fun SearchNextPageFailedPreview() {
    PreviewScreen(
        previewState(
            previewContent().copy(endReached = false, notice = SearchError.NetworkUnavailable),
            mode = ResultsMode.LIST,
        ),
    )
}
