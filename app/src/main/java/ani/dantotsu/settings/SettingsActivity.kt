package ani.dantotsu.settings

import android.content.res.Configuration
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.dantotsu.BuildConfig
import ani.dantotsu.databinding.ActivitySettingsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.refresh
import ani.dantotsu.settings.fragment.SettingsAboutFragment
import ani.dantotsu.settings.fragment.SettingsAddonFragment
import ani.dantotsu.settings.fragment.SettingsAnimeFragment
import ani.dantotsu.settings.fragment.SettingsCommonFragment
import ani.dantotsu.settings.fragment.SettingsExtensionsFragment
import ani.dantotsu.settings.fragment.SettingsMainFragment
import ani.dantotsu.settings.fragment.SettingsMangaFragment
import ani.dantotsu.settings.fragment.SettingsNotificationFragment
import ani.dantotsu.settings.fragment.SettingsSystemFragment
import ani.dantotsu.settings.fragment.SettingsThemeFragment
import ani.dantotsu.settings.fragment.UserInterfaceFragment
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.LauncherWrapper
import bit.himitsu.setBaseline


class SettingsActivity : AppCompatActivity() {
    lateinit var binding: ActivitySettingsBinding
    private val contract = ActivityResultContracts.OpenDocumentTree()
    private lateinit var launcher: LauncherWrapper
    private var silentExit: Boolean = false

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        launcher = LauncherWrapper(this, contract)

        binding.apply {

            settingsViewPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
            }

            settingsViewPager.setBaseline(resources.configuration)

            onBackPressedDispatcher.addCallback(this@SettingsActivity) {
                when {
                    silentExit -> finish()
                    binding.settingsViewPager.currentItem != 0 -> setFragment(Page.MAIN)
                    else -> refresh()
                }
            }

            binding.settingsViewPager.adapter = ViewPagerAdapter(
                supportFragmentManager,
                lifecycle
            )

            intent?.getStringExtra("fragment")?.let {
                silentExit = true
                setFragment(Page.valueOf(it))
            }
        }
    }

    private class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle
    ) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 11

        override fun createFragment(position: Int): Fragment = when (position) {
            Page.MAIN.ordinal -> SettingsMainFragment()
            Page.UI.ordinal -> UserInterfaceFragment()
            Page.THEME.ordinal -> SettingsThemeFragment()
            Page.COMMON.ordinal -> SettingsCommonFragment()
            Page.ANIME.ordinal -> SettingsAnimeFragment()
            Page.MANGA.ordinal -> SettingsMangaFragment()
            Page.EXTENSION.ordinal -> SettingsExtensionsFragment()
            Page.ADDON.ordinal-> SettingsAddonFragment()
            Page.NOTIFICATION.ordinal -> SettingsNotificationFragment()
            Page.SYSTEM.ordinal -> SettingsSystemFragment()
            Page.ABOUT.ordinal -> SettingsAboutFragment()
            else -> SettingsMainFragment()
        }
    }

    fun setFragment(page: Page) {
        binding.settingsViewPager.setCurrentItem(page.ordinal, false)
    }

    fun backToMenu() {
        if (silentExit) {
            finish()
        } else {
            binding.settingsViewPager.setCurrentItem(Page.MAIN.ordinal, false)
        }
    }

    fun getLauncher(): LauncherWrapper? {
        return if (this::launcher.isInitialized) launcher else null
    }

    companion object {
        fun getDeviceInfo(): String {
            return """
                Himitsu ${BuildConfig.COMMIT}
                Device: $BRAND $DEVICE
                Architecture: ${getArch()}
                OS Version: $CODENAME $RELEASE ($SDK_INT)
            """.trimIndent()
        }

        private fun getArch(): String {
            SUPPORTED_ABIS.forEach {
                when (it) {
                    "arm64-v8a" -> return "aarch64"
                    "armeabi-v7a" -> return "arm"
                    "x86_64" -> return "x86_64"
                    "x86" -> return "i686"
                }
            }
            return System.getProperty("os.arch") ?: System.getProperty("os.product.cpu.abi")
            ?: "Unknown Architecture"
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.settingsViewPager.setBaseline(newConfig)
    }
}
