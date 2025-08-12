plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.recovereasy"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.recovereasy"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
    vectorDrawables.useSupportLibrary = true
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
  kotlinOptions { jvmTarget = "17" }

  buildFeatures {
    viewBinding = true
    // ถ้าใช้ Compose อยู่ค่อยเปิดตรงนี้
    // compose = true
  }
}

dependencies {
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.activity:activity-ktx:1.9.2")
  implementation("androidx.recyclerview:recyclerview:1.3.2")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

  // รูปภาพ (ถ้าใช้)
  implementation("io.coil-kt:coil:2.6.0")

  // อ่าน metadata รูป/วิดีโอ (ถ้าใช้)
  implementation("androidx.exifinterface:exifinterface:1.3.7")
}

configurations.all {
  // กันเวอร์ชันกระโดดจนชนกัน (แกนหลักให้ล็อกตามนี้)
  resolutionStrategy {
    force(
      "androidx.core:core-ktx:1.13.1",
      "androidx.appcompat:appcompat:1.7.0",
      "com.google.android.material:material:1.12.0",
      "androidx.activity:activity-ktx:1.9.2",
      "androidx.recyclerview:recyclerview:1.3.2"
    )
  }
}
