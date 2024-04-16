package ani.dantotsu.settings

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.comments.CommentsAPI
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.databinding.ActivitySettingsAccountsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.loadImage
import ani.dantotsu.navBarHeight
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsAccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsAccountsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this

        binding = ActivitySettingsAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            settingsAccountLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            onBackPressedDispatcher.addCallback(this@SettingsAccountActivity) {
                startActivity(Intent(this@SettingsAccountActivity, SettingsActivity::class.java))
                finish()
            }
            accountSettingsBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

            settingsCommentsApi.isChecked = PrefManager.getVal(PrefName.CommentsOptIn)
            settingsCommentsApi.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.CommentsOptIn, isChecked)
                if (isChecked)
                    CoroutineScope(Dispatchers.IO).launch { CommentsAPI.fetchAuthToken() }
                else
                    CommentsAPI.logout()
            }

            settingsAccountHelp.setOnClickListener {
                val title = getString(R.string.account_help)
                val full = getString(R.string.full_account_help)
                CustomBottomDialog.newInstance().apply {
                    setTitleText(title)
                    addView(
                        TextView(it.context).apply {
                            val markWon = Markwon.builder(it.context)
                                .usePlugin(SoftBreakAddsNewLinePlugin.create()).build()
                            markWon.setMarkdown(this, full)
                        }
                    )
                }.show(supportFragmentManager, "dialog")
            }

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
                        settingsMALLogin.setText(R.string.sign_in)
                        settingsMALLogin.setOnClickListener {
                            MAL.loginIntent(context)
                        }
                    }
                } else {
                    settingsAnilistAvatar.setImageResource(R.drawable.ic_round_person_32)
                    settingsAnilistUsername.visibility = View.GONE
                    settingsAnilistLogin.setText(R.string.sign_in)
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
                    var initialStatus = when (PrefManager.getVal<String>(PrefName.DiscordStatus)) {
                        "online" -> R.drawable.discord_status_online
                        "idle" -> R.drawable.discord_status_idle
                        "dnd" -> R.drawable.discord_status_dnd
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
                    settingsDiscordLogin.setText(R.string.sign_in)
                    settingsDiscordLogin.setOnClickListener {
                        Discord.warning(context)
                            .show(supportFragmentManager, "dialog")
                    }
                }
            }
            reload()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}