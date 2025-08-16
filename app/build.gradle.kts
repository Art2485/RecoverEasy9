dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // ต้องมีแน่ ๆ เพราะ FileAdapter.kt ใช้ RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ต้องมีแน่ ๆ เพราะ FileAdapter.kt ใช้ coil.load / ImageRequest / Scale
    implementation("io.coil-kt:coil:2.6.0")
    // ถ้าจะพรีวิววิดีโอด้วย แนะนำเพิ่มตัวนี้ (ปลอดภัย ไม่พังก่อน)
    implementation("io.coil-kt:coil-video:2.6.0")
}
