package ani.dantotsu.home

import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityStatusBinding
import ani.dantotsu.initActivity
import ani.dantotsu.others.getSerialized
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.home.status.listener.StoriesCallback
import ani.dantotsu.navBarHeight
import ani.dantotsu.profile.User
import ani.dantotsu.statusBarHeight

class StatusActivity : AppCompatActivity(), StoriesCallback {
    private lateinit var activity: ArrayList<User>
    private lateinit var binding: ActivityStatusBinding
    private var position: Int = -1
    private lateinit var slideInLeft: Animation
    private lateinit var slideOutRight: Animation
    private lateinit var slideOutLeft: Animation
    private lateinit var slideInRight: Animation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        activity = intent.getSerialized("user")!!
        position = intent.getIntExtra("position", -1)
        binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }
        slideInLeft = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        slideOutRight = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)
        slideOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)

        binding.stories.setStoriesList(activity[position].activity, this)
    }

    override fun onPause() {
        super.onPause()
        binding.stories.pause()
    }
    override fun onResume() {
        super.onResume()
        binding.stories.resume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            binding.stories.resume()
        } else {
            binding.stories.pause()
        }
    }
    override fun onStoriesEnd() {
        position += 1
        if (position < activity.size - 1) {
            binding.stories.startAnimation(slideOutLeft)
            binding.stories.setStoriesList(activity[position].activity, this)
            binding.stories.startAnimation(slideInRight)
        } else {
            finish()
        }
    }

    override fun onStoriesStart() {
        position -= 1
        if (position >= 0) {
            binding.stories.startAnimation(slideOutRight)
            binding.stories.setStoriesList(activity[position].activity, this)
            binding.stories.startAnimation(slideInLeft)
        } else {
            finish()
        }
    }

}