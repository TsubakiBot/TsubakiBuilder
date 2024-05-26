package ani.dantotsu.settings.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.addons.AddonDownloader
import ani.dantotsu.addons.download.DownloadAddonManager
import ani.dantotsu.addons.torrent.TorrentAddonManager
import ani.dantotsu.addons.torrent.TorrentServerService
import ani.dantotsu.databinding.ActivitySettingsAddonsBinding
import ani.dantotsu.databinding.ItemSettingsBinding
import ani.dantotsu.settings.Settings
import ani.dantotsu.settings.SettingsActivity
import ani.dantotsu.settings.SettingsAdapter
import ani.dantotsu.settings.SettingsView
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import bit.himitsu.torrServerKill
import bit.himitsu.torrServerStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.core.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsAddonFragment : Fragment() {
    private lateinit var binding: ActivitySettingsAddonsBinding
    private val downloadAddonManager: DownloadAddonManager = Injekt.get()
    private val torrentAddonManager: TorrentAddonManager = Injekt.get()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsAddonsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = requireActivity() as SettingsActivity

        binding.apply {
            binding.addonSettingsBack.setOnClickListener {
                settings.backToMenu()
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.anime_downloader_addon),
                        desc = getString(R.string.not_installed),
                        icon = R.drawable.ic_download_24,
                        hasTransition = true,
                        attach = {
                            setStatus(
                                view = it,
                                context = settings,
                                status = downloadAddonManager.hadError(settings),
                                hasUpdate = downloadAddonManager.hasUpdate
                            )
                            var job = Job()
                            downloadAddonManager.addListenerAction { _ ->
                                job.cancel()
                                it.settingsIconRight.animate().cancel()
                                it.settingsIconRight.rotation = 0f
                                setStatus(
                                    view = it,
                                    context = settings,
                                    status = downloadAddonManager.hadError(settings),
                                    hasUpdate = false
                                )
                            }
                            it.settingsIconRight.setOnClickListener { _ ->
                                if (it.settingsDesc.text == getString(R.string.installed)) {
                                    downloadAddonManager.uninstall()
                                    return@setOnClickListener
                                } else {
                                    job = Job()
                                    val scope = CoroutineScope(Dispatchers.Main + job)
                                    it.settingsIconRight.setImageResource(R.drawable.ic_sync)
                                    scope.launch {
                                        while (isActive) {
                                            withContext(Dispatchers.Main) {
                                                it.settingsIconRight.animate()
                                                    .rotationBy(360f)
                                                    .setDuration(1000)
                                                    .setInterpolator(LinearInterpolator())
                                                    .start()
                                            }
                                            delay(1000)
                                        }
                                    }
                                    snackString(getString(R.string.downloading))
                                    lifecycleScope.launchIO {
                                        AddonDownloader.update(
                                            activity = settings,
                                            downloadAddonManager,
                                            repo = DownloadAddonManager.REPO,
                                            currentVersion = downloadAddonManager.getVersion() ?: ""
                                        )
                                    }
                                }
                            }
                        },
                    ), Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.torrent_addon),
                        desc = getString(R.string.not_installed),
                        icon = R.drawable.ic_round_magnet_24,
                        hasTransition = true,
                        attach = {
                            setStatus(
                                view = it,
                                context = settings,
                                status = torrentAddonManager.hadError(settings),
                                hasUpdate = torrentAddonManager.hasUpdate
                            )
                            var job = Job()
                            torrentAddonManager.addListenerAction { _ ->
                                job.cancel()
                                it.settingsIconRight.animate().cancel()
                                it.settingsIconRight.rotation = 0f
                                setStatus(
                                    view = it,
                                    context = settings,
                                    status = torrentAddonManager.hadError(settings),
                                    hasUpdate = false
                                )
                            }
                            it.settingsIconRight.setOnClickListener { _ ->
                                if (it.settingsDesc.text == getString(R.string.installed)) {
                                    TorrentServerService.stop()
                                    torrentAddonManager.uninstall()
                                    return@setOnClickListener
                                } else {
                                    job = Job()
                                    val scope = CoroutineScope(Dispatchers.Main + job)
                                    it.settingsIconRight.setImageResource(R.drawable.ic_sync)
                                    scope.launch {
                                        while (isActive) {
                                            withContext(Dispatchers.Main) {
                                                it.settingsIconRight.animate()
                                                    .rotationBy(360f)
                                                    .setDuration(1000)
                                                    .setInterpolator(LinearInterpolator())
                                                    .start()
                                            }
                                            delay(1000)
                                        }
                                    }
                                    snackString(getString(R.string.downloading))
                                    lifecycleScope.launchIO {
                                        AddonDownloader.update(
                                            activity = settings,
                                            torrentAddonManager,
                                            repo = TorrentAddonManager.REPO,
                                            currentVersion = torrentAddonManager.getVersion() ?: "",
                                        )
                                    }
                                }
                            }
                        },
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.enable_server),
                        desc = getString(R.string.enable_server_desc),
                        icon = R.drawable.ic_round_dns_24,
                        isChecked = PrefManager.getVal(PrefName.TorrServerEnabled),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.TorrServerEnabled, isChecked)
                            Injekt.get<TorrentAddonManager>().extension?.let {
                                if (isChecked) {
                                    torrServerStart()
                                } else {
                                    torrServerKill()
                                }
                            }
                        },
                        isVisible = torrentAddonManager.isAvailable(false)
                    )
                )
            )
            binding.settingsRecyclerView.layoutManager =
                LinearLayoutManager(settings, LinearLayoutManager.VERTICAL, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        torrentAddonManager.removeListenerAction()
        downloadAddonManager.removeListenerAction()
    }

    private fun setStatus(
        view: ItemSettingsBinding,
        context: Context,
        status: String?,
        hasUpdate: Boolean
    ) {
        try {
            when (status) {
                context.getString(R.string.loaded_successfully) -> {
                    view.settingsIconRight.setImageResource(R.drawable.ic_round_delete_24)
                    view.settingsIconRight.rotation = 0f
                    view.settingsDesc.text = context.getString(R.string.installed)
                }

                null -> {
                    view.settingsIconRight.setImageResource(R.drawable.ic_download_24)
                    view.settingsIconRight.rotation = 0f
                    view.settingsDesc.text = context.getString(R.string.not_installed)
                }

                else -> {
                    view.settingsIconRight.setImageResource(R.drawable.ic_round_new_releases_24)
                    view.settingsIconRight.rotation = 0f
                    view.settingsDesc.text = context.getString(R.string.error_msg, status)
                }
            }
            if (hasUpdate) {
                view.settingsIconRight.setImageResource(R.drawable.ic_round_sync_24)
                view.settingsDesc.text = context.getString(R.string.update_addon)
            }
        } catch (e: Exception) {
            Logger.log(e)
        }
    }
}