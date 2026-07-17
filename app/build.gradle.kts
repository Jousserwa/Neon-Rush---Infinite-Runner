plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics") version "2.9.9"
}

android {
    namespace = "com.neonrush.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neonrushinfinite.game"
        minSdk = 24
        targetSdk = 34
        versionCode = 19
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Firebase (using BOM for version alignment)
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")           // TRACK EVERYTHING
    implementation("com.google.firebase:firebase-crashlytics-ktx")         // CRASH REPORTING
    implementation("com.google.firebase:firebase-messaging-ktx")           // PUSH NOTIFICATIONS
    implementation("com.google.firebase:firebase-config-ktx")              // REMOTE CONFIG / A-B TESTS
    implementation("com.google.firebase:firebase-auth-ktx")                // ANONYMOUS AUTH FOR LEADERBOARD

    // Google Play Games Services (Cloud Save + Achievements + Leaderboards)
    implementation("com.google.android.gms:play-services-games-v2:19.0.0")
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Ads
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    // IronSource / AppLovin MAX Mediation (uncomment when ready to maximize eCPM)
    // implementation("com.applovin:applovin-sdk:12.3.0")
    // implementation("com.ironsource.sdk:mediationsdk:8.0.0")

    // RevenueCat - IAP & Subscriptions
    implementation("com.revenuecat.purchases:purchases:8.20.0")

    // Offer Wall for non-payers (Tapjoy / ironSource)
    // implementation("com.tapjoy:tapjoy-android-sdk:13.2.0")

    // Image Loading (for remote assets, future-proofing)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Networking (for future API calls, offer walls, etc.)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // DataStore (modern replacement for SharedPreferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
