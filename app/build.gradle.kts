plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.recovereasy"      // ให้ตรงกับ package ใน AndroidManifest.xml
    compileSdk = 34

    defaultConfig {
        applicationId = "com.recovereasy"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // เปิดใช้ทั้ง ViewBinding + Compose (เพื่อให้โค้ดเดิมที่ใช้ Compose คอมไพล์ผ่าน)
    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        // เวอร์ชัน compiler ของ Compose ที่เข้าคู่กับ Kotlin 1.9.24
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/DEPENDENCIES"
        )
    }
}

dependencies {
    // AndroidX base
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // ---- Compose (เผื่อมีโค้ด Compose เดิม ๆ: Text/Row/Column/Modifier/dp ฯลฯ) ----
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // พรีวิวรูป/วิดีโอ (ทั้ง View และ Compose)
    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // SAF/เมตาดาต้าไฟล์
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}
