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
import coil.size.Scale
import coil.request.videoFrameMillis

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

            // ห้ามแก้ currentList ตรง ๆ — ต้องสร้างลิสต์ใหม่แล้ว submitList
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

            // โหลดภาพ/เฟรมวิดีโอด้วย Coil (ต้องมี dependency: coil + coil-video)
            thumb.load(item.uri) {
                crossfade(true)
                scale(Scale.FILL)
                // ถ้าเป็นวิดีโอ coil-video จะดึงเฟรมแรก (0 ms)
                videoFrameMillis(0)
            }
        }

        private fun readableSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val u = arrayOf("B", "KB", "MB", "GB", "TB")
            val grp = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
            val value = bytes / kotlin.math.pow(1024.0, grp.toDouble())
            return String.format("%.1f %s", value, u[grp])
        }
    }
}
