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
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
import bit.himitsu.update.MatagiUpdater
import bit.himitsu.withFlexibleMargin
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
            }

            settingsContainer.withFlexibleMargin(resources.configuration)

            onBackPressedDispatcher.addCallback(this@SettingsActivity) {
                startMainActivity(this@SettingsActivity)
            }

            settingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            binding.settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.ui_settings),
                        desc = getString(R.string.ui_settings_desc),
                        icon = R.drawable.ic_round_auto_awesome_24,
                        onClick = {
                            startActivity(
                                Intent(
                                    context,
                                    UserInterfaceSettingsActivity::class.java
                                )
                            )
                            finish()
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.theme),
                        desc = getString(R.string.theme_desc),
                        icon = R.drawable.ic_palette,
                        onClick = {
                            startActivity(Intent(context, SettingsThemeActivity::class.java))
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.common),
                        desc = getString(R.string.common_desc),
                        icon = R.drawable.ic_lightbulb_24,
                        onClick = {
                            startActivity(Intent(context, SettingsCommonActivity::class.java))
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.anime),
                        desc = getString(R.string.anime_desc),
                        icon = R.drawable.ic_round_movie_filter_24,
                        onClick = {
                            startActivity(Intent(context, SettingsAnimeActivity::class.java))
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.manga),
                        desc = getString(R.string.manga_desc),
                        icon = R.drawable.ic_round_import_contacts_24,
                        onClick = {
                            startActivity(Intent(context, SettingsMangaActivity::class.java))
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.extensions),
                        desc = getString(R.string.extensions_desc),
                        icon = R.drawable.ic_extension,
                        onClick = {
                            startActivity(Intent(context, SettingsExtensionsActivity::class.java))
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.addons),
                        desc = getString(R.string.addons_desc),
                        icon = R.drawable.ic_round_restaurant_24,
                        onClick = {
                            startActivity(Intent(context, SettingsAddonActivity::class.java))
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.notifications),
                        desc = getString(R.string.notifications_desc),
                        icon = R.drawable.ic_round_notifications_none_24,
                        onClick = {
                            startActivity(Intent(context, SettingsNotificationActivity::class.java))
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.system),
                        desc = getString(R.string.system_desc),
                        icon = R.drawable.ic_admin_panel_settings_24,
                        onClick = {
                            startActivity(Intent(context, SettingsSystemActivity::class.java))
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.anilist),
                        desc = getString(R.string.ani_setting_desc),
                        icon = R.drawable.ic_anilist,
                        onClick = {
                            openLinkInBrowser("https://anilist.co/settings")
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.about),
                        desc = getString(R.string.about_desc),
                        icon = R.drawable.ic_round_info_24,
                        onClick = {
                            startActivity(Intent(context, SettingsAboutActivity::class.java))
                        },
                        hasTransition = true
                    )
                )
            )

            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }

            binding.apply {
                fun reload() {
                    if (Anilist.token != null) {
                        settingsAnilistLogin.setText(R.string.logout)
                        settingsAnilistLogin.setOnClickListener {
                            Anilist.removeSavedToken()
                            recreate()
                            reload()
                        }
                        settingsAnilistUsername.visibility = View.VISIBLE
                        settingsAnilistUsername.text = Anilist.username
                        settingsAnilistAvatar.loadImage(Anilist.avatar)
                        settingsAnilistAvatar.setOnClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            val anilistLink = getString(
                                R.string.anilist_link,
                                PrefManager.getVal<String>(PrefName.AnilistUserName)
                            )
                            openLinkInBrowser(anilistLink)
                        }

                        settingsMALLoginRequired.visibility = View.GONE
                        settingsMALLogin.visibility = View.VISIBLE
                        settingsMALUsername.visibility = View.VISIBLE

                        if (MAL.token != null) {
                            settingsMALLogin.setText(R.string.logout)
                            settingsMALLogin.setOnClickListener {
                                MAL.removeSavedToken()
                                recreate()
                                reload()
                            }
                            settingsMALUsername.visibility = View.VISIBLE
                            settingsMALUsername.text = MAL.username
                            settingsMALAvatar.loadImage(MAL.avatar)
                            settingsMALAvatar.setOnClickListener {
                                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                openLinkInBrowser(getString(R.string.myanilist_link, MAL.username))
                            }
                        } else {
                            settingsMALAvatar.setImageResource(R.drawable.ic_round_person_32)
                            settingsMALUsername.visibility = View.GONE
                            settingsMALLogin.setText(R.string.login)
                            settingsMALLogin.setOnClickListener {
                                MAL.loginIntent(context)
                            }
                        }
                    } else {
                        settingsAnilistAvatar.setImageResource(R.drawable.ic_round_person_32)
                        settingsAnilistUsername.visibility = View.GONE
                        settingsAnilistLogin.setText(R.string.login)
                        settingsAnilistLogin.setOnClickListener {
                            Anilist.loginIntent(context)
                        }
                        settingsMALLoginRequired.visibility = View.VISIBLE
                        settingsMALLogin.visibility = View.GONE
                        settingsMALUsername.visibility = View.GONE
                    }

                    if (Discord.token != null) {
                        val id = PrefManager.getVal(PrefName.DiscordId, null as String?)
                        val avatar = PrefManager.getVal(PrefName.DiscordAvatar, null as String?)
                        val username = PrefManager.getVal(PrefName.DiscordUserName, null as String?)
                        if (id != null && avatar != null) {
                            settingsDiscordAvatar.loadImage("https://cdn.discordapp.com/avatars/$id/$avatar.png")
                            settingsDiscordAvatar.setOnClickListener {
                                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                val discordLink = getString(R.string.discord_link, id)
                                openLinkInBrowser(discordLink)
                            }
                        }
                        settingsDiscordUsername.visibility = View.VISIBLE
                        settingsDiscordUsername.text =
                            username ?: Discord.token?.replace(Regex("."), "*")
                        settingsDiscordLogin.setText(R.string.logout)
                        settingsDiscordLogin.setOnClickListener {
                            Discord.removeSavedToken(context)
                            recreate()
                            reload()
                        }

                        settingsImageSwitcher.visibility = View.VISIBLE
                        var initialStatus =
                            when (PrefManager.getVal<String>(PrefName.DiscordStatus)) {
                                "online" -> R.drawable.discord_status_online
                                "idle" -> R.drawable.discord_status_idle
                                "dnd" -> R.drawable.discord_status_dnd
                                "invisible" -> R.drawable.discord_status_invisible
                                else -> R.drawable.discord_status_online
                            }
                        settingsImageSwitcher.setImageResource(initialStatus)

                        val zoomInAnimation =
                            AnimationUtils.loadAnimation(context, R.anim.bounce_zoom)
                        settingsImageSwitcher.setOnClickListener {
                            var status = "online"
                            initialStatus = when (initialStatus) {
                                R.drawable.discord_status_online -> {
                                    status = "idle"
                                    R.drawable.discord_status_idle
                                }

                                R.drawable.discord_status_idle -> {
                                    status = "dnd"
                                    R.drawable.discord_status_dnd
                                }

                                R.drawable.discord_status_dnd -> {
                                    status = "invisible"
                                    R.drawable.discord_status_invisible
                                }

                                R.drawable.discord_status_invisible -> {
                                    status = "online"
                                    R.drawable.discord_status_online
                                }

                                else -> R.drawable.discord_status_online
                            }

                            PrefManager.setVal(PrefName.DiscordStatus, status)
                            settingsImageSwitcher.setImageResource(initialStatus)
                            settingsImageSwitcher.startAnimation(zoomInAnimation)
                        }
                        settingsImageSwitcher.setOnLongClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            DiscordDialogFragment().show(supportFragmentManager, "dialog")
                            true
                        }
                    } else {
                        settingsImageSwitcher.visibility = View.GONE
                        settingsDiscordAvatar.setImageResource(R.drawable.ic_round_person_32)
                        settingsDiscordUsername.visibility = View.GONE
                        settingsDiscordLogin.setText(R.string.login)
                        settingsDiscordLogin.setOnClickListener {
                            Discord.warning(context)
                                .show(supportFragmentManager, "dialog")
                        }
                    }
                }
                reload()
            }

            if (!BuildConfig.FLAVOR.contains("fdroid")) {
                binding.settingsLogo.setOnLongClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    lifecycleScope.launch(Dispatchers.IO) {
                        MatagiUpdater.check(this@SettingsActivity, true)
                    }
                    true
                }
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
