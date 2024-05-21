package ani.dantotsu.profile

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.ActivityProfileBinding
import ani.dantotsu.databinding.ItemProfileAppBarBinding
import ani.dantotsu.initActivity
import ani.dantotsu.loadImage
import ani.dantotsu.media.user.ListActivity
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.profile.activity.FeedFragment
import ani.dantotsu.setBaseline
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.dantotsu.updateLayoutParams
import ani.dantotsu.updateMargins
import ani.dantotsu.util.MarkdownCreatorActivity
import ani.dantotsu.view.dialog.ImageViewDialog
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joery.animatedbottombar.AnimatedBottomBar
import kotlin.math.abs


class ProfileActivity : AppCompatActivity(), AppBarLayout.OnOffsetChangedListener {
    lateinit var binding: ActivityProfileBinding
    private lateinit var bindingProfileAppBar: ItemProfileAppBarBinding
    private var selected: Int = 0
    lateinit var navBar: AnimatedBottomBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val context = this
        screenWidth = resources.displayMetrics.widthPixels.toFloat()
        navBar = binding.profileNavBar.apply {
            updateLayoutParams(resources.configuration.orientation)
        }
        navBar.visibility = View.GONE
        binding.profileViewPager.isUserInputEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val userid = intent.getIntExtra("userId", -1)
            val username = intent.getStringExtra("username") ?: ""
            val respond =
                if (userid != -1) Anilist.query.getUserProfile(userid) else
                    Anilist.query.getUserProfile(username)
            val user = respond?.data?.user
            if (user == null) {
                toast("User not found")
                finish()
                return@launch
            }

