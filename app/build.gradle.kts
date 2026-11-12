plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.cibotimer"
    compileSdk = 34

    val enableLiveUpdate = (project.findProperty("enableLiveUpdate") as? String)?.toBoolean() ?: true

    defaultConfig {
        applicationId = "com.cibotimer"
        minSdk = 26
        targetSdk = 34
        versionCode = 60
        versionName = "1.0.59"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("Boolean", "ENABLE_LIVE_UPDATE", "$enableLiveUpdate")
        buildConfigField("Boolean", "IS_PLAY_STORE_BUILD", "${!enableLiveUpdate}")
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile(if (enableLiveUpdate) "src/main/AndroidManifest-liveupdate.xml" else "src/main/AndroidManifest.xml")
        }
    }

    val keystoreFile = rootProject.file("keystore/cibotimer.keystore")
    val hasKeystore = keystoreFile.exists()

    signingConfigs {
        create("ciboRelease") {
            if (hasKeystore) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "cibotimer123"
                keyAlias = System.getenv("KEY_ALIAS") ?: "cibotimer"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "cibotimer123"
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("ciboRelease")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("ciboRelease")
            }
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
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
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
