package ani.dantotsu.media.anime

import StreamingEpisode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.databinding.ItemEpisodeListBinding
import ani.dantotsu.loadImage
import bit.himitsu.webkit.StreamBottomDialog
import ani.dantotsu.toPx

class StreamingAdapter(
    var manager: FragmentManager, var streamingEpisodes: List<StreamingEpisode>
) : RecyclerView.Adapter<StreamingAdapter.EpisodeViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding = ItemEpisodeListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.itemDownload.visibility = View.GONE
        binding.itemEpisodeDesc.visibility = View.GONE
        binding.itemEpisodeViewed.visibility = View.GONE
        binding.itemEpisodeHeading.updatePadding(right = 8.toPx)
        return EpisodeViewHolder(binding)
    }

    fun appendEpisodes(additional: List<StreamingEpisode>) {
        streamingEpisodes = streamingEpisodes.plus(additional)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val binding = holder.binding
        val episode = streamingEpisodes[position]
        binding.itemEpisodeImage.loadImage(episode.thumbnail)
        val number = episode.title?.removePrefix("Episode ")?.substringBefore(" -")
            ?: "${holder.absoluteAdapterPosition + 1}"
        binding.itemEpisodeNumber.text = number
        binding.itemEpisodeTitle.text = episode.title
    }

    override fun getItemCount(): Int = streamingEpisodes.size

    inner class EpisodeViewHolder(val binding: ItemEpisodeListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val episode = streamingEpisodes[bindingAdapterPosition]
                episode.url?.let {
                    StreamBottomDialog.newInstance(it).apply {
                        show(manager, "dialog")
                    }
                }
            }
        }
    }
}
