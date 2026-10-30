package bo.saludencasa.features.home.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PersonName
import bo.saludencasa.features.auth.domain.model.AuthError
import bo.saludencasa.features.auth.domain.model.AuthSession
import bo.saludencasa.features.auth.presentation.AccountUiState
import bo.saludencasa.features.auth.presentation.AccountViewModel
import bo.saludencasa.features.auth.presentation.RoleSection
import bo.saludencasa.features.auth.presentation.messageRes
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ProfileRoles
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.presentation.messageRes
import bo.saludencasa.features.profile.presentation.shortLabelRes
import bo.saludencasa.features.requests.domain.model.InboxRequest
import bo.saludencasa.features.requests.domain.model.RequestModality
import bo.saludencasa.features.requests.domain.model.RequestStatus
import bo.saludencasa.features.requests.domain.model.ServiceRequest
import bo.saludencasa.features.requests.presentation.InboxScreen
import bo.saludencasa.features.requests.presentation.MyRequestsScreen
import bo.saludencasa.features.requests.presentation.NegotiationEntry
import bo.saludencasa.ui.components.BottomNavigationBar
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.initialsOf
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.Instant

// docs/decisions.md, 2026-10-30, HT-25: the same four destinations for both
// roles, and none for the administrator, who has one.
enum class HomeTab(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(R.string.common_nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    SETTINGS(R.string.common_nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
    REQUESTS(R.string.common_nav_requests, Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List),
    ACCOUNT(R.string.common_nav_account, Icons.Outlined.Person, Icons.Filled.Person),
}

// Every screen AccountScreen used to reach, so none of them loses its way in.
data class HomeNavigation(
    val onOpenImmediateCare: () -> Unit = {},
    val onOpenScheduling: () -> Unit = {},
    val onOpenNegotiation: (NegotiationEntry) -> Unit = {},
    val onOpenInboxRequest: (String) -> Unit = {},
    val onOpenProfile: () -> Unit = {},
    val onOpenMyServices: () -> Unit = {},
    val onOpenAvailability: () -> Unit = {},
    val onOpenAddresses: () -> Unit = {},
    val onOpenVerification: () -> Unit = {},
    val onOpenDocumentReview: () -> Unit = {},
)

internal data class HomeActions(
    val onSignOut: () -> Unit = {},
    val onDismissError: () -> Unit = {},
    val onReloadRoles: () -> Unit = {},
    val onSwitchTo: (UserRole) -> Unit = {},
    val onActivate: (AssignableRole) -> Unit = {},
    val onAvailabilityChange: (Boolean) -> Unit = {},
    val onRetryProfessionalHome: () -> Unit = {},
)

@Composable
fun HomeShell(
    onSignedOut: () -> Unit,
    navigation: HomeNavigation,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is AccountUiState.SignedOut) onSignedOut()
    }

    val activeRole = (uiState as? AccountUiState.Content)?.roleSection?.roles?.active
    // Keyed on the role: switching lands on Inicio, a rotation keeps the tab.
    var tab by rememberSaveable(activeRole) { mutableStateOf(HomeTab.HOME) }

    val patientViewModel =
        if (activeRole == UserRole.PATIENT) koinViewModel<PatientHomeViewModel>() else null
    val professionalViewModel =
        if (activeRole == UserRole.PROFESSIONAL) koinViewModel<ProfessionalHomeViewModel>() else null
    patientViewModel?.let { home ->
        LifecycleResumeEffect(home) {
            home.refresh()
            onPauseOrDispose {}
        }
    }
    professionalViewModel?.let { home ->
        LifecycleResumeEffect(home) {
            home.refresh()
            onPauseOrDispose {}
        }
    }
    val patientHome = patientViewModel?.uiState?.collectAsStateWithLifecycle()?.value ?: PatientHomeUiState.Loading
    val professionalHome =
        professionalViewModel?.uiState?.collectAsStateWithLifecycle()?.value ?: ProfessionalHomeUiState.Loading

    HomeShellContent(
        uiState = uiState,
        tab = tab,
        onTabSelected = { tab = it },
        patientHome = patientHome,
        professionalHome = professionalHome,
        navigation = navigation,
        actions =
            HomeActions(
                onSignOut = viewModel::signOut,
                onDismissError = viewModel::dismissError,
                onReloadRoles = viewModel::reloadRoles,
                onSwitchTo = viewModel::switchTo,
                onActivate = viewModel::activate,
                onAvailabilityChange = { professionalViewModel?.onAvailabilityChange(it) },
                onRetryProfessionalHome = { professionalViewModel?.refresh() },
            ),
        modifier = modifier,
        requestsTab = {
            // Embedded without a header of their own: the shell already shows the title.
            if (activeRole == UserRole.PROFESSIONAL) {
                InboxScreen(onBack = null, onOpenRequest = navigation.onOpenInboxRequest)
            } else {
                MyRequestsScreen(
                    justCreated = false,
                    onBack = null,
                    onCreateRequest = navigation.onOpenImmediateCare,
                    onOpenRequest = navigation.onOpenNegotiation,
                )
            }
        },
    )
}

