package ani.dantotsu.settings.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.databinding.ActivitySettingsMangaBinding
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.media.MediaType
import ani.dantotsu.settings.ReaderSettingsActivity
import ani.dantotsu.settings.Settings
import ani.dantotsu.settings.SettingsActivity
import ani.dantotsu.settings.SettingsAdapter
import ani.dantotsu.settings.SettingsView
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.customAlertDialog
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsMangaFragment : Fragment() {
    private lateinit var binding: ActivitySettingsMangaBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsMangaBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = requireActivity() as SettingsActivity

        binding.apply {
            mangaSettingsBack.setOnClickListener {
                settings.backToMenu()
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.reader_settings),
                        desc = getString(R.string.reader_settings_desc),
                        icon = R.drawable.ic_round_reader_settings,
                        onClick = {
                            startActivity(Intent(settings, ReaderSettingsActivity::class.java))
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.purge_manga_downloads),
                        desc = getString(R.string.purge_manga_downloads_desc),
                        icon = R.drawable.ic_round_delete_24,
                        onClick = {
                            settings.customAlertDialog().apply {
                                setTitle(R.string.purge_manga_downloads)
                                setMessage(R.string.purge_confirm, getString(R.string.manga))
                                setPosButton(R.string.yes, onClick = {
                                    val downloadsManager = Injekt.get<DownloadsManager>()
                                    downloadsManager.purgeDownloads(MediaType.MANGA)
                                })
                                setNegButton(R.string.no)
                                show()
                            }
                        },
                        isActivity = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.purge_novel_downloads),
                        desc = getString(R.string.purge_novel_downloads_desc),
                        icon = R.drawable.ic_round_delete_24,
                        onClick = {
                            settings.customAlertDialog().apply {
                                setTitle(R.string.purge_novel_downloads)
                                setMessage(R.string.purge_confirm, getString(R.string.novels))
                                setPosButton(R.string.yes) {
                                    val downloadsManager = Injekt.get<DownloadsManager>()
                                    downloadsManager.purgeDownloads(MediaType.NOVEL)
                                }
                                setNegButton(R.string.no)
                                show()
                            }
                        },
                        isActivity = true
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.include_list),
                        desc = getString(R.string.include_list_desc),
                        icon = R.drawable.view_list_24,
                        isChecked = PrefManager.getVal(PrefName.IncludeMangaList),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.IncludeMangaList, isChecked)
                            Refresh.all()
                        }
                    ),
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(settings, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }

            var previousChp: View = when (PrefManager.getVal<Int>(PrefName.MangaDefaultView)) {
                0 -> settingsChpList
                1 -> settingsChpCompact
                else -> settingsChpList
            }
            previousChp.alpha = 1f
            fun uiChp(mode: Int, current: View) {
                previousChp.alpha = 0.33f
                previousChp = current
                current.alpha = 1f
                PrefManager.setVal(PrefName.MangaDefaultView, mode)
            }

            settingsChpList.setOnClickListener {
                uiChp(0, it)
            }

            settingsChpCompact.setOnClickListener {
                uiChp(1, it)
            }
        }
    }
}