package ani.dantotsu.settings

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ActivitySettingsAboutBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.pop
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

            settingsFAQ.setOnClickListener {
                startActivity(Intent(this@SettingsAboutActivity, FAQActivity::class.java))
            }

            settingsDev.setOnClickListener {
                DevelopersDialogFragment().show(supportFragmentManager, "dialog")
            }
            settingsForks.setOnClickListener {
                ForksDialogFragment().show(supportFragmentManager, "dialog")
            }

            settingsDisclaimer.setOnClickListener {
                val text = TextView(this@SettingsAboutActivity).apply {
                    setText(R.string.full_disclaimer)
                }

                CustomBottomDialog.newInstance().apply {
                    setTitleText(context.getString(R.string.disclaimer))
                    addView(text)
                    setNegativeButton(context.getString(R.string.close)) {
                        dismiss()
                    }
                    show(supportFragmentManager, "dialog")
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

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
