package ani.dantotsu.profile.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityFeedBinding
import ani.dantotsu.initActivity
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.updateMargins
import ani.dantotsu.util.MarkdownCreatorActivity
import bit.himitsu.os.Version
import bit.himitsu.setBaseline
import nl.joery.animatedbottombar.AnimatedBottomBar

class FeedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeedBinding
    private var selected: Int = 0
    lateinit var navBar: AnimatedBottomBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navBar = binding.feedNavBar.apply {
            updateMargins(resources.configuration.orientation)
        }
        binding.feedViewPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin += statusBarHeight
        }
        binding.feedViewPager.setBaseline(navBar, resources.configuration)
        val personalTab = navBar.createTab(R.drawable.ic_round_person_32, getString(R.string.follow))
        val globalTab = navBar.createTab(R.drawable.ic_globe_24, getString(R.string.global))
        navBar.addTab(personalTab)
        navBar.addTab(globalTab)
        binding.listTitle.text = getString(R.string.activities)
        binding.listToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        val activityId = intent.getIntExtra("activityId", -1)
        binding.feedViewPager.adapter =
            ViewPagerAdapter(supportFragmentManager, lifecycle, activityId)
        binding.feedViewPager.isUserInputEnabled = false
        navBar.setupWithViewPager2(binding.feedViewPager)
        navBar.onTabSelected = { selected = navBar.selectedIndex }
        binding.feedViewPager.setCurrentItem(selected, false)
        navBar.selectTabAt(selected, false)

        binding.listBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val popup = if (Version.isLollipopMR)
            PopupMenu(this, binding.activityFAB, Gravity.END, 0, R.style.MyPopup)
        else
            PopupMenu(this, binding.activityFAB)
        popup.menuInflater.inflate(R.menu.menu_feed_post, popup.menu)

        binding.activityFAB.setOnClickListener {
            popup.show()
            popup.setOnMenuItemClickListener { item ->
                ContextCompat.startActivity(
                    this,
                    Intent(this, MarkdownCreatorActivity::class.java).apply {
                        when (item.itemId) {
                            R.id.activity -> putExtra("type", "activity")
                            R.id.review -> putExtra("type", "review")
                        }
                    }, null
                )
                true
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        navBar.apply {
            updateMargins(newConfig.orientation)
        }
        binding.feedViewPager.setBaseline(navBar, newConfig)
    }

    override fun onRestart() {
        super.onRestart()
        if (this::navBar.isInitialized) navBar.selectTabAt(selected)
    }

    private class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        private val activityId: Int
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FeedFragment.newInstance(null, false, activityId)
                else -> FeedFragment.newInstance(null, true, -1)
            }
        }
    }
}
