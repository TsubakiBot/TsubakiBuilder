package ani.dantotsu.media.reviews

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.ActivityFollowBinding
import ani.dantotsu.navBarHeight
import ani.dantotsu.statusBarHeight
import ani.dantotsu.util.MarkdownCreatorActivity
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewFragment : Fragment() {
    private var _binding: ActivityFollowBinding? = null
    private val binding get() = _binding!!
    val adapter = GroupieAdapter()
    private val reviews = mutableListOf<Query.Review>()
    var mediaId = 0
    private var currentPage: Int = 1
    private var hasNextPage: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFollowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.listToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
        }
        binding.listFrameLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }

        mediaId = arguments?.getInt("mediaId", -1) ?: -1
        if (mediaId == -1) return

        binding.listBack.visibility = View.GONE
        binding.followerGrid.visibility = View.GONE
        binding.followerList.visibility = View.GONE
        binding.followFilterButton.setImageResource(R.drawable.ic_add)
        binding.followFilterButton.setOnClickListener {
            ContextCompat.startActivity(
                requireContext(),
                Intent(requireContext(), MarkdownCreatorActivity::class.java)
                    .putExtra("type", "review"),
                null
            )
        }
        binding.followFilterButton.visibility = View.GONE
        binding.listToolbar.visibility = View.GONE

        val mediaTitle = arguments?.getString("title")
        binding.emptyRecyclerText.text = getString(R.string.reviews_empty, mediaTitle)
        binding.listRecyclerView.adapter = adapter
        binding.listRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.listProgressBar.visibility = View.VISIBLE

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
            startActivity(Intent(requireContext(), ReviewViewActivity::class.java)
                .putExtra("review", review))
        }
    }
}