package ani.dantotsu.media

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.dantotsu.GesturesListener
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.ZoomOutPageTransformer
import ani.dantotsu.blurImage
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ActivityMediaBinding
import ani.dantotsu.initActivity
import ani.dantotsu.loadImage
import ani.dantotsu.media.anime.AnimeWatchFragment
import ani.dantotsu.media.comments.CommentsFragment
import ani.dantotsu.media.manga.MangaReadFragment
import ani.dantotsu.media.novel.NovelReadFragment
import ani.dantotsu.media.reviews.ReviewFragment
import ani.dantotsu.navBarHeight
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.openLinkInYouTube
import ani.dantotsu.others.AndroidBug5497Workaround
import ani.dantotsu.others.getSerialized
import ani.dantotsu.setBaseline
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toPx
import ani.dantotsu.torrServerStart
import ani.dantotsu.updateLayoutParams
import ani.dantotsu.updateMargins
import ani.dantotsu.util.LauncherWrapper
import ani.dantotsu.view.dialog.ImageViewDialog
import com.flaviofaria.kenburnsview.RandomTransitionGenerator
import com.google.android.material.appbar.AppBarLayout
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import eu.kanade.tachiyomi.util.system.getThemeColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import nl.joery.animatedbottombar.AnimatedBottomBar
import java.io.Serializable
import kotlin.math.abs
import kotlin.math.min


class MediaDetailsActivity : AppCompatActivity(), AppBarLayout.OnOffsetChangedListener {
    lateinit var launcher: LauncherWrapper
    lateinit var binding: ActivityMediaBinding
    private val scope = lifecycleScope
    private val model: MediaDetailsViewModel by viewModels()
    var selected = 0
    lateinit var navBar: AnimatedBottomBar
    var anime = true
    private var adult = false
    private var tubePlayer: YouTubePlayer? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var media: Media = intent.getSerialized("media") ?: mediaSingleton ?: emptyMedia()
        val id = intent.getIntExtra("mediaId", -1)
        if (id != -1) {
            runBlocking {
                withContext(Dispatchers.IO) {
                    media = Anilist.query.getMedia(id, false) ?: emptyMedia()
                }
            }
        }
        if (media.name == "No media found") {
            snackString(media.name)
            onBackPressedDispatcher.onBackPressed()
            return
        }
        val contract = ActivityResultContracts.OpenDocumentTree()
        launcher = LauncherWrapper(this, contract)

        mediaSingleton = null
        ThemeManager(this).applyTheme(MediaSingleton.bitmap)
        MediaSingleton.bitmap = null

        binding = ActivityMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        screenWidth = resources.displayMetrics.widthPixels.toFloat()
        navBar = binding.mediaBottomBar.apply {
            updateLayoutParams(resources.configuration.orientation)
        }

        // Ui init

