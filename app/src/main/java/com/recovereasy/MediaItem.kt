package com.recovereasy
import android.net.Uri
data class MediaItem(
    val uri: Uri,
    val name: String,
    val mime: String?,
    val isAudio: Boolean
)
