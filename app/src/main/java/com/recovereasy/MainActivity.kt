package com.recovereasy

import android.net.Uri
import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.recovereasy.storage.DeepScanner
import com.recovereasy.storage.OtgPicker
import com.recovereasy.storage.SafeCopier
import com.recovereasy.storage.FileRepair
import com.recovereasy.storage.ScannedItem
import com.recovereasy.ui.FileAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var otgTreeUri: Uri? = null
    private lateinit var otgPicker: OtgPicker

    private lateinit var list: RecyclerView
    private lateinit var adapter: FileAdapter
    private lateinit var txtCount: TextView
    private lateinit var chkDeep: CheckBox

    private val selected = LinkedHashSet<ScannedItem>()
    private var lastScan: List<ScannedItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ทำให้ Coil รองรับวิดีโอเฟรม
        val imgLoader = ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
        Coil.setImageLoader(imgLoader)

        otgPicker = OtgPicker(this) { picked ->
            otgTreeUri = picked
            toast("เลือก OTG แล้ว")
        }

        // binding
        findViewById<android.view.View>(R.id.btnOtg).setOnClickListener { otgPicker.open(otgTreeUri) }
        findViewById<android.view.View>(R.id.btnStartScan).setOnClickListener { startScan() }
        findViewById<android.view.View>(R.id.btnToggleAll).setOnClickListener { toggleAll() }
        findViewById<android.view.View>(R.id.btnCopy).setOnClickListener { copySelected() }
        findViewById<android.view.View>(R.id.btnRepair).setOnClickListener { repairSelected() }

        chkDeep = findViewById(R.id.chkDeep)
        txtCount = findViewById(R.id.txtCount)
        list = findViewById(R.id.recycler)
        list.layoutManager = LinearLayoutManager(this)

        adapter = FileAdapter(
            onToggle = { item, checked ->
                if (checked) selected.add(item) else selected.remove(item)
                updateCount()
            }
        )
        list.adapter = adapter

        updateCount()
    }

    private fun startScan() {
        val tree = otgTreeUri
        if (tree == null) {
            toast("กรุณาเลือก OTG/การ์ดก่อน")
            return
        }
        val deep = chkDeep.isChecked
        lifecycleScope.launch {
            toast(if (deep) "กำลังสแกน (ลึก)..." else "กำลังสแกน (เร็ว)...")
            val items = DeepScanner.scanTree(this@MainActivity, tree, deep)
            lastScan = items
            selected.clear()
            adapter.submit(items)
            updateCount()
            toast("สแกนเสร็จ พบ ${items.size} ไฟล์")
        }
    }

    private fun toggleAll() {
        if (selected.size < lastScan.size) {
            selected.clear()
            selected.addAll(lastScan)
            adapter.setAllChecked(true)
        } else {
            selected.clear()
            adapter.setAllChecked(false)
        }
        updateCount()
    }

    private fun copySelected() {
        if (selected.isEmpty()) { toast("ยังไม่เลือกรายการ"); return }
        // ให้ผู้ใช้เลือกปลายทาง (โฟลเดอร์ใดก็ได้ผ่าน SAF)
        val picker = OtgPicker(this) { dest ->
            lifecycleScope.launch(Dispatchers.IO) {
                var i = 0
                for (item in selected) {
                    SafeCopier.copyToDir(
                        context = this@MainActivity,
                        source = item.uri,
                        destDirTree = dest,
                        desiredName = item.name
                    )
                    i++
                }
                withContext(Dispatchers.Main) { toast("คัดลอกเสร็จ $i ไฟล์") }
            }
        }
        picker.open()
    }

    private fun repairSelected() {
        if (selected.isEmpty()) { toast("ยังไม่เลือกรายการ"); return }
        val picker = OtgPicker(this) { dest ->
            lifecycleScope.launch(Dispatchers.IO) {
                var ok = 0
                for (f in selected) {
                    try {
                        if (f.isImage) {
                            FileRepair.tryRepairImageTo(this@MainActivity, f.uri, dest, f.name.substringBeforeLast('.'))
                            ok++
                        } else if (f.isVideo) {
                            FileRepair.tryRepairVideoTo(this@MainActivity, f.uri, dest, f.name.substringBeforeLast('.'))
                            ok++
                        } else {
                            // ไฟล์อื่นคัดลอกแทน
                            SafeCopier.copyToDir(this@MainActivity, f.uri, dest, f.name)
                            ok++
                        }
                    } catch (_: Exception) { /* ข้ามไฟล์ที่ซ่อมไม่ได้ */ }
                }
                withContext(Dispatchers.Main) { toast("ซ่อม/คัดลอกเสร็จ $ok ไฟล์") }
            }
        }
        picker.open()
    }

    private fun updateCount() {
        txtCount.text = "${selected.size} / ${lastScan.size} รายการ"
    }

    private fun toast(msg: String) =
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
}
