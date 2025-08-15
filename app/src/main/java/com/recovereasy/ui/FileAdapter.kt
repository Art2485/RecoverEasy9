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
import coil.request.videoFrameMillis
import coil.size.Scale
import kotlin.math.max

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

    /** กดครั้งแรกเลือกทั้งหมด, กดซ้ำยกเลิกทั้งหมด */
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
        private val name: TextView  = v.findViewById(R.id.name)
        private val meta: TextView  = v.findViewById(R.id.meta)
        private val check: CheckBox = v.findViewById(R.id.check)

        fun bind(item: MediaItem) {
            name.text = item.name
            meta.text = "${readableSize(item.size)} • ${item.mime.ifBlank { "unknown" }}"

            // ห้ามแก้ currentList โดยตรง — สร้างลิสต์ใหม่แล้ว submit
            check.setOnCheckedChangeListener(null)
            check.isChecked = item.checked
            check.setOnCheckedChangeListener { _, isChecked ->
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val updated = currentList.toMutableList()
                    updated[pos] = item.copy(checked = isChecked)
                    submitList(updated)
                }
            }

            // พรีวิวภาพ/วิดีโอ (ต้องมี coil + coil-video)
            thumb.load(item.uri) {
                crossfade(true)
                scale(Scale.FILL)
                if (item.mime.startsWith("video/")) {
                    videoFrameMillis(0) // ดึงเฟรมแรกของวิดีโอ
                }
            }
        }

        /** เลี่ยงการใช้ pow() → หาร 1024 ทีละสเต็ปเพื่อความเข้ากันได้ */
        private fun readableSize(bytes: Long): String {
            var size = max(0L, bytes).toDouble()
            val units = arrayOf("B","KB","MB","GB","TB","PB")
            var idx = 0
            while (size >= 1024.0 && idx < units.lastIndex) {
                size /= 1024.0
                idx++
            }
            return String.format("%.1f %s", size, units[idx])
        }
    }
}
