package ani.dantotsu.home

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
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
import ani.dantotsu.connections.anilist.AnilistMangaViewModel
import ani.dantotsu.connections.anilist.SearchResults
import ani.dantotsu.databinding.FragmentAnimeBinding
import ani.dantotsu.loadFragment
import ani.dantotsu.media.MediaAdaptor
import ani.dantotsu.media.ProgressAdapter
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.toPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MangaFragment : Fragment() {
    private var _binding: FragmentAnimeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mangaPageAdapter: MangaPageAdapter

    val model: AnilistMangaViewModel by activityViewModels()

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

        mangaPageAdapter = MangaPageAdapter()
        var loading = true
        if (model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults(
                "MANGA",
                isAdult = false,
                onList = false,
                results = arrayListOf(),
                hasNextPage = true,
                sort = Anilist.sortBy[1]
            )
        }
        val popularAdaptor = MediaAdaptor(1, model.searchResults.results, requireActivity())
        val progressAdaptor = ProgressAdapter(searched = model.searched)
        binding.animePageRecyclerView.adapter =
            ConcatAdapter(mangaPageAdapter, popularAdaptor, progressAdaptor)
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
        mangaPageAdapter.ready.observe(viewLifecycleOwner) { i ->
            if (i == true) {
                model.getPopularNovel().observe(viewLifecycleOwner) {
                    if (it != null) {
                        mangaPageAdapter.updateNovel(MediaAdaptor(0, it, requireActivity()), it)
                    }
                }
                model.getPopularManga().observe(viewLifecycleOwner) {
                    if (it != null) {
                        mangaPageAdapter.updateTrendingManga(MediaAdaptor(0, it, requireActivity()), it)
                        mangaPageAdapter.setReviewImageFromTrending(it[Random.nextInt(it.size)])
                    }
                }
                model.getPopularManhwa().observe(viewLifecycleOwner) {
                    if (it != null) {
                        mangaPageAdapter.updateTrendingManhwa(
                            MediaAdaptor(
                                0,
                                it,
                                requireActivity()
                            ), it
                        )
                    }
                }
                model.getTopRated().observe(viewLifecycleOwner) {
                    if (it != null) {
                        mangaPageAdapter.updateTopRated(MediaAdaptor(0, it, requireActivity()), it)
                    }
                }
                model.getMostFav().observe(viewLifecycleOwner) {
                    if (it != null) {
                        mangaPageAdapter.updateMostFav(MediaAdaptor(0, it, requireActivity()), it)
                    }
                }
                if (mangaPageAdapter.trendingViewPager != null) {
                    mangaPageAdapter.updateHeight()
                    model.getTrending().observe(viewLifecycleOwner) {
                        if (it != null) {
                            mangaPageAdapter.updateTrending(
                                MediaAdaptor(
                                    if (PrefManager.getVal(PrefName.SmallView)) 3 else 2,
                                    it,
                                    requireActivity(),
                                    viewPager = mangaPageAdapter.trendingViewPager
                                )
                            )
                            mangaPageAdapter.updateAvatar()
                        }
                    }
                }
                binding.animePageScrollTop.translationY =
                    -(navBarHeight + bottomBar.height + bottomBar.marginBottom).toFloat()

            }
        }

        var oldIncludeList = true

        mangaPageAdapter.onIncludeListClick = { checked ->
            oldIncludeList = !checked
            loading = true
            model.searchResults.results.clear()
            popularAdaptor.notifyDataSetChanged()
            scope.launch(Dispatchers.IO) {
                model.loadPopular("MANGA", sort = Anilist.sortBy[1], onList = checked)
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

        fun load() = scope.launch(Dispatchers.Main) {
            mangaPageAdapter.updateAvatar()
        }

        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(false) }
        live.observe(viewLifecycleOwner) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        loadFragment(requireActivity()) { load() }
                        model.loaded = true
                        model.loadTrending()
                        model.loadAll()
                        model.loadPopular(
                            "MANGA", sort = Anilist.sortBy[1], onList = PrefManager.getVal(
                                PrefName.PopularMangaList
                            )
                        )
                    }
                    live.postValue(false)
                    _binding?.animeRefresh?.isRefreshing = false
                }
            }
        }
    }

    override fun onResume() {
        if (!model.loaded) Refresh.activity[this.hashCode()]!!.postValue(true)
        //make sure mangaPageAdapter is initialized
        if (mangaPageAdapter.trendingViewPager != null) {
            binding.root.requestApplyInsets()
            binding.root.requestLayout()
        }
        if (this::mangaPageAdapter.isInitialized && _binding != null) {
            mangaPageAdapter.updateNotificationCount()
        }
        super.onResume()
    }

}