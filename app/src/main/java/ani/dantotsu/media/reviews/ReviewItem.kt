package ani.dantotsu.media.reviews

import android.view.View
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.ItemReviewBinding
import ani.dantotsu.loadImage
import ani.dantotsu.profile.activity.ActivityItemBuilder
import com.xwray.groupie.viewbinding.BindableItem

class ReviewItem(
    private val review: Query.Review,
    val clickCallback: (Int) -> Unit
) : BindableItem<ItemReviewBinding>() {
    private lateinit var binding: ItemReviewBinding

    override fun bind(viewBinding: ItemReviewBinding, position: Int) {
        binding = viewBinding
        binding.notificationCover.loadImage(review.user?.avatar?.medium)
        binding.notificationBanner.loadImage(review.user?.bannerImage ?: review.media?.coverImage?.large)
        binding.reviewItemName.text = review.user?.name
        binding.reviewItemScore.text = review.score.toString()
        val formattedQuote = "\"${review.summary}\""
        binding.notificationText.text = formattedQuote
        binding.notificationDate.text = ActivityItemBuilder.getDateTime(review.createdAt)
        binding.root.setOnClickListener { clickCallback(review.id) }
    }

    override fun getLayout(): Int {
        return R.layout.item_review
    }

    override fun initializeViewBinding(view: View): ItemReviewBinding {
        return ItemReviewBinding.bind(view)
    }
}