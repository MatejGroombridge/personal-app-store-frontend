plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.matejgroombridge.store"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.matejgroombridge.store"
        minSdk = 26          // Android 8.0+ (covers ~95% of devices, allows modern APIs)
        targetSdk = 35       // Android 15
        versionCode = 1
        versionName = "0.1.0"

        // The Store App is itself one of the apps in the manifest, so it can self-update.
        // The URL below is the manifest endpoint the app polls. Replace with your own
        // GitHub Releases / R2 / domain URL once available.
        buildConfigField(
            "String",
            "MANIFEST_URL",
            "\"https://raw.githubusercontent.com/matejgroombridge/personal-app-manifest/main/manifest.json\""
        )
        buildConfigField("boolean", "USE_MOCK_MANIFEST", "true")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signing config will be wired up by the GitHub Actions pipeline.
            // For local release builds, configure ~/.gradle/gradle.properties with:
            //   STORE_FILE=..., STORE_PASSWORD=..., KEY_ALIAS=..., KEY_PASSWORD=...
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android + Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Networking
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    // Image loading (app icons)
    implementation(libs.coil.compose)

    // Settings storage
    implementation(libs.androidx.datastore.preferences)

    // Background update checks
    implementation(libs.androidx.work.runtime.ktx)
}
