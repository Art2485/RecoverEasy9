package com.recovereasy

import android.net.Uri

data class MediaItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mime: String,
    var checked: Boolean = false
)
