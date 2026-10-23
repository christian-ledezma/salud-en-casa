package bo.saludencasa.features.notifications.presentation

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import bo.saludencasa.ProjectSources
import bo.saludencasa.features.notifications.domain.model.NotificationEvent
import bo.saludencasa.features.notifications.everyPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            val public = payload.publicText(resources)
            assertFalse(public.title.contains(payload.subject.serviceTypeName))
            assertFalse(public.body.contains(payload.subject.serviceTypeName))
            assertTrue(payload.text(resources, locale).body.contains(payload.subject.serviceTypeName))
        }
    }
}
