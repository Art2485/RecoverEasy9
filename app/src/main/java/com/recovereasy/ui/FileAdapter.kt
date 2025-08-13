package com.recovereasy.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.recovereasy.R
import com.recovereasy.storage.ScannedItem
import java.text.DecimalFormat

class FileAdapter(
    private val onToggle: (ScannedItem, Boolean) -> Unit
) : RecyclerView.Adapter<FileAdapter.VH>() {

    private val items = ArrayList<ScannedItem>()
    private val checked = HashSet<UriKey>()

    data class UriKey(val s: String)

    fun submit(newItems: List<ScannedItem>) {
        items.clear()
        items.addAll(newItems)
        checked.clear()
        notifyDataSetChanged()
    }

    fun setAllChecked(all: Boolean) {
        checked.clear()
        if (all) items.forEach { checked.add(UriKey(it.uri.toString())) }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val it = items[position]
        h.name.text = it.name
        h.meta.text = "${pretty(it.size)} • ${it.mime ?: "-"}"
        h.check.setOnCheckedChangeListener(null)
        h.check.isChecked = checked.contains(UriKey(it.uri.toString()))

        // พรีวิว: รูป/วิดีโอ ได้เฟรมแรก
        h.thumb.load(it.uri) {
            if (it.isVideo) {
                videoFrameMillis(0)
            }
        }

        h.itemView.setOnClickListener {
            val now = !h.check.isChecked
            h.check.isChecked = now
        }
        h.check.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) checked.add(UriKey(it.uri.toString()))
            else checked.remove(UriKey(it.uri.toString()))
            onToggle(it, isChecked)
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val thumb: ImageView = v.findViewById(R.id.thumb)
        val name: TextView = v.findViewById(R.id.name)
        val meta: TextView = v.findViewById(R.id.meta)
        val check: CheckBox = v.findViewById(R.id.check)
    }

    private fun pretty(bytes: Long): String {
        if (bytes < 0) return "-"
        if (bytes < 1024) return "$bytes B"
        val z = (63 - java.lang.Long.numberOfLeadingZeros(bytes)) / 10
        val df = DecimalFormat("#,##0.#")
        return df.format(bytes / Math.pow(1024.0, z.toDouble())) + " " + " KMGTPE"[z] + "B"
    }
}
