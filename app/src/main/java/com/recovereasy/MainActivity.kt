package com.recovereasy

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.recovereasy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val adapter = MediaAdapter(emptyList())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        loadImages()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.btnImages.setOnClickListener { ensurePerms { loadImages() } }
        binding.btnVideos.setOnClickListener { ensurePerms { loadVideos() } }
        binding.btnAudios.setOnClickListener { ensurePerms { loadAudios() } }

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
        val need = perms.any {
            ContextCompat.checkSelfPermission(this, it)
                != PackageManager.PERMISSION_GRANTED
        }
        if (need) permissionLauncher.launch(perms) else onOk()
    }

    private fun loadImages() {
        adapter.submit(queryMedia(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE
            ),
            isAudio = false,
            idColumn = MediaStore.Images.Media._ID
        ))
        binding.status.text = getString(R.string.images)
    }

    private fun loadVideos() {
        adapter.submit(queryMedia(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.MIME_TYPE
            ),
            isAudio = false,
            idColumn = MediaStore.Video.Media._ID
        ))
        binding.status.text = getString(R.string.videos)
    }

    private fun loadAudios() {
        adapter.submit(queryMedia(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.MIME_TYPE
            ),
            isAudio = true,
            idColumn = MediaStore.Audio.Media._ID
        ))
        binding.status.text = getString(R.string.audios)
    }

    private fun queryMedia(
        collection: Uri,
        projection: Array<String>,
        isAudio: Boolean,
        idColumn: String
    ): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(idColumn)
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIdx) ?: ""
                val mime = cursor.getString(mimeIdx)
                val contentUri = ContentUris.withAppendedId(collection, id)
                items.add(MediaItem(contentUri, name, mime, isAudio))
            }
        }
        return items
    }
}
