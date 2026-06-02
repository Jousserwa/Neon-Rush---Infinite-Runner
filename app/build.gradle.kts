plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.neonrush.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neonrushinfinite.game"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "neonrush123"
            keyAlias = "neonrush"
            keyPassword = "neonrush123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-graphics:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Play Store Submission Integrations
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    implementation("com.google.firebase:firebase-firestore-ktx:24.11.0")
    implementation("com.google.firebase:firebase-common-ktx:20.4.3")
    implementation("com.revenuecat.purchases:purchases:7.12.0")

    testImplementation("junit:junit:4.13.2")
}

tasks.register("generateKeystore") {
    doLast {
        val keystoreFile = file("release.keystore")
        if (!keystoreFile.exists()) {
            println("Generating release keystore...")
            val process = ProcessBuilder(
                "keytool", "-genkeypair", "-noprompt",
                "-keystore", keystoreFile.absolutePath,
                "-alias", "neonrush",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000",
                "-dname", "CN=NeonRush, O=NeonRush, C=US",
                "-storepass", "neonrush123",
                "-keypass", "neonrush123"
            ).inheritIO().start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw RuntimeException("Failed to generate keystore, exit code: $exitCode")
            }
            println("Release keystore generated successfully at ${keystoreFile.absolutePath}")
        } else {
            println("Release keystore already exists.")
        }
    }
}

// Ensure the keystore is generated before compiling release bundles or assets
tasks.matching { it.name.startsWith("bundle") || it.name.startsWith("assemble") }.configureEach {
    dependsOn("generateKeystore")
}

