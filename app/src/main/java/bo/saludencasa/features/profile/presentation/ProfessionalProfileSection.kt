package bo.saludencasa.features.profile.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import bo.saludencasa.R
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.ui.components.FormField
import bo.saludencasa.ui.components.RadioOptionGroup
import bo.saludencasa.ui.theme.Spacing

@Composable
internal fun ProfessionalProfileSection(
    form: ProfileForm,
    availability: AvailabilityState,
    fieldError: ProfileError?,
    onFormChange: (ProfileForm) -> Unit,
    onAvailabilityChange: (Boolean) -> Unit,
    onOpenPublicProfile: () -> Unit,
    onOpenMyServices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeLabels = ProfessionalType.entries.associateWith { stringResource(it.labelRes()) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        RadioOptionGroup(
            label = stringResource(R.string.profile_professional_type_label),
            options = ProfessionalType.entries,
            selected = form.professionalType,
            onSelectedChange = { onFormChange(form.copy(professionalType = it)) },
            optionLabel = { typeLabels.getValue(it) },
        )

        FormField(
            label = stringResource(R.string.profile_specialty_label),
            value = form.specialty,
            onValueChange = { onFormChange(form.copy(specialty = it)) },
        )

        FormField(
            label = stringResource(R.string.profile_biography_label),
            value = form.biography,
            onValueChange = { onFormChange(form.copy(biography = it)) },
            singleLine = false,
        )

        FormField(
            label = stringResource(R.string.profile_years_of_experience_label),
            value = form.yearsOfExperience,
            onValueChange = { onFormChange(form.copy(yearsOfExperience = it)) },
            errorText = fieldError.messageFor(ProfileError.InvalidYearsOfExperience),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        FormField(
            label = stringResource(R.string.profile_base_rate_label),
            value = form.baseRateBob,
            onValueChange = { onFormChange(form.copy(baseRateBob = it)) },
            errorText = fieldError.messageFor(ProfileError.InvalidBaseRate),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        FormField(
            label = stringResource(R.string.profile_coverage_radius_label),
            value = form.coverageRadiusKm,
            onValueChange = { onFormChange(form.copy(coverageRadiusKm = it)) },
            errorText = fieldError.messageFor(ProfileError.InvalidCoverageRadius),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        AvailabilitySwitch(availability = availability, onAvailabilityChange = onAvailabilityChange)

        OutlinedButton(onClick = onOpenMyServices, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.profile_open_my_services))
        }

        OutlinedButton(onClick = onOpenPublicProfile, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.profile_open_public_profile))
        }
    }
}

@Composable
private fun AvailabilitySwitch(
    availability: AvailabilityState,
    onAvailabilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = availability.availableNow,
                        role = Role.Switch,
                        onValueChange = onAvailabilityChange,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_available_now_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.profile_available_now_description),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = availability.availableNow, onCheckedChange = null)
        }

        if (availability.error != null) {
            Text(
                text = stringResource(availability.error.messageRes()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@StringRes
internal fun ProfessionalType.labelRes(): Int =
    when (this) {
        ProfessionalType.DOCTOR -> R.string.profile_professional_type_doctor
        ProfessionalType.NURSE -> R.string.profile_professional_type_nurse
        ProfessionalType.PHYSIOTHERAPIST -> R.string.profile_professional_type_physiotherapist
        ProfessionalType.STUDENT -> R.string.profile_professional_type_student
    }
