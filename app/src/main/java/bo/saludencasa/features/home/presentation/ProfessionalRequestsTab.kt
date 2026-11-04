package bo.saludencasa.features.home.presentation

import android.content.res.Configuration
import androidx.annotation.StringRes
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
import bo.saludencasa.features.services.presentation.MyAttentionsActions
import bo.saludencasa.features.services.presentation.MyAttentionsContent
import bo.saludencasa.features.services.presentation.MyAttentionsScreen
import bo.saludencasa.features.services.presentation.MyAttentionsUiState
import bo.saludencasa.ui.components.SegmentedControl
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing

internal enum class ProfessionalRequestsView(
    @param:StringRes val labelRes: Int,
) {
    AVAILABLE(R.string.home_requests_view_available),
    MY_ATTENTIONS(R.string.home_requests_view_my_attentions),
}

// docs/decisions.md, 2026-11-03, HT-26. Leaving the destination starts it over on
// the requests available, which is where «Ver todas» of Inicio has to land; coming
// back from an attention keeps the view, because the entry saves its state.
@Composable
internal fun ProfessionalRequestsTab(
    navigation: HomeNavigation,
    modifier: Modifier = Modifier,
) {
    var view by rememberSaveable { mutableStateOf(ProfessionalRequestsView.AVAILABLE) }

    ProfessionalRequestsContent(
        view = view,
        onViewChange = { view = it },
        modifier = modifier,
        available = { InboxScreen(onBack = null, onOpenRequest = navigation.onOpenInboxRequest) },
        myAttentions = { MyAttentionsScreen(onOpenAttention = navigation.onOpenAttention) },
    )
}

@Composable
internal fun ProfessionalRequestsContent(
    view: ProfessionalRequestsView,
    onViewChange: (ProfessionalRequestsView) -> Unit,
    modifier: Modifier = Modifier,
    available: @Composable () -> Unit = {},
    myAttentions: @Composable () -> Unit = {},
) {
    val labels = ProfessionalRequestsView.entries.associateWith { stringResource(it.labelRes) }

    Column(modifier = modifier.fillMaxSize()) {
        SegmentedControl(
            options = ProfessionalRequestsView.entries,
            selected = view,
            onSelectedChange = onViewChange,
            optionLabel = labels::getValue,
            modifier = Modifier.padding(horizontal = Spacing.screenMargin, vertical = Spacing.scale8),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (view) {
                ProfessionalRequestsView.AVAILABLE -> available()
                ProfessionalRequestsView.MY_ATTENTIONS -> myAttentions()
            }
        }
    }
}

@Composable
private fun ProfessionalRequestsPreviewFrame(view: ProfessionalRequestsView) {
    SaludEnCasaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProfessionalRequestsContent(
                view = view,
                onViewChange = {},
                myAttentions = {
                    MyAttentionsContent(uiState = MyAttentionsUiState.Empty, actions = MyAttentionsActions())
                },
            )
        }
    }
}

@Preview(name = "Professional requests", showBackground = true)
@Composable
private fun ProfessionalRequestsPreview() {
    ProfessionalRequestsPreviewFrame(ProfessionalRequestsView.MY_ATTENTIONS)
}

@Preview(name = "Professional requests dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfessionalRequestsDarkPreview() {
    ProfessionalRequestsPreviewFrame(ProfessionalRequestsView.AVAILABLE)
}

@Preview(name = "Professional requests large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun ProfessionalRequestsLargeFontPreview() {
    ProfessionalRequestsPreviewFrame(ProfessionalRequestsView.MY_ATTENTIONS)
}