            withContext(Dispatchers.Main) {
                binding.profileViewPager.adapter =
                    ViewPagerAdapter(supportFragmentManager, lifecycle, user)
                binding.profileViewPager.setOffscreenPageLimit(3)
                navBar.visibility = View.VISIBLE
                navBar.setupWithViewPager2(binding.profileViewPager)
                navBar.onTabSelected = { selected = navBar.selectedIndex }
                binding.profileViewPager.setCurrentItem(selected, false)
                navBar.selectTabAt(selected, false)

                binding.profileViewPager.setBaseline(
                    navBar,
                    resources.configuration
                )

                bindingProfileAppBar = ItemProfileAppBarBinding.bind(binding.root).apply {
                    binding.profileProgressBar.visibility = View.GONE
                    followButton.isGone =
                        user.id == Anilist.userid || Anilist.userid == null

                    fun followText(): String {
                        return getString(
                            when {
                                user.isFollowing && user.isFollower -> R.string.mutual
                                user.isFollowing -> R.string.unfollow
                                user.isFollower -> R.string.follows_you
                                else -> R.string.follow
                            }
                        )
                    }

                    followButton.text = followText()

                    followButton.setOnClickListener {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val res = Anilist.query.toggleFollow(user.id)
                            if (res?.data?.toggleFollow != null) {
                                withContext(Dispatchers.Main) {
                                    snackString(R.string.success)
                                    user.isFollowing = res.data.toggleFollow.isFollowing
                                    followButton.text = followText()
                                }
                            }
                        }
                    }
                    profileAppBar.visibility = View.VISIBLE
                    profileMenuButton.setOnClickListener {
                        val popup = PopupMenu(context, profileMenuButton)
                        popup.menuInflater.inflate(R.menu.menu_profile, popup.menu)
                        popup.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.action_view_on_anilist -> {
                                    openLinkInBrowser("https://anilist.co/user/${user.name}")
                                    true
                                }
                                R.id.action_create_new_activity -> {
                                    ContextCompat.startActivity(
                                        context,
                                        Intent(context, MarkdownCreatorActivity::class.java)
                                            .putExtra("type", "activity"),
                                        null
                                    )
                                    true
                                }
                                else -> false
                            }
                        }
                        popup.show()
                    }

                    profileUserAvatar.loadImage(user.avatar?.medium)
                    profileUserAvatar.setOnLongClickListener {
                        ImageViewDialog.newInstance(
                            context,
                            getString(R.string.avatar, user.name),
                            user.avatar?.medium
                        )
                    }
                    profileUserName.text = user.name
                    val bannerAnimations: ImageView = if (PrefManager.getVal(PrefName.BannerAnimations))
                        profileBannerImage
                    else profileBannerImageNoKen

                    bannerAnimations.blurImage(user.bannerImage ?: user.avatar?.medium)
                    profileBannerImage.updateLayoutParams { height += statusBarHeight }
                    profileBannerImageNoKen.updateLayoutParams { height += statusBarHeight }
                    profileBannerGradient.updateLayoutParams { height += statusBarHeight }
                    profileCloseButton.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
                    profileMenuButton.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
                    profileButtonContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }

                    profileBannerImage.setOnLongClickListener {
                        ImageViewDialog.newInstance(
                            context,
                            getString(R.string.banner, user.name),
                            user.bannerImage
                        )
                    }

                    mMaxScrollSize = profileAppBar.totalScrollRange
                    profileAppBar.addOnOffsetChangedListener(context)


                    profileFollowerCount.text = (respond.data.followerPage?.pageInfo?.total ?: 0).toString()
                    profileFollowerCountContainer.setOnClickListener {
                        ContextCompat.startActivity(
                            context,
                            Intent(context, FollowActivity::class.java)
                                .putExtra("title", getString(R.string.followers))
                                .putExtra("userId", user.id),
                            null
                        )
                    }
                    profileFollowingCount.text = (respond.data.followingPage?.pageInfo?.total ?: 0).toString()
                    profileFollowingCountContainer.setOnClickListener {
                        ContextCompat.startActivity(
                            context,
                            Intent(context, FollowActivity::class.java)
                                .putExtra("title", "Following")
                                .putExtra("userId", user.id),
                            null
                        )
                    }

                    profileAnimeCount.text = user.statistics.anime.count.toString()
                    profileAnimeCountContainer.setOnClickListener {
                        ContextCompat.startActivity(
                            context,
                            Intent(context, ListActivity::class.java)
                                .putExtra("anime", true)
                                .putExtra("userId", user.id)
                                .putExtra("username", user.name),
                            null
                        )
                    }

                    profileMangaCount.text = user.statistics.manga.count.toString()
                    profileMangaCountContainer.setOnClickListener {
                        ContextCompat.startActivity(
                            context,
                            Intent(context, ListActivity::class.java)
                                .putExtra("anime", false)
                                .putExtra("userId", user.id)
                                .putExtra("username", user.name),
                            null
                        )
                    }

                    profileCloseButton.setOnClickListener {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        }
    }

    //Collapsing UI Stuff
    private var isCollapsed = false
    private val percent = 65
    private var mMaxScrollSize = 0
    private var screenWidth: Float = 0f

    override fun onOffsetChanged(appBar: AppBarLayout, i: Int) {
        if (mMaxScrollSize == 0) mMaxScrollSize = appBar.totalScrollRange
        val percentage = abs(i) * 100 / mMaxScrollSize

        with(bindingProfileAppBar) {
            profileUserAvatarContainer.visibility =
                if (profileUserAvatarContainer.scaleX == 0f) View.GONE else View.VISIBLE
            val duration = (200 * (PrefManager.getVal(PrefName.AnimationSpeed) as Float)).toLong()
            if (percentage >= percent && !isCollapsed) {
                isCollapsed = true
                ObjectAnimator.ofFloat(profileUserDataContainer, "translationX", screenWidth)
                    .setDuration(duration).start()
                ObjectAnimator.ofFloat(profileUserAvatarContainer, "translationX", screenWidth)
                    .setDuration(duration).start()
                ObjectAnimator.ofFloat(profileButtonContainer, "translationX", screenWidth)
                    .setDuration(duration).start()
                profileBannerImage.pause()
            }
            if (percentage <= percent && isCollapsed) {
                isCollapsed = false
                ObjectAnimator.ofFloat(profileUserDataContainer, "translationX", 0f)
                    .setDuration(duration).start()
                ObjectAnimator.ofFloat(profileUserAvatarContainer, "translationX", 0f)
                    .setDuration(duration).start()
                ObjectAnimator.ofFloat(profileButtonContainer, "translationX", 0f)
                    .setDuration(duration).start()

                if (PrefManager.getVal(PrefName.BannerAnimations)) profileBannerImage.resume()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        navBar.apply {
            updateMargins(newConfig.orientation)
        }
        binding.profileViewPager.setBaseline(navBar, newConfig)
    }

    override fun onRestart() {
        super.onRestart()
        if (this::navBar.isInitialized) navBar.selectTabAt(selected)
    }

    private class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        private val user: Query.UserProfile
    ) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> ProfileFragment.newInstance(user)
            1 -> FeedFragment.newInstance(user.id, false, -1)
            2 -> StatsFragment.newInstance(user)
            else -> ProfileFragment.newInstance(user)
        }
    }
}
