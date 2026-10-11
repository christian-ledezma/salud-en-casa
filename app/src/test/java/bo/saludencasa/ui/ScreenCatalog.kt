package bo.saludencasa.ui

import android.graphics.Bitmap
import android.view.View
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.reflect.getDeclaredComposableMethod
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.core.view.drawToBitmap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

// Not a test: it renders every screen preview to a PNG under app/build/screenshots,
// or the directory given as -Pscreenshots=<directory>,
// and the default unit test run excludes it (docs/decisions.md, 2026-10-09).
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenCatalog(
    private val className: String,
    private val methodName: String,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun record() {
        val method =
            Class
                .forName(className)
                .getDeclaredComposableMethod(methodName)
                .also { it.asMethod().isAccessible = true }
        lateinit var view: View
        composeRule.setContent {
            view = LocalView.current
            val density = LocalDensity.current
            val fontScale = if (methodName.contains("LargeFont")) 2f else 1f
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                method.invoke(currentComposer, null)
            }
        }
        composeRule.waitForIdle()

        val screen = className.substringAfterLast('.').removeSuffix("Kt")
        val output = File(OUTPUT_DIR, "${screen}__${methodName.removeSuffix("Preview")}.png")
        output.parentFile?.mkdirs()
        output.outputStream().use { view.rootView.drawToBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    companion object {
        private val OUTPUT_DIR = File(System.getProperty("screenshots.dir") ?: "build/screenshots")

        private val SCREEN_FILES =
            listOf(
                "bo.saludencasa.features.auth.presentation.StartupScreenKt",
                "bo.saludencasa.features.auth.presentation.WelcomeScreenKt",
                "bo.saludencasa.features.profile.presentation.RoleSelectionScreenKt",
                "bo.saludencasa.features.auth.presentation.AccountScreenKt",
                "bo.saludencasa.features.profile.presentation.ProfileScreenKt",
                "bo.saludencasa.features.profile.presentation.PublicProfileScreenKt",
                "bo.saludencasa.features.location.presentation.AddressListScreenKt",
                "bo.saludencasa.features.location.presentation.AddressScreenKt",
                "bo.saludencasa.features.verification.presentation.VerificationScreenKt",
                "bo.saludencasa.features.verification.presentation.DocumentReviewQueueScreenKt",
                "bo.saludencasa.features.verification.presentation.DocumentReviewScreenKt",
                "bo.saludencasa.features.verification.presentation.FullScreenDocumentImageKt",
                "bo.saludencasa.features.catalog.presentation.MyServicesScreenKt",
                "bo.saludencasa.features.search.presentation.ProfessionalSearchScreenKt",
            )

        // The Compose compiler appends a Composer and a changed-flags Int to every
        // composable, which is the signature a parameterless preview compiles to.
        private val PREVIEW_PARAMETERS = arrayOf(Composer::class.java, Int::class.javaPrimitiveType)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{1}")
        fun previews(): List<Array<Any>> =
            SCREEN_FILES.flatMap { className ->
                Class
                    .forName(className, false, ScreenCatalog::class.java.classLoader)
                    .declaredMethods
                    .filter { method ->
                        method.name.endsWith("Preview") &&
                            method.parameterTypes.contentEquals(PREVIEW_PARAMETERS)
                    }.map { arrayOf<Any>(className, it.name) }
                    .sortedBy { it[1] as String }
            }
    }
}
