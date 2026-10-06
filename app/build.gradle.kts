import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    // Both read app/google-services.json at build time, so from HT-08 onward the
    // file is required to compile. Registered in docs/decisions.md.
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// RNF-06: secrets never live in source. In development they come from
// local.properties, which is excluded from version control; in continuous
// integration the same names arrive as environment variables generated from the
// repository secrets. A missing value yields an empty string so that a clean
// checkout still compiles: the failure then surfaces at run time, where it is
// readable, instead of as a Gradle error nobody can act on.
val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use(::load)
    }

fun secret(name: String): String = localProperties.getProperty(name) ?: System.getenv(name) ?: ""

android {
    namespace = "bo.saludencasa"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "bo.saludencasa.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${secret("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secret("SUPABASE_ANON_KEY")}\"")
        // The client identifier handed to Credential Manager is the Web one,
        // not the Android one. Registered in docs/decisions.md.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${secret("GOOGLE_WEB_CLIENT_ID")}\"")
        // The Maps SDK reads its key from the manifest and not from BuildConfig,
        // so this one secret travels as a placeholder. Registered in
        // docs/decisions.md, 2026-09-14.
        manifestPlaceholders["MAPS_API_KEY"] = secret("MAPS_API_KEY")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(platform(libs.supabase.bom))
    implementation(libs.bundles.supabase)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.exifinterface)
    implementation(libs.bundles.auth)
    implementation(libs.bundles.maps)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)

    testImplementation(libs.bundles.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

ktlint {
    // The engine version comes from the catalog, not the build script.
    version.set(libs.versions.ktlintEngine.get())
}

// The project's own rules -- the dependency rule and the hardcoded-text rule --
// are JVM tests rather than ktlint rules (docs/decisions.md, 2026-09-12), so the
// static-analysis stage has to run both tools to cover them.
tasks.register("staticAnalysis") {
    group = "verification"
    description = "Runs ktlint and the project's own architecture and internationalization rules."
    dependsOn("ktlintCheck", "testDebugUnitTest")
}
