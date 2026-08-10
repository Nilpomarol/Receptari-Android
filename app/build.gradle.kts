import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 applies Kotlin support itself; applying kotlin-android here would clash.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "cat.receptari.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "cat.receptari.app"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Development utilities such as the sample-data seeder live in androidTest but must
        // never run as part of the suite; they are invoked explicitly with
        // -Pandroid.testInstrumentationRunnerArguments.annotation=cat.receptari.app.ManualOnly
        testInstrumentationRunnerArguments["notAnnotation"] = "cat.receptari.app.ManualOnly"
    }

    androidResources {
        // Catalan is the default resource set; Spanish and English are overlays (ADR-004).
        localeFilters += listOf("ca", "es", "en")
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        // Every string must exist in the default (Catalan) resource set (ADR-004).
        // HardcodedText only inspects XML layouts, which this app has none of — the
        // Compose equivalent is enforced by the checkNoHardcodedUiText task instead.
        error += listOf("MissingDefaultResource")
        // Overlay languages are allowed to lag behind and fall back to Catalan.
        disable += listOf("MissingTranslation")
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    // Ships the exported Room schemas to the device so MigrationTestHelper can read them.
    sourceSets.getByName("androidTest") {
        assets.directories.add("$projectDir/schemas")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

apply(from = rootProject.file("gradle/hardcoded-ui-text.gradle.kts"))

dependencies {
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit.ktx)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
