package ani.dantotsu.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.databinding.ActivitySettingsThemeBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.Logger
import ani.matagi.os.Version
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.color.SimpleColorDialog

class SettingsThemeActivity : AppCompatActivity(), SimpleDialog.OnDialogResultListener {
    private lateinit var binding: ActivitySettingsThemeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this

        binding = ActivitySettingsThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            settingsThemeLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            onBackPressedDispatcher.addCallback(this@SettingsThemeActivity) {
                startActivity(Intent(this@SettingsThemeActivity, SettingsActivity::class.java))
                finish()
            }
            themeSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.ui_settings),
                        desc = getString(R.string.ui_settings),
                        icon = R.drawable.ic_round_auto_awesome_24,
                        onClick = {
                            startActivity(Intent(context, UserInterfaceSettingsActivity::class.java))
                            finish()
                        },
                        isActivity = true
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.oled_theme_variant),
                        desc = getString(R.string.oled_theme_variant),
                        icon = R.drawable.ic_round_brightness_4_24,
                        isChecked = PrefManager.getVal(PrefName.UseOLED),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.UseOLED, isChecked)
                            recreate()
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.use_material_you),
                        desc = getString(R.string.use_material_you),
                        icon = R.drawable.ic_round_new_releases_24,
                        isChecked = PrefManager.getVal(PrefName.UseMaterialYou),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.UseMaterialYou, isChecked)
                            if (isChecked) PrefManager.setVal(PrefName.UseCustomTheme, false)
                            recreate()
                        },
                        isVisible = Version.isSnowCone
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.use_unique_theme_for_each_item),
                        desc = getString(R.string.use_unique_theme_for_each_item),
                        icon = R.drawable.ic_palette,
                        isChecked = PrefManager.getVal(PrefName.UseSourceTheme),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.UseSourceTheme, isChecked)
                            recreate()
                        },
                        isVisible = Version.isSnowCone
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.use_custom_theme),
                        desc = getString(R.string.use_custom_theme),
                        icon = R.drawable.ic_palette,
                        isChecked = PrefManager.getVal(PrefName.UseCustomTheme),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.UseCustomTheme, isChecked)
                            if (isChecked) PrefManager.setVal(PrefName.UseMaterialYou, false)
                            recreate()
                        },
                        isVisible = Version.isSnowCone
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.color_picker),
                        desc = getString(R.string.color_picker),
                        icon = R.drawable.ic_palette,
                        onClick = {
                            val originalColor: Int = PrefManager.getVal(PrefName.CustomThemeInt)
                            class CustomColorDialog : SimpleColorDialog() {
                                override fun onPositiveButtonClick() {
                                    recreate()
                                    super.onPositiveButtonClick()
                                }
                            }
                            val tag = "colorPicker"
                            CustomColorDialog().title(R.string.custom_theme).colorPreset(originalColor)
                                .colors(context, SimpleColorDialog.MATERIAL_COLOR_PALLET)
                                .allowCustom(true).showOutline(0x46000000).gridNumColumn(5)
                                .choiceMode(SimpleColorDialog.SINGLE_CHOICE).neg()
                                .show(context, tag)
                        },
                        isVisible = Version.isSnowCone
                    )
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }

            val themeString: String = PrefManager.getVal(PrefName.Theme)
            val themeText = themeString.substring(0, 1) + themeString.substring(1).lowercase()
            themeSwitcher.setText(themeText)

            themeSwitcher.setOnItemClickListener { _, _, i, _ ->
                PrefManager.setVal(PrefName.Theme, ThemeManager.Companion.Theme.entries[i].theme)
                //ActivityHelper.shouldRefreshMainActivity = true
                themeSwitcher.clearFocus()
                recreate()
            }

            var previous: View = when (PrefManager.getVal<Int>(PrefName.DarkMode)) {
                0 -> settingsUiAuto
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
                Refresh.all()
                recreate()
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

    override fun onResume() {
        super.onResume()

        binding.themeSwitcher.setAdapter(
            ArrayAdapter(
                this@SettingsThemeActivity,
                R.layout.item_dropdown,
                ThemeManager.Companion.Theme.entries
                    .map { it.theme.substring(0, 1) + it.theme.substring(1).lowercase() })
        )
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

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}