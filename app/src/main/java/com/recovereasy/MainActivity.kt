package com.recovereasy

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.recovereasy.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ScanScope {
  object PhoneFast : ScanScope()
  data class Folder(val uri: Uri, val label: String) : ScanScope()
  object PhoneDeep : ScanScope()
}

class MainActivity : AppCompatActivity() {

  private lateinit var b: ActivityMainBinding
  private lateinit var adapter: MediaAdapter
  private val selected = linkedSetOf<MediaItem>()
  private var currScope: ScanScope = ScanScope.PhoneFast

  // pick folder (ใช้ได้ทั้ง OTG/การ์ด และโฟลเดอร์ในเครื่อง)
  private val pickTree = registerForActivityResult(
    ActivityResultContracts.OpenDocumentTree()
  ) { uri ->
    if (uri != null) {
      // persist
      contentResolver.takePersistableUriPermission(
        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
      )
      currScope = ScanScope.Folder(uri, "folder")
      b.tvScope.text = "พื้นที่สแกน: โฟลเดอร์ที่เลือก"
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    b = ActivityMainBinding.inflate(layoutInflater)
    setContentView(b.root)

    adapter = MediaAdapter(
      provideImageLoader(b.root),
      onToggle = { item ->
        if (!selected.add(item)) selected.remove(item)
        updateSelectedUi()
      },
      isSelected = { selected.contains(it) }
    )
    b.rv.layoutManager = GridLayoutManager(this, 3)
    b.rv.adapter = adapter

    b.btnPhone.setOnClickListener {
      currScope = if (b.cbDeep.isChecked) ScanScope.PhoneDeep else ScanScope.PhoneFast
      b.tvScope.text = if (b.cbDeep.isChecked) "พื้นที่สแกน: ทั้งเครื่อง (Deep)" else "พื้นที่สแกน: ทั้งเครื่อง (เร็ว)"
    }
    b.btnOtg.setOnClickListener {
      // ให้ผู้ใช้เลือก root ของ USB/OTG
      pickTree.launch(null)
    }
    b.btnPick.setOnClickListener { pickTree.launch(null) }

    b.btnScan.setOnClickListener { startScan() }

    b.btnToggleAll.setOnClickListener {
      if (selected.size == adapter.itemCount) {
        selected.clear()
        b.btnToggleAll.text = "เลือกทั้งหมด"
      } else {
        selected.clear()
        adapter.currentList.forEach { selected.add(it) }
        b.btnToggleAll.text = "ยกเลิกทั้งหมด"
      }
      adapter.notifyDataSetChanged()
      updateSelectedUi()
    }

    requestReadPermissionsIfNeeded()
  }

  private fun updateSelectedUi() {
    b.tvCount.text = "${selected.size} รายการ"
  }

  private fun requestReadPermissionsIfNeeded() {
    if (Build.VERSION.SDK_INT >= 33) {
      permLauncher.launch(arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO
      ))
    } else {
      permLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
    }
  }

  private val permLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { /* ignore - เราจะลองสแกน ถ้าอดก็แสดงผลว่างๆ */ }

  private fun ensureManageAllFilesIfNeeded(): Boolean {
    if (currScope is ScanScope.PhoneDeep) {
      if (!Environment.isExternalStorageManager()) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
        return false
      }
    }
    return true
  }

  private fun startScan() {
    if (!ensureManageAllFilesIfNeeded()) return

    b.btnScan.isEnabled = false
    b.tvCount.text = "กำลังสแกน..."
    selected.clear()

    lifecycleScope.launch {
      val items = when (val s = currScope) {
        is ScanScope.PhoneFast -> Scanner.scanPhoneFast(this@MainActivity)
        is ScanScope.Folder -> Scanner.scanTreeDeep(this@MainActivity, s.uri, s.label)
        is ScanScope.PhoneDeep -> Scanner.scanWholeStorageDeep(this@MainActivity)
      }

      adapter.submitList(items)
      selected.clear()
      b.btnToggleAll.text = "เลือกทั้งหมด"
      updateSelectedUi()
      b.btnScan.isEnabled = true
    }
  }
}
