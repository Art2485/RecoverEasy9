package com.recovereasy

import android.net.Uri

data class MediaItem(
    val uri: Uri,
    val name: String,
    val mime: String,
    val size: Long,
    val date: Long,
    val durationMs: Long? = null,
    val isTrashed: Boolean = false,
    val isCorrupted: Boolean = false
)
