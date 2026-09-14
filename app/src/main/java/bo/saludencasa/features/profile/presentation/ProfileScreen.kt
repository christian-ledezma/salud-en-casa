package bo.saludencasa.features.profile.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ProfileAvatar
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        uiState = uiState,
        onFormChange = viewModel::onFormChange,
        onSaveClick = viewModel::save,
        onRetryClick = viewModel::load,
        modifier = modifier,
    )
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onFormChange: (ProfileForm) -> Unit,
    onSaveClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.cd_loading)

    when (uiState) {
        ProfileUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                )
            }
        }

        is ProfileUiState.Failed -> {
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
                    Text(
                        text = stringResource(uiState.error.messageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetryClick)
                }
            }
        }

        is ProfileUiState.Content -> {
            EditableProfile(
                content = uiState,
                onFormChange = onFormChange,
                onSaveClick = onSaveClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun EditableProfile(
    content: ProfileUiState.Content,
    onFormChange: (ProfileForm) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = content.form
    val fieldError = (content.status as? SaveStatus.Failed)?.error

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin, vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        ProfileAvatar(
            photoUrl = content.header.photoUrl,
            contentDescription = stringResource(R.string.cd_profile_photo),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        content.header.role?.let { role ->
            Text(
                text = stringResource(role.labelRes()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        FormField(
            label = stringResource(R.string.profile_full_name_label),
            value = form.fullName,
            onValueChange = { onFormChange(form.copy(fullName = it)) },
            errorText = fieldError.messageFor(ProfileError.InvalidName),
        )

        FormField(
            label = stringResource(R.string.profile_phone_label),
            value = form.phone,
            onValueChange = { onFormChange(form.copy(phone = it)) },
            errorText = fieldError.messageFor(ProfileError.InvalidPhone),
        )

        if (content.header.role == UserRole.PATIENT) {
            BirthDateField(
                birthDate = form.birthDate,
                errorText = fieldError.messageFor(ProfileError.InvalidBirthDate),
                onBirthDateChange = { onFormChange(form.copy(birthDate = it)) },
            )

            FormField(
                label = stringResource(R.string.profile_emergency_contact_label),
                value = form.emergencyContact,
                onValueChange = { onFormChange(form.copy(emergencyContact = it)) },
            )

            FormField(
                label = stringResource(R.string.profile_medical_notes_label),
                value = form.medicalNotes,
                onValueChange = { onFormChange(form.copy(medicalNotes = it)) },
                singleLine = false,
            )
        }

        SaveSection(status = content.status, onSaveClick = onSaveClick)
    }
}

@Composable
private fun SaveSection(
    status: SaveStatus,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val savingDescription = stringResource(R.string.cd_loading)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (status is SaveStatus.Saved) {
            Text(
                text = stringResource(R.string.profile_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val generalError = (status as? SaveStatus.Failed)?.error?.takeIf { !it.isAboutAField() }
        if (generalError != null) {
            Text(
                text = stringResource(generalError.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (status is SaveStatus.Saving) {
            CircularProgressIndicator(
                modifier = Modifier.semantics { contentDescription = savingDescription },
            )
        } else {
            PrimaryButton(text = stringResource(R.string.profile_save), onClick = onSaveClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateField(
    birthDate: LocalDate?,
    errorText: String?,
    onBirthDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.labelToField),
    ) {
        Text(
            text = stringResource(R.string.profile_birth_date_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { pickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = birthDate?.format(formatter) ?: stringResource(R.string.profile_birth_date_unset),
            )
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (!pickerOpen) return

    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = birthDate?.toUtcMillis(),
            selectableDates = PastDatesOnly,
        )

    DatePickerDialog(
        onDismissRequest = { pickerOpen = false },
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.toLocalDate()?.let(onBirthDateChange)
                    pickerOpen = false
                },
            ) {
                Text(text = stringResource(R.string.common_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = { pickerOpen = false }) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

// The picker works in UTC midnight, so the conversion has to use UTC in both
// directions or a birth date shifts by a day for anyone west of Greenwich.
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
private object PastDatesOnly : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis.toLocalDate().isBefore(LocalDate.now())

    override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
}

@StringRes
private fun UserRole.labelRes(): Int =
    when (this) {
        UserRole.PATIENT -> R.string.profile_role_patient
        UserRole.PROFESSIONAL -> R.string.profile_role_professional
        UserRole.ADMIN -> R.string.profile_role_admin
    }

private fun ProfileError.isAboutAField(): Boolean =
    this == ProfileError.InvalidName ||
        this == ProfileError.InvalidPhone ||
        this == ProfileError.InvalidBirthDate

@Composable
private fun ProfileError?.messageFor(field: ProfileError): String? =
    if (this == field) stringResource(messageRes()) else null

private fun previewContent(
    role: UserRole = UserRole.PATIENT,
    status: SaveStatus = SaveStatus.Idle,
): ProfileUiState.Content =
    ProfileUiState.Content(
        header = ProfileHeader(photoUrl = null, role = role),
        form =
            ProfileForm(
                fullName = "Ana Quispe",
                phone = "+59171234567",
                birthDate = LocalDate.of(1990, 5, 14),
                emergencyContact = "Luis Quispe, 71234568",
                medicalNotes = "Hipertensión controlada.",
            ),
        status = status,
    )

@Preview(showBackground = true, name = "Perfil de paciente, claro")
@Composable
private fun ProfilePatientLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ProfileContent(previewContent(), {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Perfil de paciente, oscuro")
@Composable
private fun ProfilePatientDarkPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        ProfileContent(previewContent(), {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Perfil de profesional")
@Composable
private fun ProfileProfessionalPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ProfileContent(previewContent(role = UserRole.PROFESSIONAL), {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Perfil con telefono invalido")
@Composable
private fun ProfileInvalidPhonePreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ProfileContent(previewContent(status = SaveStatus.Failed(ProfileError.InvalidPhone)), {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Perfil cargando")
@Composable
private fun ProfileLoadingPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ProfileContent(ProfileUiState.Loading, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Perfil que no se pudo cargar")
@Composable
private fun ProfileFailedPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        ProfileContent(ProfileUiState.Failed(ProfileError.NetworkUnavailable), {}, {}, {})
    }
}
