package com.recovereasy

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile

object Scanner {

    /** สแกนจากโฟลเดอร์/DocumentTree ที่ผู้ใช้เลือก (รองรับโฟลเดอร์ในเครื่อง/OTG/SD ผ่าน SAF) */
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
                val mime = f.type ?: guessMimeFromName(name) ?: ""
                val size = try { f.length() } catch (_: Throwable) { 0L }
                out += MediaItem(
                    uri = f.uri,
                    name = name,
                    size = size,
                    mime = mime,
                    checked = false
                )
            }
        }
    }

    private fun guessMimeFromName(name: String): String? {
        val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return if (ext.isEmpty()) null else MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }
}
