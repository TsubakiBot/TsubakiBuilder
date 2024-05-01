package ani.dantotsu.media

import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityMediaListViewBinding
import ani.dantotsu.hideSystemBarsExtendView
import ani.dantotsu.initActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.showSystemBarsRetractView
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import eu.kanade.tachiyomi.util.system.getThemeColor

class MediaListViewActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMediaListViewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMediaListViewBinding.inflate(layoutInflater)
        ThemeManager(this).applyTheme()
        initActivity(this)

        if (PrefManager.getVal(PrefName.ImmersiveMode)) {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            hideSystemBarsExtendView()
            binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
            }
        } else {
            showSystemBarsRetractView()
            this.window.statusBarColor =
                ContextCompat.getColor(this, R.color.nav_bg_inv)

        }
        setContentView(binding.root)

        binding.listBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val primaryColor = getThemeColor(com.google.android.material.R.attr.colorSurface)

        window.statusBarColor = primaryColor
        window.navigationBarColor = primaryColor
        binding.listAppBar.setBackgroundColor(primaryColor)
        val screenWidth = resources.displayMetrics.run { widthPixels / density }
        binding.listTitle.text = intent.getStringExtra("title")
        binding.mediaRecyclerView.adapter = MediaAdaptor(0, mediaList, this)
        binding.mediaRecyclerView.layoutManager = GridLayoutManager(
            this,
            (screenWidth / 120f).toInt()
        )
    }
    companion object{
        var mediaList: ArrayList<Media> = arrayListOf()
    }
}
