package ani.dantotsu.settings

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivitySettingsCommonBinding
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.restartApp
import ani.dantotsu.savePrefsToDownloads
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.internal.Location
import ani.dantotsu.settings.saving.internal.PreferenceKeystore
import ani.dantotsu.settings.saving.internal.PreferencePackager
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.dantotsu.util.LauncherWrapper
import ani.dantotsu.util.StoragePermissions
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsCommonActivity: AppCompatActivity(){
    private lateinit var binding: ActivitySettingsCommonBinding
    private lateinit var launcher: LauncherWrapper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)

        binding = ActivitySettingsCommonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val contract = ActivityResultContracts.OpenDocumentTree()
        launcher = LauncherWrapper(this, contract)
        val managers = arrayOf("Default", "1DM", "ADM")
        val downloadManagerDialog =
            AlertDialog.Builder(this, R.style.MyPopup).setTitle(R.string.download_manager)
        var downloadManager: Int = PrefManager.getVal(PrefName.DownloadManager)

        binding.apply {
            settingsCommonLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            commonSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            settingsDownloadManager.setOnClickListener {
                val dialog = downloadManagerDialog.setSingleChoiceItems(
                    managers,
                    downloadManager
                ) { dialog, count ->
                    downloadManager = count
                    PrefManager.setVal(PrefName.DownloadManager, downloadManager)
                    dialog.dismiss()
                }.show()
                dialog.window?.setDimAmount(0.8f)
            }

            val exDns = listOf(
                "None",
                "Cloudflare",
                "Google",
                "AdGuard",
                "Quad9",
                "AliDNS",
                "DNSPod",
                "360",
                "Quad101",
                "Mullvad",
                "Controld",
                "Njalla",
                "Shecan",
                "Libre"
            )

            settingsExtensionDns.setText(exDns[PrefManager.getVal(PrefName.DohProvider)])
            settingsExtensionDns.setAdapter(
                ArrayAdapter(
                    this@SettingsCommonActivity,
                    R.layout.item_dropdown,
                    exDns
                )
            )
            settingsExtensionDns.setOnItemClickListener { _, _, i, _ ->
                PrefManager.setVal(PrefName.DohProvider, i)
                settingsExtensionDns.clearFocus()
                restartApp()
            }

            settingsContinueMedia.isChecked = PrefManager.getVal(PrefName.ContinueMedia)
            settingsContinueMedia.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.ContinueMedia, isChecked)
            }

            settingsSearchSources.isChecked = PrefManager.getVal(PrefName.SearchSources)
            settingsSearchSources.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.SearchSources, isChecked)
            }

            settingsRecentlyListOnly.isChecked = PrefManager.getVal(PrefName.RecentlyListOnly)
            settingsRecentlyListOnly.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.RecentlyListOnly, isChecked)
            }

            settingsSocialInMedia.isChecked = PrefManager.getVal(PrefName.SocialInMedia)
            settingsSocialInMedia.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.SocialInMedia, isChecked)
            }

            settingsAdultAnimeOnly.isChecked = PrefManager.getVal(PrefName.AdultOnly)
            settingsAdultAnimeOnly.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.AdultOnly, isChecked)
                restartApp()
            }

            settingsDownloadLocation.setOnClickListener {
                val dialog = AlertDialog.Builder(this@SettingsCommonActivity, R.style.MyPopup)
                    .setTitle(R.string.change_download_location)
                    .setMessage(R.string.download_location_msg)
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        val oldUri = PrefManager.getVal<String>(PrefName.DownloadsDir)
                        launcher.registerForCallback { success ->
                            if (success) {
                                toast(getString(R.string.please_wait))
                                val newUri = PrefManager.getVal<String>(PrefName.DownloadsDir)
                                GlobalScope.launch(Dispatchers.IO) {
                                    Injekt.get<DownloadsManager>().moveDownloadsDir(
                                        this@SettingsCommonActivity,
                                        Uri.parse(oldUri), Uri.parse(newUri)
                                    ) { finished, message ->
                                        if (finished) {
                                            toast(getString(R.string.success))
                                        } else {
                                            toast(message)
                                        }
                                    }
                                }
                            } else {
                                toast(getString(R.string.error))
                            }
                        }
                        launcher.launch()
                        dialog.dismiss()
                    }
                    .setNeutralButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            var previousStart: View = when (PrefManager.getVal<Int>(PrefName.DefaultStartUpTab)) {
                0 -> uiSettingsAnime
                1 -> uiSettingsHome
                2 -> uiSettingsManga
                else -> uiSettingsHome
            }
            previousStart.alpha = 1f
            fun uiDefault(mode: Int, current: View) {
                previousStart.alpha = 0.33f
                previousStart = current
                current.alpha = 1f
                PrefManager.setVal(PrefName.DefaultStartUpTab, mode)
                initActivity(this@SettingsCommonActivity)
            }

            uiSettingsAnime.setOnClickListener {
                uiDefault(0, it)
            }

            uiSettingsHome.setOnClickListener {
                uiDefault(1, it)
            }

            uiSettingsManga.setOnClickListener {
                uiDefault(2, it)
            }
        }
    }
}