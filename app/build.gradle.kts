plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.android_minigame"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.android_minigame"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // WorkManager
    implementation(libs.work.runtime)

    //GIF
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    //Gson
    implementation(libs.gson);

    //GoogleMaps
    implementation(libs.play.services.maps);
    implementation(libs.play.services.location);
}