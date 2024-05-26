package ani.dantotsu.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.updateLayoutParams
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
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.fragment.DiscordDialogFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.startMainActivity
import ani.dantotsu.statusBarHeight
import ani.dantotsu.toast
import bit.himitsu.update.MatagiUpdater
import bit.himitsu.withFlexibleMargin
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
        val context = requireActivity() as SettingsActivity

        binding.apply {
            settingsBack.setOnClickListener {
                startMainActivity(context)
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
                            context.finish()
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.theme),
                        desc = getString(R.string.theme_desc),
                        icon = R.drawable.ic_palette,
                        onClick = {
                            (context as SettingsActivity).setFragment(1)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.common),
                        desc = getString(R.string.common_desc),
                        icon = R.drawable.ic_lightbulb_24,
                        onClick = {
                            (context as SettingsActivity).setFragment(2)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.anime),
                        desc = getString(R.string.anime_desc),
                        icon = R.drawable.ic_round_movie_filter_24,
                        onClick = {
                            (context as SettingsActivity).setFragment(3)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.manga),
                        desc = getString(R.string.manga_desc),
                        icon = R.drawable.ic_round_import_contacts_24,
                        onClick = {
                            (context as SettingsActivity).setFragment(4)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.extensions),
                        desc = getString(R.string.extensions_desc),
                        icon = R.drawable.ic_extension,
                        onClick = {
                            (context as SettingsActivity).setFragment(5)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.addons),
                        desc = getString(R.string.addons_desc),
                        icon = R.drawable.ic_round_restaurant_24,
                        onClick = {
                            (context as SettingsActivity).setFragment(6)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.notifications),
                        desc = getString(R.string.notifications_desc),
                        icon = R.drawable.ic_round_notifications_none_24,
                        onClick = {
                            (context as SettingsActivity).setFragment(7)
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.system),
                        desc = getString(R.string.system_desc),
                        icon = R.drawable.ic_admin_panel_settings_24,
                        onClick = {
                            (context as SettingsActivity).setFragment(8)
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
                            (context as SettingsActivity).setFragment(9)
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
                            context.recreate()
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
                                context.recreate()
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
                            context.recreate()
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
                            DiscordDialogFragment().show(context.supportFragmentManager, "dialog")
                            true
                        }
                    } else {
                        settingsImageSwitcher.visibility = View.GONE
                        settingsDiscordAvatar.setImageResource(R.drawable.ic_round_person_32)
                        settingsDiscordUsername.visibility = View.GONE
                        settingsDiscordLogin.setText(R.string.login)
                        settingsDiscordLogin.setOnClickListener {
                            Discord.warning(context)
                                .show(context.supportFragmentManager, "dialog")
                        }
                    }
                }
                reload()
            }

            if (!BuildConfig.FLAVOR.contains("fdroid")) {
                binding.settingsLogo.setOnLongClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    lifecycleScope.launch(Dispatchers.IO) {
                        MatagiUpdater.check(context, true)
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
                    snackString(array[(Math.random() * array.size).toInt()], context)
                }
            }
        }
    }
}
