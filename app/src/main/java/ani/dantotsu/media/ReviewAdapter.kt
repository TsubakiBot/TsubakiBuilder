package ani.dantotsu.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.databinding.ItemReviewBinding
import ani.dantotsu.loadImage
import ani.dantotsu.profile.activity.ActivityItemBuilder

class ReviewAdapter(val parentActivity: ReviewActivity, val reviews: List<Review>)
    : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val binding = holder.binding
        val review = reviews[position]
        binding.reviewItemName.text = review.media?.mainName()
        binding.reviewItemScore.text = review.score.toString()
        binding.notificationCover.loadImage(review.user?.pfp)
        binding.notificationBanner.loadImage(review.media?.banner ?: review.media?.cover)
        binding.notificationBanner.alpha = 0.5f
        binding.notificationText.text = review.summary
        binding.notificationDate.text = ActivityItemBuilder.getDateTime(review.createdAt)
        val formattedName = " - ${review.user?.name}"
        binding.reviewItemUser.text = formattedName
    }

    override fun getItemCount(): Int = reviews.size
    inner class ReviewViewHolder(val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                parentActivity.openReview(reviews[bindingAdapterPosition])
            }
        }
    }
}