@Composable
internal fun HomeShellContent(
    uiState: AccountUiState,
    tab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    patientHome: PatientHomeUiState,
    professionalHome: ProfessionalHomeUiState,
    navigation: HomeNavigation,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    requestsTab: @Composable () -> Unit = {},
) {
    when (uiState) {
        AccountUiState.Loading, AccountUiState.SignedOut -> {
            CenteredProgress(modifier = modifier.fillMaxSize())
        }

        is AccountUiState.Error -> {
            SignOutFailure(
                error = uiState.error,
                onRetry = actions.onSignOut,
                onKeepSession = actions.onDismissError,
                modifier = modifier,
            )
        }

        is AccountUiState.Content -> {
            val active = uiState.roleSection.roles.active
            if (active == null) {
                RolesUnavailable(
                    roleSection = uiState.roleSection,
                    onRetry = actions.onReloadRoles,
                    modifier = modifier,
                )
            } else {
                SignedInShell(
                    session = uiState.session,
                    roleSection = uiState.roleSection,
                    active = active,
                    tab = tab,
                    onTabSelected = onTabSelected,
                    patientHome = patientHome,
                    professionalHome = professionalHome,
                    navigation = navigation,
                    actions = actions,
                    requestsTab = requestsTab,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun SignedInShell(
    session: AuthSession,
    roleSection: RoleSection,
    active: UserRole,
    tab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    patientHome: PatientHomeUiState,
    professionalHome: ProfessionalHomeUiState,
    navigation: HomeNavigation,
    actions: HomeActions,
    requestsTab: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val admin = active == UserRole.ADMIN
    val shownTab = if (admin) HomeTab.HOME else tab
    val labels = HomeTab.entries.associateWith { stringResource(it.labelRes) }
    val loadingDescription = stringResource(R.string.cd_loading)
    val greeting: @Composable () -> Unit = {
        HomeGreeting(
            firstName = session.fullName?.value?.let(::firstNameOf),
            roleLabel = stringResource(active.shortLabelRes()),
        )
    }
    val verified = (professionalHome as? ProfessionalHomeUiState.Content)?.verified

    Column(modifier = modifier.fillMaxSize()) {
        HomeTopBar(
            title = topBarTitleRes(shownTab, active)?.let { stringResource(it) },
            session = session,
            roles = roleSection.roles,
            busy = roleSection.busy,
            onSwitchTo = actions.onSwitchTo,
            onSignOut = actions.onSignOut,
        )
        if (roleSection.busy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = loadingDescription },
            )
        }
        roleSection.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Spacing.screenMargin, vertical = Spacing.scale8),
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                admin -> {
                    AdminHome(greeting = greeting, onOpenDocumentReview = navigation.onOpenDocumentReview)
                }

                shownTab == HomeTab.HOME && active == UserRole.PATIENT -> {
                    PatientHome(
                        uiState = patientHome,
                        greeting = greeting,
                        onOpenImmediateCare = navigation.onOpenImmediateCare,
                        onOpenScheduling = navigation.onOpenScheduling,
                    )
                }

                shownTab == HomeTab.HOME -> {
                    ProfessionalHome(
                        uiState = professionalHome,
                        greeting = greeting,
                        onAvailabilityChange = actions.onAvailabilityChange,
                        onRetry = actions.onRetryProfessionalHome,
                        onOpenVerification = navigation.onOpenVerification,
                        onOpenRequest = navigation.onOpenInboxRequest,
                        onSeeAllRequests = { onTabSelected(HomeTab.REQUESTS) },
                    )
                }

                shownTab == HomeTab.SETTINGS -> {
                    SettingsContent(role = active, verified = verified, navigation = navigation)
                }

                shownTab == HomeTab.REQUESTS -> {
                    requestsTab()
                }

                else -> {
                    AccountTabContent(
                        session = session,
                        roleSection = roleSection,
                        onOpenProfile = navigation.onOpenProfile,
                        onActivate = actions.onActivate,
                        onSignOut = actions.onSignOut,
                    )
                }
            }
        }
        if (!admin) {
            BottomNavigationBar(
                destinations = HomeTab.entries,
                selected = shownTab,
                onSelectedChange = onTabSelected,
                label = { labels.getValue(it) },
                icon = { destination, isSelected -> if (isSelected) destination.selectedIcon else destination.icon },
            )
        }
    }
}

