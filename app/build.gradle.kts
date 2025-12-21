plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Firebase configuration from google-services.json
        // Project ID: group7-66146
        // API Key: AIzaSyDyKnTKeQN3hr-qGxdDfLcfnyROmiv8OUU
        // Database URL: https://group7-66146-default-rtdb.firebaseio.com
        // Mobile SDK App ID: 1:873564343719:android:e730a81906076d82daba82
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"group7-66146\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"AIzaSyDyKnTKeQN3hr-qGxdDfLcfnyROmiv8OUU\"")
        buildConfigField("String", "FIREBASE_DATABASE_URL", "\"https://group7-66146-default-rtdb.firebaseio.com\"")
        buildConfigField("String", "FIREBASE_APPLICATION_ID", "\"1:873564343719:android:e730a81906076d82daba82\"")
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.applandeo:material-calendar-view:1.9.2")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.viewpager2)
    implementation(libs.recyclerview)
    implementation(libs.fragment)
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.1.1")
    testImplementation("org.mockito:mockito-core:5.11.0")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}