package bo.saludencasa.features.home.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import bo.saludencasa.R
import bo.saludencasa.features.requests.presentation.InboxScreen
import bo.saludencasa.features.requests.presentation.MyRequestsScreen
import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.presentation.MyAttentionsActions
import bo.saludencasa.features.services.presentation.MyAttentionsContent
import bo.saludencasa.features.services.presentation.MyAttentionsScreen
import bo.saludencasa.features.services.presentation.MyAttentionsUiState
import bo.saludencasa.ui.components.SegmentedControl
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

internal enum class RequestsView {
    REQUESTS,
    MY_ATTENTIONS,
}

// docs/decisions.md, 2026-11-03, HT-26. Leaving the destination starts it over on
// the first view, which is where «Ver todas» of Inicio has to land; coming back from
// an attention keeps the view, because the entry saves its state.
@Composable
internal fun ProfessionalRequestsTab(
    navigation: HomeNavigation,
    modifier: Modifier = Modifier,
) {
    var view by rememberSaveable { mutableStateOf(RequestsView.REQUESTS) }

    RequestsTabContent(
        view = view,
        onViewChange = { view = it },
        requestsLabel = stringResource(R.string.home_requests_view_available),
        modifier = modifier,
        requests = { InboxScreen(onBack = null, onOpenRequest = navigation.onOpenInboxRequest) },
        myAttentions = {
            MyAttentionsScreen(side = AttentionSide.PROFESSIONAL, onOpenAttention = navigation.onOpenAttention)
        },
    )
}

// docs/decisions.md, 2026-11-04, HT-27, the two views of the patient.
@Composable
internal fun PatientRequestsTab(
    navigation: HomeNavigation,
    modifier: Modifier = Modifier,
) {
    var view by rememberSaveable { mutableStateOf(RequestsView.REQUESTS) }

    RequestsTabContent(
        view = view,
        onViewChange = { view = it },
        requestsLabel = stringResource(R.string.home_requests_view_my_requests),
        modifier = modifier,
        requests = {
            MyRequestsScreen(
                justCreated = false,
                onBack = null,
                onCreateRequest = navigation.onOpenImmediateCare,
                onOpenRequest = navigation.onOpenNegotiation,
            )
        },
        myAttentions = {
            MyAttentionsScreen(side = AttentionSide.PATIENT, onOpenAttention = navigation.onOpenAttention)
        },
    )
}

@Composable
internal fun RequestsTabContent(
    view: RequestsView,
    onViewChange: (RequestsView) -> Unit,
    requestsLabel: String,
    modifier: Modifier = Modifier,
    requests: @Composable () -> Unit = {},
    myAttentions: @Composable () -> Unit = {},
) {
    val myAttentionsLabel = stringResource(R.string.home_requests_view_my_attentions)

    Column(modifier = modifier.fillMaxSize()) {
        SegmentedControl(
            options = RequestsView.entries,
            selected = view,
            onSelectedChange = onViewChange,
            optionLabel = { option ->
                when (option) {
                    RequestsView.REQUESTS -> requestsLabel
                    RequestsView.MY_ATTENTIONS -> myAttentionsLabel
                }
            },
            modifier = Modifier.padding(horizontal = Spacing.screenMargin, vertical = Spacing.scale8),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (view) {
                RequestsView.REQUESTS -> requests()
                RequestsView.MY_ATTENTIONS -> myAttentions()
            }
        }
    }
}

@Composable
private fun RequestsTabPreviewFrame(view: RequestsView) {
    SaludEnCasaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RequestsTabContent(
                view = view,
                onViewChange = {},
                requestsLabel = stringResource(R.string.home_requests_view_my_requests),
                myAttentions = {
                    MyAttentionsContent(uiState = MyAttentionsUiState.Empty, actions = MyAttentionsActions())
                },
            )
        }
    }
}

@Preview(name = "Requests tab", showBackground = true)
@Composable
private fun RequestsTabPreview() {
    RequestsTabPreviewFrame(RequestsView.MY_ATTENTIONS)
}

@Preview(name = "Requests tab dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RequestsTabDarkPreview() {
    RequestsTabPreviewFrame(RequestsView.REQUESTS)
}

@Preview(name = "Requests tab large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun RequestsTabLargeFontPreview() {
    RequestsTabPreviewFrame(RequestsView.MY_ATTENTIONS)
}
