package com.recovereasy

import android.net.Uri

enum class MediaKind { IMAGE, VIDEO, AUDIO, OTHER }

data class MediaItem(
    val uri: Uri,
    val name: String,
    val mime: String?,
    val kind: MediaKind,
    val isTrashed: Boolean = false,
    var checked: Boolean = false
)
