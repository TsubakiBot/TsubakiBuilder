package ani.dantotsu.home

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LayoutAnimationController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.MediaPageTransformer
import ani.dantotsu.R
import ani.dantotsu.Strings.getString
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.currContext
import ani.dantotsu.databinding.ItemAnimePageBinding
import ani.dantotsu.databinding.ItemListContainerBinding
import ani.dantotsu.databinding.LayoutTrendingBinding
import ani.dantotsu.loadImage
import ani.dantotsu.media.CalendarActivity
import ani.dantotsu.media.GenreActivity
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaAdaptor
import ani.dantotsu.media.MediaListViewActivity
import ani.dantotsu.media.ReviewPopupActivity
import ani.dantotsu.media.SearchActivity
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.setSlideIn
import ani.dantotsu.setSlideUp
import ani.dantotsu.settings.SettingsDialogFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.toPx
import ani.himitsu.update.MatagiUpdater
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import eu.kanade.tachiyomi.util.system.getThemeColor

class AnimePageAdapter : RecyclerView.Adapter<AnimePageAdapter.AnimePageViewHolder>() {
    val ready = MutableLiveData(false)
    lateinit var binding: ItemAnimePageBinding
    private lateinit var bindingListContainer: ItemListContainerBinding
    lateinit var trendingBinding: LayoutTrendingBinding
    private var trendHandler: Handler? = null
    private lateinit var trendRun: Runnable
    var trendingViewPager: ViewPager2? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimePageViewHolder {
        val binding =
            ItemAnimePageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimePageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimePageViewHolder, position: Int) {
        binding = holder.binding
        trendingBinding = LayoutTrendingBinding.bind(binding.root)
        trendingViewPager = trendingBinding.trendingViewPager

        val textInputLayout = holder.itemView.findViewById<TextInputLayout>(R.id.searchBar)
        val currentColor = textInputLayout.boxBackgroundColor
        val semiTransparentColor = (currentColor and 0x00FFFFFF) or 0xA8000000.toInt()
        textInputLayout.boxBackgroundColor = semiTransparentColor
        val materialCardView =
            holder.itemView.findViewById<MaterialCardView>(R.id.userAvatarContainer)
        materialCardView.setCardBackgroundColor(semiTransparentColor)
        val color = currContext().getThemeColor(android.R.attr.windowBackground)

        textInputLayout.boxBackgroundColor = (color and 0x00FFFFFF) or 0x28000000
        materialCardView.setCardBackgroundColor((color and 0x00FFFFFF) or 0x28000000)

        trendingBinding.titleContainer.updatePadding(top = statusBarHeight)

        if (PrefManager.getVal(PrefName.SmallView)) {
            trendingBinding.trendingContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = (-108f).toPx
            }
        }

        updateAvatar()

        trendingBinding.searchBar.hint = "ANIME"
        trendingBinding.searchBarText.setOnClickListener {
            ContextCompat.startActivity(
                it.context,
                Intent(it.context, SearchActivity::class.java).putExtra("type", "ANIME"),
                null
            )
        }

