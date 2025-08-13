package com.recovereasy.model
import android.net.Uri
data class MediaItem(
    val uri: Uri,
    val mime: String?,
    val name: String?,
    val size: Long
)
