package com.recovereasy

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ImageRequest
import coil.size.Scale

data class MediaItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mime: String,
    var checked: Boolean = false
)

class FileAdapter :
    ListAdapter<MediaItem, FileAdapter.VH>(diff) {

    companion object {
        private val diff = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                oldItem.uri == newItem.uri

            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                oldItem == newItem
        }
    }

    fun selectedCount(): Int = currentList.count { it.checked }

    fun toggleAll() {
        val allSelected = currentList.all { it.checked }
        val updated = currentList.map { it.copy(checked = !allSelected) }
        submitList(updated)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val thumb: ImageView = v.findViewById(R.id.thumb)
        private val name: TextView = v.findViewById(R.id.name)
        private val meta: TextView = v.findViewById(R.id.meta)
        private val check: CheckBox = v.findViewById(R.id.check)

        fun bind(item: MediaItem) {
            name.text = item.name
            meta.text = readableSize(item.size)

            check.setOnCheckedChangeListener(null)
            check.isChecked = item.checked
            check.setOnCheckedChangeListener { _, isChecked ->
                currentList[bindingAdapterPosition].checked = isChecked
            }

            // พรีวิวรูป/วิดีโอ/ไฟล์ทั่วไปด้วย Coil
            val req = ImageRequest.Builder(thumb.context)
                .data(item.uri)
                .crossfade(true)
                .scale(Scale.FILL)
                .build()
            thumb.load(req)
        }

        private fun readableSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val u = arrayOf("B", "KB", "MB", "GB", "TB")
            val grp = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format("%.1f %s", bytes / Math.pow(1024.0, grp.toDouble()), u[grp])
        }
    }
}
