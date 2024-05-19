package ani.dantotsu.media

import android.os.Bundle
import android.view.View
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
import ani.dantotsu.others.getSerialized
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.showSystemBarsRetractView
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toPx
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

        val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface)

        window.statusBarColor = surfaceColor
        window.navigationBarColor = surfaceColor
        binding.listAppBar.setBackgroundColor(surfaceColor)
        val screenWidth = resources.displayMetrics.widthPixels
        val mediaList = intent.getSerialized("media") as? ArrayList<Media> ?: ArrayList()
        val view = PrefManager.getCustomVal("mediaView", 0)
        var mediaView: View = when (view) {
            1 -> binding.mediaList
            0 -> binding.mediaGrid
            else -> binding.mediaGrid
        }
        mediaView.alpha = 1f
        fun changeView(mode: Int, current: View) {
            mediaView.alpha = 0.33f
            mediaView = current
            current.alpha = 1f
            PrefManager.setCustomVal("mediaView", mode)
            binding.mediaRecyclerView.adapter = MediaAdaptor(mode, mediaList, this)
            binding.mediaRecyclerView.layoutManager = GridLayoutManager(
                this,
                if (mode == 1) 1 else (screenWidth / 120.toPx)
            )
        }
        binding.mediaList.setOnClickListener {
            changeView(1, binding.mediaList)
        }
        binding.mediaGrid.setOnClickListener {
            changeView(0, binding.mediaGrid)
        }
        val text = "${intent.getStringExtra("title")} (${mediaList.count()})"
        binding.listTitle.text = text
        binding.mediaRecyclerView.adapter = MediaAdaptor(view, mediaList, this)
        binding.mediaRecyclerView.layoutManager = GridLayoutManager(
            this,
            if (view == 1) 1 else (screenWidth / 120f).toInt()
        )
    }
}
