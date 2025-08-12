package com.recovereasy

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

  private lateinit var recycler: RecyclerView
  private lateinit var status: TextView
  private val adapter = MediaAdapter(emptyList())

  private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { loadImages() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    setSupportActionBar(findViewById(R.id.toolbar))

    recycler = findViewById(R.id.recycler)
    status = findViewById(R.id.status)

    recycler.layoutManager = LinearLayoutManager(this)
    recycler.adapter = adapter

    findViewById<Button>(R.id.btnImages).setOnClickListener { ensurePerms { loadImages() } }
    findViewById<Button>(R.id.btnVideos).setOnClickListener { ensurePerms { loadVideos() } }
    findViewById<Button>(R.id.btnAudios).setOnClickListener { ensurePerms { loadAudios() } }

    ensurePerms { loadImages() }
  }

  private fun ensurePerms(onOk: () -> Unit) {
    val perms = if (Build.VERSION.SDK_INT >= 33) {
      arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO
      )
    } else {
      arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val need = perms.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
    if (need) permissionLauncher.launch(perms) else onOk()
  }

  private fun loadImages() {
    adapter.submit(query(
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.MIME_TYPE
      ),
      isAudio = false,
      idColumn = MediaStore.Images.Media._ID
    ))
    status.text = getString(R.string.images)
  }

  private fun loadVideos() {
    adapter.submit(query(
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
      arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.MIME_TYPE
      ),
      isAudio = false,
      idColumn = MediaStore.Video.Media._ID
    ))
    status.text = getString(R.string.videos)
  }

  private fun loadAudios() {
    adapter.submit(query(
      MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
      arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.MIME_TYPE
      ),
      isAudio = true,
      idColumn = MediaStore.Audio.Media._ID
    ))
    status.text = getString(R.string.audios)
  }

  private fun query(
    collection: Uri,
    projection: Array<String>,
    isAudio: Boolean,
    idColumn: String
  ): List<MediaItem> {
    val items = mutableListOf<MediaItem>()
    contentResolver.query(
      collection, projection, null, null,
      "${MediaStore.MediaColumns.DATE_ADDED} DESC"
    )?.use { c ->
      val idIdx = c.getColumnIndexOrThrow(idColumn)
      val nameIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
      val mimeIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
      while (c.moveToNext()) {
        val id = c.getLong(idIdx)
        val name = c.getString(nameIdx) ?: ""
        val mime = c.getString(mimeIdx)
        items.add(
          MediaItem(
            ContentUris.withAppendedId(collection, id),
            name,
            mime,
            isAudio
          )
        )
      }
    }
    return items
  }
}
