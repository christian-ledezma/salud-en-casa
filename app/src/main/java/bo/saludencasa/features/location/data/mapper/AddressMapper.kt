package bo.saludencasa.features.location.data.mapper

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.location.data.model.AddressDto
import bo.saludencasa.features.location.data.model.AddressRow
import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.location.domain.model.AddressUpdate
import java.util.Locale

internal fun AddressDto.toAddress(): Address? {
    val coordinate = Coordinate.create(latitude, longitude).getOrNull() ?: return null
    return Address(
        id = id,
        alias = alias,
        addressText = addressText,
        reference = reference?.trim()?.takeIf(String::isNotBlank),
        city = city,
        coordinate = coordinate,
        isPrimary = isPrimary,
        isProfessionalBase = isProfessionalBase,
    )
}

internal fun AddressUpdate.toAddressRow(profileId: String): AddressRow =
    AddressRow(
        profileId = profileId,
        alias = alias.value,
        addressText = addressText.value,
        reference = reference?.value,
        city = city.value,
        location = coordinate.toEwkt(),
    )

// PostGIS reads the point as longitude first and latitude second. Inverting the
// two saves without error and puts the address in another hemisphere, which is
// the failure .claude/rules/supabase.md warns about because nothing reports it.
//
// The numbers are written with a fixed decimal count and the root locale on
// purpose: a device set to Spanish would otherwise format them with a comma,
// and a coordinate close to zero would leave in scientific notation. Neither is
// text PostGIS parses.
internal fun Coordinate.toEwkt(): String = "SRID=4326;POINT(${format(longitude)} ${format(latitude)})"

private const val STORED_DECIMALS = 7

private fun format(value: Double): String = String.format(Locale.ROOT, "%.${STORED_DECIMALS}f", value)
