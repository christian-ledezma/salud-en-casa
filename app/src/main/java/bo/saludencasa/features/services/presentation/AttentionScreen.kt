package bo.saludencasa.features.services.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.formatBob
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.Counterpart
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.ui.components.CardAvatar
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal
import java.time.Instant

// The sealed agreement, reached after acceptance from the thread, from a card of
// «Mis solicitudes» or from the acceptance notification. The patient opens the
// professional's existing public profile from here; the professional sees the
// patient's name, now that it is no longer hidden. No phone is shown: coordination
// happens in the conversation (HU-21). docs/decisions.md, 2026-10-30.
@Composable
fun AttentionScreen(
    requestId: String,
    onBack: () -> Unit,
    onOpenProfessionalProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AttentionViewModel = koinViewModel { parametersOf(requestId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AttentionContent(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onOpenProfessionalProfile = onOpenProfessionalProfile,
        modifier = modifier,
    )
}

@Composable
internal fun AttentionContent(
    uiState: AttentionUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenProfessionalProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            AttentionUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            AttentionUiState.NotFound -> {
                MessageColumn(onBack = onBack) {
                    Text(
                        text = stringResource(R.string.attention_not_found_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.attention_not_found_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is AttentionUiState.Failed -> {
                MessageColumn(onBack = onBack) {
                    Text(
                        text = stringResource(uiState.error.messageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetry)
                }
            }

            is AttentionUiState.Content -> {
                AttentionBody(
                    attention = uiState.attention,
                    onBack = onBack,
                    onOpenProfessionalProfile = onOpenProfessionalProfile,
                )
            }
        }
    }
}

@Composable
private fun AttentionBody(
    attention: Attention,
    onBack: () -> Unit,
    onOpenProfessionalProfile: (String) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val margin = Modifier.padding(horizontal = Spacing.screenMargin)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        ScreenHeader(title = stringResource(R.string.attention_title), onBack = onBack, modifier = margin)

        Surface(
            modifier = margin.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = stringResource(R.string.attention_sealed_banner),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(Spacing.scale16),
            )
        }

        CounterpartCard(
            counterpart = attention.counterpart,
            iAmThePatient = attention.iAmThePatient,
            onOpenProfessionalProfile = onOpenProfessionalProfile,
            modifier = margin,
        )

        LabeledValue(
            label = stringResource(R.string.attention_amount_label),
            value = formatBob(attention.finalAmountBob, locale),
            modifier = margin,
        )
        LabeledValue(
            label = stringResource(R.string.attention_status_label),
            value = stringResource(attention.status.labelRes()),
            modifier = margin,
        )
    }
}

// The patient taps through to the professional's public profile, the one the
// product already has; the professional only sees the patient's name, since a
// patient has no public profile.
@Composable
private fun CounterpartCard(
    counterpart: Counterpart,
    iAmThePatient: Boolean,
    onOpenProfessionalProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val roleLabel =
        if (iAmThePatient) {
            R.string.attention_counterpart_professional
        } else {
            R.string.attention_counterpart_patient
        }
    val cardModifier =
        if (iAmThePatient) {
            modifier
                .fillMaxWidth()
                .clickable { onOpenProfessionalProfile(counterpart.id) }
        } else {
            modifier.fillMaxWidth()
        }

    Surface(
        modifier = cardModifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.scale16),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardAvatar(name = counterpart.name, photoUrl = counterpart.photoUrl)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
                    Text(
                        text = stringResource(roleLabel),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = counterpart.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            if (iAmThePatient) {
                Text(
                    text = stringResource(R.string.attention_view_professional),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.scale4)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MessageColumn(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenMargin)
                .padding(vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        ScreenHeader(title = stringResource(R.string.attention_title), onBack = onBack)
        content()
    }
}

@StringRes
private fun ServiceStatus.labelRes(): Int =
    when (this) {
        ServiceStatus.ASSIGNED -> R.string.attention_status_assigned
        ServiceStatus.IN_PROGRESS -> R.string.attention_status_in_progress
        ServiceStatus.COMPLETED -> R.string.attention_status_completed
        ServiceStatus.CANCELLED -> R.string.attention_status_cancelled
    }

@StringRes
private fun ServiceError.messageRes(): Int =
    when (this) {
        ServiceError.NetworkUnavailable -> R.string.error_network_unavailable
        ServiceError.NotSignedIn, ServiceError.Unexpected -> R.string.error_unexpected
    }

private val previewAttention =
    Attention(
        serviceId = "s1",
        requestId = "r1",
        status = ServiceStatus.ASSIGNED,
        finalAmountBob = BigDecimal("150.00"),
        serviceTypeName = "Curación de heridas",
        counterpart =
            Counterpart(
                id = "p1",
                name = "María Fernanda Quispe",
                photoUrl = null,
            ),
        iAmThePatient = true,
        createdAt = Instant.parse("2026-10-29T12:00:00Z"),
    )

@Preview(name = "Attention", showBackground = true)
@Composable
private fun AttentionPreview() {
    SaludEnCasaTheme {
        AttentionContent(
            uiState = AttentionUiState.Content(previewAttention),
            onBack = {},
            onRetry = {},
            onOpenProfessionalProfile = {},
        )
    }
}

@Preview(name = "Attention dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttentionDarkPreview() {
    SaludEnCasaTheme {
        AttentionContent(
            uiState = AttentionUiState.Content(previewAttention),
            onBack = {},
            onRetry = {},
            onOpenProfessionalProfile = {},
        )
    }
}

@Preview(name = "Attention large font", showBackground = true, fontScale = 2f)
@Composable
private fun AttentionLargeFontPreview() {
    SaludEnCasaTheme {
        AttentionContent(
            uiState = AttentionUiState.Content(previewAttention),
            onBack = {},
            onRetry = {},
            onOpenProfessionalProfile = {},
        )
    }
}
