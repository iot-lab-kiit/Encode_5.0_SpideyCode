plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "in.iot.spidey_code"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "in.iot.spidey_code"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Nearly every phone since ~2017 is arm64. Shipping x86_64/x86/armeabi-v7a
        // native libs too (CameraX, ML Kit, Media3) quadruples lib/ for zero benefit
        // on a real device -- ~24MB of dead weight. Trade-off: this APK won't install
        // on x86 emulators or old 32-bit-only devices; fine for installing directly on
        // known event-booth phones, not for wide public distribution via Play Store
        // (which would use App Bundles + per-device delivery instead of this filter).
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            // NOTE: R8 shrinking was briefly enabled to cut APK size, but it broke the
            // camera screen at runtime -- a DisposableEffect teardown lambda in
            // CameraScreen.kt started crashing with an NPE right when camera permission
            // is granted (only reproduces in the minified build; traced via the R8
            // mapping file, not yet root-caused to a specific fix). Reverted to keep the
            // app working; re-enabling this needs on-device testing time this isn't safe
            // to ship without. See proguard-rules.pro for where keep rules would go.
            optimization {
                enable = false
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("main") {
            assets.directories.add("assets")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Lottie Compose
    implementation(libs.lottie.compose)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)

    // ML Kit Face Detection
    implementation(libs.mlkit.face.detection)

    // Media3 (video post-processing for the recorded-video frame overlay, and playback in Review)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}