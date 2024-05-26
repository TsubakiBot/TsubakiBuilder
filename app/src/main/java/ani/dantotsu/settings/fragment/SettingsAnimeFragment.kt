package ani.dantotsu.settings.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.databinding.ActivitySettingsAnimeBinding
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.media.MediaType
import ani.dantotsu.settings.PlayerSettingsActivity
import ani.dantotsu.settings.Settings
import ani.dantotsu.settings.SettingsActivity
import ani.dantotsu.settings.SettingsAdapter
import ani.dantotsu.settings.SettingsView
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import bit.himitsu.torrServerStart
import bit.himitsu.torrServerStop
import eu.kanade.tachiyomi.data.torrentServer.TorrentServerUtils
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsAnimeFragment : Fragment() {
    private lateinit var binding: ActivitySettingsAnimeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsAnimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = requireActivity() as SettingsActivity

        binding.apply {
            animeSettingsBack.setOnClickListener {
                settings.backToMenu()
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.player_settings),
                        desc = getString(R.string.player_settings_desc),
                        icon = R.drawable.ic_round_video_settings_24,
                        onClick = {
                            startActivity(Intent(settings, PlayerSettingsActivity::class.java))
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.purge_anime_downloads),
                        desc = getString(R.string.purge_anime_downloads_desc),
                        icon = R.drawable.ic_round_delete_24,
                        onClick = {
                            val dialog = AlertDialog.Builder(settings, R.style.MyPopup)
                                .setTitle(R.string.purge_anime_downloads)
                                .setMessage(
                                    getString(
                                        R.string.purge_confirm,
                                        getString(R.string.anime)
                                    )
                                )
                                .setPositiveButton(R.string.yes) { dialog, _ ->
                                    val downloadsManager = Injekt.get<DownloadsManager>()
                                    downloadsManager.purgeDownloads(MediaType.ANIME)
                                    dialog.dismiss()
                                }.setNegativeButton(R.string.no) { dialog, _ ->
                                    dialog.dismiss()
                                }.create()
                            dialog.window?.setDimAmount(0.8f)
                            dialog.show()
                        }

                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.prefer_dub),
                        desc = getString(R.string.prefer_dub_desc),
                        icon = R.drawable.ic_round_audiotrack_24,
                        pref = PrefName.SettingsPreferDub
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.show_yt),
                        desc = getString(R.string.show_yt_desc),
                        icon = R.drawable.ic_round_play_circle_24,
                        pref = PrefName.ShowYtButton
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.include_list),
                        desc = getString(R.string.include_list_anime_desc),
                        icon = R.drawable.view_list_24,
                        isChecked = PrefManager.getVal(PrefName.IncludeAnimeList),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.IncludeAnimeList, isChecked)
                            Refresh.all()
                        }
                    ),
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(settings, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }

            var previousEp: View = when (PrefManager.getVal<Int>(PrefName.AnimeDefaultView)) {
                0 -> settingsEpList
                1 -> settingsEpGrid
                2 -> settingsEpCompact
                else -> settingsEpList
            }
            previousEp.alpha = 1f
            fun uiEp(mode: Int, current: View) {
                previousEp.alpha = 0.33f
                previousEp = current
                current.alpha = 1f
                PrefManager.setVal(PrefName.AnimeDefaultView, mode)
            }

            settingsEpList.setOnClickListener {
                uiEp(0, it)
            }

            settingsEpGrid.setOnClickListener {
                uiEp(1, it)
            }

            settingsEpCompact.setOnClickListener {
                uiEp(2, it)
            }

            settingsTorrServer.isChecked = PrefManager.getVal(PrefName.TorrServerEnabled)
            settingsTorrServer.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked)
                    torrServerStart()
                else
                    torrServerStop()
                PrefManager.setVal(PrefName.TorrServerEnabled, isChecked)
            }

            torrentPortNumber.setText(TorrentServerUtils.port)
            torrentPortNumber.setOnEditorActionListener(
                TextView.OnEditorActionListener { view, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.action == KeyEvent.ACTION_DOWN
                                && event.keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                        if (event == null || !event.isShiftPressed) {
                            if (view.text.toString().toInt() < 0
                                || view.text.toString().toInt() > 65535
                            ) {
                                snackString(R.string.invalid_port)
                            }
                            torrServerStop()
                            TorrentServerUtils.port = view.text.toString()
                            torrServerStart()
                            return@OnEditorActionListener true
                        }
                    }
                    false
                }
            )
        }
    }
}