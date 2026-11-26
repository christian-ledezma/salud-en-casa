package bo.saludencasa.features.catalog.domain.model

import java.text.Normalizer

private val COMBINING_MARKS = Regex("\\p{M}+")

// Tildes and capitals do not count: a keyboard without Spanish accents has to
// find "médica" by typing "medica" (docs/decisions.md, 2026-11-25, HT-32).
fun <T> List<T>.matchingName(
    query: String,
    nameOf: (T) -> String,
): List<T> {
    val needle = query.searchable()
    if (needle.isEmpty()) return this

    return filter { nameOf(it).searchable().contains(needle) }
}

private fun String.searchable(): String =
    Normalizer
        .normalize(trim(), Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase()
