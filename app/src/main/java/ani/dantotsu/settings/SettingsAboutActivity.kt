package ani.dantotsu.settings

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ActivitySettingsAboutBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.dialog.CustomBottomDialog
import ani.dantotsu.pop
import ani.dantotsu.settings.fragment.DevelopersDialogFragment
import ani.dantotsu.settings.fragment.ForksDialogFragment
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import kotlinx.coroutines.launch

class SettingsAboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this

        binding = ActivitySettingsAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            settingsAboutLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            aboutSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }


            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.faq),
                        desc = getString(R.string.faq_desc),
                        icon = R.drawable.ic_round_help_24,
                        onClick = {
                            startActivity(Intent(context, FAQActivity::class.java))
                        },
                        isActivity = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.devs),
                        desc = getString(R.string.devs_desc),
                        icon = R.drawable.ic_round_accessible_forward_24,
                        onClick = {
                            DevelopersDialogFragment().show(supportFragmentManager, "dialog")
                        }
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.forks),
                        desc = getString(R.string.forks_desc),
                        icon = R.drawable.ic_round_restaurant_24,
                        onClick = {
                            ForksDialogFragment().show(supportFragmentManager, "dialog")
                        }
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.disclaimer),
                        desc = getString(R.string.disclaimer_desc),
                        icon = R.drawable.ic_round_info_24,
                        onClick = {
                            val text = TextView(context)
                            text.setText(R.string.full_disclaimer)

                            CustomBottomDialog.newInstance().apply {
                                setTitleText(context.getString(R.string.disclaimer))
                                addView(text)
                                setNegativeButton(context.getString(R.string.close)) {
                                    dismiss()
                                }
                                show(supportFragmentManager, "dialog")
                            }
                        }
                    ),
                )
            )
            binding.settingsRecyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

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

            settingsVersion.apply {
                text = getString(R.string.version_current, BuildConfig.VERSION_NAME)

                setOnLongClickListener {
                    copyToClipboard(SettingsActivity.getDeviceInfo(), false)
                    toast(getString(R.string.copied_device_info))
                    return@setOnLongClickListener true
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
