package com.recovereasy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvScope: TextView
    private lateinit var btnAllDevice: Button
    private lateinit var btnOtg: Button
    private lateinit var btnPickFolder: Button
    private lateinit var cbDeep: CheckBox
    private lateinit var btnScan: Button
    private lateinit var btnSelectAll: Button
    private lateinit var tvSelected: TextView
    private lateinit var progress: ProgressBar
    private lateinit var rv: RecyclerView
    private lateinit var adapter: MediaAdapter

    private var pickedTree: Uri? = null
    private var mode: Mode = Mode.ALL_DEVICE
    private var scanJob: Job? = null

    enum class Mode { ALL_DEVICE, TREE }

    private val reqPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* ignore; กดสแกนอีกครั้งได้เลย */ }

    private val pickTree = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
        pickedTree = uri
        mode = Mode.TREE
        tvScope.text = "พื้นที่สแกน: ${getTreeLabel(uri)}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvScope = findViewById(R.id.tvScope)
        btnAllDevice = findViewById(R.id.btnAllDevice)
        btnOtg = findViewById(R.id.btnOtg)
        btnPickFolder = findViewById(R.id.btnPickFolder)
        cbDeep = findViewById(R.id.cbDeepScan)
        btnScan = findViewById(R.id.btnScan)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        tvSelected = findViewById(R.id.tvSelected)
        progress = findViewById(R.id.progress)
        rv = findViewById(R.id.rv)

        rv.layoutManager = GridLayoutManager(this, 3)
        adapter = MediaAdapter { tvSelected.text = "$it รายการ" }
        rv.adapter = adapter

        btnAllDevice.setOnClickListener {
            mode = Mode.ALL_DEVICE
            tvScope.text = "พื้นที่สแกน: ทั้งเครื่อง (เร็ว)"
        }
        btnOtg.setOnClickListener { pickTree.launch(null) }       // user เลือก OTG root
        btnPickFolder.setOnClickListener { pickTree.launch(null) } // หรือเลือกโฟลเดอร์

        btnScan.setOnClickListener { startScan() }

        btnSelectAll.setOnClickListener {
            val selectAll = adapter.selectedCount() != adapter.itemCount
            adapter.toggleAll(selectAll)
            btnSelectAll.text = if (selectAll) "ยกเลิกทั้งหมด" else "เลือกทั้งหมด"
        }

        requestNeededPermissionsOnce()
    }

    private fun requestNeededPermissionsOnce() {
        val needs = buildList {
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needs.isNotEmpty()) reqPerms.launch(needs.toTypedArray())
    }

    private fun startScan() {
        scanJob?.cancel()
        progress.visibility = View.VISIBLE
        progress.progress = 0

        scanJob = lifecycleScope.launch {
            try {
                val items = when (mode) {
                    Mode.ALL_DEVICE -> FileScanner.quickScan(this@MainActivity) {
                        progress.progress = it
                    }
                    Mode.TREE -> {
                        val tree = pickedTree
                        if (tree == null) {
                            Toast.makeText(this@MainActivity, "ยังไม่เลือกโฟลเดอร์/OTG", Toast.LENGTH_SHORT).show()
                            progress.visibility = View.GONE
                            return@launch
                        }
                        if (!cbDeep.isChecked) {
                            // แม้ไม่ติ๊ก Deep ก็ยังต้องเดินใน Tree แต่จะหยาบ (ปล่อย same)
                        }
                        FileScanner.deepScanTree(this@MainActivity, tree) {
                            progress.progress = it
                        }
                    }
                }
                adapter.submitList(items)
                tvSelected.text = "0 รายการ"
                btnSelectAll.text = "เลือกทั้งหมด"
            } catch (e: SecurityException) {
                Toast.makeText(this@MainActivity, "สิทธิ์ไม่พอ กรุณาอนุญาตการเข้าถึงไฟล์", Toast.LENGTH_LONG).show()
                // เปิดหน้า Permission (กรณีผู้ใช้ปิดถาวร)
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun getTreeLabel(uri: Uri): String {
        val last = uri.lastPathSegment ?: return "โฟลเดอร์ที่เลือก"
        return when {
            last.contains("otg", true) || last.contains("usb", true) -> "OTG"
            else -> "โฟลเดอร์ที่เลือก"
        }
    }
}
