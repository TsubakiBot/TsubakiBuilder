package ani.dantotsu.settings.fragment

import android.annotation.SuppressLint
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.databinding.ActivitySettingsMainBinding
import ani.dantotsu.loadImage
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.openLinkInYouTube
import ani.dantotsu.refresh
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.Page
import ani.dantotsu.settings.Settings
import ani.dantotsu.settings.SettingsActivity
import ani.dantotsu.settings.SettingsAdapter
import ani.dantotsu.settings.ViewType
import ani.dantotsu.settings.extension.DiscordDialogFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.toast
import bit.himitsu.update.MatagiUpdater
import bit.himitsu.webkit.ChromeIntegration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SettingsMainFragment : Fragment() {
    lateinit var binding: ActivitySettingsMainBinding
    private var cursedCounter = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = requireActivity() as SettingsActivity

        binding.apply {
            settingsBack.setOnClickListener { settings.refresh() }

            binding.settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.anilist),
                        desc = getString(R.string.ani_setting_desc),
                        icon = R.drawable.ic_anilist,
                        onClick = {
                            ChromeIntegration.openCustomTab(
                                requireContext(), "https://anilist.co/settings"
                            )
                        },
                        isActivity = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.ui_settings),
                        desc = getString(R.string.ui_settings_desc),
                        icon = R.drawable.ic_round_auto_awesome_24,
                        onClick = {
                            settings.setFragment(Page.UI)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.theme),
                        desc = getString(R.string.theme_desc),
                        icon = R.drawable.ic_palette,
                        onClick = {
                            settings.setFragment(Page.THEME)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.common),
                        desc = getString(R.string.common_desc),
                        icon = R.drawable.ic_lightbulb_24,
                        onClick = {
                            settings.setFragment(Page.COMMON)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.anime),
                        desc = getString(R.string.anime_desc),
                        icon = R.drawable.ic_round_movie_filter_24,
                        onClick = {
                            settings.setFragment(Page.ANIME)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.manga),
                        desc = getString(R.string.manga_desc),
                        icon = R.drawable.ic_round_import_contacts_24,
                        onClick = {
                            settings.setFragment(Page.MANGA)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.extensions),
                        desc = getString(R.string.extensions_desc),
                        icon = R.drawable.ic_extension,
                        onClick = {
                            settings.setFragment(Page.EXTENSION)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.addons),
                        desc = getString(R.string.addons_desc),
                        icon = R.drawable.ic_sports_kabaddi_24,
                        onClick = {
                            settings.setFragment(Page.ADDON)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.notifications),
                        desc = getString(R.string.notifications_desc),
                        icon = R.drawable.ic_round_notifications_none_24,
                        onClick = {
                            settings.setFragment(Page.NOTIFICATION)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.system),
                        desc = getString(R.string.system_desc),
                        icon = R.drawable.ic_admin_panel_settings_24,
                        onClick = {
                            settings.setFragment(Page.SYSTEM)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.about),
                        desc = getString(R.string.about_desc),
                        icon = R.drawable.ic_round_info_24,
                        onClick = {
                            settings.setFragment(Page.ABOUT)
                        },
                        hasTransition = true
                    )
                )
            )

            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(settings, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }

            binding.apply {
                fun reload() {
                    if (Anilist.token != null) {
                        settingsAnilistLogin.setText(R.string.logout)
                        settingsAnilistLogin.setOnClickListener {
                            Anilist.removeSavedToken()
                            settings.recreate()
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
                                settings.recreate()
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
                                MAL.loginIntent(settings)
                            }
                        }
                    } else {
                        settingsAnilistAvatar.setImageResource(R.drawable.ic_round_person_32)
                        settingsAnilistUsername.visibility = View.GONE
                        settingsAnilistLogin.setText(R.string.login)
                        settingsAnilistLogin.setOnClickListener {
                            Anilist.loginIntent(settings)
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
                            Discord.removeSavedToken(settings)
                            settings.recreate()
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
                            AnimationUtils.loadAnimation(settings, R.anim.bounce_zoom)
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
                            DiscordDialogFragment().show(settings.supportFragmentManager, "dialog")
                            true
                        }
                    } else {
                        settingsImageSwitcher.visibility = View.GONE
                        settingsDiscordAvatar.setImageResource(R.drawable.ic_round_person_32)
                        settingsDiscordUsername.visibility = View.GONE
                        settingsDiscordLogin.setText(R.string.login)
                        settingsDiscordLogin.setOnClickListener {
                            Discord.warning(settings)
                                .show(settings.supportFragmentManager, "dialog")
                        }
                    }
                }
                reload()
            }

            if (!BuildConfig.FLAVOR.contains("fdroid")) {
                binding.settingsLogo.setOnLongClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    lifecycleScope.launch(Dispatchers.IO) {
                        MatagiUpdater.check(settings, true)
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
                    snackString(array[(Math.random() * array.size).toInt()], settings)
                }
            }
        }
    }
}
