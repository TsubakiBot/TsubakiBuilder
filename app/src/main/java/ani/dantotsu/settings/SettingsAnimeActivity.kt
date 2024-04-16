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

            settingsPlayer.setOnClickListener {
                startActivity(Intent(this@SettingsAnimeActivity, PlayerSettingsActivity::class.java))
            }

            purgeAnimeDownloads.setOnClickListener {
                val dialog = AlertDialog.Builder(this@SettingsAnimeActivity, R.style.MyPopup)
                    .setTitle(R.string.purge_anime_downloads)
                    .setMessage(getString(R.string.purge_confirm, getString(R.string.anime)))
                    .setPositiveButton(R.string.yes) { dialog, _ ->
                        val downloadsManager = Injekt.get<DownloadsManager>()
                        downloadsManager.purgeDownloads(MediaType.ANIME)
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.no) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            settingsPreferDub.isChecked = PrefManager.getVal(PrefName.SettingsPreferDub)
            settingsPreferDub.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.SettingsPreferDub, isChecked)
            }


            settingsShowYt.isChecked = PrefManager.getVal(PrefName.ShowYtButton)
            settingsShowYt.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.ShowYtButton, isChecked)
            }
            settingsIncludeAnimeList.isChecked = PrefManager.getVal(PrefName.IncludeAnimeList)
            settingsIncludeAnimeList.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.IncludeAnimeList, isChecked)
                Refresh.all()
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
                    torrServerStart(this@SettingsAnimeActivity)
                else
                    torrServerStop(this@SettingsAnimeActivity)
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
                            torrServerStop(this@SettingsAnimeActivity)
                            TorrentServerUtils.port = view.text.toString()
                            torrServerStart(this@SettingsAnimeActivity)
                            return@OnEditorActionListener true
                        }
                    }
                    false
                }
            )
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}