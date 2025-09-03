import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.mojgrad"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mojgrad"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        
        // R2 credentials
        buildConfigField("String", "R2_ACCESS_KEY", "\"${localProperties.getProperty("R2_ACCESS_KEY") ?: ""}\"")
        buildConfigField("String", "R2_SECRET_KEY", "\"${localProperties.getProperty("R2_SECRET_KEY") ?: ""}\"")
        buildConfigField("String", "R2_BUCKET_NAME", "\"${localProperties.getProperty("R2_BUCKET_NAME") ?: ""}\"")
        buildConfigField("String", "R2_ENDPOINT", "\"${localProperties.getProperty("R2_ENDPOINT") ?: ""}\"")
        buildConfigField("String", "R2_PUBLIC_URL", "\"${localProperties.getProperty("R2_PUBLIC_URL") ?: ""}\"")
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Navigation dependencies
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    // Lifecycle dependencies
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Firebase dependencies
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.firestore)
    // GeoFire dependency for geohash queries
    implementation("com.firebase:geofire-android-common:3.2.0")
    // Coroutines for Firebase Tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    // AWS SDK for Kotlin (supports EnableAwsChunked for R2 compatibility)
    implementation("aws.sdk.kotlin:s3:1.0.30")
    implementation("aws.sdk.kotlin:aws-core:1.0.30")
    // Other dependencies
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("com.google.maps.android:maps-compose:2.11.4")
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
}