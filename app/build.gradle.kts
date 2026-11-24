plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "uk.sugar.lock"
    compileSdk = 35

    defaultConfig {
        applicationId = "uk.sugar.lock"
        minSdk = 26
        targetSdk = 35
        // GitHub Actions passes its run number so every build has a higher version.
        val build = (System.getenv("BUILD_NUMBER") ?: "1").toInt()
        versionCode = build
        versionName = "1.0.$build"
    }
    // Release signing for Google Play. The upload key is kept in GitHub Secrets,
    // never in the repo. Without it, the release build is skipped.
    val ksFile = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (ksFile != null) {
            create("upload") {
                storeFile = file(ksFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (ksFile != null) signingConfig = signingConfigs.getByName("upload")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
