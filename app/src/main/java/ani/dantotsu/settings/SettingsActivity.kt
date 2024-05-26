package ani.dantotsu.settings

import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Animatable
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.databinding.ActivitySettingsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.loadImage
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.openLinkInYouTube
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.fragment.DiscordDialogFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.startMainActivity
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.dantotsu.util.LauncherWrapper
import bit.himitsu.update.MatagiUpdater
import bit.himitsu.withFlexibleMargin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SettingsActivity : AppCompatActivity() {
    lateinit var binding: ActivitySettingsBinding
    private val contract = ActivityResultContracts.OpenDocumentTree()
    private lateinit var launcher: LauncherWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        launcher = LauncherWrapper(this, contract)

        binding.apply {

            settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
            }

            settingsContainer.withFlexibleMargin(resources.configuration)

            onBackPressedDispatcher.addCallback(this@SettingsActivity) {
                startMainActivity(this@SettingsActivity)
            }

            binding.settingsViewPager.adapter = ViewPagerAdapter(
                supportFragmentManager,
                lifecycle
            )
        }
    }

    private class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle
    ) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 10

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> SettingsMainFragment()
            1 -> SettingsThemeFragment()
            2 -> SettingsCommonFragment()
            3 -> SettingsAnimeFragment()
            4 -> SettingsMangaFragment()
            5 -> SettingsExtensionsFragment()
            6-> SettingsAddonFragment()
            7 -> SettingsNotificationFragment()
            8 -> SettingsSystemFragment()
            9 -> SettingsAboutFragment()
            else -> SettingsAboutFragment()
        }
    }

    fun setFragment(index: Int) {
        binding.settingsViewPager.currentItem = index
    }

    fun backToMenu() {
        binding.settingsViewPager.currentItem = 0
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
        binding.settingsContainer.withFlexibleMargin(newConfig)
    }
}
