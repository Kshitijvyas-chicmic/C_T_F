plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.clap_to_find_sandbox"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.clap_to_find_sandbox"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        mlModelBinding = false
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            assets.srcDirs("src/main/assets")
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.jtransforms)
    implementation("org.tensorflow:tensorflow-lite:+")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
