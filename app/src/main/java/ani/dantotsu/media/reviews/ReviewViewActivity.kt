package ani.dantotsu.media.reviews

import android.content.Intent
import android.content.res.Configuration
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
import ani.dantotsu.hideSystemBarsExtendView
import ani.dantotsu.initActivity
import ani.dantotsu.loadCover
import ani.dantotsu.loadImage
import ani.dantotsu.navBarHeight
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.activity.ActivityItemBuilder
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.dantotsu.util.AniMarkdown
import bit.himitsu.setBaseline
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
        }
        hideSystemBarsExtendView()
        binding.reviewScroller.setBaseline(resources.configuration)
        binding.reviewClose.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        review = intent.getSerializableExtraCompat<Query.Review>("review")!!
        binding.reviewMediaCover.loadCover(review.media?.coverImage)
        binding.profileUserBanner.loadImage(review.user?.bannerImage)
        binding.profileUserAvatar.loadImage(review.user?.avatar?.medium)
        binding.reviewBodyContent.settings.loadWithOverviewMode = true
        binding.reviewBodyContent.settings.useWideViewPort = true
        binding.reviewBodyContent.setInitialScale(1)
        AniMarkdown.getFullAniHTML(
            review.body,
            ContextCompat.getColor(this, R.color.bg_opp)
        ).let { styledHtml ->
            binding.reviewBodyContent.loadDataWithBaseURL(
                null,
                styledHtml,
                "text/html",
                "utf-8",
                null
            )
        }
        binding.reviewBodyContent.setBackgroundColor(
            ContextCompat.getColor(this, android.R.color.transparent)
        )
        binding.profileUserName.text = review.user?.name
        binding.reviewItemName.text = review.media?.title?.userPreferred
        val formattedScore = "${review.score}/100 • ${ActivityItemBuilder.getDateTime(review.createdAt)}"
        binding.reviewItemRating.text = formattedScore
        binding.profileBannerContainer.setOnClickListener {
            ContextCompat.startActivity(
                this,
                Intent(this, ProfileActivity::class.java)
                    .putExtra("userId", review.userId), null
            )
        }
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
                    review.userRating = res.userRating.also {
                        userVote(it)
                        binding.voteCount.text = review.rating.toString()
                        binding.voteText.text = getString(
                            R.string.vote_out_of_total,
                            review.rating.toString(),
                            review.ratingAmount.toString()
                        )
                        enableVote()
                    }
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.reviewScroller.setBaseline(newConfig)
    }
}