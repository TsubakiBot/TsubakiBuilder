package ani.dantotsu.settings

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivitySettingsCommonBinding
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.initActivity
import ani.dantotsu.restartApp
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsCommonFragment : Fragment() {
    private lateinit var binding: ActivitySettingsCommonBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsCommonBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = requireActivity() as SettingsActivity

        val managers = resources.getStringArray(R.array.downloadManagers)
        val downloadManagerDialog =
            AlertDialog.Builder(settings, R.style.MyPopup).setTitle(R.string.download_manager)
        var downloadManager: Int = PrefManager.getVal(PrefName.DownloadManager)

        binding.apply {
            commonSettingsBack.setOnClickListener {
                settings.backToMenu()
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
                    settings,
                    R.layout.item_dropdown,
                    exDns
                )
            )
            settingsExtensionDns.setOnItemClickListener { _, _, i, _ ->
                PrefManager.setVal(PrefName.DohProvider, i)
                settingsExtensionDns.clearFocus()
                settings.restartApp()
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.always_continue_content),
                        desc = getString(R.string.always_continue_content_desc),
                        icon = R.drawable.ic_round_delete_24,
                        pref = PrefName.ContinueMedia
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.search_source_list),
                        desc = getString(R.string.search_source_list_desc),
                        icon = R.drawable.ic_round_search_sources_24,
                        pref = PrefName.SearchSources
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.recentlyListOnly),
                        desc = getString(R.string.recentlyListOnly_desc),
                        icon = R.drawable.ic_round_new_releases_24,
                        pref = PrefName.RecentlyListOnly
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.download_manager_select),
                        desc = getString(R.string.download_manager_select_desc),
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
                        desc = getString(R.string.change_download_location_desc),
                        icon = R.drawable.ic_round_source_24,
                        onClick = {
                            val dialog = AlertDialog.Builder(settings, R.style.MyPopup)
                                .setTitle(R.string.change_download_location)
                                .setMessage(R.string.download_location_msg)
                                .setPositiveButton(R.string.ok) { dialog, _ ->
                                    val oldUri = PrefManager.getVal<String>(PrefName.DownloadsDir)
                                    settings.getLauncher()?.registerForCallback { success ->
                                        if (success) {
                                            toast(getString(R.string.please_wait))
                                            val newUri =
                                                PrefManager.getVal<String>(PrefName.DownloadsDir)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                Injekt.get<DownloadsManager>().moveDownloadsDir(
                                                    settings, Uri.parse(oldUri), Uri.parse(newUri)
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
                                    settings.getLauncher()?.launch()
                                    dialog.dismiss()
                                }.setNeutralButton(R.string.cancel) { dialog, _ ->
                                    dialog.dismiss()
                                }.create()
                            dialog.window?.setDimAmount(0.8f)
                            dialog.show()
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.social_in_media),
                        icon = R.drawable.ic_emoji_people_24,
                        pref = PrefName.SocialInMedia
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.adult_only_content),
                        desc = getString(R.string.adult_only_content_desc),
                        icon = R.drawable.ic_round_nsfw_24,
                        isChecked = PrefManager.getVal(PrefName.AdultOnly),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.AdultOnly, isChecked)
                            settings.restartApp()
                        }
                    )
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(settings, LinearLayoutManager.VERTICAL, false)
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
                initActivity(settings)
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