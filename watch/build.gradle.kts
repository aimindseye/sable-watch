// The watch app: complication data sources the Sable face (and any other face) can show.
// It has the same package name and signing key as :phone, which the Wear OS Data Layer needs
// to let the two talk to each other.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.aimindseye.sablewatch"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.aimindseye.sablewatch"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.wear.complications.data.source.ktx)
    testImplementation(libs.junit)
}
