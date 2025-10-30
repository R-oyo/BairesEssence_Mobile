plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
}

android {
    namespace = "com.example.bairesessence"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bairesessence"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // Core Android
    implementation("androidx.core:core-ktx:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2025.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")
    implementation("androidx.navigation:navigation-runtime-ktx:2.9.5")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")
    implementation("com.google.firebase:firebase-common-ktx:21.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.6")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.1")
}

apply(plugin = "com.google.gms.google-services")
