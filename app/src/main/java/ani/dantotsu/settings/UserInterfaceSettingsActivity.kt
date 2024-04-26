package ani.dantotsu.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityUserInterfaceSettingsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager

class UserInterfaceSettingsActivity : AppCompatActivity() {
    lateinit var binding: ActivityUserInterfaceSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)

        binding = ActivityUserInterfaceSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            uiSettingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }

            onBackPressedDispatcher.addCallback(this@UserInterfaceSettingsActivity) {
                startActivity(
                    Intent(
                        this@UserInterfaceSettingsActivity, SettingsThemeActivity::class.java
                    )
                )
                finish()
            }

            uiSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            uiSettingsHomeLayout.setOnClickListener {
                val set = PrefManager.getVal<List<Boolean>>(PrefName.HomeLayoutShow).toMutableList()
                val views = resources.getStringArray(R.array.home_layouts)
                val dialog =
                    AlertDialog.Builder(this@UserInterfaceSettingsActivity, R.style.MyPopup)
                        .setTitle(getString(R.string.home_layout_show)).apply {
                            setMultiChoiceItems(
                                views,
                                PrefManager.getVal<List<Boolean>>(PrefName.HomeLayoutShow)
                                    .toBooleanArray()
                            ) { _, i, value ->
                                set[i] = value
                            }
                            setPositiveButton("Done") { _, _ ->
                                PrefManager.setVal(PrefName.HomeLayoutShow, set)
                            }
                        }.show()
                dialog.window?.setDimAmount(0.8f)
            }

            uiSettingsStories.isChecked = PrefManager.getVal(PrefName.Stories)
            uiSettingsStories.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.Stories, isChecked)
            }

            uiSettingsSmallView.isChecked = PrefManager.getVal(PrefName.SmallView)
            uiSettingsSmallView.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.SmallView, isChecked)
            }

            uiSettingsImmersive.isChecked = PrefManager.getVal(PrefName.ImmersiveMode)
            uiSettingsImmersive.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.ImmersiveMode, isChecked)
                recreate()
            }
            uiSettingsBannerAnimation.isChecked =
                PrefManager.getVal(PrefName.BannerAnimations)
            uiSettingsBannerAnimation.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.BannerAnimations, isChecked)
            }

            uiSettingsLayoutAnimation.isChecked =
                PrefManager.getVal(PrefName.LayoutAnimations)
            uiSettingsLayoutAnimation.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.LayoutAnimations, isChecked)
                recreate()
            }

            uiSettingsTrendingScroller.isChecked =
                PrefManager.getVal(PrefName.TrendingScroller)
            uiSettingsTrendingScroller.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.TrendingScroller, isChecked)
            }

            val map = mapOf(
                2f to 0.5f,
                1.75f to 0.625f,
                1.5f to 0.75f,
                1.25f to 0.875f,
                1f to 1f,
                0.75f to 1.25f,
                0.5f to 1.5f,
                0.25f to 1.75f,
                0f to 0f
            )
            val mapReverse = map.map { it.value to it.key }.toMap()
            uiSettingsAnimationSpeed.value =
                mapReverse[PrefManager.getVal(PrefName.AnimationSpeed)] ?: 1f
            uiSettingsAnimationSpeed.addOnChangeListener { _, value, _ ->
                PrefManager.setVal(PrefName.AnimationSpeed, map[value] ?: 1f)
            }

            uiSettingsBlurBanners.isChecked = PrefManager.getVal(PrefName.BlurBanners)
            uiSettingsBlurBanners.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.BlurBanners, isChecked)
            }

            uiSettingsBlurRadius.value = (PrefManager.getVal(PrefName.BlurRadius) as Float)
            uiSettingsBlurRadius.addOnChangeListener { _, value, _ ->
                PrefManager.setVal(PrefName.BlurRadius, value)
            }

            uiSettingsBlurSampling.value =
                (PrefManager.getVal(PrefName.BlurSampling) as Float)
            uiSettingsBlurSampling.addOnChangeListener { _, value, _ ->
                PrefManager.setVal(PrefName.BlurSampling, value)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
