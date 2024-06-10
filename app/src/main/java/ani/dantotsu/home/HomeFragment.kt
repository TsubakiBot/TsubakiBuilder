package ani.dantotsu.home

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LayoutAnimationController
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.widget.PopupMenu
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isGone
import androidx.core.view.isInvisible
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
import ani.dantotsu.databinding.HomeListContainerBinding
import ani.dantotsu.home.status.UserStatusAdapter
import ani.dantotsu.loadFragment
import ani.dantotsu.loadImage
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaAdaptor
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.media.MediaListViewActivity
import ani.dantotsu.media.MediaType
import ani.dantotsu.media.ViewType
import ani.dantotsu.media.emptyMedia
import ani.dantotsu.media.user.ListActivity
import ani.dantotsu.notifications.NotificationActivity
import ani.dantotsu.notifications.subscription.SubscriptionHelper
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.setSlideIn
import ani.dantotsu.setSlideUp
import ani.dantotsu.settings.SettingsDialogFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefManager.asLiveBool
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.toPx
import ani.dantotsu.toRoundImage
import ani.dantotsu.util.BitmapUtil.toSquare
import ani.dantotsu.widgets.resumable.ResumableWidget
import bit.himitsu.forceShowIcons
import bit.himitsu.isOverlapping
import bit.himitsu.launcher.ResumableShortcuts
import bit.himitsu.os.Version
import bit.himitsu.update.MatagiUpdater
import bit.himitsu.widget.FABulous
import bit.himitsu.withFlexibleMargin
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import kotlin.math.max
import kotlin.math.min


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding by lazy { _binding!! }
    private lateinit var homeListContainerBinding: HomeListContainerBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeListContainerBinding = HomeListContainerBinding.bind(binding.root)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    val model: AnilistHomeViewModel by activityViewModels()

    fun load() {
        if (activity != null && _binding != null) {
            lifecycleScope.launch(Dispatchers.Main) {
                binding.homeUserName.text = Anilist.username
                binding.homeUserEpisodesWatched.text = Anilist.episodesWatched.toString()
                binding.homeUserChaptersRead.text = Anilist.chapterRead.toString()
                binding.homeUserAvatar.loadImage(Anilist.avatar, 52.toPx)
                binding.avatarFabulous.toRoundImage(Anilist.avatar, 52.toPx)
                val banner = if (PrefManager.getVal(PrefName.BannerAnimations))
                    binding.homeUserBg
                else
                    binding.homeUserBgNoKen
                banner.blurImage(Anilist.bg)
                binding.homeUserDataProgressBar.visibility = View.GONE
                setActiveNotificationCount()

                binding.homeUserAvatarContainer.startAnimation(setSlideUp())
                binding.avatarFabulous.startAnimation(setSlideUp())
                binding.homeUserDataContainer.visibility = View.VISIBLE
                binding.homeUserDataContainer.layoutAnimation =
                    LayoutAnimationController(setSlideUp(), 0.25f)

                homeListContainerBinding.apply {
                    rotateButtonsToBlades(resources.configuration)
                    homeAnimeList.setOnClickListener {
                        ContextCompat.startActivity(
                            requireActivity(), Intent(requireActivity(), ListActivity::class.java)
                                .putExtra("anime", true)
                                .putExtra("userId", Anilist.userid)
                                .putExtra("username", Anilist.username), null
                        )
                    }
                    homeMangaList.setOnClickListener {
                        ContextCompat.startActivity(
                            requireActivity(), Intent(requireActivity(), ListActivity::class.java)
                                .putExtra("anime", false)
                                .putExtra("userId", Anilist.userid)
                                .putExtra("username", Anilist.username), null
                        )
                    }
                    homeAnimeList.visibility = View.VISIBLE
                    homeMangaList.visibility = View.VISIBLE
                    homeRandomAnime.visibility = View.VISIBLE
                    homeRandomManga.visibility = View.VISIBLE
                    homeListContainer.layoutAnimation =
                        LayoutAnimationController(setSlideIn(), 0.25f)

                    homeListContainerBinding.homeListContainer.postDelayed({
                        rotateBackToStraight(resources.configuration)
                    }, 900 * PrefManager.getVal<Float>(PrefName.AnimationSpeed).toLong())
                }
            }
        } else {
            snackString(R.string.please_reload)
        }
    }

    @OptIn(ExperimentalBadgeUtils::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.homeUserAvatarContainer.setSafeOnClickListener {
            SettingsDialogFragment.newInstance(SettingsDialogFragment.Companion.PageType.HOME).show(
                (it.context as androidx.appcompat.app.AppCompatActivity).supportFragmentManager,
                "dialog"
            )
        }
        binding.homeUserAvatarContainer.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            ContextCompat.startActivity(
                requireContext(), Intent(requireContext(), NotificationActivity::class.java), null
            )
            true
        }

        binding.homeTopContainer.withFlexibleMargin(resources.configuration)
        binding.homeUserBg.updateLayoutParams { height += statusBarHeight }
        binding.homeUserBgNoKen.updateLayoutParams { height += statusBarHeight }
        binding.homeTopContainer.updatePadding(top = statusBarHeight)

        binding.avatarFabulous.apply {
            isVisible = PrefManager.getVal(PrefName.FloatingAvatar)
            if (isVisible) {
                setAnchor(binding.homeUserAvatarContainer)
                (behavior as FloatingActionButton.Behavior).isAutoHideEnabled = false

                setDefaultPosition()

                val handler = Handler(Looper.getMainLooper())
                val mRunnable = Runnable {
                    if (isOverlapping(binding.homeUserAvatarContainer)) {
                        setDefaultPosition()
                    }
                }

                setOnMoveListener(object : FABulous.OnViewMovedListener {
                    override fun onActionMove(x: Float, y: Float) {
                        handler.removeCallbacksAndMessages(mRunnable)
                        if (isOverlapping(binding.homeUserAvatarContainer)) {
                            handler.postDelayed(mRunnable, 1000)
                        }
                        setActiveNotificationCount()
                    }
                })
                setOnLongClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    if (isOverlapping(binding.homeUserAvatarContainer)) {
                        binding.homeUserAvatarContainer.performLongClick()
                    } else {
                        false
                    }
                }
            }
        }

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

        // UserData
        binding.homeUserDataProgressBar.visibility = View.VISIBLE
        binding.homeUserDataContainer.visibility = View.GONE
        if (model.loaded) load()
        // List Images
        model.getListImages().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                homeListContainerBinding.homeAnimeListImage.loadImage(it[0] ?: "https://bit.ly/31bsIHq")
                homeListContainerBinding.homeMangaListImage.loadImage(it[1] ?: "https://bit.ly/2ZGfcuG")
            }
        }

        fun getRandomRecommended(recommended: ArrayList<Media>?) {
            if (recommended.isNullOrEmpty()) {
                homeListContainerBinding.homeRandomAnime.setOnClickListener {
                    snackString(R.string.no_recommendations)
                }
                homeListContainerBinding.homeRandomManga.setOnClickListener {
                    snackString(R.string.no_recommendations)
                }
            } else {
                fun getRandomMedia(type: MediaType): Media {
                    var media: Media?
                    do {
                        media = recommended.randomOrNull()?.takeIf { item ->
                            (type == MediaType.ANIME && item.anime != null)
                                    || (type == MediaType.MANGA && item.manga != null)
                        }
                    } while (media == null)
                    val imageView = if (type == MediaType.MANGA)
                        homeListContainerBinding.homeRandomMangaImage
                    else
                        homeListContainerBinding.homeRandomAnimeImage
                    imageView.loadImage(media.banner ?: media.cover)
                    val buttonImage = if (type == MediaType.MANGA)
                        homeListContainerBinding.homeRandomMangaImageHorz
                    else
                        homeListContainerBinding.homeRandomAnimeImageHorz
                    buttonImage.loadImage(media.banner ?: media.cover)
                    return media
                }

                var randomAnime = if (recommended.none { it.anime != null }) {
                    homeListContainerBinding.homeRandomAnime.setOnClickListener {
                        snackString(R.string.no_recommendations)
                    }
                    emptyMedia()
                } else {
                    getRandomMedia(MediaType.ANIME)
                }
                var randomManga = if (recommended.none { it.manga != null }) {
                    homeListContainerBinding.homeRandomManga.setOnClickListener {
                        snackString(R.string.no_recommendations)
                    }
                    emptyMedia()
                } else {
                    getRandomMedia(MediaType.MANGA)
                }

                fun onRandomClick(type: MediaType) {
                    val media = if (type == MediaType.MANGA) randomManga else randomAnime
                    ContextCompat.startActivity(
                        requireContext(),
                        Intent(requireContext(), MediaDetailsActivity::class.java)
                            .putExtra("media", media as Serializable), null
                    )
                }
                homeListContainerBinding.homeRandomAnime.setOnClickListener {
                    onRandomClick(MediaType.ANIME)
                    randomAnime = getRandomMedia(MediaType.ANIME)
                }
                homeListContainerBinding.homeRandomManga.setOnClickListener {
                    onRandomClick(MediaType.MANGA)
                    randomManga = getRandomMedia(MediaType.MANGA)
                }
            }
        }

        fun getSubscriptionPopup(subscriptions: ArrayList<Media>?) {
            if (!binding.avatarFabulous.isVisible) return
            if (subscriptions.isNullOrEmpty()) {
                binding.avatarFabulous.setOnClickListener {
                    if (binding.avatarFabulous.isOverlapping(binding.homeUserAvatarContainer)) {
                        binding.homeUserAvatarContainer.performClick()
                    }
                }
                return
            }
            val popup = if (Version.isLollipopMR)
                PopupMenu(requireContext(), binding.avatarFabulous, Gravity.END, 0, R.style.MyPopup)
            else
                PopupMenu(requireContext(), binding.avatarFabulous)
            popup.forceShowIcons()

            subscriptions.forEach { media ->
                val item = popup.menu.add(media.mainName())
                Glide.with(requireContext()).asBitmap().load(media.cover).into(object : CustomTarget<Bitmap?>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                        item.setIcon(
                            if (Version.isOreo) {
                                resource.toSquare().toDrawable(resources)
                            } else {
                                resource.toDrawable(resources)
                            }
                        )
                    }

                    override fun onLoadCleared(placeholder: Drawable?) { }
                })
                item.setIntent(Intent(
                    requireContext(), MediaDetailsActivity::class.java
                ).putExtra(
                    "media", media.apply { cameFromContinue = true } as Serializable
                ))
            }

            binding.avatarFabulous.setOnClickListener {
                if (binding.avatarFabulous.isOverlapping(binding.homeUserAvatarContainer)) {
                    binding.homeUserAvatarContainer.performClick()
                } else {
                    popup.show()
                    popup.setOnMenuItemClickListener { item ->
                        item.intent?.let { intent -> startActivity(intent) }
                        true
                    }
                }
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
            title: TextView,
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
                if (string == getString(R.string.recommended)) {
                    getRandomRecommended(it)
                } else if (string == getString(R.string.subscriptions)) {
                    getSubscriptionPopup(it)
                }
                if (!isEnabled) {
                    container.isVisible = false
                    return@observe
                }
                recyclerView.visibility = View.GONE
                empty.visibility = View.GONE
                if (it != null) {
                    if (it.isNotEmpty()) {
                        recyclerView.adapter = MediaAdaptor(ViewType.COMPACT, it, requireActivity()).apply {
                            disableLongClick = string == getString(R.string.subscriptions)
                        }
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
                        more.visibility = View.VISIBLE
                        more.startAnimation(setSlideUp())
                    } else {
                        empty.visibility = View.VISIBLE
                    }
                    title.text = string
                    title.visibility = View.VISIBLE
                    title.startAnimation(setSlideUp())
                    progress.visibility = View.GONE
                }
            }
        }

        // Recycler Views
        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[0],
            model.getSubscriptions(),
            binding.homeSubscribedItemContainer.homeItemContainer,
            binding.homeSubscribedItemContainer.homeItemRecyclerView,
            binding.homeSubscribedItemContainer.homeItemProgressBar,
            binding.homeSubscribedItemContainer.homeItemEmpty,
            binding.homeSubscribedItemContainer.homeItemTitle,
            binding.homeSubscribedItemContainer.homeItemMore,
            getString(R.string.subscriptions)
        )
        binding.homeSubscribedItemContainer.homeItemBrowseButton.text = getString(R.string.subscribe_lists)

        // Recycler Views
        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[1],
            model.getAnimeContinue(),
            binding.homeContinueWatchingContainer.homeItemContainer,
            binding.homeContinueWatchingContainer.homeItemRecyclerView,
            binding.homeContinueWatchingContainer.homeItemProgressBar,
            binding.homeContinueWatchingContainer.homeItemEmpty,
            binding.homeContinueWatchingContainer.homeItemTitle,
            binding.homeContinueWatchingContainer.homeItemMore,
            getString(R.string.continue_watching)
        )
        binding.homeContinueWatchingContainer.homeItemBrowseButton.text = getString(R.string.browse_anime)
        binding.homeContinueWatchingContainer.homeItemBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(0)
        }

        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[2],
            model.getAnimeFav(),
            binding.homeFavAnimeContainer.homeItemContainer,
            binding.homeFavAnimeContainer.homeItemRecyclerView,
            binding.homeFavAnimeContainer.homeItemProgressBar,
            binding.homeFavAnimeContainer.homeItemEmpty,
            binding.homeFavAnimeContainer.homeItemTitle,
            binding.homeFavAnimeContainer.homeItemMore,
            getString(R.string.fav_anime)
        )
        binding.homeFavAnimeContainer.homeItemBrowseButton.text = getString(R.string.browse_anime)
        binding.homeFavAnimeContainer.homeItemBrowseButton.isVisible = false

        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[3],
            model.getAnimePlanned(),
            binding.homePlannedAnimeContainer.homeItemContainer,
            binding.homePlannedAnimeContainer.homeItemRecyclerView,
            binding.homePlannedAnimeContainer.homeItemProgressBar,
            binding.homePlannedAnimeContainer.homeItemEmpty,
            binding.homePlannedAnimeContainer.homeItemTitle,
            binding.homePlannedAnimeContainer.homeItemMore,
            getString(R.string.planned_anime)
        )
        binding.homePlannedAnimeContainer.homeItemBrowseButton.text = getString(R.string.browse_anime)
        binding.homePlannedAnimeContainer.homeItemBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(0)
        }

        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[4],
            model.getMangaContinue(),
            binding.homeContinueReadingContainer.homeItemContainer,
            binding.homeContinueReadingContainer.homeItemRecyclerView,
            binding.homeContinueReadingContainer.homeItemProgressBar,
            binding.homeContinueReadingContainer.homeItemEmpty,
            binding.homeContinueReadingContainer.homeItemTitle,
            binding.homeContinueReadingContainer.homeItemMore,
            getString(R.string.continue_reading)
        )
        binding.homeContinueReadingContainer.homeItemBrowseButton.text = getString(R.string.browse_manga)
        binding.homeContinueReadingContainer.homeItemBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(2)
        }

        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[5],
            model.getMangaFav(),
            binding.homeFavMangaContainer.homeItemContainer,
            binding.homeFavMangaContainer.homeItemRecyclerView,
            binding.homeFavMangaContainer.homeItemProgressBar,
            binding.homeFavMangaContainer.homeItemEmpty,
            binding.homeFavMangaContainer.homeItemTitle,
            binding.homeFavMangaContainer.homeItemMore,
            getString(R.string.fav_manga)
        )
        binding.homeFavMangaContainer.homeItemBrowseButton.text = getString(R.string.browse_manga)
        binding.homeFavMangaContainer.homeItemBrowseButton.isVisible = false

        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[6],
            model.getMangaPlanned(),
            binding.homePlannedMangaContainer.homeItemContainer,
            binding.homePlannedMangaContainer.homeItemRecyclerView,
            binding.homePlannedMangaContainer.homeItemProgressBar,
            binding.homePlannedMangaContainer.homeItemEmpty,
            binding.homePlannedMangaContainer.homeItemTitle,
            binding.homePlannedMangaContainer.homeItemMore,
            getString(R.string.planned_manga)
        )
        binding.homePlannedMangaContainer.homeItemBrowseButton.text = getString(R.string.browse_manga)
        binding.homePlannedMangaContainer.homeItemBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(2)
        }

        initRecyclerView(
            PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout)[7],
            model.getRecommendation(),
            binding.homeRecommendedContainer.homeItemContainer,
            binding.homeRecommendedContainer.homeItemRecyclerView,
            binding.homeRecommendedContainer.homeItemProgressBar,
            binding.homeRecommendedContainer.homeItemEmpty,
            binding.homeRecommendedContainer.homeItemTitle,
            binding.homeRecommendedContainer.homeItemMore,
            getString(R.string.recommended)
        )
        binding.homeRecommendedContainer.homeItemEmptyTitle.text = getString(R.string.no_suggestions)
        binding.homeRecommendedContainer.homeItemBrowseButton.isVisible = false

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
                container.visibility = View.GONE
                anchorView.setOnLongClickListener { view ->
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    snackString(getString(R.string.no_hidden_items))
                    true
                }
            } else {
                recyclerView.adapter = MediaAdaptor(ViewType.COMPACT, items, requireActivity())
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
                binding.homeContinueWatchingContainer.homeItemTitle,
                binding.homeHiddenAnimeContainer,
                binding.homeHiddenAnimeTitle,
                binding.homeHiddenAnimeRecyclerView,
                binding.homeHiddenAnimeMore
            )
        }

        model.getHiddenManga().observe(viewLifecycleOwner) {
            getHiddenLayout(
                it,
                binding.homeContinueReadingContainer.homeItemTitle,
                binding.homeHiddenMangaContainer,
                binding.homeHiddenMangaTitle,
                binding.homeHiddenMangaRecyclerView,
                binding.homeHiddenMangaMore
            )
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

        val containers = arrayOf(
            binding.homeSubscribedItemContainer.homeItemContainer,
            binding.homeContinueWatchingContainer.homeItemContainer,
            binding.homeFavAnimeContainer.homeItemContainer,
            binding.homePlannedAnimeContainer.homeItemContainer,
            binding.homeContinueReadingContainer.homeItemContainer,
            binding.homeFavMangaContainer.homeItemContainer,
            binding.homePlannedMangaContainer.homeItemContainer,
            binding.homeRecommendedContainer.homeItemContainer,
            binding.homeUserStatusContainer,
        )

        val live = Refresh.activity.getOrPut(1) { MutableLiveData(false) }
        live.observe(viewLifecycleOwner) {
            if (it) {
                lifecycleScope.launch(Dispatchers.IO) {
                    // Get userData First
                    loadFragment(requireActivity()) { load() }
                    model.loaded = true
                    model.setListImages()
                    var empty = true
                    val homeLayoutShow: List<Boolean> =
                        PrefManager.getVal(PrefName.HomeLayout)
                    model.initHomePage()
                    (containers.indices).forEach { i ->
                        if (homeLayoutShow.elementAt(i)) {
                            empty = false
                        } else withContext(Dispatchers.Main) {
                            containers[i].visibility = View.GONE
                        }
                    }

                    binding.homeSubscribedItemContainer.homeItemBrowseButton.setOnClickListener {
                        val userList = arrayListOf<Media>().apply {
                            model.getAnimeContinue().value?.let { items -> addAll(items) }
                            model.getAnimePlanned().value?.let { items -> addAll(items) }
                            model.getMangaContinue().value?.let { items -> addAll(items) }
                            model.getMangaPlanned().value?.let { items -> addAll(items) }
                        }
                        if (userList.isEmpty()) {
                            snackString(R.string.no_current_items)
                        } else {
                            userList.forEach { media ->
                                SubscriptionHelper.saveSubscription(media, true)
                            }
                            model.setSubscriptions(userList)
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

    fun setActiveNotificationCount() {
        val count = Anilist.unreadNotificationCount + MatagiUpdater.hasUpdate
        if (binding.avatarFabulous.isOverlapping(binding.homeUserAvatarContainer)) {
            binding.homeNotificationCount.isVisible = false
            binding.avatarFabulous.setBadgeDrawable(count)
        } else {
            binding.homeNotificationCount.text = count.toString()
            binding.homeNotificationCount.isVisible = count > 0
            binding.avatarFabulous.setBadgeDrawable(null)
        }
    }

    override fun onResume() {
        if (!model.loaded) Refresh.activity[1]!!.postValue(true)
        if (_binding != null) {
            binding.avatarFabulous.setDefaultPosition()
            setActiveNotificationCount()
        }
        super.onResume()
    }

    private val alphaTime = 450L * PrefManager.getVal<Float>(PrefName.AnimationSpeed).toLong()

    private fun onAlphaDissolved(configuration: Configuration) {
        val portrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        val rotateTime = 600L * PrefManager.getVal<Float>(PrefName.AnimationSpeed).toLong()
        val adjustTime = (rotateTime + 150L) * PrefManager.getVal<Float>(PrefName.AnimationSpeed).toLong()

        homeListContainerBinding.homeListContainer.run {
            ValueAnimator.ofInt(measuredHeight, 76.toPx).apply {
                addUpdateListener { valueAnimator ->
                    val layoutParams: ViewGroup.LayoutParams = layoutParams
                    layoutParams.height = (valueAnimator.getAnimatedValue() as Int)
                    setLayoutParams(layoutParams)
                }
            }.setDuration(adjustTime).start()
            ValueAnimator.ofInt(
                (layoutParams as ViewGroup.MarginLayoutParams).topMargin,
                if (portrait) 8.toPx else 0
            ).apply {
                addUpdateListener { valueAnimator ->
                    updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = (valueAnimator.getAnimatedValue() as Int)
                    }
                }
            }.setDuration(adjustTime).start()
            ValueAnimator.ofInt(
                (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin,
                if (portrait) 0 else 24.toPx
            ).apply {
                addUpdateListener { valueAnimator ->
                    updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = (valueAnimator.getAnimatedValue() as Int)
                    }
                }
                doOnStart {
                    binding.homeTopGradient.updatePadding(bottom = 0)
                    homeListContainerBinding.homeRandomContainer.postDelayed({
                        homeListContainerBinding.homeRandomContainer.isVisible = true
                        ObjectAnimator.ofFloat(
                            homeListContainerBinding.homeRandomContainer, View.ALPHA, 0f, 1.0f
                        ).setDuration(alphaTime).start()

                        homeListContainerBinding.homeRandomAnimeHorz.setOnClickListener {
                            homeListContainerBinding.homeRandomAnime.performClick()
                        }
                        homeListContainerBinding.homeRandomMangaHorz.setOnClickListener {
                            homeListContainerBinding.homeRandomManga.performClick()
                        }
                    }, alphaTime)
                }
            }.setDuration(adjustTime).start()
        }

        homeListContainerBinding.homeAnimeList.run {
            ObjectAnimator.ofFloat(
                this, View.ROTATION, rotation, 0f
            ).setDuration(rotateTime).apply {
                doOnEnd {
                    homeListContainerBinding.homeRandomAnime.isGone = true
                    updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        marginStart = 8.toPx
                        marginEnd = 8.toPx
                    }
                }
            }. start()
        }

        homeListContainerBinding.homeMangaList.run {
            ObjectAnimator.ofFloat(
                this, View.ROTATION, rotation, 0f
            ).setDuration(rotateTime).apply {
                doOnEnd {
                    homeListContainerBinding.homeRandomManga.isGone = true
                    updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        marginStart = 8.toPx
                        marginEnd = 8.toPx
                    }
                }
            }.start()
        }
    }

    private fun rotateBackToStraight(configuration: Configuration) {
        if (!PrefManager.getVal<Boolean>(PrefName.HomeMainHide)) return

        homeListContainerBinding.homeRandomManga.run {
            ObjectAnimator.ofFloat(
                this, View.ALPHA, 1.0f, 0f
            ).setDuration(alphaTime).apply {
                doOnEnd {
                    isInvisible = true
                    homeListContainerBinding.homeRandomAnime.run {
                        ObjectAnimator.ofFloat(
                            this, View.ALPHA, 1.0f, 0f
                        ).setDuration(alphaTime).apply {
                            doOnEnd {
                                isInvisible = true
                                onAlphaDissolved(configuration)
                            }
                        }
                    }.start()
                }
            }
        }.start()
    }

    private fun rotateButtonsToBlades(configuration: Configuration) {
        homeListContainerBinding.homeRandomContainer.isGone = true
        homeListContainerBinding.homeRandomAnime.alpha = 1.0f
        homeListContainerBinding.homeRandomAnime.isVisible = true
        homeListContainerBinding.homeRandomManga.alpha = 1.0f
        homeListContainerBinding.homeRandomManga.isVisible = true
        val portrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        homeListContainerBinding.homeListContainer.run {
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (portrait) {
                    height = 186.toPx
                    marginStart = 16.toPx
                    marginEnd = 16.toPx
                    bottomMargin = 0
                } else {
                    height = 140.toPx
                    marginStart = 24.toPx
                    marginEnd = 24.toPx
                    bottomMargin = 24.toPx
                }
            }
        }

        val angle = if (portrait) {
            (((resources.displayMetrics.widthPixels - 32.toPx) / 186.toPx) + -45).toFloat()
        } else {
            (((resources.displayMetrics.widthPixels - 48.toPx) / 140.toPx) + -15).toFloat()
        }

        homeListContainerBinding.homeAnimeList.run {
            rotation = angle
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (portrait) {
                    marginStart = 0
                    marginEnd = (-72).toPx
                } else {
                    marginStart = 4.toPx
                    marginEnd = (-16).toPx
                }
            }
        }

        homeListContainerBinding.homeRandomAnime.run {
            rotation = angle
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (portrait) {
                    marginStart = (-24).toPx
                    marginEnd = (-48).toPx
                } else {
                    marginStart = (-6).toPx
                    marginEnd = (-12).toPx
                }
            }
        }

        homeListContainerBinding.homeMangaList.run {
            alpha = 1f
            isVisible = true
            rotation = angle
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (portrait) {
                    marginStart = (-48).toPx
                    marginEnd = (-24).toPx
                } else {
                    marginStart = (-12).toPx
                    marginEnd = (-6).toPx
                }
            }
        }

        homeListContainerBinding.homeRandomManga.run {
            alpha = 1f
            isVisible = true
            rotation = angle
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (portrait) {
                    marginStart = (-72).toPx
                    marginEnd = 0
                } else {
                    marginStart = (-16).toPx
                    marginEnd = 4.toPx
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.homeTopContainer.withFlexibleMargin(newConfig)
        if (PrefManager.getVal<Boolean>(PrefName.HomeMainHide)) {
            val portrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
            homeListContainerBinding.homeListContainer.run {
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = if (portrait) 8.toPx else 0
                    bottomMargin = if (portrait) 0 else 24.toPx
                }
            }
        } else {
            rotateButtonsToBlades(newConfig)
        }
    }
}
