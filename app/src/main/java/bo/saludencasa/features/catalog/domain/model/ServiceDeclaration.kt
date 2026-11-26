package bo.saludencasa.features.catalog.domain.model

import bo.saludencasa.core.vo.AmountBob

data class ServiceDeclaration(
    val serviceTypeId: String,
    val price: AmountBob,
)
