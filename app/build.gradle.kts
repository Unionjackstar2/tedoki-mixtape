plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

// Signatur kommt aus GitHub Actions (Secrets KEYSTORE_BASE64 / KEYSTORE_PASSWORD).
// Ohne Schlüssel wird das Release mit dem Debug-Schlüssel signiert, damit der Build trotzdem durchläuft.
val releaseKeystore: String? = System.getenv("KEYSTORE_FILE")
val hasReleaseKey = !releaseKeystore.isNullOrBlank() && file(releaseKeystore).exists()

android {
  namespace = "de.tedoki.mixtape"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "de.tedoki.mixtape"
    minSdk = 24
    targetSdk = 36
    // Fortlaufende Versionsnummer aus GitHub Actions, damit jede neue APK als Update installiert
    versionCode = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    if (hasReleaseKey) {
      create("release") {
        storeFile = file(releaseKeystore!!)
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      isCrunchPngs = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = if (hasReleaseKey) signingConfigs.getByName("release")
                      else signingConfigs.getByName("debug")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }

  // Kein Google-Abhängigkeits-Blob in der APK (wichtig u. a. für F-Droid)
  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.common)
  implementation(libs.coil.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
