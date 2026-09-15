plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

android {
    namespace = "com.joshua.glasscalc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.joshua.glasscalc"
        minSdk = 26 // blur/glass effects need 31+, but app runs (falls back) from 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        ndk {
            // Chaquopy needs at least one ABI; these cover virtually all real devices.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
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
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

// Chaquopy's pip config lives in its own top-level extension in the Kotlin
// DSL — it does NOT hang off android.defaultConfig the way the Groovy docs
// show. That mismatch is what caused "Unresolved reference: python/pip".
chaquopy {
    defaultConfig {
        pip {
            // install("numpy")  // uncomment if you want numpy inside formulas
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
