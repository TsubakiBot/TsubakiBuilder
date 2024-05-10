package ani.dantotsu.media

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.ItemGenreBinding
import ani.dantotsu.toPx
import kotlinx.coroutines.runBlocking
import java.io.Serializable

class ReviewAdapter(
    private val type: String,
    private val big: Boolean = false
) : RecyclerView.Adapter<ReviewAdapter.GenreViewHolder>() {
    var reviews = listOf<Review>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val binding = ItemGenreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (big) binding.genreCard.updateLayoutParams { height = 72.toPx }
        return GenreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val binding = holder.binding
            val review = reviews[position]
            binding.genreTitle.text = review.summary
    }

    override fun getItemCount(): Int = reviews.size
    inner class GenreViewHolder(val binding: ItemGenreBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val media = runBlocking {
                    Anilist.query.getMedia(reviews[bindingAdapterPosition].mediaId)
                }
                ContextCompat.startActivity(
                    itemView.context,
                    Intent(itemView.context, MediaDetailsActivity::class.java)
                        .putExtra("media", media as Serializable), null
                )
            }
        }
    }
}