@StringRes
private fun topBarTitleRes(
    tab: HomeTab,
    active: UserRole,
): Int? =
    when (tab) {
        HomeTab.HOME -> {
            null
        }

        HomeTab.SETTINGS -> {
            R.string.home_settings_title
        }

        HomeTab.REQUESTS -> {
            if (active == UserRole.PROFESSIONAL) R.string.request_inbox_title else R.string.request_list_title
        }

        HomeTab.ACCOUNT -> {
            R.string.common_nav_account
        }
    }

// The brand on Inicio and the screen's own title everywhere else, in the same
// place and weight, with the avatar that opens the account menu on the right.
@Composable
private fun HomeTopBar(
    title: String?,
    session: AuthSession,
    roles: ProfileRoles,
    busy: Boolean,
    onSwitchTo: (UserRole) -> Unit,
    onSignOut: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.scale8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (title == null) {
            BrandLockup(modifier = Modifier.weight(1f))
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }
        AccountMenu(session = session, roles = roles, busy = busy, onSwitchTo = onSwitchTo, onSignOut = onSignOut)
    }
}

@Composable
private fun BrandLockup(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The mark's canvas is mostly the icon's safe zone; the drawing itself
        // fits this box, so the house lines up with the screen margin.
        Box(modifier = Modifier.size(Spacing.scale24), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.ic_brand_mark),
                contentDescription = null,
                modifier = Modifier.requiredSize(Spacing.scale48),
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_brand_wordmark),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.height(Spacing.headerWordmarkHeight),
        )
    }
}

