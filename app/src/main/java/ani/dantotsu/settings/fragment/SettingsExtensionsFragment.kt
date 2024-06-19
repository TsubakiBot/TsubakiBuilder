package ani.dantotsu.settings.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ActivitySettingsExtensionsBinding
import ani.dantotsu.databinding.DialogUserAgentBinding
import ani.dantotsu.databinding.ItemRepositoryBinding
import ani.dantotsu.media.MediaType
import ani.dantotsu.parsers.ParserTestActivity
import ani.dantotsu.parsers.novel.NovelExtensionManager
import ani.dantotsu.settings.Settings
import ani.dantotsu.settings.SettingsActivity
import ani.dantotsu.settings.SettingsAdapter
import ani.dantotsu.settings.ViewType
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.customAlertDialog
import bit.himitsu.onCompletedAction
import bit.himitsu.os.Version
import com.google.android.material.textfield.TextInputEditText
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsExtensionsFragment : Fragment() {
    private lateinit var binding: ActivitySettingsExtensionsBinding
    private val extensionInstaller = Injekt.get<BasePreferences>().extensionInstaller()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsExtensionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = requireActivity() as SettingsActivity

        binding.apply {
            extensionSettingsBack.setOnClickListener {
                settings.backToMenu()
            }

            fun setExtensionOutput(repoInventory: ViewGroup, type: MediaType) {
                repoInventory.removeAllViews()
                val prefName: PrefName = when (type) {
                    MediaType.ANIME -> { PrefName.AnimeExtensionRepos }
                    MediaType.MANGA -> { PrefName.MangaExtensionRepos }
                    MediaType.NOVEL -> { PrefName.NovelExtensionRepos }
                }
                prefName.let { repoList ->
                    PrefManager.getVal<Set<String>>(repoList).forEach { item ->
                        val repoView = ItemRepositoryBinding.inflate(
                            LayoutInflater.from(repoInventory.context), repoInventory, true
                        )
                        repoView.repositoryItem.text =
                            item.removePrefix("https://raw.githubusercontent.com")
                        repoView.repositoryItem.setOnClickListener {
                            AlertDialog.Builder(settings, R.style.MyPopup)
                                .setTitle(R.string.rem_repository)
                                .setMessage(item)
                                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                                    val repos =
                                        PrefManager.getVal<Set<String>>(repoList).minus(item)
                                    PrefManager.setVal(repoList, repos)
                                    setExtensionOutput(repoInventory, type)
                                    dialog.dismiss()
                                }
                                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .create()
                                .show()
                        }
                        repoView.repositoryItem.setOnLongClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            copyToClipboard(item, true)
                            true
                        }
                    }
                    repoInventory.isVisible = repoInventory.childCount > 0
                }
            }

            fun processUserInput(input: String, mediaType: MediaType, view: ViewGroup) {
                val entry = if (input.endsWith("/") || input.endsWith(".min.json"))
                    input.substring(0, input.lastIndexOf("/")) else input
                val prefName: PrefName = when (mediaType) {
                    MediaType.ANIME -> { PrefName.AnimeExtensionRepos }
                    MediaType.MANGA -> { PrefName.MangaExtensionRepos }
                    MediaType.NOVEL -> { PrefName.NovelExtensionRepos }
                }
                val media = PrefManager.getVal<Set<String>>(prefName).plus(entry)
                PrefManager.setVal(prefName, media)
                setExtensionOutput(view, mediaType)
            }

            fun processEditorAction(
                dialog: AlertDialog, editText: TextInputEditText, mediaType: MediaType, view: ViewGroup
            ) {
                editText.setOnEditorActionListener(onCompletedAction {
                    processUserInput(editText.text.toString(), mediaType, view)
                    dialog.dismiss()
                })
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.anime_add_repository),
                        desc = getString(R.string.add_repository_desc, MediaType.ANIME.text),
                        icon = R.drawable.ic_github_mark,
                        onClick = {
                            val dialogView = DialogUserAgentBinding.inflate(layoutInflater)
                            val editText = dialogView.userAgentTextBox.apply {
                                hint = getString(R.string.anime_add_repository)
                            }
                            settings.customAlertDialog().apply {
                                setCancellable(true)
                                setTitle(R.string.anime_add_repository)
                                setCustomView(dialogView.root)
                                setPosButton(getString(R.string.ok)) {
                                    if (!editText.text.isNullOrBlank()) {
                                        processUserInput(
                                            editText.text.toString(),
                                            MediaType.ANIME,
                                            it.attachView
                                        )
                                    }
                                }
                                setNegButton(getString(R.string.cancel))
                                attach { dialog ->
                                    processEditorAction(
                                        dialog,
                                        editText,
                                        MediaType.ANIME,
                                        it.attachView
                                    )
                                }
                                show()
                            }
                        },
                        attach = {
                            setExtensionOutput(it.attachView, MediaType.ANIME)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.manga_add_repository),
                        desc = getString(R.string.add_repository_desc, MediaType.MANGA.text),
                        icon = R.drawable.ic_github_mark,
                        onClick = {
                            val dialogView = DialogUserAgentBinding.inflate(layoutInflater)
                            val editText = dialogView.userAgentTextBox.apply {
                                hint = getString(R.string.manga_add_repository)
                            }
                            settings.customAlertDialog().apply {
                                setCancellable(true)
                                setTitle(R.string.manga_add_repository)
                                setCustomView(dialogView.root)
                                setPosButton(getString(R.string.ok)) {
                                    if (!editText.text.isNullOrBlank()) {
                                        processUserInput(
                                            editText.text.toString(),
                                            MediaType.MANGA,
                                            it.attachView
                                        )
                                    }
                                }
                                setNegButton(getString(R.string.cancel))
                                attach { dialog ->
                                    processEditorAction(
                                        dialog,
                                        editText,
                                        MediaType.MANGA,
                                        it.attachView
                                    )
                                }
                                show()
                            }
                        },
                        attach = {
                            setExtensionOutput(it.attachView, MediaType.MANGA)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.novel_add_repository),
                        desc = getString(R.string.add_repository_desc, MediaType.NOVEL.text),
                        icon = R.drawable.ic_github_mark,
                        onClick = {
                            val dialogView = DialogUserAgentBinding.inflate(layoutInflater)
                            val editText = dialogView.userAgentTextBox.apply {
                                hint = getString(R.string.novel_add_repository)
                            }
                            settings.customAlertDialog().apply {
                                setCancellable(true)
                                setTitle(R.string.novel_add_repository)
                                setCustomView(dialogView.root)
                                setPosButton(getString(R.string.ok)) {
                                    if (!editText.text.isNullOrBlank()) {
                                        processUserInput(
                                            editText.text.toString(),
                                            MediaType.NOVEL,
                                            it.attachView
                                        )
                                    }
                                }
                                setNegButton(getString(R.string.cancel))
                                attach { dialog ->
                                    processEditorAction(
                                        dialog,
                                        editText,
                                        MediaType.NOVEL,
                                        it.attachView
                                    )
                                }
                                show()
                            }
                        },
                        attach = {
                            setExtensionOutput(it.attachView, MediaType.NOVEL)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.extension_test),
                        desc = getString(R.string.extension_test_desc),
                        icon = R.drawable.ic_network_check_24,
                        isActivity = true,
                        onClick = {
                            ContextCompat.startActivity(
                                settings,
                                Intent(settings, ParserTestActivity::class.java),
                                null
                            )
                        }
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.force_legacy_installer),
                        desc = getString(R.string.force_legacy_installer_desc),
                        icon = R.drawable.ic_round_new_releases_24,
                        isChecked = extensionInstaller.get() == BasePreferences.ExtensionInstaller.LEGACY,
                        switch = { isChecked, _ ->
                            if (isChecked) {
                                extensionInstaller.set(BasePreferences.ExtensionInstaller.LEGACY)
                            } else {
                                extensionInstaller.set(BasePreferences.ExtensionInstaller.PACKAGEINSTALLER)
                            }
                        },
                        isVisible = Version.isLowerThan(Build.VERSION_CODES.Q)
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.skip_loading_extension_icons),
                        desc = getString(R.string.skip_loading_extension_icons_desc),
                        icon = R.drawable.ic_round_no_icon_24,
                        pref = PrefName.SkipExtensionIcons
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.NSFWExtention),
                        desc = getString(R.string.NSFWExtention_desc),
                        icon = R.drawable.ic_round_nsfw_24,
                        pref = PrefName.NSFWExtension

                    )
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(settings, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }
        }
    }

    override fun onDestroyView() {
        CoroutineScope(Dispatchers.IO).launch {
            val animeExtensionManager: AnimeExtensionManager by injectLazy()
            animeExtensionManager.findAvailableExtensions()
        }
        CoroutineScope(Dispatchers.IO).launch {
            val mangaExtensionManager: MangaExtensionManager by injectLazy()
            mangaExtensionManager.findAvailableExtensions()
        }
        CoroutineScope(Dispatchers.IO).launch {
            val novelExtensionManager: NovelExtensionManager by injectLazy()
            novelExtensionManager.findAvailableExtensions()
            novelExtensionManager.findAvailablePlugins()
        }
        super.onDestroyView()
    }
}