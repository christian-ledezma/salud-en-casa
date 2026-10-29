package bo.saludencasa.features.requests.presentation

import bo.saludencasa.features.location.domain.model.Address

// Which address stays chosen when the list is read again on coming back to a
// form. The person left to add one, so the one just added is the one they mean;
// otherwise what they had chosen stays, and if it is gone, the primary address,
// which is the home where they receive care (RN-02). docs/decisions.md,
// 2026-10-27, HT-24.
internal fun List<Address>.selectionAfterRefresh(
    previous: List<Address>,
    previousSelectedId: String,
): String {
    val added = filter { fresh -> previous.none { it.id == fresh.id } }
    return added.singleOrNull()?.id
        ?: previousSelectedId.takeIf { id -> any { it.id == id } }
        ?: firstOrNull { it.isPrimary }?.id
        ?: first().id
}
