package ani.dantotsu.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.databinding.ActivitySettingsAnimeBinding
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.initActivity
import ani.dantotsu.media.MediaType
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.torrServerStart
import ani.dantotsu.torrServerStop
import eu.kanade.tachiyomi.data.torrentServer.TorrentServerUtils
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsAnimeActivity: AppCompatActivity(){
    private lateinit var binding: ActivitySettingsAnimeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this

        binding = ActivitySettingsAnimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            settingsAnimeLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            animeSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.player_settings),
                        desc = getString(R.string.player_settings),
                        icon = R.drawable.ic_round_video_settings_24,
                        onClick = {
                            startActivity(Intent(context, PlayerSettingsActivity::class.java))
                        },
                        isActivity = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.purge_anime_downloads),
                        desc = getString(R.string.purge_anime_downloads),
                        icon = R.drawable.ic_round_delete_24,
                        onClick = {
                            val dialog = AlertDialog.Builder(context, R.style.MyPopup)
                                .setTitle(R.string.purge_anime_downloads)
                                .setMessage(getString(R.string.purge_confirm, getString(R.string.anime)))
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
                        desc = getString(R.string.prefer_dub),
                        icon = R.drawable.ic_round_audiotrack_24,
                        isChecked = PrefManager.getVal(PrefName.SettingsPreferDub),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.SettingsPreferDub, isChecked)
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.show_yt),
                        desc = getString(R.string.show_yt),
                        icon = R.drawable.ic_round_play_circle_24,
                        isChecked = PrefManager.getVal(PrefName.ShowYtButton),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.ShowYtButton, isChecked)
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.include_list),
                        desc = getString(R.string.include_list),
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
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
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

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}