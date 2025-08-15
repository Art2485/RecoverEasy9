package com.recovereasy.ui

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
import coil.size.Scale
import coil.request.videoFrameMillis
import com.recovereasy.MediaItem
import com.recovereasy.R
import kotlin.math.log10

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

            // อย่าแก้ currentList ตรง ๆ — ต้อง submitList ใหม่
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

            // พรีวิวรูป/วิดีโอด้วย Coil (ต้องมี coil + coil-video)
            thumb.load(item.uri) {
                crossfade(true)
                scale(Scale.FILL)
                if (item.mime.startsWith("video/")) {
                    videoFrameMillis(0) // เฟรมแรกของวิดีโอ
                }
            }
        }

        /** หลีกเลี่ยง pow() → ใช้วิธีหาร 1024 ทีละขั้น เพื่อเลี่ยง Unresolved reference: pow */
        private fun readableSize(bytes: Long): String {
            var size = if (bytes < 0L) 0.0 else bytes.toDouble()
            val units = arrayOf("B","KB","MB","GB","TB","PB")
            var i = 0
            while (size >= 1024 && i < units.lastIndex) {
                size /= 1024.0
                i++
            }
            return String.format("%.1f %s", size, units[i])
        }
    }
}
