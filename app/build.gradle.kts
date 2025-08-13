plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.recovereasy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.recovereasy"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            // เปิด log เต็ม ๆ เวลา CI
            isMinifyEnabled = false
        }
    }

    // **สำคัญ** เปิด viewBinding ให้สร้าง ActivityMainBinding
    buildFeatures {
        viewBinding = true
        // dataBinding = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }

    // ใช้โครงสร้าง src/main/java|res ปกติเท่านั้น
    // (ห้ามเพิ่ม srcDirs แปลก ๆ อย่าง src/main/appsrc อีก)
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // preview รูป/วิดีโอ
    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")

    // จัดการไฟล์ผ่าน SAF/OTG
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}
