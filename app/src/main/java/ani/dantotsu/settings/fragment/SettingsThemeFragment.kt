package ani.dantotsu.settings.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivitySettingsThemeBinding
import ani.dantotsu.settings.Settings
import ani.dantotsu.settings.SettingsActivity
import ani.dantotsu.settings.SettingsAdapter
import ani.dantotsu.settings.ViewType
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.Logger
import bit.himitsu.os.Version
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.color.SimpleColorDialog

class SettingsThemeFragment : Fragment(), SimpleDialog.OnDialogResultListener {
    private lateinit var binding: ActivitySettingsThemeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsThemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = requireActivity() as SettingsActivity

        binding.apply {
            themeSettingsBack.setOnClickListener {
                settings.backToMenu()
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.oled_theme_variant),
                        desc = getString(R.string.oled_theme_variant_desc),
                        icon = R.drawable.ic_invert_colors_24,
                        isChecked = PrefManager.getVal(PrefName.UseOLED),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.UseOLED, isChecked)
                            requireActivity().recreate()
                        }
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.use_material_you),
                        desc = getString(R.string.use_material_you_desc),
                        icon = R.drawable.ic_wallpaper_24,
                        isChecked = PrefManager.getVal(PrefName.UseMaterialYou),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.UseMaterialYou, isChecked)
                            if (isChecked) PrefManager.setVal(PrefName.UseCustomTheme, false)
                            requireActivity().recreate()
                        },
                        isVisible = Version.isSnowCone
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.use_unique_theme_for_each_item),
                        desc = getString(R.string.use_unique_theme_for_each_item_desc),
                        icon = R.drawable.ic_colorize_24,
                        isChecked = PrefManager.getVal(PrefName.UseSourceTheme),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.UseSourceTheme, isChecked)
                            requireActivity().recreate()
                        },
                        isVisible = Version.isSnowCone
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.use_custom_theme),
                        desc = getString(R.string.use_custom_theme_desc),
                        icon = R.drawable.ic_format_color_fill_24,
                        isChecked = PrefManager.getVal(PrefName.UseCustomTheme),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.UseCustomTheme, isChecked)
                            if (isChecked) PrefManager.setVal(PrefName.UseMaterialYou, false)
                            requireActivity().recreate()
                        },
                        isVisible = Version.isSnowCone
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.color_picker),
                        desc = getString(R.string.color_picker_desc),
                        icon = R.drawable.ic_palette,
                        onClick = {
                            val originalColor: Int = PrefManager.getVal(PrefName.CustomThemeInt)

                            class CustomColorDialog : SimpleColorDialog() {
                                override fun onPositiveButtonClick() {
                                    super.onPositiveButtonClick()
                                    requireActivity().recreate()
                                }
                            }

                            val tag = "colorPicker"
                            CustomColorDialog().title(R.string.custom_theme)
                                .colorPreset(originalColor)
                                .colors(settings, SimpleColorDialog.MATERIAL_COLOR_PALLET)
                                .allowCustom(true).showOutline(0x46000000).gridNumColumn(5)
                                .choiceMode(SimpleColorDialog.SINGLE_CHOICE).neg()
                                .show(settings, tag)
                        },
                        isVisible = Version.isSnowCone,
                        isDialog = true
                    )
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(settings, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }

            val prefTheme: String = PrefManager.getVal(PrefName.Theme)
            val themeText = prefTheme.substring(0, 1) + prefTheme.substring(1).lowercase()
            binding.themeSwitcher.setText(themeText)
            themeSwitcher.setOnItemClickListener { _, _, i, _ ->
                PrefManager.setVal(PrefName.Theme, ThemeManager.Companion.Theme.entries[i].theme)
                themeSwitcher.clearFocus()
                requireActivity().recreate()
            }
            themePicker.children.forEachIndexed { index, view ->
                view.setOnClickListener {
                    val theme = ThemeManager.Companion.Theme.entries[index].theme
                    PrefManager.setVal(PrefName.Theme, theme)
                    requireActivity().recreate()
                    val themeName = theme.substring(0, 1) + theme.substring(1).lowercase()
                    binding.themeSwitcher.setText(themeName)
                }
            }

            var previous: View = when (PrefManager.getVal<Int>(PrefName.DarkMode)) {
                1 -> settingsUiLight
                2 -> settingsUiDark
                else -> settingsUiAuto
            }
            previous.alpha = 1f
            fun uiTheme(mode: Int, current: View) {
                previous.alpha = 0.33f
                previous = current
                current.alpha = 1f
                PrefManager.setVal(PrefName.DarkMode, mode)
                requireActivity().recreate()
            }

            settingsUiAuto.setOnClickListener {
                uiTheme(0, it)
            }

            settingsUiLight.setOnClickListener {
                val oledSwitch = settingsRecyclerView.findViewHolderForAdapterPosition(1)
                        as SettingsAdapter.SettingsSwitchViewHolder
                oledSwitch.binding.settingsButton.isChecked = false
                PrefManager.setVal(PrefName.UseOLED, false)
                uiTheme(1, it)
            }

            settingsUiDark.setOnClickListener {
                uiTheme(2, it)
            }
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
            if (dialogTag == "colorPicker") {
                val color = extras.getInt(SimpleColorDialog.COLOR)
                PrefManager.setVal(PrefName.CustomThemeInt, color)
                Logger.log("Custom Theme: $color")
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        binding.themeSwitcher.setAdapter(ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            ThemeManager.Companion.Theme.entries.map {
                it.theme.substring(0, 1) + it.theme.substring(1).lowercase()
            }
        ))
    }
}