// docs/decisions.md, 2026-10-30, HT-25: changing role and signing out live in a
// menu on the initials, so they are reachable from all four destinations.
@Composable
private fun AccountMenu(
    session: AuthSession,
    roles: ProfileRoles,
    busy: Boolean,
    onSwitchTo: (UserRole) -> Unit,
    onSignOut: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val name = session.fullName?.value
    val menuDescription = stringResource(R.string.cd_account_menu)

    Box {
        Box(
            modifier =
                Modifier
                    .size(Spacing.minTouchTarget)
                    .clip(CircleShape)
                    .background(
                        if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    ).clickable { expanded = true }
                    .semantics { contentDescription = menuDescription },
            contentAlignment = Alignment.Center,
        ) {
            val color =
                if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
            if (name != null) {
                Text(
                    text = initialsOf(name),
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            } else {
                Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = color)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
            ) {
                name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = session.email?.value ?: stringResource(R.string.auth_account_email_unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            // A single role has nowhere to switch into, so the choice is not drawn.
            if (roles.canSwitch) {
                Text(
                    text = stringResource(R.string.home_menu_use_as),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
                )
                roles.switchable.forEach { role ->
                    val isActive = role == roles.active
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
                                Text(text = stringResource(role.shortLabelRes()))
                                Text(
                                    text = stringResource(role.menuHintRes()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onSwitchTo(role)
                        },
                        modifier = Modifier.semantics { selected = isActive },
                        trailingIcon =
                            if (isActive) {
                                { Icon(imageVector = Icons.Filled.Check, contentDescription = null) }
                            } else {
                                null
                            },
                        enabled = !busy,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            DropdownMenuItem(
                text = {
                    Text(text = stringResource(R.string.auth_account_sign_out), color = MaterialTheme.colorScheme.error)
                },
                onClick = {
                    expanded = false
                    onSignOut()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
            )
        }
    }
}

@StringRes
private fun UserRole.menuHintRes(): Int =
    when (this) {
        UserRole.PATIENT -> R.string.home_menu_hint_patient
        UserRole.PROFESSIONAL -> R.string.home_menu_hint_professional
        UserRole.ADMIN -> R.string.profile_role_admin
    }

@Composable
private fun SignOutFailure(
    error: AuthError,
    onRetry: () -> Unit,
    onKeepSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        Text(
            text = stringResource(error.messageRes()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetry)
        OutlinedButton(onClick = onKeepSession, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.auth_account_keep_session))
        }
    }
}

@Composable
private fun RolesUnavailable(
    roleSection: RoleSection,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = roleSection.error
    if (error == null) {
        CenteredProgress(modifier = modifier.fillMaxSize())
    } else {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
        ) {
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetry)
        }
    }
}

private fun previewSession(): AuthSession =
    AuthSession(
        userId = "00000000-0000-0000-0000-000000000000",
        email = Email.create("ana.quispe@example.com").getOrNull(),
        fullName = PersonName.create("Ana Quispe").getOrNull(),
    )

private fun previewAccount(
    held: Set<UserRole>,
    active: UserRole,
): AccountUiState =
    AccountUiState.Content(
        session = previewSession(),
        roleSection = RoleSection(roles = ProfileRoles.create(held, active).getOrThrow()),
    )

private val bothRoles = setOf(UserRole.PATIENT, UserRole.PROFESSIONAL)

private fun previewInProgress(): PatientHomeUiState =
    PatientHomeUiState.InProgress(
        ServiceRequest(
            id = "r1",
            serviceTypeName = "Curación de herida",
            addressText = "Av. 6 de Agosto 2170",
            city = "La Paz",
            status = RequestStatus.NEGOTIATING,
            modality = RequestModality.IMMEDIATE,
            description = null,
            suggestedBudgetBob = BigDecimal("120.00"),
            expiresAt = null,
            createdAt = Instant.parse("2026-10-30T14:12:00Z"),
            scheduledAt = null,
            professionalName = null,
        ),
    )

private fun previewNearby(
    id: String,
    scheduledAt: Instant? = null,
): InboxRequest =
    InboxRequest(
        id = id,
        serviceTypeName = "Colocación y control de vía venosa",
        city = "Sopocachi",
        description = null,
        suggestedBudgetBob = BigDecimal("150.00"),
        distanceM = 1800,
        status = RequestStatus.PUBLISHED,
        createdAt = Instant.parse("2026-10-30T13:00:00Z"),
        modality = if (scheduledAt == null) RequestModality.IMMEDIATE else RequestModality.SCHEDULED,
        scheduledAt = scheduledAt,
    )

private fun previewProfessional(
    verified: Boolean = false,
    nearby: NearbyRequests =
        NearbyRequests.Some(
            listOf(
                previewNearby("n1"),
                previewNearby("n2", scheduledAt = Instant.parse("2026-10-31T13:00:00Z")),
            ),
        ),
): ProfessionalHomeUiState =
    ProfessionalHomeUiState.Content(
        availableNow = true,
        availabilityError = null,
        verified = verified,
        nearby = nearby,
    )

@Composable
private fun PreviewShell(
    darkTheme: Boolean,
    account: AccountUiState,
    tab: HomeTab = HomeTab.HOME,
    patientHome: PatientHomeUiState = previewInProgress(),
    professionalHome: ProfessionalHomeUiState = previewProfessional(),
) {
    SaludEnCasaTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeShellContent(
                uiState = account,
                tab = tab,
                onTabSelected = {},
                patientHome = patientHome,
                professionalHome = professionalHome,
                navigation = HomeNavigation(),
                actions = HomeActions(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, name = "Inicio del paciente, claro")
@Composable
private fun HomePatientLightPreview() {
    PreviewShell(darkTheme = false, account = previewAccount(bothRoles, UserRole.PATIENT))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, name = "Inicio del paciente, oscuro")
@Composable
private fun HomePatientDarkPreview() {
    PreviewShell(darkTheme = true, account = previewAccount(bothRoles, UserRole.PATIENT))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 2f, name = "Inicio del paciente, 200%")
@Composable
private fun HomePatientLargeFontPreview() {
    PreviewShell(darkTheme = false, account = previewAccount(bothRoles, UserRole.PATIENT))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, name = "Inicio del profesional, claro")
@Composable
private fun HomeProfessionalLightPreview() {
    PreviewShell(darkTheme = false, account = previewAccount(bothRoles, UserRole.PROFESSIONAL))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, name = "Inicio del profesional, oscuro")
@Composable
private fun HomeProfessionalDarkPreview() {
    PreviewShell(darkTheme = true, account = previewAccount(bothRoles, UserRole.PROFESSIONAL))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 2f, name = "Inicio del profesional, 200%")
@Composable
private fun HomeProfessionalLargeFontPreview() {
    PreviewShell(darkTheme = false, account = previewAccount(bothRoles, UserRole.PROFESSIONAL))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, name = "Inicio del profesional sin solicitudes")
@Composable
private fun HomeProfessionalEmptyPreview() {
    PreviewShell(
        darkTheme = false,
        account = previewAccount(bothRoles, UserRole.PROFESSIONAL),
        professionalHome = previewProfessional(verified = true, nearby = NearbyRequests.None),
    )
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, name = "Ajustes del profesional, claro")
@Composable
private fun HomeSettingsLightPreview() {
    PreviewShell(darkTheme = false, account = previewAccount(bothRoles, UserRole.PROFESSIONAL), tab = HomeTab.SETTINGS)
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, name = "Ajustes del profesional, oscuro")
@Composable
private fun HomeSettingsDarkPreview() {
    PreviewShell(darkTheme = true, account = previewAccount(bothRoles, UserRole.PROFESSIONAL), tab = HomeTab.SETTINGS)
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 2f, name = "Ajustes del profesional, 200%")
@Composable
private fun HomeSettingsLargeFontPreview() {
    PreviewShell(darkTheme = false, account = previewAccount(bothRoles, UserRole.PROFESSIONAL), tab = HomeTab.SETTINGS)
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, name = "Cuenta con un solo rol, claro")
@Composable
private fun HomeAccountLightPreview() {
    PreviewShell(
        darkTheme = false,
        account = previewAccount(setOf(UserRole.PATIENT), UserRole.PATIENT),
        tab = HomeTab.ACCOUNT,
    )
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, name = "Cuenta con un solo rol, oscuro")
@Composable
private fun HomeAccountDarkPreview() {
    PreviewShell(
        darkTheme = true,
        account = previewAccount(setOf(UserRole.PATIENT), UserRole.PATIENT),
        tab = HomeTab.ACCOUNT,
    )
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, fontScale = 2f, name = "Cuenta con un solo rol, 200%")
@Composable
private fun HomeAccountLargeFontPreview() {
    PreviewShell(
        darkTheme = false,
        account = previewAccount(setOf(UserRole.PATIENT), UserRole.PATIENT),
        tab = HomeTab.ACCOUNT,
    )
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, name = "Inicio del administrador")
@Composable
private fun HomeAdminPreview() {
    PreviewShell(darkTheme = false, account = previewAccount(setOf(UserRole.ADMIN), UserRole.ADMIN))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, name = "Inicio cargando")
@Composable
private fun HomeLoadingPreview() {
    PreviewShell(darkTheme = false, account = AccountUiState.Loading)
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720, name = "Cierre de sesión fallido")
@Composable
private fun HomeErrorPreview() {
    PreviewShell(darkTheme = false, account = AccountUiState.Error(AuthError.NetworkUnavailable))
}
