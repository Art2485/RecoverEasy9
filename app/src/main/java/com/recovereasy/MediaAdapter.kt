package com.recovereasy

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.videoFrameMillis

class MediaAdapter(
    private val items: MutableList<MediaItem>
) : RecyclerView.Adapter<MediaAdapter.Holder>() {

    class Holder(val root: ViewGroup) : RecyclerView.ViewHolder(root) {
        val image = ImageView(root.context).apply {
            layoutParams = ViewGroup.LayoutParams(200, 200)
            adjustViewBounds = true
        }
        val title = TextView(root.context)
        init {
            root.addView(image)
            root.addView(title)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val container = object : ViewGroup(parent.context) {
            override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
                // เรียงภาพ-ชื่อแบบง่าย ๆ
                val w = width
                val hImg = image.measuredHeight
                getChildAt(0).layout(0, 0, hImg, hImg)
                getChildAt(1).layout(hImg + 16, 0, w, hImg)
            }
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                measureChildren(widthMeasureSpec, heightMeasureSpec)
                val h = getChildAt(0).measuredHeight
                setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), h + 16)
            }
        }
        return Holder(container)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val it = items[position]
        h.title.text = it.name
        h.image.load(it.uri) {
            // ถ้าเป็นวิดีโอให้ดึงเฟรมแรกมาแสดง (ต้องมี import coil.request.videoFrameMillis)
            if (it.kind == MediaKind.VIDEO) {
                videoFrameMillis(0)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<MediaItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}
