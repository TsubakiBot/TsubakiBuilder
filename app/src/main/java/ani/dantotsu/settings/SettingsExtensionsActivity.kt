package ani.dantotsu.settings

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ActivitySettingsExtensionsBinding
import ani.dantotsu.databinding.DialogUserAgentBinding
import ani.dantotsu.databinding.ItemRepositoryBinding
import ani.dantotsu.initActivity
import ani.dantotsu.media.MediaType
import ani.dantotsu.navBarHeight
import ani.dantotsu.parsers.novel.NovelExtensionManager
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.customAlertDialog
import ani.himitsu.os.Version
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsExtensionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsExtensionsBinding
    private val extensionInstaller = Injekt.get<BasePreferences>().extensionInstaller()
    private val animeExtensionManager: AnimeExtensionManager by injectLazy()
    private val mangaExtensionManager: MangaExtensionManager by injectLazy()
    private val novelExtensionManager: NovelExtensionManager by injectLazy()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this

        val extensionScope = CoroutineScope(Dispatchers.IO)

        binding = ActivitySettingsExtensionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            settingsExtensionsLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            extensionSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
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
                        val view = ItemRepositoryBinding.inflate(
                            LayoutInflater.from(repoInventory.context), repoInventory, true
                        )
                        view.repositoryItem.text =
                            item.removePrefix("https://raw.githubusercontent.com")
                        view.repositoryItem.setOnClickListener {
                            AlertDialog.Builder(this@SettingsExtensionsActivity, R.style.MyPopup)
                                .setTitle(R.string.rem_repository)
                                .setMessage(item)
                                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                                    val repos =
                                        PrefManager.getVal<Set<String>>(repoList).minus(item)
                                    PrefManager.setVal(repoList, repos)
                                    setExtensionOutput(repoInventory, type)
                                    extensionScope.launch {
                                        when (type) {
                                            MediaType.ANIME -> {
                                                animeExtensionManager.findAvailableExtensions()
                                            }

                                            MediaType.MANGA -> {
                                                mangaExtensionManager.findAvailableExtensions()
                                            }

                                            MediaType.NOVEL -> {
                                                novelExtensionManager.findAvailableExtensions()
                                                novelExtensionManager.findAvailablePlugins()
                                            }
                                        }
                                    }
                                    dialog.dismiss()
                                }
                                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .create()
                                .show()
                        }
                        view.repositoryItem.setOnLongClickListener {
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
                extensionScope.launch {
                    when (mediaType) {
                        MediaType.ANIME -> {
                            animeExtensionManager.findAvailableExtensions()
                        }

                        MediaType.MANGA -> {
                            mangaExtensionManager.findAvailableExtensions()
                        }

                        MediaType.NOVEL -> {
                            novelExtensionManager.findAvailableExtensions()
                            novelExtensionManager.findAvailablePlugins()
                        }
                    }
                }
                setExtensionOutput(view, mediaType)
            }

            fun processEditorAction(
                dialog: AlertDialog, editText: EditText, mediaType: MediaType, view: ViewGroup
            ) {
                editText.setOnEditorActionListener { textView, action, keyEvent ->
                    if (action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE ||
                        (keyEvent?.action == KeyEvent.ACTION_UP
                                && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                        return@setOnEditorActionListener if (textView.text.isNullOrBlank()) {
                            false
                        } else {
                            processUserInput(textView.text.toString(), mediaType, view)
                            dialog.dismiss()
                            true
                        }
                    }
                    false
                }
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.anime_add_repository),
                        desc = getString(R.string.anime_add_repository_desc),
                        icon = R.drawable.ic_github,
                        onClick = {
                            val dialogView = DialogUserAgentBinding.inflate(layoutInflater)
                            val editText = dialogView.userAgentTextBox.apply {
                                hint = getString(R.string.anime_add_repository)
                            }
                            context.customAlertDialog().apply {
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
                        }
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.manga_add_repository),
                        desc = getString(R.string.manga_add_repository_desc),
                        icon = R.drawable.ic_github,
                        onClick = {
                            val dialogView = DialogUserAgentBinding.inflate(layoutInflater)
                            val editText = dialogView.userAgentTextBox.apply {
                                hint = getString(R.string.manga_add_repository)
                            }
                            context.customAlertDialog().apply {
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
                        }
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.novel_add_repository),
                        desc = getString(R.string.novel_add_repository),
                        icon = R.drawable.ic_github,
                        onClick = {
                            val dialogView = DialogUserAgentBinding.inflate(layoutInflater)
                            val editText = dialogView.userAgentTextBox.apply {
                                hint = getString(R.string.novel_add_repository)
                            }
                            context.customAlertDialog().apply {
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
                        }
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.user_agent),
                        desc = getString(R.string.user_agent_desc),
                        icon = R.drawable.ic_round_video_settings_24,
                        onClick = {
                            val dialogView = DialogUserAgentBinding.inflate(layoutInflater)
                            val editText = dialogView.userAgentTextBox
                            editText.setText(PrefManager.getVal<String>(PrefName.DefaultUserAgent))
                            val alertDialog = AlertDialog.Builder(context, R.style.MyPopup)
                                .setTitle(R.string.user_agent).setView(dialogView.root)
                                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                                    PrefManager.setVal(
                                        PrefName.DefaultUserAgent,
                                        editText.text.toString()
                                    )
                                    dialog.dismiss()
                                }.setNeutralButton(getString(R.string.reset)) { dialog, _ ->
                                    PrefManager.removeVal(PrefName.DefaultUserAgent)
                                    editText.setText("")
                                    dialog.dismiss()
                                }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                }.create()

                            alertDialog.show()
                            alertDialog.window?.setDimAmount(0.8f)
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
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
                        type = SettingsView.SWITCH,
                        name = getString(R.string.skip_loading_extension_icons),
                        desc = getString(R.string.skip_loading_extension_icons_desc),
                        icon = R.drawable.ic_round_no_icon_24,
                        pref = PrefName.SkipExtensionIcons
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.NSFWExtention),
                        desc = getString(R.string.NSFWExtention_desc),
                        icon = R.drawable.ic_round_nsfw_24,
                        pref = PrefName.NSFWExtension

                    )
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}