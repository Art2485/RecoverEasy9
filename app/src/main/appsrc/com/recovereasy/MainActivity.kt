package com.recovereasy

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.recovereasy.model.MediaItem
import com.recovereasy.model.ScanScope
import com.recovereasy.scan.Scanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val scope = remember { mutableStateOf(ScanScope.PHONE) }
                    var deep by remember { mutableStateOf(false) }
                    var pickedTree by remember { mutableStateOf<Uri?>(null) }
                    var loading by remember { mutableStateOf(false) }
                    var list by remember { mutableStateOf(listOf<MediaItem>()) }
                    var selected by remember { mutableStateOf(setOf<Uri>()) }

                    val ioScope = rememberCoroutineScope()

                    // ขอ permission ตามเวอร์ชัน
                    val perms = if (Build.VERSION.SDK_INT >= 33)
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                        )
                    else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

                    val requestPerms = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { /* ignore */ }

                    // เปิดโฟลเดอร์ (SAF)
                    val openTree = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocumentTree()
                    ) { uri ->
                        if (uri != null) {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            )
                            pickedTree = uri
                        }
                    }

                    LaunchedEffect(Unit) { requestPerms.launch(perms) }

                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("พื้นที่สแกน: ทั้งเครื่อง (เร็ว) / OTG / โฟลเดอร์", style = MaterialTheme.typography.titleMedium)

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { scope.value = ScanScope.PHONE }) { Text("ทั้งเครื่อง") }
                            Button(onClick = {
                                scope.value = ScanScope.OTG
                                openTree.launch(null)
                            }) { Text("OTG / การ์ด") }
                            Button(onClick = {
                                scope.value = ScanScope.FOLDER
                                openTree.launch(null)
                            }) { Text("เลือกโฟลเดอร์") }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = deep, onCheckedChange = { deep = it })
                            Text("สแกนละเอียด (Deep Scan)")
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    requestPerms.launch(perms)
                                    loading = true
                                    ioScope.launch(Dispatchers.IO) {
                                        val result = Scanner.scan(
                                            this@MainActivity,
                                            scope.value,
                                            deep,
                                            pickedTree
                                        )
                                        list = result
                                        selected = emptySet()
                                        loading = false
                                    }
                                }
                            ) { Text("เริ่มสแกน") }

                            Button(
                                onClick = {
                                    selected =
                                        if (selected.size == list.size) emptySet()
                                        else list.map { it.uri }.toSet()
                                }
                            ) {
                                Text(
                                    if (selected.size == list.size && list.isNotEmpty())
                                        "ยกเลิกทั้งหมด" else "เลือกทั้งหมด"
                                )
                            }

                            Text("${selected.size} / ${list.size} รายการ", modifier = Modifier.align(Alignment.CenterVertically))
                        }

                        if (loading) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }

                        // พรีวิวเป็นตาราง
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(120.dp),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            content = {
                                items(list, key = { it.uri }) { item ->
                                    Card(
                                        onClick = {
                                            selected =
                                                if (selected.contains(item.uri))
                                                    selected - item.uri
                                                else selected + item.uri
                                        },
                                        modifier = Modifier
                                            .height(140.dp)
                                            .fillMaxWidth()
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(this@MainActivity)
                                                .data(item.uri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = item.name
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
