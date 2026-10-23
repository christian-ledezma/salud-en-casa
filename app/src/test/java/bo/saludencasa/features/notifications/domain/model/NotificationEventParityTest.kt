package bo.saludencasa.features.notifications.domain.model

import bo.saludencasa.ProjectSources
import org.junit.Assert.assertEquals
import org.junit.Test

// The event names live twice: in request_push_body, which decides what is
// sent, and here, which decides what the phone can show. An event added on one
// side only is a notification that is sent and silently discarded, or a
// branch that never fires. This reads the migration so that fails the build.
class NotificationEventParityTest {
    @Test
    fun theEventsTheEngineSendsAreTheEventsThePhoneUnderstands() {
        val defining =
            ProjectSources
                .migrationFiles()
                .map { it.readText() }
                .lastOrNull { it.contains("function public.request_push_body") }
        checkNotNull(defining) { "No migration defines request_push_body." }

        val sent = eventBranch.findAll(defining).map { it.groupValues[1] }.toSet()

        assertEquals(NotificationEvent.entries.map { it.name }.toSet(), sent)
    }

    private companion object {
        val eventBranch = Regex("""p_event = '([A-Z_]+)'""")
    }
}
