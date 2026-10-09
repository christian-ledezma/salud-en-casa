package bo.saludencasa.features.verification.presentation

import android.icu.text.ListFormatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.presentation.labelRes
import bo.saludencasa.features.verification.domain.model.DocumentReviewDossier
import bo.saludencasa.features.verification.domain.model.DocumentReviewSubject
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationChecklist
import bo.saludencasa.features.verification.domain.model.VerificationDocument
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val DOCUMENT_ASPECT_RATIO = 4f / 3f

@Composable
fun DocumentReviewScreen(
    profileId: String,
    documentType: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentReviewViewModel =
        koinViewModel { parametersOf(profileId, DocumentType.valueOf(documentType)) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DocumentReviewContent(
        uiState = uiState,
        onBack = onBack,
        onRetryLoad = viewModel::load,
        actions =
            ReviewActions(
                onSelect = viewModel::onSelect,
                onRetryImage = viewModel::onRetryImage,
                onApprove = viewModel::onApprove,
                onRejectClick = viewModel::onRejectClick,
                onRejectReasonChanged = viewModel::onRejectReasonChanged,
                onRejectDismiss = viewModel::onRejectDismiss,
                onRejectConfirm = viewModel::onRejectConfirm,
                onApproveProfessionalClick = viewModel::onApproveProfessionalClick,
                onApproveProfessionalDismiss = viewModel::onApproveProfessionalDismiss,
                onApproveProfessionalConfirm = viewModel::onApproveProfessionalConfirm,
            ),
        modifier = modifier,
    )
}

internal data class ReviewActions(
    val onSelect: (DocumentType) -> Unit = {},
    val onRetryImage: () -> Unit = {},
    val onApprove: () -> Unit = {},
    val onRejectClick: () -> Unit = {},
    val onRejectReasonChanged: (String) -> Unit = {},
    val onRejectDismiss: () -> Unit = {},
    val onRejectConfirm: () -> Unit = {},
    val onApproveProfessionalClick: () -> Unit = {},
    val onApproveProfessionalDismiss: () -> Unit = {},
    val onApproveProfessionalConfirm: () -> Unit = {},
)

@Composable
internal fun DocumentReviewContent(
    uiState: DocumentReviewUiState,
    onBack: () -> Unit,
    onRetryLoad: () -> Unit,
    actions: ReviewActions,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    when (uiState) {
        DocumentReviewUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        is DocumentReviewUiState.Failed -> {
            CenteredColumn(modifier = modifier) {
                Text(
                    text = stringResource(uiState.error.messageRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetryLoad)
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            }
        }

        is DocumentReviewUiState.Content -> {
            ReviewBody(content = uiState, actions = actions, modifier = modifier)
            RejectDialog(content = uiState, actions = actions)
            ApproveProfessionalDialog(content = uiState, actions = actions)
        }
    }
}

@Composable
private fun ReviewBody(
    content: DocumentReviewUiState.Content,
    actions: ReviewActions,
    modifier: Modifier = Modifier,
) {
    val checklist = content.dossier.checklist
    val selected = checklist.documentFor(content.selectedType)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        Text(
            text = stringResource(R.string.review_detail_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        SubjectCard(subject = content.dossier.subject)

        Text(
            text = stringResource(R.string.review_documents_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        content.dossier.reviewableTypes.forEach { type ->
            DocumentChoiceRow(
                type = type,
                document = checklist.documentFor(type),
                isSelected = type == content.selectedType,
                onClick = { actions.onSelect(type) },
            )
        }

        DocumentImage(image = content.image, onRetry = actions.onRetryImage)

        selected?.let { document ->
            document.caption?.let { caption ->
                Text(
                    text = stringResource(R.string.verification_stored_caption_prefix, caption),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            document.rejectionReason?.let { reason ->
                Text(
                    text = stringResource(R.string.verification_rejection_reason_prefix, reason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            VerdictButtons(document = document, busy = content.busy, actions = actions)
        }

        if (content.dossier.canApproveProfessional) {
            PrimaryButton(
                text = stringResource(R.string.review_action_approve_professional),
                onClick = actions.onApproveProfessionalClick,
                enabled = !content.busy,
            )
        }

        content.notice?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SubjectCard(subject: DocumentReviewSubject) {
    val roleLabels = subject.heldRoles.sorted().map { stringResource(it.labelRes()) }
    val roles = ListFormatter.getInstance().format(roleLabels)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Text(text = subject.fullName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.review_subject_email, subject.email),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.review_subject_roles, roles),
                style = MaterialTheme.typography.bodyMedium,
            )
            subject.professionalType?.let { type ->
                Text(
                    text = stringResource(R.string.review_subject_professional_type, stringResource(type.labelRes())),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            subject.professionalStatus?.let { status ->
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.review_subject_professional_status,
                                stringResource(status.badgeRes()),
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    StatusBadge(status = status)
                }
            }
        }
    }
}

@Composable
private fun DocumentChoiceRow(
    type: DocumentType,
    document: VerificationDocument?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        selected = isSelected,
        onClick = onClick,
        enabled = document != null,
        modifier = Modifier.fillMaxWidth().semantics { role = Role.RadioButton },
        shape = MaterialTheme.shapes.medium,
        color =
            if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        contentColor =
            if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    ) {
        // FlowRow, as in DocumentRow, so a long title and the badge wrap.
        FlowRow(
            modifier = Modifier.padding(Spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Text(
                text = stringResource(type.titleRes()),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (document != null) {
                StatusBadge(status = document.status)
            } else {
                Text(
                    text = stringResource(R.string.review_document_missing),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun DocumentImage(
    image: DocumentImageState,
    onRetry: () -> Unit,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(DOCUMENT_ASPECT_RATIO)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when (image) {
            DocumentImageState.Missing -> {
                Text(
                    text = stringResource(R.string.review_document_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DocumentImageState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
            }

            is DocumentImageState.Failed -> {
                ImageError(onRetry = onRetry)
            }

            is DocumentImageState.Ready -> {
                val context = LocalContext.current
                // docs/decisions.md, 2026-10-06, review images are never cached on disk.
                val request =
                    remember(image.url) {
                        ImageRequest
                            .Builder(context)
                            .data(image.url)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build()
                    }
                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = stringResource(R.string.cd_document_image),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.semantics { contentDescription = loadingDescription },
                            )
                        }
                    },
                    error = { ImageError(onRetry = onRetry) },
                )
            }
        }
    }
}

@Composable
private fun ImageError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(Spacing.cardPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
    ) {
        Text(
            text = stringResource(R.string.review_image_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.common_retry))
        }
    }
}

@Composable
private fun VerdictButtons(
    document: VerificationDocument,
    busy: Boolean,
    actions: ReviewActions,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
    ) {
        if (document.status != ReviewStatus.APPROVED) {
            PrimaryButton(
                text = stringResource(R.string.review_action_approve),
                onClick = actions.onApprove,
                enabled = !busy,
            )
        }
        if (document.status != ReviewStatus.REJECTED) {
            OutlinedButton(onClick = actions.onRejectClick, enabled = !busy) {
                Text(text = stringResource(R.string.review_action_reject))
            }
        }
    }
}

@Composable
private fun RejectDialog(
    content: DocumentReviewUiState.Content,
    actions: ReviewActions,
) {
    val reason = content.rejectionDraft ?: return

    AlertDialog(
        onDismissRequest = actions.onRejectDismiss,
        title = { Text(text = stringResource(R.string.review_reject_dialog_title)) },
        text = {
            // Scrolls: at 200 % font size the supporting text, the field and the
            // error no longer fit a dialog (the HU-06 dialog clipped the same way).
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
            ) {
                Text(
                    text = stringResource(R.string.review_reject_dialog_supporting),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FormField(
                    label = stringResource(R.string.review_reject_reason_label),
                    value = reason,
                    onValueChange = actions.onRejectReasonChanged,
                    singleLine = false,
                    errorText = content.notice?.let { stringResource(it.messageRes()) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onRejectConfirm, enabled = !content.busy) {
                Text(text = stringResource(R.string.review_reject_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onRejectDismiss, enabled = !content.busy) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun ApproveProfessionalDialog(
    content: DocumentReviewUiState.Content,
    actions: ReviewActions,
) {
    if (!content.confirmingProfessionalApproval) return

    AlertDialog(
        onDismissRequest = actions.onApproveProfessionalDismiss,
        title = { Text(text = stringResource(R.string.review_approve_professional_dialog_title)) },
        text = {
            Text(
                text =
                    stringResource(
                        R.string.review_approve_professional_dialog_body,
                        content.dossier.subject.fullName,
                    ),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = actions.onApproveProfessionalConfirm, enabled = !content.busy) {
                Text(text = stringResource(R.string.review_approve_professional_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onApproveProfessionalDismiss, enabled = !content.busy) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

private fun previewDocument(
    type: DocumentType,
    status: ReviewStatus,
    reason: String? = null,
    caption: String? = null,
): VerificationDocument =
    VerificationDocument(
        type = type,
        status = status,
        storagePath = "00000000-0000-0000-0000-000000000001/${type.name}.jpg",
        caption = caption,
        rejectionReason = reason,
    )

private fun previewDossier(
    documents: List<VerificationDocument>,
    professionalStatus: ReviewStatus? = ReviewStatus.PENDING,
    name: String = "Ernesto Arancibia",
): DocumentReviewDossier {
    val subject =
        DocumentReviewSubject(
            profileId = "00000000-0000-0000-0000-000000000001",
            fullName = name,
            email = "ernesto.arancibia@example.com",
            heldRoles = setOf(UserRole.PROFESSIONAL),
            professionalType = ProfessionalType.NURSE,
            professionalStatus = professionalStatus,
        )
    return DocumentReviewDossier(
        subject = subject,
        checklist = VerificationChecklist.create(subject.heldRoles, subject.professionalType, documents),
    )
}

private fun previewContent(
    dossier: DocumentReviewDossier,
    selectedType: DocumentType = DocumentType.DEGREE,
    image: DocumentImageState = DocumentImageState.Loading,
    rejectionDraft: String? = null,
    confirmingProfessionalApproval: Boolean = false,
    notice: VerificationError? = null,
): DocumentReviewUiState.Content =
    DocumentReviewUiState.Content(
        dossier = dossier,
        selectedType = selectedType,
        image = image,
        busy = false,
        rejectionDraft = rejectionDraft,
        confirmingProfessionalApproval = confirmingProfessionalApproval,
        notice = notice,
    )

private val partlyReviewed =
    listOf(
        previewDocument(DocumentType.ID_FRONT, ReviewStatus.APPROVED),
        previewDocument(DocumentType.ID_BACK, ReviewStatus.APPROVED),
        previewDocument(DocumentType.DEGREE, ReviewStatus.PENDING),
        previewDocument(DocumentType.OTHER, ReviewStatus.PENDING, caption = "Diploma de posgrado"),
    )

private val allApproved =
    listOf(
        DocumentType.ID_FRONT,
        DocumentType.ID_BACK,
        DocumentType.SELFIE,
        DocumentType.DEGREE,
        DocumentType.LICENSE,
    ).map { previewDocument(it, ReviewStatus.APPROVED) }

@Preview(showBackground = true, heightDp = 1500, name = "Revision con documentos pendientes, claro")
@Composable
private fun ReviewLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val content = previewContent(previewDossier(partlyReviewed), image = DocumentImageState.Missing)
        DocumentReviewContent(content, {}, {}, ReviewActions())
    }
}

@Preview(showBackground = true, heightDp = 1500, name = "Revision lista para verificar, oscuro")
@Composable
private fun ReviewReadyDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        val content =
            previewContent(
                previewDossier(allApproved),
                image = DocumentImageState.Failed(VerificationError.NetworkUnavailable),
            )
        DocumentReviewContent(content, {}, {}, ReviewActions())
    }
}

@Preview(showBackground = true, heightDp = 2600, fontScale = 2f, name = "Revision al 200 %")
@Composable
private fun ReviewLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val documents = partlyReviewed + previewDocument(DocumentType.SELFIE, ReviewStatus.REJECTED, "Foto borrosa.")
        val content =
            previewContent(
                previewDossier(documents, name = "Maria del Carmen Condori de la Torre"),
                selectedType = DocumentType.SELFIE,
                image = DocumentImageState.Missing,
                notice = VerificationError.RequiredDocumentsNotApproved,
            )
        DocumentReviewContent(content, {}, {}, ReviewActions())
    }
}

@Preview(showBackground = true, heightDp = 900, fontScale = 2f, name = "Rechazo al 200 %")
@Composable
private fun ReviewRejectDialogLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        val content =
            previewContent(
                previewDossier(partlyReviewed),
                image = DocumentImageState.Missing,
                rejectionDraft = "La foto esta borrosa y no se lee el numero de matricula.",
                notice = VerificationError.InvalidRejectionReason,
            )
        DocumentReviewContent(content, {}, {}, ReviewActions())
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Confirmar verificacion, oscuro")
@Composable
private fun ReviewPromoteDialogDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        val content =
            previewContent(
                previewDossier(allApproved),
                image = DocumentImageState.Missing,
                confirmingProfessionalApproval = true,
            )
        DocumentReviewContent(content, {}, {}, ReviewActions())
    }
}

@Preview(showBackground = true, name = "Revision con fallo")
@Composable
private fun ReviewFailedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        DocumentReviewContent(
            DocumentReviewUiState.Failed(VerificationError.NetworkUnavailable),
            {},
            {},
            ReviewActions(),
        )
    }
}

@Preview(showBackground = true, name = "Revision cargando")
@Composable
private fun ReviewLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        DocumentReviewContent(DocumentReviewUiState.Loading, {}, {}, ReviewActions())
    }
}
