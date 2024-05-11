package ani.dantotsu.media

import android.content.Intent
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemReviewBinding
import ani.dantotsu.databinding.ItemReviewContentBinding
import ani.dantotsu.loadImage
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.activity.ActivityItemBuilder
import ani.himitsu.os.Version
import java.io.Serializable


class ReviewAdapter(val parentActivity: ReviewActivity, val reviews: List<Review>)
    : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val binding = holder.binding
        val review = reviews[position]
        binding.notificationCover.loadImage(review.user?.pfp)
        binding.notificationBanner.loadImage(review.media?.banner ?: review.media?.cover)
        binding.reviewItemName.text = review.media?.mainName()
        binding.reviewItemScore.text = review.score.toString()
        val formattedQuote = "\"${review.summary}\"  - ${review.user?.name}"
        binding.notificationText.text = formattedQuote
        binding.notificationDate.text = ActivityItemBuilder.getDateTime(review.createdAt)
    }

    override fun getItemCount(): Int = reviews.size
    inner class ReviewViewHolder(val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
            val review = reviews[bindingAdapterPosition]
                val dialogView = ItemReviewContentBinding.inflate(parentActivity.layoutInflater)
                dialogView.notificationCover.loadImage(review.media?.cover)
                dialogView.profileUserAvatar.loadImage(review.user?.pfp)
                dialogView.profileUserBanner.loadImage(review.user?.banner)
                dialogView.profileUserName.text = review.user?.name
                dialogView.reviewItemName.text = review.media?.mainName()
                val formattedScore = "${review.score}/100 (${ActivityItemBuilder.getDateTime(review.createdAt)})"
                dialogView.reviewItemRating.text = formattedScore
                dialogView.reviewItemText.text = if (Version.isNougat) {
                    Html.fromHtml(review.body, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    @Suppress("DEPRECATION") Html.fromHtml(review.body)
                }
                dialogView.profileBannerContainer.setOnClickListener {
                    ContextCompat.startActivity(
                        parentActivity,
                        Intent(parentActivity, ProfileActivity::class.java)
                            .putExtra("userId", review.userId), null
                    )
                }
                val alertD = AlertDialog.Builder(parentActivity, R.style.MyPopup)
                alertD.setView(dialogView.root)
                alertD.setPositiveButton(R.string.view) { _, _ ->
                    ContextCompat.startActivity(
                        parentActivity,
                        Intent(parentActivity, MediaDetailsActivity::class.java)
                            .putExtra("media", review.media as Serializable), null
                    )
                }
                alertD.setNegativeButton(R.string.close) { _, _ -> }
                val dialog = alertD.show()
                dialog.window?.setDimAmount(0.8f)
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window?.attributes)
                lp.width = WindowManager.LayoutParams.MATCH_PARENT
                lp.height = WindowManager.LayoutParams.MATCH_PARENT
                dialog.window?.setAttributes(lp)
            }
        }
    }
}
