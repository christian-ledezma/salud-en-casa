package bo.saludencasa.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListRefresh(
    val refreshing: Boolean = false,
    val failed: Boolean = false,
)

// docs/decisions.md, 2026-11-04, HT-27, a failed pull says so and a resume does not.
class ListRefresher(
    private val scope: CoroutineScope,
) {
    private val current = MutableStateFlow(ListRefresh())
    val state: StateFlow<ListRefresh> = current.asStateFlow()

    fun pull(read: suspend () -> Boolean) {
        if (current.value.refreshing) return
        current.value = ListRefresh(refreshing = true)
        scope.launch { current.value = ListRefresh(failed = !read()) }
    }

    fun clearFailure() {
        current.update { it.copy(failed = false) }
    }
}
