package ani.dantotsu.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.R
import ani.dantotsu.copyToClipboard
import ani.dantotsu.currContext
import ani.dantotsu.databinding.ActivityExtensionsBinding
import ani.dantotsu.databinding.DialogRepositoriesBinding
import ani.dantotsu.databinding.ItemRepositoryBinding
import ani.dantotsu.initActivity
import ani.dantotsu.media.MediaType
import ani.dantotsu.navBarHeight
import ani.dantotsu.others.AndroidBug5497Workaround
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.parsers.novel.NovelExtensionManager
import ani.dantotsu.settings.fragment.AnimeExtensionsFragment
import ani.dantotsu.settings.fragment.InstalledAnimeExtensionsFragment
import ani.dantotsu.settings.fragment.InstalledMangaExtensionsFragment
import ani.dantotsu.settings.fragment.InstalledNovelExtensionsFragment
import ani.dantotsu.settings.fragment.MangaExtensionsFragment
import ani.dantotsu.settings.fragment.NovelExtensionsFragment
import ani.dantotsu.settings.fragment.NovelPluginsFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.tryWithSuspend
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy

class ExtensionsActivity : AppCompatActivity() {
    lateinit var binding: ActivityExtensionsBinding

    private val animeExtensionManager: AnimeExtensionManager by injectLazy()
    private val mangaExtensionManager: MangaExtensionManager by injectLazy()
    private val novelExtensionManager: NovelExtensionManager by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        binding = ActivityExtensionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initActivity(this)
        AndroidBug5497Workaround.assistActivity(this) {
            if (it) {
                binding.searchView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = statusBarHeight
                }
            } else {
                binding.searchView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = statusBarHeight + navBarHeight
                }
            }
        }

        binding.listBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.searchView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = statusBarHeight + navBarHeight
        }

        binding.viewPager.offscreenPageLimit = 1

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 7

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> InstalledAnimeExtensionsFragment()
                    1 -> AnimeExtensionsFragment()
                    2 -> InstalledMangaExtensionsFragment()
                    3 -> MangaExtensionsFragment()
                    4 -> InstalledNovelExtensionsFragment()
                    5 -> NovelExtensionsFragment()
                    6 -> NovelPluginsFragment()
                    else -> AnimeExtensionsFragment()
                }
            }

        }

        val searchView: AutoCompleteTextView = findViewById(R.id.searchViewText)

        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    searchView.setText("")
                    searchView.clearFocus()
                    binding.tabLayout.clearFocus()
                    binding.languageselect.isVisible = tab.text?.contains(
                        getString(R.string.available_extensions, "")
                    ) == true
                    binding.viewPager.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }

                    when {
                        tab.text?.contains(MediaType.ANIME.asText()) == true -> {
                            generateRepositoryButton(MediaType.ANIME)
                        }
                        tab.text?.contains(MediaType.MANGA.asText()) == true -> {
                            generateRepositoryButton(MediaType.MANGA)
                        }
                        tab.text?.contains(MediaType.NOVEL.asText()) == true -> {
                            generateRepositoryButton(MediaType.NOVEL)
                        }
                        else -> {}
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    binding.viewPager.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    binding.tabLayout.clearFocus()
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    binding.viewPager.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    // Do nothing
                }
            }
        )

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.installed_extensions, MediaType.ANIME.asText())
                1 -> getString(R.string.available_extensions, MediaType.ANIME.asText())
                2 -> getString(R.string.installed_extensions, MediaType.MANGA.asText())
                3 -> getString(R.string.available_extensions, MediaType.MANGA.asText())
                4 -> getString(R.string.installed_extensions, MediaType.NOVEL.asText())
                5 -> getString(R.string.available_extensions, MediaType.NOVEL.asText())
                6 -> getString(R.string.available_plugins, MediaType.NOVEL.asText())
                else -> null
            }
        }.attach()

        searchView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentFragment =
                    supportFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}")
                if (currentFragment is SearchQueryHandler) {
                    currentFragment.updateContentBasedOnQuery(s?.toString()?.trim())
                }
            }
        })

        initActivity(this)
        binding.languageselect.setOnClickListener {
            val languageOptions =
                LanguageMapper.Language.entries.map { it.name }.toTypedArray()
            val builder = AlertDialog.Builder(currContext(), R.style.MyPopup)
            val listOrder: String = PrefManager.getVal(PrefName.LangSort)
            val index = LanguageMapper.Language.entries.toTypedArray()
                .indexOfFirst { it.code == listOrder }
            builder.setTitle(R.string.language)
            builder.setSingleChoiceItems(languageOptions, index) { dialog, i ->
                PrefManager.setVal(
                    PrefName.LangSort,
                    LanguageMapper.Language.entries[i].code
                )
                val currentFragment =
                    supportFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}")
                if (currentFragment is SearchQueryHandler) {
                    currentFragment.notifyDataChanged()
                }
                dialog.dismiss()
            }
            val dialog = builder.show()
            dialog.window?.setDimAmount(0.8f)
        }
        binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }
    }

    private val onChangeSettings = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _: ActivityResult ->

    }

    private fun generateRepositoryButton(type: MediaType) {
        binding.openSettingsButton.setOnClickListener {
            onChangeSettings.launch(Intent(this, SettingsExtensionsActivity::class.java))
        }
    }
}

interface SearchQueryHandler {
    fun updateContentBasedOnQuery(query: String?)
    fun notifyDataChanged()
}
