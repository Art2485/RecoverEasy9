package com.recovereasy.storage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import androidx.media3.common.MediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// ----------------- Data -----------------
data class ScannedItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mime: String?,
    val parent: String?,
    val lastModified: Long,
    val isImage: Boolean,
    val isVideo: Boolean
)

// ----------------- SAF Picker -----------------
class OtgPicker(
    activity: ComponentActivity,
    private val onPicked: (Uri) -> Unit
) {
    private val launcher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                val data = res.data ?: return@registerForActivityResult
                val tree = data.data ?: return@registerForActivityResult
                val flags = data.flags and
                        (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                try {
                    activity.contentResolver.takePersistableUriPermission(tree, flags)
                } catch (_: SecurityException) { /* บางรุ่น read-only ก็พอ */ }
                onPicked(tree)
            }
        }

    fun open(initial: Uri? = null) {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
            if (initial != null) putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial)
        }
        launcher.launch(i)
    }
}

// ----------------- Deep Scanner -----------------
object DeepScanner {
    suspend fun scanTree(context: Context, treeUri: Uri, deep: Boolean): List<ScannedItem> =
        withContext(Dispatchers.IO) {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
            val out = ArrayList<ScannedItem>(512)

            fun mimeOf(df: DocumentFile): String? = df.type ?: run {
                val n = df.name ?: return@run null
                val ext = n.substringAfterLast('.', "").lowercase(Locale.ROOT)
                if (ext.isNotEmpty()) MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) else null
            }

            fun accept(f: DocumentFile) = f.isFile && f.canRead()

            fun walk(dir: DocumentFile) {
                dir.listFiles().forEach { f ->
                    if (f.isDirectory) {
                        if (deep) walk(f) else {
                            f.listFiles().forEach { sub ->
                                if (accept(sub)) {
                                    val mime = mimeOf(sub)
                                    out.add(
                                        ScannedItem(
                                            uri = sub.uri,
                                            name = sub.name ?: "(no-name)",
                                            size = sub.length(),
                                            mime = mime,
                                            parent = dir.name,
                                            lastModified = sub.lastModified(),
                                            isImage = mime?.startsWith("image") == true,
                                            isVideo = mime?.startsWith("video") == true
                                        )
                                    )
                                }
                            }
                        }
                    } else if (accept(f)) {
                        val mime = mimeOf(f)
                        out.add(
                            ScannedItem(
                                uri = f.uri,
                                name = f.name ?: "(no-name)",
                                size = f.length(),
                                mime = mime,
                                parent = dir.name,
                                lastModified = f.lastModified(),
                                isImage = mime?.startsWith("image") == true,
                                isVideo = mime?.startsWith("video") == true
                            )
                        )
                    }
                }
            }

            walk(root)
            out
        }
}

// ----------------- Safe Copy -----------------
object SafeCopier {
    suspend fun copyToDir(
        context: Context,
        source: Uri,
        destDirTree: Uri,
        desiredName: String? = null,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val destDir = DocumentFile.fromTreeUri(context, destDirTree) ?: return@withContext null
        if (!destDir.canWrite()) return@withContext null

        val name = (desiredName ?: "file").ifBlank { "file" }
        val mime = resolver.getType(source) ?: "application/octet-stream"
        val tempDf = destDir.createFile(mime, "$name.part") ?: return@withContext null

        val total = try {
            resolver.openAssetFileDescriptor(source, "r")?.use { it.length } ?: -1L
        } catch (_: Exception) { -1L }

        resolver.openInputStream(source)?.use { inp ->
            resolver.openOutputStream(tempDf.uri, "w")?.use { out ->
                val buf = ByteArray(128 * 1024)
                var sum = 0L
                while (true) {
                    val n = inp.read(buf)
                    if (n <= 0) break
                    out.write(buf, 0, n)
                    sum += n
                    onProgress?.invoke(sum, total)
                }
                out.flush()
                if (out is FileOutputStream) try { out.fd.sync() } catch (_: Exception) {}
            }
        }

        // rename .part -> ชื่อจริง
        try { DocumentsContract.renameDocument(resolver, tempDf.uri, name) } catch (_: Exception) {}
        destDir.listFiles().firstOrNull { it.name == name }?.uri ?: tempDf.uri
    }
}

// ----------------- Basic Repair -----------------
object FileRepair {

    suspend fun tryRepairImageTo(
        context: Context,
        source: Uri,
        destDirTree: Uri,
        outBase: String? = null
    ): Uri? = withContext(Dispatchers.IO) {
        val cr = context.contentResolver
        val destDir = DocumentFile.fromTreeUri(context, destDirTree) ?: return@withContext null
        val outDf = destDir.createFile("image/jpeg", "${outBase ?: "repaired"}.jpg")
            ?: return@withContext null

        val bmp: Bitmap = cr.openInputStream(source)?.use { BitmapFactory.decodeStream(it) }
            ?: return@withContext null

        // orientation
        val rotation = try {
            cr.openInputStream(source)?.use { ExifInterface(it).rotationDegrees }
        } catch (_: Exception) { 0 } ?: 0

        val rotated = if (rotation == 90 || rotation == 180 || rotation == 270) {
            val m = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        } else bmp

        cr.openOutputStream(outDf.uri, "w")?.use { os ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 95, os)
            os.flush()
            if (os is FileOutputStream) try { os.fd.sync() } catch (_: Exception) {}
        }
        outDf.uri
    }

    suspend fun tryRepairVideoTo(
        context: Context,
        source: Uri,
        destDirTree: Uri,
        outBase: String? = null
    ): Uri? = withContext(Dispatchers.IO) {
        val destDir = DocumentFile.fromTreeUri(context, destDirTree) ?: return@withContext null
        val outDf = destDir.createFile("video/mp4", "${outBase ?: "repaired"}.mp4")
            ?: return@withContext null

        // สร้างไฟล์ชั่วคราวให้ Transformer เขียนลง ก่อนคัดลอกเข้าปลายทาง (ผ่าน SAF)
        val tmp = File.createTempFile("repair_", ".mp4", context.cacheDir)
        val transformer = Transformer.Builder(context).build()

        val latch = CountDownLatch(1)
        var success = false
        var error: Exception? = null

        transformer.addListener(object : Transformer.Listener {
            override fun onCompleted(composition: ExportResult) { success = true; latch.countDown() }
            override fun onError(exception: ExportException) { error = exception; latch.countDown() }
        })

        transformer.start(MediaItem.fromUri(source), tmp.absolutePath)
        latch.await(10, TimeUnit.MINUTES)

        if (!success) {
            tmp.delete()
            throw (error ?: RuntimeException("Transformer failed"))
        }

        // คัดลอกไฟล์ชั่วคราวเข้า DocumentFile แบบปลอดภัย
        FileInputStream(tmp).use { inp ->
            context.contentResolver.openOutputStream(outDf.uri, "w")?.use { out ->
                val buf = ByteArray(128 * 1024)
                while (true) {
                    val n = inp.read(buf)
                    if (n <= 0) break
                    out.write(buf, 0, n)
                }
                out.flush()
                if (out is FileOutputStream) try { out.fd.sync() } catch (_: Exception) {}
            }
        }
        tmp.delete()
        outDf.uri
    }
}
