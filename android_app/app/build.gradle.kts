import java.io.FileInputStream
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    id("kotlin-kapt")
}

hilt {
    enableAggregatingTask = false
}

android {
    namespace = "com.health.openscale"
    compileSdk = 36

    // --- Versioning ---
    val upstreamVersion = "3.0.1"
    val forkRevision = 1

    defaultConfig {
        applicationId = "lol.fynn.openscale"
        minSdk = 31
        targetSdk = 36
        versionCode = 71
        versionName = "$upstreamVersion-r$forkRevision"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appName"] = "openScale Redesigned"
        manifestPlaceholders["appNameLauncher"] = "openScale"
        manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
        manifestPlaceholders["appRoundIcon"] = "@mipmap/ic_launcher_round"
    }

    buildTypes {
        configureEach {
            buildConfigField("String", "GIT_SHA", "\"${gitSha()}\"")
            buildConfigField("String", "BUILD_TIME_UTC", "\"${buildTimeUtc()}\"")
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this
            if (output is com.android.build.gradle.internal.api.BaseVariantOutputImpl) {
                output.outputFileName = "openScale-Redesigned-${variant.versionName}-${variant.buildType.name}.apk"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
        correctErrorTypes = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.worker)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    implementation(libs.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.work)
    kapt(libs.hilt.androidx.compiler)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Vico charts
    implementation(libs.compose.charts)
    implementation(libs.compose.charts.m3)

    // Glance
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Compose reorderable
    implementation(libs.compose.reorderable)
    implementation(libs.compose.material.icons.extended)

    // Kotlin-CSV
    implementation(libs.kotlin.csv.jvm)

    // Blessed Kotlin
    implementation(libs.blessed.kotlin)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

fun safeExec(vararg cmd: String): String = try {
    val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
    p.inputStream.bufferedReader().use { it.readText() }.trim().ifEmpty { "unknown" }
} catch (_: Exception) { "unknown" }

fun gitSha(): String {
    System.getenv("GIT_SHA")?.takeIf { it.isNotBlank() }?.let { return it }
    return safeExec("git", "rev-parse", "--short", "HEAD")
}

fun buildTimeUtc(): String {
    System.getenv("BUILD_TIME_UTC")?.takeIf { it.isNotBlank() }?.let { return it }
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}