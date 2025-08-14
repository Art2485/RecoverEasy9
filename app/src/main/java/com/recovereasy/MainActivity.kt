package com.recovereasy

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.recovereasy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: FileAdapter

    private lateinit var pickOtgDir: ActivityResultLauncher<Intent>
    private lateinit var pickFolderDir: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView
        adapter = FileAdapter()
        binding.recycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            itemAnimator = null
            setHasFixedSize(true)
        }

        // ActivityResult launchers
        pickOtgDir = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { res ->
            val uri = res.data?.data ?: return@registerForActivityResult
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            scanUriRoot(uri)
        }

        pickFolderDir = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { res ->
            val uri = res.data?.data ?: return@registerForActivityResult
            scanUriRoot(uri)
        }

        // ปุ่ม “ทั้งเครื่อง (เร็ว)”
        
        binding.btnAll.setOnClickListener {
            // สแกนทุก volume (primary + sdcard + usb ที่เมานท์) ผ่าน SAF
            val sm = getSystemService(android.os.storage.StorageManager::class.java)
            val volumes = sm?.storageVolumes ?: emptyList()
            val uris = mutableListOf<android.net.Uri>()
            volumes.forEach { vol ->
                val id = try { vol.uuid ?: "primary" } catch (_: Throwable) { "primary" }
                val docId = if (id == "primary") "primary:" else "$id:"
                uris += android.provider.DocumentsContract.buildTreeDocumentUri(
                    "com.android.externalstorage.documents",
                    docId
                )
            }
            // ถ้าไม่เจอใด ๆ ให้ fallback เป็น primary
            if (uris.isEmpty()) {
                uris += android.provider.DocumentsContract.buildTreeDocumentUri(
                    "com.android.externalstorage.documents","primary:"
                )
            }
            // รวมผลทุกตัว
            clearResults()
            uris.forEach { scanUriRoot(it) }
        }
        

        // ปุ่ม “OTG / การ์ด”
        binding.btnOtg.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
                if (Build.VERSION.SDK_INT >= 26) {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A"))
                }
            }
            pickOtgDir.launch(intent)
        }

        // ปุ่ม “เลือกโฟลเดอร์”
        binding.btnPickFolder.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }
            pickFolderDir.launch(intent)
        }

        // ปุ่ม “เริ่มสแกน”
        binding.btnScan.setOnClickListener {
            Toast.makeText(this, "เลือกพื้นที่หรือโฟลเดอร์ก่อน แล้วกดสแกนค่ะ", Toast.LENGTH_SHORT).show()
        }

        // ปุ่ม “เลือกทั้งหมด”
        binding.btnToggleAll.setOnClickListener {
            adapter.toggleAll()
            updateSelectedCount()
        }

        updateSelectedCount()
    }

    private fun scanUriRoot(rootUri: Uri) {
        binding.progress.visibility = View.VISIBLE
        binding.empty.visibility = View.GONE
        binding.txtCount.text = "กำลังสแกน…"

        val rootDf = DocumentFile.fromTreeUri(this, rootUri)
        if (rootDf == null || !rootDf.isDirectory) {
            binding.progress.visibility = View.GONE
            Toast.makeText(this, "เข้าถึงโฟลเดอร์ไม่ได้", Toast.LENGTH_SHORT).show()
            return
        }

        // สแกนแบบ breadth-first โดยใช้ SAF => ครอบคลุม OTG/การ์ด ได้แน่
        val results = mutableListOf<MediaItem>()
        fun walk(df: DocumentFile) {
            df.listFiles().forEach { f ->
                if (f.isDirectory) {
                    walk(f)
                } else if (f.isFile) {
                    // เก็บทุกสกุลไฟล์
                    results.add(
                        MediaItem(
                            uri = f.uri,
                            name = f.name ?: "(no name)",
                            size = f.length(),
                            mime = f.type ?: ""
                        )
                    )
                }
            }
        }
        try {
            walk(rootDf)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        binding.progress.visibility = View.GONE
        adapter.submitList(results)
        binding.empty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        updateSelectedCount()
    }

    
    private fun clearResults() {
        adapter.submitList(emptyList())
        updateSelectedCount()
    }

    private fun updateSelectedCount() {
        binding.txtCount.text = "${adapter.selectedCount()} / ${adapter.itemCount} รายการ"
    }
}
