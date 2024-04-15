package ani.dantotsu.settings

import android.graphics.drawable.Animatable
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ActivitySettingsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.openLinkInYouTube
import ani.dantotsu.pop
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.snackString
import ani.dantotsu.startMainActivity
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.matagi.update.MatagiUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SettingsActivity : AppCompatActivity() {
    private val restartMainActivity = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = startMainActivity(this@SettingsActivity)
    }
    lateinit var binding: ActivitySettingsBinding
    private var cursedCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val context = this
        binding.apply {

            settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }

            onBackPressedDispatcher.addCallback(this@SettingsActivity) {
                startMainActivity(this@SettingsActivity)
            }

            settingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            settingsVersion.apply {
                text = getString(R.string.version_current, BuildConfig.VERSION_NAME)

                setOnLongClickListener {
                    copyToClipboard(getDeviceInfo(), false)
                    toast(getString(R.string.copied_device_info))
                    return@setOnLongClickListener true
                }
            }

            val settings = arrayListOf(
                Settings(
                    getString(R.string.accounts),
                    R.drawable.ic_round_person_32,
                    getString(R.string.accounts_desc),
                    SettingsAccountActivity::class.java
                ),
                Settings(
                    getString(R.string.theme_ui),
                    R.drawable.ic_palette,
                    getString(R.string.theme_desc),
                    SettingsThemeActivity::class.java
                ),
                Settings(
                    getString(R.string.extensions),
                    R.drawable.ic_extension,
                    getString(R.string.extensions_desc),
                    SettingsExtensionsActivity::class.java
                ),
                Settings(
                    getString(R.string.common),
                    R.drawable.ic_lightbulb_24,
                    getString(R.string.common_desc),
                    SettingsCommonActivity::class.java
                ),
                Settings(
                    getString(R.string.anime),
                    R.drawable.ic_round_movie_filter_24,
                    getString(R.string.anime_desc),
                    SettingsAnimeActivity::class.java
                ),
                Settings(
                    getString(R.string.manga),
                    R.drawable.ic_round_import_contacts_24,
                    getString(R.string.manga_desc),
                    SettingsMangaActivity::class.java
                ),
                Settings(
                    getString(R.string.notifications),
                    R.drawable.ic_round_notifications_none_24,
                    getString(R.string.notifications_desc),
                    SettingsNotificationActivity::class.java
                ),
                Settings(
                    getString(R.string.system),
                    R.drawable.ic_admin_panel_settings_24,
                    getString(R.string.system_desc),
                    SettingsSystemActivity::class.java
                ),
                Settings(
                    getString(R.string.about),
                    R.drawable.ic_round_info_24,
                    getString(R.string.about_desc),
                    SettingsAboutActivity::class.java
                ),
            )

            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                adapter = SettingsAdapter(settings)
                setHasFixedSize(true)
            }

            if (!BuildConfig.FLAVOR.contains("fdroid")) {
                binding.settingsLogo.setOnLongClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        MatagiUpdater.check(this@SettingsActivity, true)
                    }
                    true
                }
            }

            settingBuyMeCoffee.setOnClickListener {
                lifecycleScope.launch {
                    it.pop()
                }
                openLinkInBrowser(getString(R.string.coffee))
            }
            lifecycleScope.launch {
                settingBuyMeCoffee.pop()
            }

            loginDiscord.setOnClickListener {
                openLinkInBrowser(getString(R.string.discord))
            }
            loginGithub.setOnClickListener {
                openLinkInBrowser(getString(R.string.github))
            }
            loginTelegram.setOnClickListener {
                openLinkInBrowser(getString(R.string.telegram))
            }


            (settingsLogo.drawable as Animatable).start()
            val array = resources.getStringArray(R.array.tips)

            settingsLogo.setSafeOnClickListener {
                cursedCounter++
                (binding.settingsLogo.drawable as Animatable).start()
                if (cursedCounter % 7 == 0) {
                    toast(R.string.you_cursed)
                    openLinkInYouTube(getString(R.string.cursed_yt))
                    // PrefManager.setVal(PrefName.ImageUrl, !PrefManager.getVal(PrefName.ImageUrl, false))
                } else {
                    snackString(array[(Math.random() * array.size).toInt()], this@SettingsActivity)
                }
            }
        }
    }

    companion object {
        fun getDeviceInfo(): String {
            return """
                dantotsu Version: ${BuildConfig.VERSION_NAME}
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
}
