package ani.dantotsu.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivitySettingsSystemBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.restartApp
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.Logger
import ani.matagi.update.MatagiUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsSystemActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsSystemBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)

        binding = ActivitySettingsSystemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            settingsSystemLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            settingsSystemTitle.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

            settingsUseFoldable.isChecked = PrefManager.getVal(PrefName.UseFoldable)
            settingsUseFoldable.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.UseFoldable, isChecked)
            }

            CoroutineScope(Dispatchers.IO).launch {
                WindowInfoTracker.getOrCreate(this@SettingsSystemActivity)
                    .windowLayoutInfo(this@SettingsSystemActivity)
                    .collect { newLayoutInfo ->
                        withContext(Dispatchers.Main) {
                            settingsUseFoldable.isVisible =
                                newLayoutInfo.displayFeatures.find { it is FoldingFeature } != null
                        }
                    }
            }

            settingsUseShortcuts.isChecked = PrefManager.getVal(PrefName.UseShortcuts)
            settingsUseShortcuts.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.UseShortcuts, isChecked)
                restartApp()
            }

            if (!BuildConfig.FLAVOR.contains("fdroid")) {

                settingsCheckUpdate.isChecked = PrefManager.getVal(PrefName.CheckUpdate)
                settingsCheckUpdate.setOnCheckedChangeListener { _, isChecked ->
                    PrefManager.setVal(PrefName.CheckUpdate, isChecked)
                    if (!isChecked) {
                        snackString(getString(R.string.long_click_to_check_update))
                    }
                }

                settingsCheckUpdate.setOnLongClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        MatagiUpdater.check(this@SettingsSystemActivity, true)
                    }
                    true
                }

                settingsShareUsername.isChecked = PrefManager.getVal(PrefName.SharedUserID)
                settingsShareUsername.setOnCheckedChangeListener { _, isChecked ->
                    PrefManager.setVal(PrefName.SharedUserID, isChecked)
                }

            } else {
                settingsCheckUpdate.visibility = View.GONE
                settingsShareUsername.visibility = View.GONE
                settingsCheckUpdate.isEnabled = false
                settingsShareUsername.isEnabled = false
                settingsCheckUpdate.isChecked = false
                settingsShareUsername.isChecked = false
            }

            settingsLogToFile.isChecked = PrefManager.getVal(PrefName.LogToFile)
            settingsLogToFile.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.LogToFile, isChecked)
                restartApp()
            }

            settingsShareLog.setOnClickListener {
                Logger.shareLog(this@SettingsSystemActivity)
            }
        }
    }
}