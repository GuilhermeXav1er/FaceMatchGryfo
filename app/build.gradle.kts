plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.facematchgryfo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.facematchgryfo"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)

    // The following line is optional, as the core library is included indirectly by camera-camera2
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)

    // If you want to additionally use the CameraX Lifecycle library
    implementation(libs.androidx.camera.lifecycle)

    // If you want to additionally use the CameraX View class
    implementation(libs.androidx.camera.view)

    // fuel
    implementation(libs.fuel)
    implementation(libs.fuelCoroutines)

    //coroutines
    implementation(libs.coroutinesAndroid)
    implementation(libs.coroutines)

    implementation(libs.androidx.camera.extensions)

    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}