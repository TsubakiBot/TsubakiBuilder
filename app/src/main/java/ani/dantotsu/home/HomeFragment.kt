package ani.dantotsu.home

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LayoutAnimationController
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.blurImage
import ani.dantotsu.bottomBar
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.AnilistHomeViewModel
import ani.dantotsu.databinding.FragmentHomeBinding
import ani.dantotsu.home.status.UserStatusAdapter
import ani.dantotsu.loadFragment
import ani.dantotsu.loadImage
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaAdaptor
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.media.MediaListViewActivity
import ani.dantotsu.media.MediaType
import ani.dantotsu.media.user.ListActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.setSlideIn
import ani.dantotsu.setSlideUp
import ani.dantotsu.settings.SettingsDialogFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefManager.asLiveBool
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.himitsu.launcher.ResumableShortcuts
import ani.himitsu.update.MatagiUpdater
import ani.himitsu.widgets.resumable.ResumableWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    val model: AnilistHomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val scope = lifecycleScope
        fun load() {
            if (activity != null && _binding != null) lifecycleScope.launch(Dispatchers.Main) {
                binding.homeUserName.text = Anilist.username
                binding.homeUserEpisodesWatched.text = Anilist.episodesWatched.toString()
                binding.homeUserChaptersRead.text = Anilist.chapterRead.toString()
                binding.homeUserAvatar.loadImage(Anilist.avatar)
                val banner = if (PrefManager.getVal(PrefName.BannerAnimations))
                    binding.homeUserBg
                else
                    binding.homeUserBgNoKen
                banner.blurImage(Anilist.bg)
                binding.homeUserDataProgressBar.visibility = View.GONE
                val count = Anilist.unreadNotificationCount + MatagiUpdater.hasUpdate
                binding.homeNotificationCount.isVisible = count > 0
                binding.homeNotificationCount.text = count.toString()

                binding.homeAnimeList.setOnClickListener {
                    ContextCompat.startActivity(
                        requireActivity(), Intent(requireActivity(), ListActivity::class.java)
                            .putExtra("anime", true)
                            .putExtra("userId", Anilist.userid)
                            .putExtra("username", Anilist.username), null
                    )
                }
                binding.homeMangaList.setOnClickListener {
                    ContextCompat.startActivity(
                        requireActivity(), Intent(requireActivity(), ListActivity::class.java)
                            .putExtra("anime", false)
                            .putExtra("userId", Anilist.userid)
                            .putExtra("username", Anilist.username), null
                    )
                }
                binding.homeUserAvatarContainer.startAnimation(setSlideUp())
                binding.homeUserDataContainer.visibility = View.VISIBLE
                binding.homeUserDataContainer.layoutAnimation =
                    LayoutAnimationController(setSlideUp(), 0.25f)
                binding.homeAnimeList.visibility = View.VISIBLE
                binding.homeMangaList.visibility = View.VISIBLE
                binding.homeRandomAnime.visibility = View.VISIBLE
                binding.homeRandomManga.visibility = View.VISIBLE
                binding.homeListContainer.layoutAnimation = LayoutAnimationController(setSlideIn(), 0.25f)
            }
            else {
                snackString(R.string.please_reload)
            }
        }
        binding.homeUserAvatarContainer.setSafeOnClickListener {
            val dialogFragment =
                SettingsDialogFragment.newInstance(SettingsDialogFragment.Companion.PageType.HOME)
            dialogFragment.show(
                (it.context as androidx.appcompat.app.AppCompatActivity).supportFragmentManager,
                "dialog"
            )
        }
        binding.homeUserAvatarContainer.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            ContextCompat.startActivity(
                requireContext(), Intent(requireContext(), ProfileActivity::class.java)
                    .putExtra("userId", Anilist.userid), null
            )
            false
        }

        binding.homeContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }
        binding.homeUserBg.updateLayoutParams { height += statusBarHeight }
        binding.homeUserBgNoKen.updateLayoutParams { height += statusBarHeight }
        binding.homeTopContainer.updatePadding(top = statusBarHeight)

        var reached = false
        val duration = ((PrefManager.getVal(PrefName.AnimationSpeed) as Float) * 200).toLong()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.homeScroll.setOnScrollChangeListener { _, _, _, _, _ ->
                if (!binding.homeScroll.canScrollVertically(1)) {
                    reached = true
                    bottomBar.animate().translationZ(0f).setDuration(duration).start()
                    ObjectAnimator.ofFloat(bottomBar, "elevation", 4f, 0f).setDuration(duration)
                        .start()
                } else {
                    if (reached) {
                        bottomBar.animate().translationZ(12f).setDuration(duration).start()
                        ObjectAnimator.ofFloat(bottomBar, "elevation", 0f, 4f).setDuration(duration)
                            .start()
                    }
                }
            }
        }
        var height = statusBarHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = activity?.window?.decorView?.rootWindowInsets?.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size > 0) {
                    height =
                        max(
                            statusBarHeight,
                            min(
                                displayCutout.boundingRects[0].width(),
                                displayCutout.boundingRects[0].height()
                            )
                        )
                }
            }
        }
        binding.homeRefresh.setSlingshotDistance(height + 128)
        binding.homeRefresh.setProgressViewEndTarget(false, height + 128)
        binding.homeRefresh.setOnRefreshListener {
            Refresh.activity[1]!!.postValue(true)
        }

        //UserData
        binding.homeUserDataProgressBar.visibility = View.VISIBLE
        binding.homeUserDataContainer.visibility = View.GONE
        if (model.loaded) {
            load()
        }
        //List Images
        model.getListImages().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                binding.homeAnimeListImage.loadImage(it[0] ?: "https://bit.ly/31bsIHq")
                binding.homeMangaListImage.loadImage(it[1] ?: "https://bit.ly/2ZGfcuG")
            }
        }

        //Function For Recycler Views
        fun initRecyclerView(
            isEnabled: Boolean,
            mode: LiveData<ArrayList<Media>>,
            container: View,
            recyclerView: RecyclerView,
            progress: View,
            empty: View,
            title: View,
            more: View,
            string: String
        ) {
            container.visibility = View.VISIBLE
            progress.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            empty.visibility = View.GONE
            title.visibility = View.INVISIBLE
            more.visibility = View.INVISIBLE

            mode.observe(viewLifecycleOwner) {
                if (!isEnabled) {
                    container.isVisible = false
                    return@observe
                }
                recyclerView.visibility = View.GONE
                empty.visibility = View.GONE
                if (it != null) {
                    if (it.isNotEmpty()) {
                        recyclerView.adapter = MediaAdaptor(0, it, requireActivity())
                        recyclerView.layoutManager = LinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        more.setOnClickListener { i ->
                            more.isEnabled = false
                            ContextCompat.startActivity(
                                i.context, Intent(i.context, MediaListViewActivity::class.java)
                                    .putExtra("title", string)
                                    .putExtra("media", it),
                                null
                            )
                            more.isEnabled = true
                        }
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.layoutAnimation =
                            LayoutAnimationController(setSlideIn(), 0.25f)
                    } else {
                        empty.visibility = View.VISIBLE
                    }
                    more.visibility = View.VISIBLE
                    title.visibility = View.VISIBLE
                    more.startAnimation(setSlideUp())
                    title.startAnimation(setSlideUp())
                    progress.visibility = View.GONE
                }
            }
        }

        // Recycler Views
        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[0],
            model.getSubscriptions(),
            binding.homeSubscribedItemContainer,
            binding.homeSubscribedRecyclerView,
            binding.homeSubscribedProgressBar,
            binding.homeSubscribedEmpty,
            binding.homeSubscribedItem,
            binding.homeSubscribedMore,
            getString(R.string.subscriptions)
        )
        binding.homeSubscribedBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(0)
        }

        // Recycler Views
        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[1],
            model.getAnimeContinue(),
            binding.homeContinueWatchingContainer,
            binding.homeWatchingRecyclerView,
            binding.homeWatchingProgressBar,
            binding.homeWatchingEmpty,
            binding.homeContinueWatch,
            binding.homeContinueWatchMore,
            getString(R.string.continue_watching)
        )
        binding.homeWatchingBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(0)
        }

        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[2],
            model.getAnimeFav(),
            binding.homeFavAnimeContainer,
            binding.homeFavAnimeRecyclerView,
            binding.homeFavAnimeProgressBar,
            binding.homeFavAnimeEmpty,
            binding.homeFavAnime,
            binding.homeFavAnimeMore,
            getString(R.string.fav_anime)
        )

        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[3],
            model.getAnimePlanned(),
            binding.homePlannedAnimeContainer,
            binding.homePlannedAnimeRecyclerView,
            binding.homePlannedAnimeProgressBar,
            binding.homePlannedAnimeEmpty,
            binding.homePlannedAnime,
            binding.homePlannedAnimeMore,
            getString(R.string.planned_anime)
        )
        binding.homePlannedAnimeBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(0)
        }

        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[4],
            model.getMangaContinue(),
            binding.homeContinueReadingContainer,
            binding.homeReadingRecyclerView,
            binding.homeReadingProgressBar,
            binding.homeReadingEmpty,
            binding.homeContinueRead,
            binding.homeContinueReadMore,
            getString(R.string.continue_reading)
        )
        binding.homeReadingBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(2)
        }

        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[5],
            model.getMangaFav(),
            binding.homeFavMangaContainer,
            binding.homeFavMangaRecyclerView,
            binding.homeFavMangaProgressBar,
            binding.homeFavMangaEmpty,
            binding.homeFavManga,
            binding.homeFavMangaMore,
            getString(R.string.fav_manga)
        )

        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[6],
            model.getMangaPlanned(),
            binding.homePlannedMangaContainer,
            binding.homePlannedMangaRecyclerView,
            binding.homePlannedMangaProgressBar,
            binding.homePlannedMangaEmpty,
            binding.homePlannedManga,
            binding.homePlannedMangaMore,
            getString(R.string.planned_manga)
        )
        binding.homePlannedMangaBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(2)
        }

        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[7],
            model.getRecommendation(),
            binding.homeRecommendedContainer,
            binding.homeRecommendedRecyclerView,
            binding.homeRecommendedProgressBar,
            binding.homeRecommendedEmpty,
            binding.homeRecommended,
            binding.homeRecommendedMore,
            getString(R.string.recommended)
        )

        binding.homeUserStatusContainer.visibility = View.VISIBLE
        binding.homeUserStatusProgressBar.visibility = View.VISIBLE
        binding.homeUserStatusRecyclerView.visibility = View.GONE
        model.getUserStatus().observe(viewLifecycleOwner) {
            if (!PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[8]) {
                binding.homeUserStatusContainer.visibility = View.GONE
                return@observe
            }
            binding.homeUserStatusRecyclerView.visibility = View.GONE
            if (it != null) {
                if (it.isNotEmpty()) {
                    PrefManager.getLiveVal(PrefName.RefreshStatus, false).apply {
                        asLiveBool()
                        observe(viewLifecycleOwner) { _ ->
                            binding.homeUserStatusRecyclerView.adapter = UserStatusAdapter(it)
                        }
                    }
                    binding.homeUserStatusRecyclerView.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    binding.homeUserStatusRecyclerView.layoutAnimation =
                        LayoutAnimationController(setSlideIn(), 0.25f)
                    binding.homeUserStatusRecyclerView.visibility = View.VISIBLE
                } else {
                    binding.homeUserStatusContainer.visibility = View.GONE
                }
                binding.homeUserStatusProgressBar.visibility = View.GONE
            }
        }

        fun getHiddenLayout(
            items: ArrayList<Media>?,
            anchorView: TextView,
            container: LinearLayout,
            titleView: TextView,
            recyclerView: RecyclerView,
            moreButton: ImageView
        ) {
            if (items.isNullOrEmpty()) {
                anchorView.setOnLongClickListener { view ->
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    snackString(getString(R.string.no_hidden_items))
                    true
                }
            } else {
                recyclerView.adapter = MediaAdaptor(0, items, requireActivity())
                recyclerView.layoutManager = LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                recyclerView.layoutAnimation = LayoutAnimationController(setSlideIn(), 0.25f)
                anchorView.setOnLongClickListener { view ->
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    container.visibility = View.VISIBLE
                    true
                }
                moreButton.setSafeOnClickListener { _ ->
                    ContextCompat.startActivity(
                        requireActivity(),
                        Intent(requireActivity(), MediaListViewActivity::class.java)
                            .putExtra("title", titleView.text)
                            .putExtra("media", items),
                        null
                    )
                }
                titleView.setOnLongClickListener { view ->
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    container.visibility = View.GONE
                    true
                }
            }
        }

        model.getHiddenAnime().observe(viewLifecycleOwner) {
            getHiddenLayout(
                it,
                binding.homeContinueWatch,
                binding.homeHiddenAnimeContainer,
                binding.homeHiddenAnimeTitle,
                binding.homeHiddenAnimeRecyclerView,
                binding.homeHiddenAnimeMore
            )
        }

        model.getHiddenManga().observe(viewLifecycleOwner) {
            getHiddenLayout(
                it,
                binding.homeContinueRead,
                binding.homeHiddenMangaContainer,
                binding.homeHiddenMangaTitle,
                binding.homeHiddenMangaRecyclerView,
                binding.homeHiddenMangaMore
            )
        }

        model.getRecommendation().observe(viewLifecycleOwner) {
            if (it.isNullOrEmpty()) {
                binding.homeRandomAnime.setOnClickListener {
                    snackString(R.string.no_recommendations)
                }
                binding.homeRandomManga.setOnClickListener {
                    snackString(R.string.no_recommendations)
                }
            } else {
                fun getRandomMedia(type: MediaType): Media {
                    var media: Media?
                    do {
                        media = it[Random.nextInt(it.size)].takeIf { item ->
                            (type == MediaType.ANIME && item.anime != null)
                                    || (type == MediaType.MANGA && item.manga != null)
                        }
                    } while (media == null)
                    val imageView = if (type == MediaType.MANGA)
                        binding.homeRandomMangaImage
                    else binding.homeRandomAnimeImage
                    imageView.loadImage(media.banner ?: media.cover)
                    return media
                }

                var randomAnime = getRandomMedia(MediaType.ANIME)
                var randomManga = getRandomMedia(MediaType.MANGA)

                fun onRandomClick(type: MediaType) {
                    val media = if (type == MediaType.MANGA) randomManga else randomAnime
                    ContextCompat.startActivity(
                        requireContext(),
                        Intent(requireContext(), MediaDetailsActivity::class.java)
                            .putExtra("media", media as Serializable), null
                    )
                }
                binding.homeRandomAnime.setOnClickListener {
                    onRandomClick(MediaType.ANIME)
                    randomAnime = getRandomMedia(MediaType.ANIME)
                }
                binding.homeRandomManga.setOnClickListener {
                    onRandomClick(MediaType.MANGA)
                    randomManga = getRandomMedia(MediaType.MANGA)
                }
            }
        }

        binding.homeUserAvatarContainer.startAnimation(setSlideUp())

        model.empty.observe(viewLifecycleOwner)
        {
            binding.homeDantotsuContainer.isVisible = it == true
            (binding.homeDantotsuIcon.drawable as Animatable).start()
            binding.homeDantotsuContainer.startAnimation(setSlideUp())
            binding.homeDantotsuIcon.setSafeOnClickListener {
                (binding.homeDantotsuIcon.drawable as Animatable).start()
            }
        }

        val array = arrayOf(
            "Subscribed",
            "AnimeContinue",
            "AnimeFav",
            "AnimePlanned",
            "MangaContinue",
            "MangaFav",
            "MangaPlanned",
            "Recommendation",
            "UserStatus"
        )

        val containers = arrayOf(
            binding.homeSubscribedItemContainer,
            binding.homeContinueWatchingContainer,
            binding.homeFavAnimeContainer,
            binding.homePlannedAnimeContainer,
            binding.homeContinueReadingContainer,
            binding.homeFavMangaContainer,
            binding.homePlannedMangaContainer,
            binding.homeRecommendedContainer,
            binding.homeUserStatusContainer,
            binding.homeUserStatusContainer
        )

        val live = Refresh.activity.getOrPut(1) { MutableLiveData(false) }
        live.observe(viewLifecycleOwner) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        //Get userData First
                        loadFragment(requireActivity()) { load() }
                        model.loaded = true
                        CoroutineScope(Dispatchers.IO).launch {
                            model.setListImages()
                        }
                        var empty = true
                        val homeLayoutShow: List<Boolean> =
                            PrefManager.getVal(PrefName.HomeLayout)
                        model.initHomePage()
                        (array.indices).forEach { i ->
                            if (homeLayoutShow.elementAt(i)) {
                                empty = false
                            } else withContext(Dispatchers.Main) {
                                containers[i].visibility = View.GONE
                            }
                        }
                        ResumableShortcuts.updateShortcuts(
                            context,
                            model.getAnimeContinue().value,
                            model.getMangaContinue().value
                        )
                        ResumableWidget.injectUpdate(
                            context,
                            model.getAnimeContinue().value,
                            model.getMangaContinue().value
                        )
                        model.empty.postValue(empty)
                    }
                    live.postValue(false)
                    _binding?.homeRefresh?.isRefreshing = false
                }
            }
        }
    }

    override fun onResume() {
        if (!model.loaded) Refresh.activity[1]!!.postValue(true)
        if (_binding != null) {
            val count = Anilist.unreadNotificationCount + MatagiUpdater.hasUpdate
            binding.homeNotificationCount.isVisible = count > 0
            binding.homeNotificationCount.text = count.toString()
        }
        super.onResume()
    }
}
