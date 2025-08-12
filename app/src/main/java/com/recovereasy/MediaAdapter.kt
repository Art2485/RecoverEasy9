package com.recovereasy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.load
import com.recovereasy.databinding.ItemMediaBinding

class MediaAdapter(
  private val imageLoader: ImageLoader,
  private val onToggle: (MediaItem) -> Unit,
  private val isSelected: (MediaItem) -> Boolean
) : ListAdapter<MediaItem, MediaAdapter.VH>(Diff) {

  object Diff : DiffUtil.ItemCallback<MediaItem>() {
    override fun areItemsTheSame(a: MediaItem, b: MediaItem) = a.uri == b.uri
    override fun areContentsTheSame(a: MediaItem, b: MediaItem) = a == b
  }

  inner class VH(val b: ItemMediaBinding) : RecyclerView.ViewHolder(b.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    val b = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return VH(b)
  }

  override fun onBindViewHolder(holder: VH, position: Int) {
    val item = getItem(position)
    val b = holder.b

    // icon overlay
    b.icType.visibility = if (item.kind == MediaKind.VIDEO) View.VISIBLE else View.GONE

    // preview
    b.thumb.scaleType = ImageView.ScaleType.CENTER_CROP
    b.thumb.load(item.uri, imageLoader) {
      crossfade(true)
      allowHardware(false)
    }

    // selection overlay
    b.selOverlay.visibility = if (isSelected(item)) View.VISIBLE else View.GONE

    b.root.setOnClickListener { onToggle(item); notifyItemChanged(position) }
  }
}

fun provideImageLoader(view: View): ImageLoader =
  ImageLoader.Builder(view.context)
    .components { add(VideoFrameDecoder.Factory()) }
    .build()
