package ani.dantotsu.settings

import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ActivitySettingsAboutBinding
import ani.dantotsu.databinding.ActivitySettingsAccountsBinding
import ani.dantotsu.databinding.ActivitySettingsAnimeBinding
import ani.dantotsu.databinding.ActivitySettingsBinding
import ani.dantotsu.databinding.ActivitySettingsCommonBinding
import ani.dantotsu.databinding.ActivitySettingsExtensionsBinding
import ani.dantotsu.databinding.ActivitySettingsMangaBinding
import ani.dantotsu.databinding.ActivitySettingsNotificationsBinding
import ani.dantotsu.databinding.ActivitySettingsSystemBinding
import ani.dantotsu.databinding.ActivitySettingsThemeBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.openLinkInYouTube
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.pop
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.snackString
import ani.dantotsu.startMainActivity
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.dantotsu.util.LauncherWrapper
import ani.matagi.update.MatagiUpdater
import eltos.simpledialogfragment.SimpleDialog
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.random.Random


class SettingsActivity : AppCompatActivity() {
    private val restartMainActivity = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = startMainActivity(this@SettingsActivity)
    }
    lateinit var binding: ActivitySettingsBinding
    private var cursedCounter = 0

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        binding.settingsVersion.text = getString(R.string.version_current, BuildConfig.VERSION_NAME)
        binding.settingsVersion.setOnLongClickListener {
            copyToClipboard(getDeviceInfo(), false)
            toast(getString(R.string.copied_device_info))
            return@setOnLongClickListener true
        }

        binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        onBackPressedDispatcher.addCallback(this, restartMainActivity)

        binding.settingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.apply {

            settingsTheme.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, SettingsThemeActivity::class.java))
            }

            settingsAccount.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, SettingsAccountActivity::class.java))
            }

            settingsAnime.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, SettingsAnimeActivity::class.java))
            }

            settingsManga.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, SettingsMangaActivity::class.java))
            }

            settingsExtension.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, SettingsExtensionsActivity::class.java))
            }

            settingsCommon.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, SettingsCommonActivity::class.java))
            }

            settingsSystem.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, SettingsSystemActivity::class.java))
            }

            settingsNotification.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, SettingsNotificationActivity::class.java))
            }

            settingsAbout.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, SettingsAboutActivity::class.java))
            }
        }

        if (!BuildConfig.FLAVOR.contains("fdroid")) {
            binding.settingsLogo.setOnLongClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    MatagiUpdater.check(this@SettingsActivity, true)
                }
                true
            }
        }

        binding.settingBuyMeCoffee.setOnClickListener {
            lifecycleScope.launch {
                it.pop()
            }
            openLinkInBrowser(getString(R.string.coffee))
        }
        lifecycleScope.launch {
            binding.settingBuyMeCoffee.pop()
        }

        binding.loginDiscord.setOnClickListener {
            openLinkInBrowser(getString(R.string.discord))
        }
        binding.loginGithub.setOnClickListener {
            openLinkInBrowser(getString(R.string.github))
        }
        binding.loginTelegram.setOnClickListener {
            openLinkInBrowser(getString(R.string.telegram))
        }


        (binding.settingsLogo.drawable as Animatable).start()
        val array = resources.getStringArray(R.array.tips)

        binding.settingsLogo.setSafeOnClickListener {
            cursedCounter++
            (binding.settingsLogo.drawable as Animatable).start()
            if (cursedCounter % 7 == 0) {
                toast(R.string.you_cursed)
                openLinkInYouTube(getString(R.string.cursed_yt))
                //PrefManager.setVal(PrefName.ImageUrl, !PrefManager.getVal(PrefName.ImageUrl, false))
            } else {
                snackString(array[(Math.random() * array.size).toInt()], this)
            }

        }

        if (BuildConfig.BUILD_TYPE.contentEquals("matagi")) return
        lifecycleScope.launch(Dispatchers.IO) {
            delay(2000)
            runOnUiThread {
                if (Random.nextInt(0, 100) > 69) {
                    CustomBottomDialog.newInstance().apply {
                        title = this@SettingsActivity.getString(R.string.enjoying_app)
                        addView(TextView(this@SettingsActivity).apply {
                            text = context.getString(R.string.consider_donating)
                        })

                        setNegativeButton(this@SettingsActivity.getString(R.string.no_moners)) {
                            snackString(R.string.you_be_rich)
                            dismiss()
                        }

                        setPositiveButton(this@SettingsActivity.getString(R.string.donate)) {
                            binding.settingBuyMeCoffee.performClick()
                            dismiss()
                        }
                        show(supportFragmentManager, "dialog")
                    }
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
