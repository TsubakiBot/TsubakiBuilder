package ani.dantotsu.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.currContext
import ani.dantotsu.databinding.ActivitySettingsAboutBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.others.AppUpdater
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.restartApp
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
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
            settingsAboutTitle.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

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
                val title = getString(R.string.disclaimer)
                val text = TextView(this@SettingsAboutActivity)
                text.setText(R.string.full_disclaimer)

                CustomBottomDialog.newInstance().apply {
                    setTitleText(title)
                    addView(text)
                    setNegativeButton(currContext()!!.getString(R.string.close)) {
                        dismiss()
                    }
                    show(supportFragmentManager, "dialog")
                }
            }
        }
    }
}