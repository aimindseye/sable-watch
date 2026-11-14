// The phone companion. Two builds of one app, with the same package name:
//   sable    for SableOS, where it reads Sable Calendar, Sable Weather and Hub directly;
//   generic  for any Android phone (for example with TenSeven Launcher), using the standard
//            Android calendar and notification APIs.
// Same package name and signing key as :watch, which the Wear OS Data Layer needs.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.aimindseye.sablewatch.phone"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.aimindseye.sablewatch"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    flavorDimensions += "platform"
    productFlavors {
        create("sable") {
            dimension = "platform"
            buildConfigField("boolean", "SABLE_OS", "true")
        }
        create("generic") {
            dimension = "platform"
            buildConfigField("boolean", "SABLE_OS", "false")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}
