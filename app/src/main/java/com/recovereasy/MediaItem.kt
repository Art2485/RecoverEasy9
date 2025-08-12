package com.recovereasy

import android.net.Uri

enum class MediaKind { IMAGE, VIDEO, AUDIO, OTHER }

data class MediaItem(
  val uri: Uri,
  val name: String,
  val size: Long,
  val kind: MediaKind,
  val sourceLabel: String,   // phone / otg / folder
  val suspectCorrupted: Boolean = false
)
