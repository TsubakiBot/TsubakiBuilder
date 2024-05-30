package ani.dantotsu.home

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.bottomBar
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.AnilistAnimeViewModel
import ani.dantotsu.connections.anilist.SearchResults
import ani.dantotsu.databinding.FragmentAnimeBinding
import ani.dantotsu.loadFragment
import ani.dantotsu.media.MediaAdaptor
import ani.dantotsu.media.ProgressAdapter
import ani.dantotsu.media.SearchActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.toPx
import ani.dantotsu.toRoundImage
import bit.himitsu.isOverlapping
import bit.himitsu.update.MatagiUpdater
import bit.himitsu.widget.FABulous
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


class AnimeFragment : Fragment() {
    private var _binding: FragmentAnimeBinding? = null
    private val binding by lazy { _binding!! }
    private lateinit var animePageAdapter: AnimePageAdapter

    val model: AnilistAnimeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scope = viewLifecycleOwner.lifecycleScope

        var height = statusBarHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = activity?.window?.decorView?.rootWindowInsets?.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size > 0) {
                    height = max(
                        statusBarHeight,
                        min(
                            displayCutout.boundingRects[0].width(),
                            displayCutout.boundingRects[0].height()
                        )
                    )
                }
            }
        }
        binding.animeRefresh.setSlingshotDistance(height + 128)
        binding.animeRefresh.setProgressViewEndTarget(false, height + 128)
        binding.animeRefresh.setOnRefreshListener {
            Refresh.activity[this.hashCode()]!!.postValue(true)
        }

        // TODO: Investigate hardcoded values
        binding.animePageRecyclerView.updatePaddingRelative(bottom = navBarHeight + 160.toPx)

        animePageAdapter = AnimePageAdapter(this)

        var loading = true
        if (model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults(
                "ANIME",
                isAdult = false,
                onList = false,
                results = mutableListOf(),
                hasNextPage = true,
                sort = Anilist.sortBy[1]
            )
        }
        val popularAdaptor = MediaAdaptor(1, model.searchResults.results, requireActivity())
        val progressAdaptor = ProgressAdapter(searched = model.searched)
        val adapter = ConcatAdapter(animePageAdapter, popularAdaptor, progressAdaptor)
        binding.animePageRecyclerView.adapter = adapter
        val layout = LinearLayoutManager(requireContext())
        binding.animePageRecyclerView.layoutManager = layout

        var visible = false
        fun animate() {
            val start = if (visible) 0f else 1f
            val end = if (!visible) 0f else 1f
            ObjectAnimator.ofFloat(binding.animePageScrollTop, "scaleX", start, end).apply {
                duration = 300
                interpolator = OvershootInterpolator(2f)
                start()
            }
            ObjectAnimator.ofFloat(binding.animePageScrollTop, "scaleY", start, end).apply {
                duration = 300
                interpolator = OvershootInterpolator(2f)
                start()
            }
        }

        binding.animePageScrollTop.setOnClickListener {
            binding.animePageRecyclerView.scrollToPosition(4)
            binding.animePageRecyclerView.smoothScrollToPosition(0)
        }

        var oldIncludeList = true

        animePageAdapter.onIncludeListClick = { checked ->
            oldIncludeList = !checked
            loading = true
            model.searchResults.results.clear()
            popularAdaptor.notifyDataSetChanged()
            scope.launch(Dispatchers.IO) {
                model.loadPopular("ANIME", sort = Anilist.sortBy[1], onList = checked)
            }
        }

        model.getPopular().observe(viewLifecycleOwner) {
            if (it != null) {
                if (oldIncludeList == (it.onList != false)) {
                    val prev = model.searchResults.results.size
                    model.searchResults.results.addAll(it.results)
                    popularAdaptor.notifyItemRangeInserted(prev, it.results.size)
                } else {
                    model.searchResults.results.addAll(it.results)
                    popularAdaptor.notifyDataSetChanged()
                    oldIncludeList = it.onList ?: true
                }
                model.searchResults.onList = it.onList
                model.searchResults.hasNextPage = it.hasNextPage
                model.searchResults.page = it.page
                if (it.hasNextPage)
                    progressAdaptor.bar?.visibility = View.VISIBLE
                else {
                    snackString(getString(R.string.jobless_message))
                    progressAdaptor.bar?.visibility = View.GONE
                }
                loading = false
            }
        }

        binding.animePageRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
                if (!v.canScrollVertically(1)) {
                    if (model.searchResults.hasNextPage && model.searchResults.results.isNotEmpty() && !loading) {
                        scope.launch(Dispatchers.IO) {
                            loading = true
                            model.loadNextPage(model.searchResults)
                        }
                    }
                }
                if (layout.findFirstVisibleItemPosition() > 1 && !visible) {
                    binding.animePageScrollTop.visibility = View.VISIBLE
                    visible = true
                    animate()
                }

                if (!v.canScrollVertically(-1)) {
                    visible = false
                    animate()
                    scope.launch {
                        delay(300)
                        binding.animePageScrollTop.visibility = View.GONE
                    }
                }

                super.onScrolled(v, dx, dy)
            }
        })

        animePageAdapter.ready.observe(viewLifecycleOwner) { i ->
            if (i) {
                binding.avatarFabulous.apply {
                    isVisible = PrefManager.getVal(PrefName.FloatingAvatar)
                    if (isVisible) {
                        setAnchor(animePageAdapter.trendingBinding.userAvatar)
                        toRoundImage(Anilist.avatar, 52.toPx)
                        (behavior as FloatingActionButton.Behavior).isAutoHideEnabled = false

                        setDefaultPosition()

                        if (binding.avatarFabulous.isOverlapping(animePageAdapter.trendingBinding.userAvatar)) {
                            setBadgeDrawable(
                                Anilist.unreadNotificationCount + MatagiUpdater.hasUpdate
                            )
                        }

                        val handler = Handler(Looper.getMainLooper())
                        val mRunnable = Runnable {
                            if (isOverlapping(animePageAdapter.trendingBinding.userAvatar)) {
                                setDefaultPosition()
                            }
                        }

                        setOnMoveListener(object : FABulous.OnViewMovedListener {
                            override fun onActionMove(x: Float, y: Float) {
                                handler.removeCallbacksAndMessages(mRunnable)
                                if (isOverlapping(animePageAdapter.trendingBinding.userAvatar)) {
                                    handler.postDelayed(mRunnable, 1000)
                                }
                                setActiveNotificationCount()
                            }
                        })
                        setOnClickListener {
                            if (binding.avatarFabulous.isOverlapping(animePageAdapter.trendingBinding.userAvatar)) {
                                animePageAdapter.trendingBinding.userAvatar.performClick()
                            }
                        }
                        setOnLongClickListener {
                            if (isOverlapping(animePageAdapter.trendingBinding.userAvatar)) {
                                animePageAdapter.trendingBinding.userAvatar.performLongClick()
                            } else {
                                false
                            }
                        }
                    }
                }
                model.getUpdated().observe(viewLifecycleOwner) {
                    if (it != null) {
                        animePageAdapter.updateRecent(MediaAdaptor(0, it, requireActivity()), it)
                    }
                }
                model.getMovies().observe(viewLifecycleOwner) {
                    if (it != null) {
                        animePageAdapter.updateMovies(MediaAdaptor(0, it, requireActivity()), it)
                    }
                }
                model.getTopRated().observe(viewLifecycleOwner) {
                    if (it != null) {
                        animePageAdapter.updateTopRated(MediaAdaptor(0, it, requireActivity()), it)
                    }
                }
                model.getMostFav().observe(viewLifecycleOwner) {
                    if (it != null) {
                        animePageAdapter.updateMostFav(MediaAdaptor(0, it, requireActivity()), it)
                    }
                }
                if (animePageAdapter.trendingViewPager != null) {
                    animePageAdapter.updateHeight()
                    model.getTrending().observe(viewLifecycleOwner) {
                        if (it != null) {
                            animePageAdapter.updateTrending(
                                MediaAdaptor(
                                    if (PrefManager.getVal(PrefName.SmallView)) 3 else 2,
                                    it,
                                    requireActivity(),
                                    viewPager = animePageAdapter.trendingViewPager
                                )
                            )
                            animePageAdapter.updateAvatar()
                            animePageAdapter.setReviewImageFromTrending(it[Random.nextInt(it.size)])
                        }
                    }
                }
                binding.animePageScrollTop.translationY =
                    -(navBarHeight + bottomBar.height + bottomBar.marginBottom).toFloat()
            }
        }


        fun load() = scope.launch(Dispatchers.Main) {
            animePageAdapter.updateAvatar()
        }

        animePageAdapter.onSeasonClick = { i ->
            scope.launch(Dispatchers.IO) {
                model.loadTrending(i)
            }
        }

        animePageAdapter.onSeasonLongClick = { i ->
            val (season, year) = Anilist.currentSeasons[i]
            ContextCompat.startActivity(
                requireContext(),
                Intent(requireContext(), SearchActivity::class.java)
                    .putExtra("type", "ANIME")
                    .putExtra("season", season)
                    .putExtra("seasonYear", year.toString())
                    .putExtra("hideKeyboard", true),
                null
            )
            true
        }

        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(false) }
        live.observe(viewLifecycleOwner) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        loadFragment(requireActivity(), ::load)
                        model.loaded = true
                        model.loadTrending(1)
                        model.loadAll()
                        model.loadPopular(
                            "ANIME", sort = Anilist.sortBy[1], onList = PrefManager.getVal(
                                PrefName.PopularAnimeList
                            )
                        )
                    }
                    live.postValue(false)
                    _binding?.animeRefresh?.isRefreshing = false
                }
            }
        }
    }

    fun setActiveNotificationCount() {
        val count = Anilist.unreadNotificationCount + MatagiUpdater.hasUpdate
        if (binding.avatarFabulous.isOverlapping(animePageAdapter.trendingBinding.userAvatar)) {
            animePageAdapter.trendingBinding.notificationCount.isVisible = false
            binding.avatarFabulous.setBadgeDrawable(count)
        } else {
            animePageAdapter.trendingBinding.notificationCount.text = count.toString()
            animePageAdapter.trendingBinding.notificationCount.isVisible = count > 0
            binding.avatarFabulous.setBadgeDrawable(null)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!model.loaded) Refresh.activity[this.hashCode()]!!.postValue(true)
        if (this::animePageAdapter.isInitialized && _binding != null) {
            if (animePageAdapter.trendingViewPager != null) {
                binding.root.requestApplyInsets()
                // binding.root.requestLayout()
                setActiveNotificationCount()
            }
        }
    }
}