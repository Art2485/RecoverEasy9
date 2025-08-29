package com.recovereasy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.decode.GifDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtgActivity : AppCompatActivity() {

    private lateinit var grantBtn: Button
    private lateinit var scanBtn: Button
    private lateinit var copyBtn: Button
    private lateinit var toggleBtn: Button
    private lateinit var deepChk: CheckBox
    private lateinit var status: TextView
    private lateinit var list: RecyclerView
    private lateinit var progress: ProgressBar
    private val adapter = FileAdapter()

    private val ioScope = CoroutineScope(Dispatchers.IO + Job())

    private val pickTree =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                val treeUri = res.data?.data ?: return@registerForActivityResult
                // persist access
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                prefs().edit { putString(KEY_TREE, treeUri.toString()) }
                status("ได้รับสิทธิ์ OTG แล้ว")
            } else {
                status("ยกเลิกการให้สิทธิ์ OTG")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // เพิ่มโหลดเดอร์ของ Coil สำหรับพรีวิว
        if (Coil.imageLoader(this) == Coil.imageLoader(this).defaults) {
            val loader = ImageLoader.Builder(this)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                    else add(GifDecoder.Factory())
                }.build()
            Coil.setImageLoader(loader)
        }
        setContentView(R.layout.activity_otg)

        grantBtn = findViewById(R.id.btnGrant)
        scanBtn = findViewById(R.id.btnScan)
        copyBtn = findViewById(R.id.btnCopy)
        toggleBtn = findViewById(R.id.btnToggle)
        deepChk = findViewById(R.id.chkDeep)
        status = findViewById(R.id.txtStatus)
        list = findViewById(R.id.recycler)
        progress = findViewById(R.id.progress)

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        grantBtn.setOnClickListener { requestOtgAccess() }
        scanBtn.setOnClickListener { startScan(deepChk.isChecked) }
        toggleBtn.setOnClickListener {
            adapter.toggleAll()
            updateSel()
        }
        copyBtn.setOnClickListener { copySelected() }

        updateSel()
        if (treeUri() != null) status("พร้อมสแกน OTG แล้ว")
        else status("โปรดกด “ให้สิทธิ์ OTG” ก่อน")
    }

    private fun prefs() = getSharedPreferences("otg", Context.MODE_PRIVATE)

    private fun treeUri(): Uri? = prefs().getString(KEY_TREE, null)?.let(Uri::parse)

    private fun requestOtgAccess() {
        // พยายามชี้ไปยังไดรฟ์ถอดได้ (OTG) ถ้ามี
        val sm = getSystemService(StorageManager::class.java)
        val removable = try {
            sm.storageVolumes.firstOrNull { !it.isPrimary && it.isRemovable }
        } catch (_: Throwable) { null }

        val intent = if (removable != null && Build.VERSION.SDK_INT >= 29) {
            // ขอสิทธิ์โฟลเดอร์รากของไดรฟ์นั้น
            removable.createAccessIntent(null)
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }
        }
        pickTree.launch(intent)
    }

    private fun startScan(deep: Boolean) {
        val root = treeUri()
        if (root == null) {
            status("ยังไม่ได้ให้สิทธิ์ OTG")
            return
        }
        progress.visible(true)
        status("กำลังสแกน… (deep=$deep)")
        ioScope.launch {
            val items = scanDocumentTree(this@OtgActivity, root, deep)
            withContext(Dispatchers.Main) {
                adapter.submitList(items)
                progress.visible(false)
                status("พบไฟล์ ${items.size} รายการ  |  เลือกไว้ ${adapter.selectedCount()}")
            }
        }
    }

    private fun copySelected() {
        val sel = adapter.currentList.filter { it.checked }
        if (sel.isEmpty()) {
            status("ยังไม่ได้เลือกไฟล์")
            return
        }
        progress.visible(true)
        status("กำลังก๊อป ${sel.size} ไฟล์…")
        ioScope.launch {
            var ok = 0
            val outDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: filesDir
            for (m in sel) {
                runCatching {
                    val name = m.name.ifBlank { "file_${System.currentTimeMillis()}" }
                    contentResolver.openInputStream(m.uri).use { `in` ->
                        val out = java.io.File(outDir, name)
                        out.outputStream().use { `out` ->
                            if (`in` == null) error("เปิดอ่านไม่ได้")
                            `in`.copyTo(`out`, 64 * 1024)
                            `out`.fd.sync() // เขียนแบบปลอดภัย
                        }
                    }
                    ok++
                }
            }
            withContext(Dispatchers.Main) {
                progress.visible(false)
                status("ก๊อปเสร็จ: สำเร็จ $ok/${sel.size} ไฟล์ → ${outDir.absolutePath}")
            }
        }
    }

    private fun updateSel() {
        status("พบไฟล์ ${adapter.currentList.size} รายการ | เลือกไว้ ${adapter.selectedCount()}")
    }

    private fun status(msg: String) { status.text = msg }

    companion object {
        private const val KEY_TREE = "treeUri"
    }
}

/* ---------- สแกน OTG (DocumentFile) ---------- */

suspend fun scanDocumentTree(ctx: Context, root: Uri, deep: Boolean): List<MediaItem> =
    withContext(Dispatchers.IO) {
        val out = ArrayList<MediaItem>(256)
        val rootDf = DocumentFile.fromTreeUri(ctx, root) ?: return@withContext out
        fun walk(df: DocumentFile) {
            if (df.isFile) {
                val name = df.name ?: "unknown"
                val size = df.length()
                val mime = df.type ?: "application/octet-stream"
                out += MediaItem(df.uri, name, size, mime, false)
            } else if (df.isDirectory) {
                val kids = df.listFiles()
                for (f in kids) {
                    if (f.isDirectory && !deep) continue
                    walk(f)
                }
            }
        }
        walk(rootDf)
        out.sortBy { it.name.lowercase() }
        out
    }

/* ---------- ยูทิลเสริม ---------- */
private fun java.io.FileOutputStream.fd() =
    java.io.FileDescriptor::class.java.getDeclaredField("fd").let {
        it.isAccessible = true
        it.get(this)
    }.let { this.fd }

private fun ProgressBar.visible(v: Boolean) { this.visibility = if (v) android.view.View.VISIBLE else android.view.View.GONE }
