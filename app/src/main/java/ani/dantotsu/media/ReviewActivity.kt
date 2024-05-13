package ani.dantotsu.media

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.ActivityFollowBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.MarkdownCreatorActivity
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFollowBinding
    val adapter = GroupieAdapter()
    private val reviews = mutableListOf<Query.Review>()
    var mediaId = 0
    private var currentPage: Int = 1
    private var hasNextPage: Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityFollowBinding.inflate(layoutInflater)
        binding.listToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
        }
        binding.listFrameLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }
        setContentView(binding.root)
        mediaId = intent.getIntExtra("mediaId", -1)
        if (mediaId == -1) {
            finish()
            return
        }
        binding.followerGrid.visibility = View.GONE
        binding.followerList.visibility = View.GONE
        binding.followFilterButton.setImageResource(R.drawable.ic_add)
        binding.followFilterButton.setOnClickListener {
            ContextCompat.startActivity(
                this,
                Intent(this, MarkdownCreatorActivity::class.java)
                    .putExtra("type", "review"),
                null
            )
        }
        binding.followFilterButton.visibility = View.GONE
        val mediaTitle = intent.getStringExtra("title")
        binding.listTitle.ellipsize = TextUtils.TruncateAt.START
        binding.listTitle.text = getString(R.string.review_type, mediaTitle)
        binding.emptyRecyclerText.text = getString(R.string.reviews_empty, mediaTitle)
        binding.listRecyclerView.adapter = adapter
        binding.listRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.listProgressBar.visibility = View.VISIBLE
        binding.listBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.followSwipeRefresh.setOnRefreshListener {
            reviews.clear()
            appendList(reviews)
            binding.followSwipeRefresh.isRefreshing = false
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val response = Anilist.query.getReviews(mediaId)
            withContext(Dispatchers.Main) {
                binding.listProgressBar.visibility = View.GONE
                binding.listRecyclerView.setOnTouchListener { _, event ->
                    if (event?.action == MotionEvent.ACTION_UP) {
                        if (hasNextPage
                            && !binding.listRecyclerView.canScrollVertically(1)
                            && !binding.followRefresh.isVisible
                            && adapter.itemCount != 0
                            && (binding.listRecyclerView.layoutManager as LinearLayoutManager)
                                .findLastVisibleItemPosition() == (adapter.itemCount - 1)
                        ) {
                            binding.followRefresh.visibility = ViewGroup.VISIBLE
                            loadPage(++currentPage) {
                                binding.followRefresh.visibility = ViewGroup.GONE
                            }
                        }
                    }
                    false
                }
            }
            currentPage = response?.data?.page?.pageInfo?.currentPage ?: 1
            hasNextPage = response?.data?.page?.pageInfo?.hasNextPage ?: false
            response?.data?.page?.reviews?.let {
                reviews.addAll(it)
                appendList(it)
            }
            binding.emptyRecyclerText.isVisible = reviews.isEmpty()
        }
    }

    private fun loadPage(page: Int, callback: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val response = Anilist.query.getReviews(mediaId, page)
            currentPage = response?.data?.page?.pageInfo?.currentPage ?: 1
            hasNextPage = response?.data?.page?.pageInfo?.hasNextPage ?: false
            response?.data?.page?.reviews?.let {
                reviews.addAll(it)
                appendList(it)
            }
            withContext(Dispatchers.Main) {
                binding.emptyRecyclerText.isVisible = reviews.isEmpty()
                callback()
            }
        }
    }

    private fun appendList(reviews: List<Query.Review>) {
        lifecycleScope.launch(Dispatchers.Main) {
            reviews.forEach { adapter.add(ReviewItem(it, ::onUserClick)) }
        }
    }

    private fun onUserClick(userId: Int) {
        reviews.find { it.id == userId }?.let { review ->
            startActivity(Intent(this, ReviewViewActivity::class.java)
                .putExtra("review", review))
        }
    }
}