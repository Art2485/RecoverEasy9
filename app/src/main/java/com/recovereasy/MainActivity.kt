import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

// ตั้งค่านี้ในคลาส MainActivity (นอก onCreate)
private var deepScanOtg: Boolean = true  // ถ้าคุณมี checkbox อยู่แล้ว อ่านค่าจริงมาใส่ตัวนี้ก่อนสแกน

private val pickOtgTree = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { res ->
    if (res.resultCode == Activity.RESULT_OK) {
        val uri = res.data?.data ?: return@registerForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try { contentResolver.takePersistableUriPermission(uri, flags) } catch (_: SecurityException) {}
        // เริ่มสแกน
        scanOtgTree(uri)
    }
}

private fun openOtgPicker() {
    val sm = getSystemService(StorageManager::class.java)
    val removable: StorageVolume? = sm?.storageVolumes?.firstOrNull { it.isRemovable && !it.isPrimary }

    val intent = try {
        if (Build.VERSION.SDK_INT >= 29 && removable != null) {
            removable.createOpenDocumentTreeIntent().apply {
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                         Intent.FLAG_GRANT_READ_URI_PERMISSION or
                         Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                         Intent.FLAG_GRANT_READ_URI_PERMISSION or
                         Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
    } catch (_: Throwable) {
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                     Intent.FLAG_GRANT_READ_URI_PERMISSION or
                     Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    pickOtgTree.launch(intent)
}

private fun scanOtgTree(treeUri: android.net.Uri) {
    lifecycleScope.launch {
        // ถ้าคุณมีคลาส Scanner/FileScanner ของตัวเองอยู่แล้ว ให้เรียกของเดิม
        // ตัวอย่างเรียกสแกนผ่าน DocumentFile ทั้งไดรฟ์
        val root = DocumentFile.fromTreeUri(this@MainActivity, treeUri)
        val items = com.recovereasy.Scanner.scanDocumentTree(this@MainActivity, root, deepScanOtg)
        // TODO: อัปเดต UI/Adapter ตามของคุณ
        Snackbar.make(findViewById(android.R.id.content),
            "พบไฟล์ ${items.size} รายการ (OTG)", Snackbar.LENGTH_LONG).show()
    }
}
