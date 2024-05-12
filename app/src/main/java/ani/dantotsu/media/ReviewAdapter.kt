package ani.dantotsu.media

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.ActivityReviewViewBinding
import ani.dantotsu.databinding.ItemReviewBinding
import ani.dantotsu.loadImage
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.activity.ActivityItemBuilder
import ani.dantotsu.toast
import ani.dantotsu.util.AniMarkdown
import eu.kanade.tachiyomi.util.system.getThemeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class ReviewAdapter(val parentActivity: ReviewPopupActivity, val reviews: List<Review>)
    : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    private fun userVote(binding: ActivityReviewViewBinding, type: String?) {
        val selectedColor = parentActivity.getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val unselectedColor = parentActivity.getThemeColor(androidx.appcompat.R.attr.colorControlNormal)
        when (type) {
            "NO_VOTE" -> {
                binding.upvote.setColorFilter(unselectedColor)
                binding.downvote.setColorFilter(unselectedColor)
            }

            "UP_VOTE" -> {
                binding.upvote.setColorFilter(selectedColor)
                binding.downvote.setColorFilter(unselectedColor)
            }

            "DOWN_VOTE" -> {
                binding.upvote.setColorFilter(unselectedColor)
                binding.downvote.setColorFilter(selectedColor)
            }
        }
    }

    private fun rateReview(binding: ActivityReviewViewBinding, review: Review, rating: String ) {
        disableVote(binding)
        parentActivity.lifecycleScope.launch {
            val result = Anilist.mutation.rateReview(review.id, rating)
            if (result != null) {
                withContext(Dispatchers.Main) {
                    val res = result.data.rateReview
                    review.rating = res.rating
                    review.ratingAmount = res.ratingAmount
                    review.userRating = res.userRating
                    userVote(binding, review.userRating)
                    binding.voteCount.text = review.rating.toString()
                    binding.voteText.text = parentActivity.getString(
                        R.string.vote_out_of_total,
                        review.rating.toString(),
                        review.ratingAmount.toString()
                    )
                    userVote(binding, review.userRating)
                    enableVote(binding, review)
                }
            } else {
                withContext(Dispatchers.Main) {
                    toast(
                        parentActivity.getString(R.string.error_message, "response is null")
                    )
                    enableVote(binding, review)
                }
            }
        }
    }

    private fun disableVote(binding: ActivityReviewViewBinding) {
        binding.upvote.setOnClickListener(null)
        binding.downvote.setOnClickListener(null)
        binding.upvote.isEnabled = false
        binding.downvote.isEnabled = false
    }

    private fun enableVote(binding: ActivityReviewViewBinding, review: Review) {
        binding.upvote.setOnClickListener {
            if (review.userRating == "UP_VOTE") {
                rateReview(binding, review, "NO_VOTE")
            } else {
                rateReview(binding, review, "UP_VOTE")
            }
            disableVote(binding)
        }
        binding.downvote.setOnClickListener {
            if (review.userRating == "DOWN_VOTE") {
                rateReview(binding, review, "NO_VOTE")
            } else {
                rateReview(binding, review, "DOWN_VOTE")
            }
            disableVote(binding)
        }
        binding.upvote.isEnabled = true
        binding.downvote.isEnabled = true
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
                val dialogView = ActivityReviewViewBinding.inflate(parentActivity.layoutInflater)
                dialogView.reviewMediaCover.loadImage(review.media?.cover)
                dialogView.profileUserBanner.loadImage(review.user?.banner)
                dialogView.profileUserAvatar.loadImage(review.user?.pfp)
                dialogView.reviewBodyContent.settings.loadWithOverviewMode = true
                dialogView.reviewBodyContent.settings.useWideViewPort = true
                dialogView.reviewBodyContent.setInitialScale(1)
                review.body?.let {
                    AniMarkdown.getFullAniHTML(
                        it,
                        ContextCompat.getColor(parentActivity, R.color.bg_opp)
                    ).let { styledHtml ->
                        dialogView.reviewBodyContent.loadDataWithBaseURL(
                            null,
                            styledHtml,
                            "text/html",
                            "utf-8",
                            null
                        )
                    }
                }
                dialogView.profileUserName.text = review.user?.name
                dialogView.reviewItemName.text = review.media?.mainName()
                val formattedScore = "${review.score}/100 • ${ActivityItemBuilder.getDateTime(review.createdAt)}"
                dialogView.reviewItemRating.text = formattedScore
                dialogView.reviewBodyContent.setBackgroundColor(
                    ContextCompat.getColor(parentActivity, android.R.color.transparent)
                )
                dialogView.profileBannerContainer.setOnClickListener {
                    ContextCompat.startActivity(
                        parentActivity,
                        Intent(parentActivity, ProfileActivity::class.java)
                            .putExtra("userId", review.userId), null
                    )
                }
                userVote(dialogView, review.userRating)
                enableVote(dialogView, review)
                dialogView.voteCount.text = review.rating.toString()
                dialogView.voteText.text = parentActivity.getString(
                    R.string.vote_out_of_total,
                    review.rating.toString(),
                    review.ratingAmount.toString()
                )
                val alertD = AlertDialog.Builder(parentActivity, R.style.MyPopup)
                alertD.setView(dialogView.root)
                alertD.setPositiveButton(review.media?.mainName()
                    ?: parentActivity.getString(R.string.media)) { _, _ ->
                    ContextCompat.startActivity(
                        parentActivity,
                        Intent(parentActivity, MediaDetailsActivity::class.java)
                            .putExtra("media", review.media as Serializable), null
                    )
                }
                val dialog = alertD.show()
                dialogView.reviewClose.setOnClickListener { dialog.dismiss() }
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
