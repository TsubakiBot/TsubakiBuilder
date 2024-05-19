package ani.dantotsu.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivitySettingsUserInterfaceBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager

class UserInterfaceSettingsActivity : AppCompatActivity() {
    lateinit var binding: ActivitySettingsUserInterfaceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)

        binding = ActivitySettingsUserInterfaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            uiSettingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }

            onBackPressedDispatcher.addCallback(this@UserInterfaceSettingsActivity) {
                startActivity(Intent(this@UserInterfaceSettingsActivity, SettingsActivity::class.java))
                finish()
            }

            uiSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
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

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.HEADER,
                        name = getString(R.string.app)
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.immersive_mode),
                        icon = R.drawable.ic_round_fullscreen_24,
                        pref = PrefName.ImmersiveMode
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.small_view),
                        icon = R.drawable.ic_round_art_track_24,
                        pref = PrefName.SmallView
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.floating_avatar),
                        icon = R.drawable.ic_round_attractions_24,
                        pref = PrefName.FloatingAvatar,
                        onLongClick = {
                            PrefManager.setVal(PrefName.FabulousVertX, -1)
                            PrefManager.setVal(PrefName.FabulousVertY, -1)
                            PrefManager.setVal(PrefName.FabulousHorzX, -1)
                            PrefManager.setVal(PrefName.FabulousHorzY, -1)
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.hide_home_main),
                        desc = getString(R.string.hide_home_main_desc),
                        icon = R.drawable.ic_clean_hands_24,
                        pref = PrefName.HomeMainHide
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.home_layout_show),
                        icon = R.drawable.ic_round_playlist_add_24,
                        onClick = {
                            val set = PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout).toMutableList()
                            val views = resources.getStringArray(R.array.home_layouts)
                            val dialog = AlertDialog.Builder(this@UserInterfaceSettingsActivity, R.style.MyPopup)
                                .setTitle(getString(R.string.home_layout_show)).apply {
                                    setMultiChoiceItems(
                                        views,
                                        PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout).toBooleanArray()
                                    ) { _, i, value ->
                                        set[i] = value
                                    }
                                    setPositiveButton("Done") { _, _ ->
                                        PrefManager.setVal(PrefName.HomeLayout, set)
                                    }
                                }.show()
                            dialog.window?.setDimAmount(0.8f)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.trailer_banners),
                        icon = R.drawable.ic_round_photo_size_select_actual_24,
                        pref = PrefName.YouTubeBanners
                    ),
                    Settings(
                        type = SettingsView.HEADER,
                        name = getString(R.string.animations)
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.banner_animations),
                        icon = R.drawable.ic_round_photo_size_select_actual_24,
                        pref = PrefName.BannerAnimations
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.layout_animations),
                        icon = R.drawable.ic_round_animation_24,
                        pref = PrefName.LayoutAnimations
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.trending_scroller),
                        icon = R.drawable.trail_length_short,
                        pref = PrefName.TrendingScroller
                    ),
                    Settings(
                        type = SettingsView.SLIDER,
                        name = getString(R.string.animation_speed),
                        icon = R.drawable.adjust,
                        stepSize = 0.25f,
                        valueFrom = 0f,
                        valueTo = 2f,
                        value = mapReverse[PrefManager.getVal(PrefName.AnimationSpeed)] ?: 1f,
                        slider = { value, _ ->
                            PrefManager.setVal(PrefName.AnimationSpeed, map[value] ?: 1f)
                        }
                    ),
                    Settings(
                        type = SettingsView.HEADER,
                        name = getString(R.string.blur)
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.blur_banners),
                        icon = R.drawable.blur_on,
                        pref = PrefName.BlurBanners
                    ),
                    Settings(
                        type = SettingsView.SLIDER,
                        name = getString(R.string.radius),
                        icon = R.drawable.adjust,
                        pref = PrefName.BlurRadius,
                        valueFrom = 1f
                    ),
                    Settings(
                        type = SettingsView.SLIDER,
                        name = getString(R.string.sampling),
                        icon = R.drawable.stacks,
                        pref = PrefName.BlurSampling,
                        valueFrom = 1f
                    ),
                )
            )

            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
