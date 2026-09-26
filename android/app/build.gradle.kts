plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
}

ksp {
    arg(
        "room.schemaLocation",
        "$projectDir/schemas"
    )
}

android {
    namespace = "com.signaldesk.android"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.signaldesk.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


    }

    signingConfigs {
        create("release") {
            storeFile = file(
                providers.gradleProperty("SIGNALDESK_KEYSTORE_FILE").get()
            )
            storePassword =
                providers.gradleProperty("SIGNALDESK_KEYSTORE_PASSWORD").get()
            keyAlias =
                providers.gradleProperty("SIGNALDESK_KEY_ALIAS").get()
            keyPassword =
                providers.gradleProperty("SIGNALDESK_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "SIGNALDESK_BASE_URL",
                "\"http://127.0.0.1:8082\""
            )
        }

        release {
            signingConfig = signingConfigs.getByName("release")
            val signaldeskBaseUrl =
                providers.gradleProperty("signaldeskBaseUrl")
                    .orNull
                    ?: throw GradleException(
                        "Release builds require -PsignaldeskBaseUrl=https://..."
                    )

            require(signaldeskBaseUrl.startsWith("https://")) {
                "Release signaldeskBaseUrl must use HTTPS."
            }

            buildConfigField(
                "String",
                "SIGNALDESK_BASE_URL",
                "\"$signaldeskBaseUrl\""
            )

            isMinifyEnabled = false

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
        buildConfig = true
    }

}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
