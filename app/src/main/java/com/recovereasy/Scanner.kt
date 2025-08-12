package com.recovereasy

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object Scanner {

  // --------------------- FAST (MediaStore) ---------------------
  suspend fun scanPhoneFast(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
    val out = mutableListOf<MediaItem>()
    fun query(kind: MediaKind, extUri: Uri, projection: Array<String>, sort: String? = null) {
      context.contentResolver.query(extUri, projection, null, null, sort)?.use { c ->
        val id = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val name = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val size = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        while (c.moveToNext()) {
          val uri = ContentUris.withAppendedId(extUri, c.getLong(id))
          out += MediaItem(uri, c.getString(name) ?: "", c.getLong(size), kind, "phone")
        }
      }
    }
    query(MediaKind.IMAGE, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE))
    query(MediaKind.VIDEO, MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
      arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.SIZE))
    query(MediaKind.AUDIO, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
      arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.SIZE))
    out
  }

  // --------------------- DEEP (SAF / ทั้งเครื่อง) ---------------------
  suspend fun scanTreeDeep(context: Context, treeUri: Uri, label: String): List<MediaItem> =
    withContext(Dispatchers.IO) {
      val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
      val out = mutableListOf<MediaItem>()
      fun walk(df: DocumentFile) {
        if (df.isFile) {
          val kind = guessKindByName(df.name ?: "")
          val bad = isSuspectCorrupted(context, df.uri, kind)
          out += MediaItem(df.uri, df.name ?: "", df.length(), kind, label, bad)
        } else if (df.isDirectory) {
          df.listFiles().forEach { walk(it) }
        }
      }
      walk(root)
      out
    }

  // สำหรับ “ทั้งเครื่อง (Deep)” — ต้องมี MANAGE_EXTERNAL_STORAGE
  suspend fun scanWholeStorageDeep(context: Context): List<MediaItem> =
    withContext(Dispatchers.IO) {
      val out = mutableListOf<MediaItem>()
      val sm = context.getSystemService(Context.STORAGE_SERVICE) as android.os.storage.StorageManager
      val volumes = sm.storageVolumes
      for (v in volumes) {
        val uuid = v.uuid
        val tree = if (uuid == null) {
          DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents", "primary:"
          )
        } else {
          DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents", "$uuid:"
          )
        }
        out += scanTreeDeep(context, tree, "phone")
      }
      out
    }

  private fun guessKindByName(name: String): MediaKind {
    val n = name.lowercase()
    return when {
      n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp") || n.endsWith(".heic") -> MediaKind.IMAGE
      n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".mov") || n.endsWith(".avi") -> MediaKind.VIDEO
      n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".wav") || n.endsWith(".flac") -> MediaKind.AUDIO
      else -> MediaKind.OTHER
    }
  }

  // ตรวจ “ไฟล์อาจเสีย/เปิดไม่ได้” อย่างหยาบ ๆ (อ่านหัวไฟล์ / เปิด metadata)
  private fun isSuspectCorrupted(context: Context, uri: Uri, kind: MediaKind): Boolean {
    return try {
      context.contentResolver.openInputStream(uri)?.use { ins ->
        when (kind) {
          MediaKind.IMAGE -> !looksLikeImage(ins)
          MediaKind.VIDEO -> false // ปล่อยให้ coil/decoder ล้มตอนพรีวิวแล้วทำ flag ในอนาคตได้
          else -> false
        }
      } ?: true
    } catch (_: Throwable) { true }
  }

  private fun looksLikeImage(ins: InputStream): Boolean {
    val header = ByteArray(12)
    val read = ins.read(header)
    if (read < 4) return true
    // JPEG, PNG, WEBP แบบง่าย ๆ
    val jpg = header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte()
    val png = header[0] == 0x89.toByte() && header[1] == 0x50.toByte()
    val riff = header.copyOfRange(0, 4).contentEquals(byteArrayOf(0x52,0x49,0x46,0x46)) // RIFF (WEBP)
    return jpg || png || riff
  }
}
