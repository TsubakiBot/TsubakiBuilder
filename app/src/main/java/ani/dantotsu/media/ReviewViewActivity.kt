package ani.dantotsu.media

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.ActivityReviewViewBinding
import ani.dantotsu.initActivity
import ani.dantotsu.loadImage
import ani.dantotsu.navBarHeight
import ani.dantotsu.profile.activity.ActivityItemBuilder
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.dantotsu.util.AniMarkdown
import eu.kanade.tachiyomi.util.system.getSerializableExtraCompat
import eu.kanade.tachiyomi.util.system.getThemeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewViewBinding
    private lateinit var review: Query.Review

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityReviewViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.reviewContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin += statusBarHeight
            bottomMargin += navBarHeight
        }
        binding.reviewClose.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        review = intent.getSerializableExtraCompat<Query.Review>("review")!!
        binding.userName.text = review.user?.name
        binding.userAvatar.loadImage(review.user?.avatar?.medium)
        binding.notificationCover.loadImage(review.media?.coverImage?.large)
        binding.userTime.text = ActivityItemBuilder.getDateTime(review.createdAt)
        binding.profileUserBio.settings.loadWithOverviewMode = true
        binding.profileUserBio.settings.useWideViewPort = true
        binding.profileUserBio.setInitialScale(1)
        val styledHtml = AniMarkdown.getFullAniHTML(
            review.body,
            ContextCompat.getColor(this, R.color.bg_opp)
        )
        binding.profileUserBio.loadDataWithBaseURL(
            null,
            styledHtml,
            "text/html",
            "utf-8",
            null
        )
        binding.profileUserBio.setBackgroundColor(
            ContextCompat.getColor(this, android.R.color.transparent)
        )
        userVote(review.userRating)
        enableVote()
        binding.voteCount.text = review.rating.toString()
        binding.voteText.text = getString(
            R.string.vote_out_of_total,
            review.rating.toString(),
            review.ratingAmount.toString()
        )
    }

    private fun userVote(type: String) {
        val selectedColor = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val unselectedColor = getThemeColor(androidx.appcompat.R.attr.colorControlNormal)
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

    private fun rateReview(rating: String) {
        disableVote()
        lifecycleScope.launch {
            val result = Anilist.mutation.rateReview(review.id, rating)
            if (result != null) {
                withContext(Dispatchers.Main) {
                    val res = result.data.rateReview
                    review.rating = res.rating
                    review.ratingAmount = res.ratingAmount
                    review.userRating = res.userRating
                    userVote(review.userRating)
                    binding.voteCount.text = review.rating.toString()
                    binding.voteText.text = getString(
                        R.string.vote_out_of_total,
                        review.rating.toString(),
                        review.ratingAmount.toString()
                    )
                    userVote(review.userRating)
                    enableVote()
                }
            } else {
                withContext(Dispatchers.Main) {
                    toast(
                        getString(R.string.error_message, "response is null")
                    )
                    enableVote()
                }
            }
        }
    }

    private fun disableVote() {
        binding.upvote.setOnClickListener(null)
        binding.downvote.setOnClickListener(null)
        binding.upvote.isEnabled = false
        binding.downvote.isEnabled = false
    }

    private fun enableVote() {
        binding.upvote.setOnClickListener {
            if (review.userRating == "UP_VOTE") {
                rateReview("NO_VOTE")
            } else {
                rateReview("UP_VOTE")
            }
            disableVote()
        }
        binding.downvote.setOnClickListener {
            if (review.userRating == "DOWN_VOTE") {
                rateReview("NO_VOTE")
            } else {
                rateReview("DOWN_VOTE")
            }
            disableVote()
        }
        binding.upvote.isEnabled = true
        binding.downvote.isEnabled = true
    }
}