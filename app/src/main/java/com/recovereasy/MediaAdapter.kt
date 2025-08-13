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
import coil.request.VideoFrameMillis
import coil.size.Scale
import android.Manifest

class MediaAdapter(
    private val onSelectionChanged: (Int) -> Unit
) : ListAdapter<MediaItem, MediaAdapter.VH>(DIFF) {

    private val selected = linkedSetOf<MediaItem>()
    fun toggleAll(selectAll: Boolean) {
        selected.clear()
        if (selectAll) selected.addAll(currentList)
        notifyDataSetChanged()
        onSelectionChanged(selected.size)
    }
    fun selectedCount() = selected.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), selected.contains(getItem(position)))
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val thumb: ImageView = v.findViewById(R.id.thumb)
        private val name: TextView = v.findViewById(R.id.name)
        private val check: CheckBox = v.findViewById(R.id.check)

        fun bind(item: MediaItem, checked: Boolean) {
            name.text = item.name
            check.isChecked = checked

            thumb.load(item.uri) {
                crossfade(true)
                scale(Scale.FILL)
                // ถ้าเป็นวีดีโอ ให้ดึงเฟรมแรก
                if (item.mime.startsWith("video")) {
                    videoFrameMillis(VideoFrameMillis.DEFAULT)
                }
            }

            itemView.setOnClickListener {
                if (selected.remove(item).not()) selected.add(item)
                notifyItemChanged(bindingAdapterPosition)
                onSelectionChanged(selected.size)
            }
            check.setOnClickListener {
                if (check.isChecked) selected.add(item) else selected.remove(item)
                onSelectionChanged(selected.size)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                oldItem.uri == newItem.uri
            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                oldItem == newItem
        }
    }
}
