package bo.saludencasa.features.notifications.presentation

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.ProjectSources
import bo.saludencasa.features.notifications.attentionCancelled
import bo.saludencasa.features.notifications.documentVerdict
import bo.saludencasa.features.notifications.domain.model.NotificationEvent
import bo.saludencasa.features.notifications.everyPayload
import bo.saludencasa.features.notifications.professionalArrived
import bo.saludencasa.features.notifications.serviceCompleted
import bo.saludencasa.features.notifications.verificationApproved
import bo.saludencasa.features.verification.domain.model.ReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class NotificationContentTest {
    // values/ has no language of its own; a language no folder declares is
    // what makes the phone fall back to it.
    private fun localesUnderTest(): List<Locale> =
        ProjectSources.localeStringsXmlFiles().map { file ->
            when (
                val qualifier =
                    file.parentFile!!
                        .name
                        .removePrefix("values")
                        .removePrefix("-")
            ) {
                "" -> Locale.JAPANESE
                else -> Locale.forLanguageTag(qualifier.replace("-r", "-"))
            }
        }

    private fun contextIn(locale: Locale): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(configuration)
    }

    // The phone composes the text (RF-09.10), so an event without its strings
    // in some language arrives as a blank or half-formatted notification.
    @Test
    fun everyNotificationEventHasItsTextInAllLocales() {
        assertEquals(NotificationEvent.entries.toSet(), everyPayload().map { it.event }.toSet())
        localesUnderTest().forEach { locale ->
            val resources = contextIn(locale).resources
            everyPayload().forEach { payload ->
                val texts = listOf(payload.text(resources, locale), payload.publicText(resources))
                texts.flatMap { listOf(it.title, it.body) }.forEach { text ->
                    assertTrue("${payload.event} in $locale is blank", text.isNotBlank())
                    assertFalse("${payload.event} in $locale left a placeholder: $text", text.contains('%'))
                }
            }
        }
    }

    // docs/decisions.md, 2026-10-16: the service type can say what is wrong
    // with the patient, and a locked phone may be in anyone's hands.
    @Test
    fun theLockScreenVersionNeverShowsTheServiceType() {
        val locale = Locale.forLanguageTag("es")
        val resources = contextIn(locale).resources
        everyPayload().forEach { payload ->
            // HU-37's two events carry no service type: there is nothing of the
            // kind to check for either version of their text.
            val type = payload.subject.serviceTypeName ?: return@forEach
            val public = payload.publicText(resources)
            assertFalse(public.title.contains(type))
            assertFalse(public.body.contains(type))
            assertTrue(payload.text(resources, locale).body.contains(type))
        }
    }

    // A channel is what the person silences: the progress of an attention must not
    // go quiet along with the requests (docs/decisions.md, 2026-11-04, HU-23).
    @Test
    fun theProgressOfAnAttentionHasItsOwnChannel() {
        assertEquals(NotificationChannelSpec.ATTENTIONS, professionalArrived().channel())
        assertEquals(NotificationChannelSpec.ATTENTIONS, serviceCompleted().channel())
        assertEquals(NotificationChannelSpec.ATTENTIONS, attentionCancelled().channel())
    }

    // Account-level, unlike every channel above, which all follow a request or
    // a service (docs/decisions.md, 2026-11-05, HU-37).
    @Test
    fun verificationEventsHaveTheirOwnChannel() {
        assertEquals(NotificationChannelSpec.VERIFICATION, documentVerdict().channel())
        assertEquals(NotificationChannelSpec.VERIFICATION, verificationApproved().channel())
    }

    // The body says how many, in Spanish grammar, and never the administrator's
    // reason (RF-09.10): rejectsADocumentVerdictWithThePendingStatus and
    // aDocumentVerdictKeepsNoReasonEvenIfTheWireCarriedIt cover the reason on
    // the parsing side.
    @Test
    fun theDocumentVerdictBodyNamesTheCountAndTheVerdict() {
        val locale = Locale.forLanguageTag("es")
        val resources = contextIn(locale).resources

        val oneApproved = documentVerdict(status = ReviewStatus.APPROVED, documentCount = 1)
        val threeApproved = documentVerdict(status = ReviewStatus.APPROVED, documentCount = 3)
        val oneRejected = documentVerdict(status = ReviewStatus.REJECTED, documentCount = 1)

        assertTrue(oneApproved.text(resources, locale).body.contains("documento"))
        assertFalse(oneApproved.text(resources, locale).body.contains("documentos"))
        assertTrue(threeApproved.text(resources, locale).body.contains("3"))
        assertNotEquals(oneApproved.text(resources, locale).body, oneRejected.text(resources, locale).body)
    }
}
