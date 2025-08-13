package com.recovereasy

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileScanner {

    // ---------- Quick scan: ทั้งเครื่อง (media) + ถังขยะ ----------
    suspend fun quickScan(context: Context, onProgress: (Int) -> Unit): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val all = mutableListOf<MediaItem>()
            val steps = 3 + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 1 else 0
            var done = 0

            all += queryMediaStore(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            onProgress(++done * 100 / steps)

            all += queryMediaStore(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            onProgress(++done * 100 / steps)

            all += queryMediaStore(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            onProgress(++done * 100 / steps)

            // Android 11+ มี is_trashed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                all += queryTrashed(context)
                onProgress(++done * 100 / steps)
            }
            all
        }

    // ---------- Deep scan: เดินแฟ้มทุกไฟล์ใน Tree (OTG/เลือกโฟลเดอร์) ----------
    suspend fun deepScanTree(context: Context, treeUri: Uri, onProgress: (Int) -> Unit): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val out = mutableListOf<MediaItem>()
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext out
            val all = mutableListOf<DocumentFile>()
            fun walk(df: DocumentFile) {
                if (df.isFile) all += df
                else df.listFiles().forEach { walk(it) }
            }
            walk(root)

            var i = 0
            for (f in all) {
                val name = f.name ?: "(unknown)"
                val mime = f.type ?: "application/octet-stream"
                val size = f.length()
                val date = f.lastModified()

                val isCorrupted = tryCheckCorrupted(context, f.uri, mime)
                val duration = tryGetDuration(context, f.uri, mime)

                out += MediaItem(
                    uri = f.uri, name = name, mime = mime,
                    size = size, date = date, durationMs = duration,
                    isTrashed = false, isCorrupted = isCorrupted
                )
                i++
                if (i % 25 == 0) onProgress((i * 100 / all.size).coerceIn(0, 100))
            }
            onProgress(100)
            out
        }

    // ---------- Helpers ----------
    private fun queryMediaStore(context: Context, base: Uri): List<MediaItem> {
        val out = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.Video.VideoColumns.DURATION // คืนค่าได้เฉพาะวีดีโอ/ออดิโอ
        )
        val cursor: Cursor? = context.contentResolver.query(
            base, projection, null, null,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )
        cursor?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val durIdx = c.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val contentUri = ContentUris.withAppendedId(base, id)
                val mime = c.getString(mimeIdx) ?: "application/octet-stream"
                val duration = if (durIdx >= 0 && !c.isNull(durIdx)) c.getLong(durIdx) else null
                val corrupted = tryCheckCorrupted(context, contentUri, mime)

                out += MediaItem(
                    uri = contentUri,
                    name = c.getString(nameIdx) ?: "(unknown)",
                    mime = mime,
                    size = c.getLong(sizeIdx),
                    date = c.getLong(dateIdx) * 1000,
                    durationMs = duration,
                    isTrashed = false,
                    isCorrupted = corrupted
                )
            }
        }
        return out
    }

    private fun queryTrashed(context: Context): List<MediaItem> {
        val out = mutableListOf<MediaItem>()
        val base = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.IS_TRASHED
        )
        val selection = "${MediaStore.MediaColumns.IS_TRASHED}=1"
        context.contentResolver.query(base, projection, selection, null, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            while (c.moveToNext()) {
                val uri = ContentUris.withAppendedId(base, c.getLong(idIdx))
                out += MediaItem(
                    uri = uri,
                    name = c.getString(nameIdx) ?: "(unknown)",
                    mime = c.getString(mimeIdx) ?: "application/octet-stream",
                    size = c.getLong(sizeIdx),
                    date = c.getLong(dateIdx) * 1000,
                    durationMs = null,
                    isTrashed = true,
                    isCorrupted = false
                )
            }
        }
        return out
    }

    private fun tryCheckCorrupted(context: Context, uri: Uri, mime: String): Boolean {
        return try {
            when {
                mime.startsWith("image") -> {
                    context.contentResolver.openInputStream(uri)?.use {
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeStream(it, null, opts)
                        (opts.outWidth <= 0 || opts.outHeight <= 0)
                    } ?: true
                }
                mime.startsWith("video") || mime.startsWith("audio") -> {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(context, uri)
                    mmr.release()
                    false
                }
                else -> false
            }
        } catch (_: Throwable) {
            true
        }
    }

    private fun tryGetDuration(context: Context, uri: Uri, mime: String): Long? {
        if (!(mime.startsWith("video") || mime.startsWith("audio"))) return null
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)
            val d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
            mmr.release()
            d
        } catch (_: Throwable) { null }
    }
}
