plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "fr.bureaudepoche"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.bureaudepoche.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
        // Version de test : affiche dans les réglages un interrupteur qui débloque Pro sans paiement.
        // À passer à "false" avant la publication sur le Play Store.
        buildConfigField("boolean", "TEST_BUILD", "true")
    }

    signingConfigs {
        // Clé de TEST uniquement (dépôt public) : sert à installer l'APK à la main.
        // Pour le Play Store, utiliser une clé privée gardée hors du dépôt.
        create("test") {
            storeFile = rootProject.file("signature/bureau-test.jks")
            storePassword = "bdp-test-2026"
            keyAlias = "bureau"
            keyPassword = "bdp-test-2026"
        }
    }

    buildTypes {
        release {
            // réduit fortement la taille de l'APK
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("test")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Scanner de documents Google (traitement sur le téléphone)
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    // Achat unique Pro via Google Play
    implementation("com.android.billingclient:billing:8.0.0")
}