        trendingBinding.userAvatar.setSafeOnClickListener {
            val dialogFragment =
                SettingsDialogFragment.newInstance(SettingsDialogFragment.Companion.PageType.ANIME)
            dialogFragment.show((it.context as AppCompatActivity).supportFragmentManager, "dialog")
        }
        trendingBinding.userAvatar.setOnLongClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            ContextCompat.startActivity(
                view.context,
                Intent(view.context, ProfileActivity::class.java)
                    .putExtra("userId", Anilist.userid), null
            )
            false
        }

        trendingBinding.searchBar.setEndIconOnClickListener {
            trendingBinding.searchBar.performClick()
        }

        val count = Anilist.unreadNotificationCount + MatagiUpdater.hasUpdate
        trendingBinding.notificationCount.isVisible =
            !PrefManager.getVal<Boolean>(PrefName.FloatingAvatar) && count > 0
        trendingBinding.notificationCount.text = count.toString()

        listOf(
            binding.animePreviousSeason,
            binding.animeThisSeason,
            binding.animeNextSeason
        ).forEachIndexed { i, it ->
            it.setSafeOnClickListener { onSeasonClick.invoke(i) }
            it.setOnLongClickListener { onSeasonLongClick.invoke(i) }
        }

        bindingListContainer = ItemListContainerBinding.bind(binding.root).apply {
            leftButtonImage.loadImage("https://s4.anilist.co/file/anilistcdn/media/anime/banner/16498-8jpFCOcDmneX.jpg")
            rightButtonText.text = getString(R.string.release_calendar)
            rightButtonImage.loadImage("https://s4.anilist.co/file/anilistcdn/media/anime/banner/125367-hGPJLSNfprO3.jpg")

            leftListButton.setOnClickListener {
                ContextCompat.startActivity(
                    it.context,
                    Intent(it.context, GenreActivity::class.java).putExtra("type", "ANIME"),
                    null
                )
            }
            rightListButton.setOnClickListener {
                ContextCompat.startActivity(
                    it.context,
                    Intent(it.context, CalendarActivity::class.java),
                    null
                )
            }
            reviewButtonText.text = getString(R.string.review_type, "ANIME")
            reviewButton.setOnClickListener {
                ContextCompat.startActivity(
                    it.context,
                    Intent(it.context, ReviewPopupActivity::class.java).putExtra("type", "ANIME"),
                    null
                )
            }
            //reviewButtonImage.loadImage()
        }

        binding.animeIncludeList.isVisible = Anilist.userid != null

        binding.animeIncludeList.isChecked = PrefManager.getVal(PrefName.PopularAnimeList)

        binding.animeIncludeList.setOnCheckedChangeListener { _, isChecked ->
            onIncludeListClick.invoke(isChecked)
            PrefManager.setVal(PrefName.PopularAnimeList, isChecked)
        }
        if (ready.value == false)
            ready.postValue(true)
    }

    lateinit var onSeasonClick: ((Int) -> Unit)
    lateinit var onSeasonLongClick: ((Int) -> Boolean)
    lateinit var onIncludeListClick: ((Boolean) -> Unit)

    override fun getItemCount(): Int = 1

    fun updateHeight() {
        trendingViewPager!!.updateLayoutParams { height += statusBarHeight }
    }

    fun setReviewImageFromTrending(media: Media) {
        bindingListContainer.reviewButtonImage.loadImage(media.cover)
    }

    fun updateTrending(adaptor: MediaAdaptor) {
        trendingBinding.trendingProgressBar.visibility = View.GONE
        trendingBinding.trendingViewPager.adapter = adaptor
        trendingBinding.trendingViewPager.offscreenPageLimit = 3
        trendingBinding.trendingViewPager.getChildAt(0).overScrollMode =
            RecyclerView.OVER_SCROLL_NEVER
        trendingBinding.trendingViewPager.setPageTransformer(MediaPageTransformer())
        trendHandler = Handler(Looper.getMainLooper())
        trendRun = Runnable {
            trendingBinding.trendingViewPager.currentItem += 1
        }
        trendingBinding.trendingViewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    trendHandler?.removeCallbacks(trendRun)
                    if (PrefManager.getVal(PrefName.TrendingScroller)) {
                        trendHandler!!.postDelayed(trendRun, 4000)
                    }
                }
            }
        )

        trendingBinding.trendingViewPager.layoutAnimation =
            LayoutAnimationController(setSlideIn(), 0.25f)
        trendingBinding.titleContainer.startAnimation(setSlideUp())
        bindingListContainer.listContainer.layoutAnimation =
            LayoutAnimationController(setSlideIn(), 0.25f)
        binding.animeSeasonsCont.layoutAnimation =
            LayoutAnimationController(setSlideIn(), 0.25f)
    }

    fun updateRecent(adaptor: MediaAdaptor, media: MutableList<Media>) {
        binding.apply {
            init(
                adaptor,
                animeUpdatedRecyclerView,
                animeUpdatedProgressBar,
                animeRecently,
                animeRecentlyMore,
                getString(R.string.updated),
                media
            )
            animePopular.visibility = View.VISIBLE
            animePopular.startAnimation(setSlideUp())
            if (adaptor.itemCount == 0) {
                animeRecentlyContainer.visibility = View.GONE
            }
        }

    }

    fun updateMovies(adaptor: MediaAdaptor, media: MutableList<Media>) {
        binding.apply {
            init(
                adaptor,
                animeMoviesRecyclerView,
                animeMoviesProgressBar,
                animeMovies,
                animeMoviesMore,
                getString(R.string.trending_movies),
                media
            )
        }
    }

    fun updateTopRated(adaptor: MediaAdaptor, media: MutableList<Media>) {
        binding.apply {
            init(
                adaptor,
                animeTopRatedRecyclerView,
                animeTopRatedProgressBar,
                animeTopRated,
                animeTopRatedMore,
                getString(R.string.top_rated),
                media
            )
        }
    }

    fun updateMostFav(adaptor: MediaAdaptor, media: MutableList<Media>) {
        binding.apply {
            init(
                adaptor,
                animeMostFavRecyclerView,
                animeMostFavProgressBar,
                animeMostFav,
                animeMostFavMore,
                getString(R.string.most_favourite),
                media
            )
        }
    }

    fun init(adaptor: MediaAdaptor, recyclerView: RecyclerView, progress: View, title: View , more: View , string: String,  media : MutableList<Media>) {
        progress.visibility = View.GONE
        recyclerView.adapter = adaptor
        recyclerView.layoutManager =
            LinearLayoutManager(
                recyclerView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        more.setOnClickListener {
            ContextCompat.startActivity(
                it.context, Intent(it.context, MediaListViewActivity::class.java)
                    .putExtra("title", string)
                    .putExtra("media",  media as ArrayList<Media>),
                null
            )
        }
        recyclerView.visibility = View.VISIBLE
        title.visibility = View.VISIBLE
        more.visibility = View.VISIBLE
        title.startAnimation(setSlideUp())
        more.startAnimation(setSlideUp())
        recyclerView.layoutAnimation =
            LayoutAnimationController(setSlideIn(), 0.25f)
    }

    fun updateAvatar() {
        if (Anilist.avatar != null && ready.value == true) {
            trendingBinding.userAvatar.loadImage(Anilist.avatar, 52.toPx)
            trendingBinding.userAvatar.imageTintList = null
        }
    }

    fun updateNotificationCount(): Int? {
        if (this::binding.isInitialized) {
            val count = Anilist.unreadNotificationCount + MatagiUpdater.hasUpdate
            trendingBinding.notificationCount.isVisible =
                !PrefManager.getVal<Boolean>(PrefName.FloatingAvatar) && count > 0
            trendingBinding.notificationCount.text = count.toString()
            return count
        }
        return null
    }

    inner class AnimePageViewHolder(val binding: ItemAnimePageBinding) :
        RecyclerView.ViewHolder(binding.root)
}