        initActivity(this)
        val oldMargin = binding.mediaViewPager.marginBottom
        AndroidBug5497Workaround.assistActivity(this) {
            if (it) {
                binding.mediaViewPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = 0
                }
                navBar.visibility = View.GONE
            } else {
                binding.mediaViewPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = oldMargin
                }
                navBar.visibility = View.VISIBLE
            }
        }

        binding.mediaBanner.updateLayoutParams { height += statusBarHeight }
        binding.mediaBannerNoKen.updateLayoutParams { height += statusBarHeight }
        binding.youTubeBanner.updateLayoutParams { height += statusBarHeight }
        binding.mediaClose.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.incognito.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.mediaCollapsing.minimumHeight = statusBarHeight

        binding.mediaTitle.isSelected = true

        mMaxScrollSize = binding.mediaAppBar.totalScrollRange
        binding.mediaAppBar.addOnOffsetChangedListener(this)

        binding.mediaClose.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val bannerAnimations: Boolean = PrefManager.getVal(PrefName.BannerAnimations)
        if (bannerAnimations) {
            val generator = RandomTransitionGenerator(
                (10000 + 15000 * ((PrefManager.getVal(PrefName.AnimationSpeed) as Float))).toLong(),
                AccelerateDecelerateInterpolator()
            )
            binding.mediaBanner.setTransitionGenerator(generator)
        }
        val banner = if (bannerAnimations) binding.mediaBanner else binding.mediaBannerNoKen
        binding.mediaViewPager.isUserInputEnabled = false
        binding.mediaViewPager.setPageTransformer(ZoomOutPageTransformer())

        val isDownload = intent.getBooleanExtra("download", false)
        media.selected = model.loadSelected(media, isDownload)

        binding.mediaCoverImage.loadImage(media.cover)
        binding.mediaCoverImage.setOnLongClickListener {
            val coverTitle = getString(R.string.cover, media.userPreferredName)
            ImageViewDialog.newInstance(
                this,
                coverTitle,
                media.cover
            )
        }

        banner.blurImage(media.banner ?: media.cover)
        val gestureDetector = GestureDetector(this, object : GesturesListener() {
            override fun onDoubleClick(event: MotionEvent) {
                if (!(PrefManager.getVal(PrefName.BannerAnimations) as Boolean))
                    snackString(getString(R.string.enable_banner_animations))
                else {
                    binding.mediaBanner.restart()
                    binding.mediaBanner.performClick()
                }
            }

            override fun onLongClick(event: MotionEvent) {
                val bannerTitle = getString(R.string.banner, media.userPreferredName)
                ImageViewDialog.newInstance(
                    this@MediaDetailsActivity,
                    bannerTitle,
                    media.banner ?: media.cover
                )
                banner.performClick()
            }
        })
        banner.setOnTouchListener { _, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
            true
        }

        if (PrefManager.getVal(PrefName.Incognito)) {
            val mediaTitle = "    ${media.userPreferredName}"
            binding.mediaTitle.text = mediaTitle
            binding.incognito.visibility = View.VISIBLE
        } else {
            binding.mediaTitle.text = media.userPreferredName
        }
        binding.mediaTitle.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            copyToClipboard(media.userPreferredName)
            true
        }
        binding.mediaTitleCollapse.text = media.userPreferredName
        binding.mediaTitleCollapse.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            copyToClipboard(media.userPreferredName)
            true
        }
        binding.mediaStatus.text = media.status ?: ""

        //Fav Button
        val favButton = if (Anilist.userid != null) {
            if (media.isFav) binding.mediaFav.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.ic_round_favorite_24
                )
            )

            PopImageButton(
                scope,
                binding.mediaFav,
                R.drawable.ic_round_favorite_24,
                R.drawable.ic_round_favorite_border_24,
                R.color.bg_opp,
                R.color.violet_400,
                media.isFav
            ) {
                media.isFav = it
                Anilist.mutation.toggleFav(media.anime != null, media.id)
                Refresh.all()
            }
        } else {
            binding.mediaFav.visibility = View.GONE
            null
        }

        @SuppressLint("ResourceType")
        fun total() {
            val text = SpannableStringBuilder().apply {
                val white = this@MediaDetailsActivity.getThemeColor(com.google.android.material.R.attr.colorOnBackground)
                val primary = this@MediaDetailsActivity.getThemeColor(com.google.android.material.R.attr.colorPrimary)
                val secondary = this@MediaDetailsActivity.getThemeColor(com.google.android.material.R.attr.colorSecondary)
                if (media.userStatus != null) {
                    append(getString(if (media.anime != null) R.string.watched_num else R.string.read_num))
                    bold { color(secondary) { append("${media.userProgress}") } }
                    append(getString(if (media.anime != null) R.string.items_out_of else R.string.items_out_of)
                    )
                } else {
                    append(
                        getString(if (media.anime != null) R.string.items_total_of else R.string.items_total_of)
                    )
                }
                if (media.anime != null) {
                    if (media.anime!!.nextAiringEpisode != null) {
                        bold { color(primary) { append("${media.anime!!.nextAiringEpisode}") } }
                        append(" / ")
                    }
                    bold { color(white) { append("${media.anime!!.totalEpisodes ?: "??"}") } }
                } else if (media.manga != null) {
                    bold { color(white) { append("${media.manga!!.totalChapters ?: "??"}") } }
                }
            }
            binding.mediaTotal.text = text
        }

        fun progress() {
            val statuses: Array<String> = resources.getStringArray(R.array.status)
            val statusStrings =
                resources.getStringArray(if (media.manga == null) R.array.status_anime else R.array.status_manga)
            val userStatus =
                if (media.userStatus != null) statusStrings[statuses.indexOf(media.userStatus)] else statusStrings[0]

            if (media.userStatus != null) {
                binding.mediaTotal.visibility = View.VISIBLE
                binding.mediaAddToList.text = userStatus
            } else {
                binding.mediaAddToList.setText(R.string.add)
            }
            total()
            binding.mediaAddToList.setOnClickListener {
                if (Anilist.userid != null) {
                    if (supportFragmentManager.findFragmentByTag("dialog") == null)
                        MediaListDialogFragment().show(supportFragmentManager, "dialog")
                } else snackString(getString(R.string.please_login_anilist))
            }
            binding.mediaAddToList.setOnLongClickListener {
                PrefManager.setCustomVal(
                    "${media.id}_progressDialog",
                    true,
                )
                snackString(getString(R.string.auto_update_reset))
                true
            }
        }
        progress()

        model.getMedia().observe(this) {
            if (it != null) {
                media = it
                if (PrefManager.getVal(PrefName.YouTubeBanners)) {
                    media.trailer?.let { preview -> getTrailerBanner(preview) }
                }

                binding.animeTrailerYT.isVisible = media.trailer != null
                binding.animeTrailerYT.setOnClickListener {
                    openLinkInYouTube("https://www.youtube.com/watch?v=${media.trailer}")
                }

                scope.launch {
                    if (media.isFav != favButton?.clicked) favButton?.clicked()
                }

                binding.mediaNotify.setOnClickListener {
                    val i = Intent(Intent.ACTION_SEND)
                    i.type = "text/plain"
                    i.putExtra(Intent.EXTRA_TEXT, media.shareLink)
                    startActivity(Intent.createChooser(i, media.userPreferredName))
                }
                binding.mediaNotify.setOnLongClickListener {
                    openLinkInBrowser(media.shareLink)
                    true
                }
                binding.mediaCover.setOnClickListener {
                    openLinkInBrowser(media.shareLink)
                }
                progress()
            }
        }
        adult = media.isAdult
        val commentId = intent.getIntExtra("commentId", -1)
        val extension = intent.getStringExtra("extension")
        if (media.anime != null) {
            if (PrefManager.getVal(PrefName.TorrServerEnabled))
                torrServerStart()
            binding.mediaViewPager.adapter = ViewPagerAdapter(
                supportFragmentManager,
                lifecycle,
                SupportedMedia.ANIME,
                media,
                commentId,
                extension
            )
        } else if (media.manga != null) {
            binding.mediaViewPager.adapter = ViewPagerAdapter(
                supportFragmentManager,
                lifecycle,
                if (media.format == "NOVEL") SupportedMedia.NOVEL else SupportedMedia.MANGA,
                media,
                commentId,
                extension
            )
            anime = false
        }

        val hasComments = PrefManager.getVal<Boolean>(PrefName.CommentsOptIn)
        selected = media.selected!!.window
        if (!hasComments && selected == 3) selected = 0
        binding.mediaTitle.translationX = -screenWidth

        val infoTab = navBar.createTab(R.drawable.ic_round_info_24, R.string.info, R.id.info)
        val watchTab = if (anime) {
            navBar.createTab(R.drawable.ic_round_movie_filter_24, R.string.watch, R.id.watch)
        } else if (media.format == "NOVEL") {
            navBar.createTab(R.drawable.ic_round_book_24, R.string.read, R.id.read)
        } else {
            navBar.createTab(R.drawable.ic_round_import_contacts_24, R.string.read, R.id.read)
        }
        val reviewsTab = navBar.createTab(R.drawable.ic_round_rate_review_24, R.string.reviews, R.id.reviews)
        navBar.addTab(infoTab)
        navBar.addTab(watchTab)
        navBar.addTab(reviewsTab)
        if (hasComments) {
            val commentTab =
                navBar.createTab(R.drawable.ic_round_comment_24, R.string.comments, R.id.comment)
            navBar.addTab(commentTab)
        }
        if (extension != null) selected = 1
        if (model.continueMedia == null && media.cameFromContinue) {
            model.continueMedia = PrefManager.getVal(PrefName.ContinueMedia)
            selected = 1
        }
        if (intent.getStringExtra("FRAGMENT_TO_LOAD") != null && hasComments) selected = 3
        navBar.setupWithViewPager2(binding.mediaViewPager)
        binding.commentInputLayout.isVisible = (hasComments && selected == 3)
        navBar.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                selected = newIndex
                binding.commentInputLayout.isVisible = (hasComments && selected == 3)
                val sel = model.loadSelected(media, isDownload)
                sel.window = selected
                model.saveSelected(media.id, sel)
            }
        })
        binding.mediaViewPager.setCurrentItem(selected, false)
        navBar.selectTabAt(selected, false)

        binding.mediaViewPager.setBaseline(
            navBar,
            resources.configuration,
            if (hasComments) binding.commentInputLayout else null
        )

        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(this) {
            if (it) {
                scope.launch(Dispatchers.IO) {
                    model.loadMedia(media)
                    live.postValue(false)
                }
            }
        }
    }

    private fun updateVideoScale() {
        val videoScale = if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            (336.toPx + statusBarHeight) / ((screenWidth / 1280) * 720)
        } else {
            (screenWidth - navBarHeight) / ((288.toPx / 720) * 1280)
        }.toFloat()
        binding.youTubeBanner.scaleX = min(videoScale, Float.MAX_VALUE)
        binding.youTubeBanner.scaleY = min(videoScale, Float.MAX_VALUE)
    }

    private fun getTrailerBanner(trailer: String) {
        updateVideoScale()
        if (tubePlayer != null) return
        val youTubePlayerView: YouTubePlayerView = binding.youTubeBanner
        lifecycle.addObserver(youTubePlayerView)
        val youTubePlayerListener = object : AbstractYouTubePlayerListener() {
            var isVideoMuted = true
            override fun onReady(youTubePlayer: YouTubePlayer) {
                binding.youTubeBanner.visibility = View.VISIBLE
                binding.mediaMute.setOnClickListener {
                    if (isVideoMuted) youTubePlayer.unMute() else youTubePlayer.mute()
                    isVideoMuted = !isVideoMuted
                    binding.mediaMuteImage.setImageDrawable(
                        ContextCompat.getDrawable(this@MediaDetailsActivity,
                            if (isVideoMuted)
                                R.drawable.ic_volume_off_24
                            else
                                R.drawable.ic_volume_high_24
                        )
                    )
                }
                binding.mediaMute.visibility = View.VISIBLE
                tubePlayer = youTubePlayer.apply {
                    loadVideo(trailer, 0f)
                    setInterfaceTimeout()
                    mute()
                    play()
                }
                binding.mediaBanner.pause()
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState
            ) {
                super.onStateChange(youTubePlayer, state)
                if (state == PlayerConstants.PlayerState.ENDED) {
                    binding.mediaBanner.resume()
                    binding.youTubeBanner.visibility = View.GONE
                    binding.mediaMute.visibility = View.GONE
                    setInterfaceTimeout()
                }
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                binding.mediaBanner.resume()
                binding.youTubeBanner.visibility = View.GONE
                binding.mediaMute.visibility = View.GONE
                setInterfaceTimeout()
            }
        }
        youTubePlayerView.initialize(
            youTubePlayerListener,
            IFramePlayerOptions.Builder().controls(0).build()
        )
    }

    private fun fadeInterface() {
        binding.mediaCover.animate()
            .setDuration(500)
            .alpha(0.0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    binding.mediaCover.isInvisible = true
                    binding.mediaCover.alpha = 1f
                }
            })
        binding.mediaTitleCollapse.animate()
            .setDuration(500)
            .alpha(0.0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    binding.mediaTitleCollapse.isInvisible = true
                    binding.mediaTitleCollapse.alpha = 1f
                }
            })
        binding.mediaStatus.animate()
            .setDuration(500)
            .alpha(0.0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    binding.mediaStatus.isInvisible = true
                    binding.mediaStatus.alpha = 1f
                }
            })
    }

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private fun setInterfaceTimeout() {
        timeoutHandler.removeCallbacksAndMessages(null)
        if (binding.youTubeBanner.isGone) {
            binding.mediaCover.isInvisible = false
            binding.mediaTitleCollapse.isInvisible = false
            binding.mediaStatus.isInvisible = false
            return
        }
        timeoutHandler.postDelayed({ fadeInterface() }, (5000).toLong())
    }

    private var isUserLeaveHint = false
    public override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        isUserLeaveHint = true
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (isUserLeaveHint) {
            isUserLeaveHint = false
            return
        }
        binding.mediaCover.isInvisible = false
        binding.mediaTitleCollapse.isInvisible = false
        binding.mediaStatus.isInvisible = false
        setInterfaceTimeout()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        navBar.apply { updateMargins(newConfig.orientation) }
        binding.mediaViewPager.setBaseline(
            navBar, newConfig,
            if (PrefManager.getVal(PrefName.CommentsOptIn)) binding.commentInputLayout else null
        )
        updateVideoScale()
    }

    override fun onStop() {
        super.onStop()
        tubePlayer?.pause()
    }

    override fun onStart() {
        super.onStart()
        tubePlayer?.play()
    }

    override fun onDestroy() {
        binding.youTubeBanner.release()
        tubePlayer = null
        super.onDestroy()
    }

    override fun onRestart() {
        super.onRestart()
        if (this::navBar.isInitialized) navBar.selectTabAt(selected)
    }

    private enum class SupportedMedia {
        ANIME, MANGA, NOVEL
    }

    // ViewPager
    private class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        private val mediaType: SupportedMedia,
        private val media: Media,
        private val commentId: Int,
        private val extension: String? = null
    ) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> MediaInfoFragment()
            1 -> {
                val fragment = when (mediaType) {
                    SupportedMedia.ANIME -> AnimeWatchFragment()
                    SupportedMedia.MANGA -> MangaReadFragment()
                    SupportedMedia.NOVEL -> NovelReadFragment()
                }
                val bundle = Bundle()
                extension?.let { bundle.putString("extension", extension) }
                fragment.arguments = bundle
                fragment
            }

            2 -> {
                val fragment = ReviewFragment()
                val bundle = Bundle()
                bundle.putInt("mediaId", media.id)
                bundle.putString("title", media.mainName())
                media.review?.let { bundle.putSerializable("reviews",it as Serializable) }
                fragment.arguments = bundle
                fragment
            }

            3 -> {
                val fragment = CommentsFragment()
                val bundle = Bundle()
                bundle.putInt("mediaId", media.id)
                bundle.putString("mediaName", media.mainName())
                if (commentId != -1) bundle.putInt("commentId", commentId)
                fragment.arguments = bundle
                fragment
            }

            else -> MediaInfoFragment()
        }
    }

    //Collapsing UI Stuff
    private var isCollapsed = false
    private val percent = 45
    private var mMaxScrollSize = 0
    private var screenWidth: Float = 0f

    override fun onOffsetChanged(appBar: AppBarLayout, i: Int) {
        if (mMaxScrollSize == 0) mMaxScrollSize = appBar.totalScrollRange
        val percentage = abs(i) * 100 / mMaxScrollSize

        binding.mediaCover.visibility =
            if (binding.mediaCover.scaleX == 0f) View.GONE else View.VISIBLE
        val duration = (200 * (PrefManager.getVal(PrefName.AnimationSpeed) as Float)).toLong()
        if (percentage >= percent && !isCollapsed) {
            isCollapsed = true
            ObjectAnimator.ofFloat(binding.mediaTitle, "translationX", 0f).setDuration(duration)
                .start()
            ObjectAnimator.ofFloat(binding.mediaAccessContainer, "translationX", screenWidth)
                .setDuration(duration).start()
            ObjectAnimator.ofFloat(binding.mediaCover, "translationX", screenWidth)
                .setDuration(duration).start()
            ObjectAnimator.ofFloat(binding.mediaCollapseContainer, "translationX", screenWidth)
                .setDuration(duration).start()
            binding.mediaBanner.pause()
        }
        if (percentage <= percent && isCollapsed) {
            isCollapsed = false
            ObjectAnimator.ofFloat(binding.mediaTitle, "translationX", -screenWidth)
                .setDuration(duration).start()
            ObjectAnimator.ofFloat(binding.mediaAccessContainer, "translationX", 0f)
                .setDuration(duration).start()
            ObjectAnimator.ofFloat(binding.mediaCover, "translationX", 0f).setDuration(duration)
                .start()
            ObjectAnimator.ofFloat(binding.mediaCollapseContainer, "translationX", 0f)
                .setDuration(duration).start()
            if (PrefManager.getVal(PrefName.BannerAnimations)) binding.mediaBanner.resume()
        }
        if (percentage == 1 && model.scrolledToTop.value != false) model.scrolledToTop.postValue(
            false
        )
        if (percentage == 0 && model.scrolledToTop.value != true) model.scrolledToTop.postValue(true)
    }

    class PopImageButton(
        private val scope: CoroutineScope,
        private val image: ImageView,
        private val d1: Int,
        private val d2: Int,
        private val c1: Int,
        private val c2: Int,
        var clicked: Boolean,
        needsInitialClick: Boolean = false,
        callback: suspend (Boolean) -> (Unit)
    ) {
        private var disabled = false
        private val context = image.context
        private var pressable = true

        init {
            enabled(true)
            if (needsInitialClick) {
                scope.launch {
                    clicked()
                }
            }
            image.setOnClickListener {
                if (pressable && !disabled) {
                    pressable = false
                    clicked = !clicked
                    scope.launch {
                        launch(Dispatchers.IO) {
                            callback.invoke(clicked)
                        }
                        clicked()
                        pressable = true
                    }
                }
            }
        }

        suspend fun clicked() {
            ObjectAnimator.ofFloat(image, "scaleX", 1f, 0f).setDuration(69).start()
            ObjectAnimator.ofFloat(image, "scaleY", 1f, 0f).setDuration(100).start()
            delay(100)

            if (clicked) {
                ObjectAnimator.ofArgb(
                    image,
                    "ColorFilter",
                    ContextCompat.getColor(context, c1),
                    ContextCompat.getColor(context, c2)
                ).setDuration(120).start()
                image.setImageDrawable(AppCompatResources.getDrawable(context, d1))
            } else image.setImageDrawable(AppCompatResources.getDrawable(context, d2))
            ObjectAnimator.ofFloat(image, "scaleX", 0f, 1.5f).setDuration(120).start()
            ObjectAnimator.ofFloat(image, "scaleY", 0f, 1.5f).setDuration(100).start()
            delay(120)
            ObjectAnimator.ofFloat(image, "scaleX", 1.5f, 1f).setDuration(100).start()
            ObjectAnimator.ofFloat(image, "scaleY", 1.5f, 1f).setDuration(100).start()
            delay(200)
            if (clicked) {
                ObjectAnimator.ofArgb(
                    image,
                    "ColorFilter",
                    ContextCompat.getColor(context, c2),
                    ContextCompat.getColor(context, c1)
                ).setDuration(200).start()
            }
        }

        fun enabled(enabled: Boolean) {
            disabled = !enabled
            image.alpha = if (disabled) 0.33f else 1f
        }
    }

    companion object {
        var mediaSingleton: Media? = null
    }
}
