package ani.dantotsu.settings.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivitySettingsUserInterfaceBinding
import ani.dantotsu.settings.Settings
import ani.dantotsu.settings.SettingsActivity
import ani.dantotsu.settings.SettingsAdapter
import ani.dantotsu.settings.ViewType
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserInterfaceFragment : Fragment() {
    private lateinit var binding: ActivitySettingsUserInterfaceBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsUserInterfaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = requireActivity() as SettingsActivity

        binding.apply {
            uiSettingsBack.setOnClickListener {
                settings.backToMenu()
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

            var hasFoldingFeature = false
            CoroutineScope(Dispatchers.IO).launch {
                WindowInfoTracker.getOrCreate(settings)
                    .windowLayoutInfo(settings)
                    .collect { newLayoutInfo ->
                        hasFoldingFeature = newLayoutInfo.displayFeatures.find {
                            it is FoldingFeature
                        } != null
                    }
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = ViewType.HEADER,
                        name = getString(R.string.app)
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.home_layout_show),
                        icon = R.drawable.ic_round_playlist_add_24,
                        onClick = {
                            val set = PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout).toMutableList()
                            val views = resources.getStringArray(R.array.home_layouts)
                            val dialog = AlertDialog.Builder(settings, R.style.MyPopup)
                                .setTitle(getString(R.string.home_layout_show)).apply {
                                    setMultiChoiceItems(
                                        views,
                                        PrefManager.getVal<List<Boolean>>(PrefName.HomeLayout).toBooleanArray()
                                    ) { _, i, value ->
                                        set[i] = value
                                    }
                                    setPositiveButton(getString(R.string.done)) { _, _ ->
                                        PrefManager.setVal(PrefName.HomeLayout, set)
                                    }
                                }.show()
                            dialog.window?.setDimAmount(0.8f)
                        },
                        isDialog = true
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.hide_home_main),
                        desc = getString(R.string.hide_home_main_desc),
                        icon = R.drawable.ic_clean_hands_24,
                        pref = PrefName.HomeMainHide,
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.random_recommended),
                        desc = getString(R.string.random_recommended_desc),
                        icon = R.drawable.ic_auto_fix_high_24,
                        pref = PrefName.HideRandoRec
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.immersive_mode),
                        icon = R.drawable.ic_round_fullscreen_24,
                        pref = PrefName.ImmersiveMode
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.use_foldable),
                        desc = getString(R.string.use_foldable_desc),
                        icon = R.drawable.ic_devices_fold_24,
                        pref = PrefName.UseFoldable,
                        isVisible = hasFoldingFeature
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.floating_avatar),
                        icon = R.drawable.ic_round_attractions_24,
                        pref = PrefName.FloatingAvatar,
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.small_view),
                        icon = R.drawable.ic_round_art_track_24,
                        pref = PrefName.SmallView
                    ),
                    Settings(
                        type = ViewType.HEADER,
                        name = getString(R.string.animations)
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.trailer_banners),
                        icon = R.drawable.ic_video_camera_back_24,
                        pref = PrefName.YouTubeBanners
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.banner_animations),
                        icon = R.drawable.ic_round_photo_size_select_actual_24,
                        pref = PrefName.BannerAnimations
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.layout_animations),
                        icon = R.drawable.ic_round_animation_24,
                        pref = PrefName.LayoutAnimations
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.trending_scroller),
                        icon = R.drawable.trail_length_short,
                        pref = PrefName.TrendingScroller
                    ),
                    Settings(
                        type = ViewType.SLIDER,
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
                        type = ViewType.HEADER,
                        name = getString(R.string.blur)
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.blur_banners),
                        icon = R.drawable.blur_on,
                        pref = PrefName.BlurBanners
                    ),
                    Settings(
                        type = ViewType.SLIDER,
                        name = getString(R.string.radius),
                        icon = R.drawable.adjust,
                        pref = PrefName.BlurRadius,
                        valueFrom = 1f
                    ),
                    Settings(
                        type = ViewType.SLIDER,
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
}
