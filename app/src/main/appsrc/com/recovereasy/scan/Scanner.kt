package com.recovereasy.scan

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.recovereasy.model.MediaItem
import com.recovereasy.model.ScanScope

object Scanner {

    fun scan(
        context: Context,
        scope: ScanScope,
        deep: Boolean,
        folderTree: Uri?
    ): List<MediaItem> {
        return when (scope) {
            ScanScope.PHONE   -> scanMediaStore(context, deep)
            ScanScope.FOLDER,
            ScanScope.OTG     -> scanTree(context, folderTree, deep)
        }
    }

    private fun scanMediaStore(ctx: Context, deep: Boolean): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val resolver = ctx.contentResolver

        fun query(collection: Uri) {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,
                // API 30+ มี IS_TRASHED ให้เช็กไฟล์ในถังขยะ
                *(if (Build.VERSION.SDK_INT >= 30) arrayOf(MediaStore.MediaColumns.IS_TRASHED) else emptyArray())
            )

            // ถ้า deep บน API 30+ ลองดึงไฟล์ที่อยู่ใน Trash ด้วย
            var sel: String? = null
            if (deep && Build.VERSION.SDK_INT >= 30) {
                sel = "${MediaStore.MediaColumns.IS_TRASHED}=1 OR 1=1"
            }

            resolver.query(collection, projection, sel, null, null)?.use { c ->
                val idxId = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val idxName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val idxSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val idxMime = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                while (c.moveToNext()) {
                    val id = c.getLong(idxId)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    items += MediaItem(
                        uri = contentUri,
                        mime = c.getString(idxMime),
                        name = c.getString(idxName),
                        size = c.getLong(idxSize)
                    )
                }
            }
        }

        val img = if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                  else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val vid = if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                  else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val aud = if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                  else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        query(img); query(vid); query(aud)
        return items
    }

    private fun scanTree(ctx: Context, tree: Uri?, deep: Boolean): List<MediaItem> {
        if (tree == null) return emptyList()
        val root = DocumentFile.fromTreeUri(ctx, tree) ?: return emptyList()
        val out = mutableListOf<MediaItem>()

        fun walk(df: DocumentFile) {
            if (df.isFile) {
                out += MediaItem(df.uri, df.type, df.name, df.length())
            } else if (df.isDirectory) {
                df.listFiles().forEach {
                    if (deep) walk(it) else if (it.isFile) {
                        out += MediaItem(it.uri, it.type, it.name, it.length())
                    }
                }
            }
        }
        walk(root)
        return out
    }
}
