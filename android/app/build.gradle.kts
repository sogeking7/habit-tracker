import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.2.10"
}

// Read secrets (Clerk publishable key, Convex URL) from local.properties, which
// is gitignored. Falls back to a Gradle project property, then empty string.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun secret(key: String): String =
    localProperties.getProperty(key) ?: (project.findProperty(key) as String?) ?: ""

// clerk-android-ui 1.0.x references telemetry 1.0, but the published telemetry
// line is 1.0.x. Pin it so dependency resolution succeeds.
configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.clerk:clerk-android-telemetry:1.0"))
            .using(module("com.clerk:clerk-android-telemetry:1.0.3"))
    }
}

android {
    namespace = "com.example.habit_tracker"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.habit_tracker"
        minSdk = 26 // required by com.clerk:clerk-convex-kotlin
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CLERK_PUBLISHABLE_KEY", "\"${secret("CLERK_PUBLISHABLE_KEY")}\"")
        buildConfigField("String", "CONVEX_DEPLOYMENT_URL", "\"${secret("CONVEX_DEPLOYMENT_URL")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.material.icons.extended)

    // Convex backend client + Clerk auth integration.
    implementation("dev.convex:android-convexmobile:0.8.0@aar") {
        isTransitive = true
    }
    implementation("com.clerk:clerk-android-ui:1.0.16")
    implementation("com.clerk:clerk-convex-kotlin:0.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
