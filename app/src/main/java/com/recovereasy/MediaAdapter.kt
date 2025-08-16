package com.recovereasy

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

/**
 * ใช้ร่วมกับ data class MediaItem ที่มีอยู่แล้ว:
 * data class MediaItem(val uri: Uri, val name: String, val size: Long, val mime: String, var checked: Boolean = false)
 *
 * layout ที่ใช้: res/layout/item_media.xml
 *   - ImageView id: thumb
 *   - TextView  id: name
 *   - TextView  id: meta
 *   - CheckBox  id: check
 */
class MediaAdapter : ListAdapter<MediaItem, MediaAdapter.VH>(diff) {

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
        submitList(currentList.map { it.copy(checked = !allSelected) })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
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
                // อัปเดตสถานะเลือกของ item ปัจจุบัน
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    currentList[pos].checked = isChecked
                }
            }

            // โหลดพรีวิวด้วย Coil (ภาพ/วิดีโอ/ไฟล์ทั่วไป)
            val req = ImageRequest.Builder(thumb.context)
                .data(item.uri)
                .crossfade(true)
                .scale(Scale.FILL)
                .build()
            thumb.load(req)
        }

        private fun readableSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val grp = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
            val value = bytes / kotlin.math.pow(1024.0, grp.toDouble())
            return String.format("%.1f %s", value, units[grp])
        }
    }
}
