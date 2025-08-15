package com.recovereasy

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile

object Scanner {

    fun scanDocumentTree(ctx: Context, root: DocumentFile?, deep: Boolean = true): List<MediaItem> {
        if (root == null || !root.exists()) return emptyList()
        val out = ArrayList<MediaItem>(256)
        walk(root, out, deep)
        return out
    }

    private fun walk(df: DocumentFile, out: MutableList<MediaItem>, deep: Boolean) {
        df.listFiles().forEach { f ->
            if (f.isDirectory) {
                if (deep) walk(f, out, deep)
            } else {
                val name = f.name ?: "(unknown)"
                val mime = f.type
                val kind = mimeToKind(mime)
                // isTrashed: ใช้ false เป็นค่าพื้นฐาน
                out += MediaItem(
                    uri = f.uri,
                    name = name,
                    mime = mime,
                    kind = kind,
                    isTrashed = false,
                    checked = false
                )
            }
        }
    }

    fun mimeToKind(mime: String?): MediaKind = when {
        mime == null -> MediaKind.OTHER
        mime.startsWith("image/") -> MediaKind.IMAGE
        mime.startsWith("video/") -> MediaKind.VIDEO
        mime.startsWith("audio/") -> MediaKind.AUDIO
        else -> MediaKind.OTHER
    }

    fun guessMimeFromName(name: String): String? {
        val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return if (ext.isEmpty()) null else MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }
}
