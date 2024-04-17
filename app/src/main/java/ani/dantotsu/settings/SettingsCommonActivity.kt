package ani.dantotsu.settings

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.ActivitySettingsCommonBinding
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.restartApp
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.dantotsu.util.LauncherWrapper
import ani.dantotsu.util.StoragePermissions
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
        val context = this

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

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.always_continue_content),
                        desc = getString(R.string.always_continue_content),
                        icon = R.drawable.ic_round_delete_24,
                        isChecked = PrefManager.getVal(PrefName.ContinueMedia),
                        switch = {isChecked, _ ->
                            PrefManager.setVal(PrefName.ContinueMedia, isChecked)
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.search_source_list),
                        desc = getString(R.string.search_source_list),
                        icon = R.drawable.ic_round_search_sources_24,
                        isChecked = PrefManager.getVal(PrefName.SearchSources),
                        switch = {isChecked, _ ->
                            PrefManager.setVal(PrefName.SearchSources, isChecked)
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.recentlyListOnly),
                        desc = getString(R.string.recentlyListOnly),
                        icon = R.drawable.ic_round_new_releases_24,
                        isChecked = PrefManager.getVal(PrefName.RecentlyListOnly),
                        switch = {isChecked, _ ->
                            PrefManager.setVal(PrefName.RecentlyListOnly, isChecked)
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.social_in_media),
                        desc = getString(R.string.social_in_media),
                        icon = R.drawable.ic_emoji_people_24,
                        isChecked = PrefManager.getVal(PrefName.SocialInMedia),
                        switch = {isChecked, _ ->
                            PrefManager.setVal(PrefName.SocialInMedia, isChecked)
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.adult_only_content),
                        desc = getString(R.string.adult_only_content),
                        icon = R.drawable.ic_round_nsfw_24,
                        isChecked = PrefManager.getVal(PrefName.AdultOnly),
                        switch = {isChecked, _ ->
                            PrefManager.setVal(PrefName.AdultOnly, isChecked)
                            restartApp()
                        },
                        isVisible = Anilist.adult

                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.download_manager_select),
                        desc = getString(R.string.download_manager_select),
                        icon = R.drawable.ic_download_24,
                        onClick = {
                            val dialog = downloadManagerDialog.setSingleChoiceItems(
                                managers, downloadManager
                            ) { dialog, count ->
                                downloadManager = count
                                PrefManager.setVal(PrefName.DownloadManager, downloadManager)
                                dialog.dismiss()
                            }.show()
                            dialog.window?.setDimAmount(0.8f)
                        }
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.change_download_location),
                        desc = getString(R.string.change_download_location),
                        icon = R.drawable.ic_round_source_24,
                        onClick = {
                            val dialog = AlertDialog.Builder(context, R.style.MyPopup)
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
                                                    context, Uri.parse(oldUri), Uri.parse(newUri)
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
                                }.setNeutralButton(R.string.cancel) { dialog, _ ->
                                    dialog.dismiss()
                                }.create()
                            dialog.window?.setDimAmount(0.8f)
                            dialog.show()
                        }
                    )
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
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
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}