package bo.saludencasa.features.verification.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfileRoles
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.verification.domain.model.DocumentType
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import bo.saludencasa.features.verification.domain.model.VerificationChecklist
import bo.saludencasa.features.verification.domain.model.VerificationDocument
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File

@Composable
fun VerificationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerificationViewModel = koinViewModel(),
    compressor: DocumentImageCompressor = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Across process death the Uri the camera is writing to has to survive, or
    // the picture lands in a file nothing points at any more. rememberSaveable
    // of a String is enough; the DocumentType string travels alongside it so
    // the result reaches the right slot.
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraType by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf<DocumentType?>(null) }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
            val type = pendingCameraType?.let(DocumentType::valueOf)
            val uri = pendingCameraUri?.let(Uri::parse)
            pendingCameraType = null
            pendingCameraUri = null
            if (!captured || type == null || uri == null) return@rememberLauncherForActivityResult
            scope.launch { compressAndHandoff(compressor, uri, type, viewModel) }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            val type = picking
            picking = null
            if (uri == null || type == null) return@rememberLauncherForActivityResult
            scope.launch { compressAndHandoff(compressor, uri, type, viewModel) }
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val type = picking ?: return@rememberLauncherForActivityResult
            if (!granted) {
                picking = null
                return@rememberLauncherForActivityResult
            }
            val uri = newCaptureUri(context, type)
            pendingCameraType = type.name
            pendingCameraUri = uri.toString()
            picking = null
            cameraLauncher.launch(uri)
        }

    VerificationContent(
        uiState = uiState,
        onBack = onBack,
        onRetryLoad = viewModel::load,
        onCaptionChanged = viewModel::onCaptionChanged,
        onPickType = { type -> picking = type },
        onDismissPicker = { picking = null },
        onCameraRequested = { type ->
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
            if (granted) {
                val uri = newCaptureUri(context, type)
                pendingCameraType = type.name
                pendingCameraUri = uri.toString()
                picking = null
                cameraLauncher.launch(uri)
            } else {
                picking = type
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onGalleryRequested = { type ->
            picking = type
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onRetry = viewModel::onRetry,
        picking = picking,
        modifier = modifier,
    )
}

private suspend fun compressAndHandoff(
    compressor: DocumentImageCompressor,
    uri: Uri,
    type: DocumentType,
    viewModel: VerificationViewModel,
) {
    when (val result = compressor.compress(uri)) {
        is DocumentImageCompressor.Result.Success -> {
            viewModel.onImageChosen(type, result.bytes)
        }

        DocumentImageCompressor.Result.Unreadable -> {
            viewModel.onCompressionFailed(VerificationError.UnreadableImage)
        }

        DocumentImageCompressor.Result.TooLarge -> {
            viewModel.onCompressionFailed(VerificationError.ImageTooLarge)
        }
    }
}

private fun newCaptureUri(
    context: android.content.Context,
    type: DocumentType,
): Uri {
    // One file per type kept in the application's cache. The camera overwrites
    // the previous take, and the cache is cleaned up by the system; the storage
    // bucket is the durable copy.
    val dir = File(context.cacheDir, "verification").apply { mkdirs() }
    val file =
        File(dir, "capture-${type.name}.jpg").apply {
            if (!exists()) createNewFile()
        }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
private fun VerificationContent(
    uiState: VerificationUiState,
    onBack: () -> Unit,
    onRetryLoad: () -> Unit,
    onCaptionChanged: (String) -> Unit,
    onPickType: (DocumentType) -> Unit,
    onDismissPicker: () -> Unit,
    onCameraRequested: (DocumentType) -> Unit,
    onGalleryRequested: (DocumentType) -> Unit,
    onRetry: (DocumentType) -> Unit,
    picking: DocumentType?,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    when (uiState) {
        VerificationUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        is VerificationUiState.Failed -> {
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

        is VerificationUiState.Content -> {
            VerificationChecklistList(
                content = uiState,
                onCaptionChanged = onCaptionChanged,
                onPickType = onPickType,
                onRetry = onRetry,
                modifier = modifier,
            )

            picking?.let { type ->
                SourcePickerDialog(
                    type = type,
                    onCamera = { onCameraRequested(type) },
                    onGallery = { onGalleryRequested(type) },
                    onDismiss = onDismissPicker,
                )
            }
        }
    }
}

@Composable
private fun CenteredColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
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
            content = content,
        )
    }
}

@Composable
private fun VerificationChecklistList(
    content: VerificationUiState.Content,
    onCaptionChanged: (String) -> Unit,
    onPickType: (DocumentType) -> Unit,
    onRetry: (DocumentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = Spacing.screenMargin,
                vertical = Spacing.sectionGap,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                Text(
                    text = stringResource(R.string.verification_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.verification_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                content.notice?.let { error ->
                    Text(
                        text = stringResource(error.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        items(content.checklist.required, key = { "required:${it.name}" }) { type ->
            DocumentRow(
                type = type,
                document = content.checklist.documentFor(type),
                isPending = content.pendingType == type,
                hasStagedBytes = content.stagedBytes.containsKey(type),
                onPickClick = { onPickType(type) },
                onRetryClick = { onRetry(type) },
            )
        }

        item(key = "other-section") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                Text(
                    text = stringResource(R.string.verification_other_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.verification_other_section_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FormField(
                    label = stringResource(R.string.verification_other_caption_label),
                    value = content.otherCaption,
                    onValueChange = onCaptionChanged,
                    singleLine = true,
                )
            }
        }

        item(key = "other-row") {
            DocumentRow(
                type = DocumentType.OTHER,
                document = content.checklist.documentFor(DocumentType.OTHER),
                isPending = content.pendingType == DocumentType.OTHER,
                hasStagedBytes = content.stagedBytes.containsKey(DocumentType.OTHER),
                onPickClick = { onPickType(DocumentType.OTHER) },
                onRetryClick = { onRetry(DocumentType.OTHER) },
            )
        }
    }
}

@Composable
private fun DocumentRow(
    type: DocumentType,
    document: VerificationDocument?,
    isPending: Boolean,
    hasStagedBytes: Boolean,
    onPickClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            // FlowRow, not Row: a long title ("Documento de identidad, anverso")
            // squeezes the status chip to one word per line in a plain SpaceBetween
            // Row. Here the chip drops to the next line instead of stacking.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale8, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
            ) {
                Text(
                    text = stringResource(type.titleRes()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                document?.status?.let { status ->
                    StatusBadge(status = status)
                }
            }

            Text(
                text = stringResource(type.descriptionRes()),
                style = MaterialTheme.typography.bodyMedium,
            )

            document?.rejectionReason?.let { reason ->
                Text(
                    text = stringResource(R.string.verification_rejection_reason_prefix, reason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            document?.caption?.let { caption ->
                Text(
                    text = stringResource(R.string.verification_stored_caption_prefix, caption),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (isPending) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                ) {
                    // Review freezes the file in Storage (storage.objects
                    // policies require PENDING) and the row in the table too.
                    // REJECTED stays frozen in HU-07; HU-08 is where the
                    // re-upload path opens.
                    val isFrozen = document != null && document.status != ReviewStatus.PENDING
                    if (!isFrozen) {
                        TextButton(onClick = onPickClick) {
                            Text(
                                text =
                                    stringResource(
                                        if (document == null) {
                                            R.string.verification_action_pick
                                        } else {
                                            R.string.verification_action_replace
                                        },
                                    ),
                            )
                        }
                        if (hasStagedBytes) {
                            TextButton(onClick = onRetryClick) {
                                Text(text = stringResource(R.string.verification_action_retry_upload))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ReviewStatus) {
    val color =
        when (status) {
            ReviewStatus.APPROVED -> SaludEnCasaTheme.statusColors.positive
            ReviewStatus.PENDING -> SaludEnCasaTheme.statusColors.pending
            ReviewStatus.REJECTED -> SaludEnCasaTheme.statusColors.negative
        }
    Text(
        text = stringResource(status.badgeRes()),
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

@Composable
private fun SourcePickerDialog(
    type: DocumentType,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(type.titleRes())) },
        text = {
            // Buttons live inside the text slot: AlertDialog's confirm and
            // dismiss slots take one each, and HU-07 offers two paths that are
            // both affirmative. The dismiss button at the bottom cancels.
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.scale8)) {
                Text(
                    text = stringResource(R.string.verification_source_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                PrimaryButton(
                    text = stringResource(R.string.verification_source_camera),
                    onClick = onCamera,
                )
                OutlinedButton(
                    onClick = onGallery,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.verification_source_gallery))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

private fun previewRoles(vararg roles: UserRole): ProfileRoles =
    ProfileRoles.create(roles.toSet(), roles.firstOrNull()).getOrThrow()

private fun previewChecklist(
    roles: ProfileRoles,
    type: ProfessionalType?,
    documents: List<VerificationDocument> = emptyList(),
): VerificationChecklist = VerificationChecklist.create(roles, type, documents)

private fun previewContent(
    roles: ProfileRoles = previewRoles(UserRole.PATIENT),
    professionalType: ProfessionalType? = null,
    documents: List<VerificationDocument> = emptyList(),
    pendingType: DocumentType? = null,
    notice: VerificationError? = null,
    caption: String = "",
): VerificationUiState.Content =
    VerificationUiState.Content(
        checklist = previewChecklist(roles, professionalType, documents),
        otherCaption = caption,
        pendingType = pendingType,
        stagedBytes = emptyMap(),
        notice = notice,
    )

@Preview(showBackground = true, heightDp = 1100, name = "Verificacion paciente, claro")
@Composable
private fun VerificationPatientLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        VerificationContent(
            uiState = previewContent(),
            onBack = {},
            onRetryLoad = {},
            onCaptionChanged = {},
            onPickType = {},
            onDismissPicker = {},
            onCameraRequested = {},
            onGalleryRequested = {},
            onRetry = {},
            picking = null,
        )
    }
}

@Preview(showBackground = true, heightDp = 1100, name = "Verificacion profesional, oscuro")
@Composable
private fun VerificationProfessionalDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        VerificationContent(
            uiState =
                previewContent(
                    roles = previewRoles(UserRole.PROFESSIONAL),
                    professionalType = ProfessionalType.DOCTOR,
                    documents =
                        listOf(
                            VerificationDocument(
                                type = DocumentType.ID_FRONT,
                                status = ReviewStatus.APPROVED,
                                storagePath = "x/ID_FRONT.jpg",
                                caption = null,
                                rejectionReason = null,
                            ),
                            VerificationDocument(
                                type = DocumentType.DEGREE,
                                status = ReviewStatus.REJECTED,
                                storagePath = "x/DEGREE.jpg",
                                caption = null,
                                rejectionReason = "La foto está borrosa.",
                            ),
                        ),
                ),
            onBack = {},
            onRetryLoad = {},
            onCaptionChanged = {},
            onPickType = {},
            onDismissPicker = {},
            onCameraRequested = {},
            onGalleryRequested = {},
            onRetry = {},
            picking = null,
        )
    }
}

@Preview(showBackground = true, heightDp = 1600, fontScale = 2f, name = "Verificacion al 200 %")
@Composable
private fun VerificationLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        VerificationContent(
            uiState =
                previewContent(
                    roles = previewRoles(UserRole.PATIENT, UserRole.PROFESSIONAL),
                    professionalType = ProfessionalType.NURSE,
                    documents =
                        listOf(
                            VerificationDocument(
                                type = DocumentType.SELFIE,
                                status = ReviewStatus.PENDING,
                                storagePath = "x/SELFIE.jpg",
                                caption = null,
                                rejectionReason = null,
                            ),
                        ),
                    caption = "Carnet de colegio de enfermería 2024",
                ),
            onBack = {},
            onRetryLoad = {},
            onCaptionChanged = {},
            onPickType = {},
            onDismissPicker = {},
            onCameraRequested = {},
            onGalleryRequested = {},
            onRetry = {},
            picking = null,
        )
    }
}

@Preview(showBackground = true, name = "Verificacion con fallo de red")
@Composable
private fun VerificationFailedPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        VerificationContent(
            uiState = VerificationUiState.Failed(VerificationError.NetworkUnavailable),
            onBack = {},
            onRetryLoad = {},
            onCaptionChanged = {},
            onPickType = {},
            onDismissPicker = {},
            onCameraRequested = {},
            onGalleryRequested = {},
            onRetry = {},
            picking = null,
        )
    }
}

@Preview(showBackground = true, name = "Verificacion cargando")
@Composable
private fun VerificationLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        VerificationContent(
            uiState = VerificationUiState.Loading,
            onBack = {},
            onRetryLoad = {},
            onCaptionChanged = {},
            onPickType = {},
            onDismissPicker = {},
            onCameraRequested = {},
            onGalleryRequested = {},
            onRetry = {},
            picking = null,
        )
    }
}

@Preview(showBackground = true, heightDp = 500, name = "Verificacion eligiendo fuente")
@Composable
private fun VerificationSourcePickerPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        VerificationContent(
            uiState = previewContent(),
            onBack = {},
            onRetryLoad = {},
            onCaptionChanged = {},
            onPickType = {},
            onDismissPicker = {},
            onCameraRequested = {},
            onGalleryRequested = {},
            onRetry = {},
            picking = DocumentType.ID_FRONT,
        )
    }
}
