package ani.dantotsu.settings.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ActivitySettingsAboutBinding
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.pop
import ani.dantotsu.settings.FAQActivity
import ani.dantotsu.settings.Settings
import ani.dantotsu.settings.SettingsActivity
import ani.dantotsu.settings.SettingsAdapter
import ani.dantotsu.settings.SettingsView
import ani.dantotsu.settings.extension.DevelopersDialogFragment
import ani.dantotsu.settings.extension.ForksDialogFragment
import ani.dantotsu.toast
import ani.dantotsu.view.dialog.CustomBottomDialog
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import kotlinx.coroutines.launch

class SettingsAboutFragment : Fragment() {
    private lateinit var binding: ActivitySettingsAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = requireActivity() as SettingsActivity

        binding.apply {
            aboutSettingsBack.setOnClickListener {
                settings.backToMenu()
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.account_help),
                        icon = R.drawable.ic_round_help_24,
                        onClick = {
                            val title = getString(R.string.account_help)
                            val full = getString(R.string.full_account_help)
                            CustomBottomDialog.newInstance().apply {
                                setTitleText(title)
                                addView(
                                    TextView(settings).apply {
                                        val markWon = Markwon.builder(settings)
                                            .usePlugin(SoftBreakAddsNewLinePlugin.create()).build()
                                        markWon.setMarkdown(this, full)
                                    }
                                )
                            }.show(settings.supportFragmentManager, "dialog")
                        },
                        isActivity = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.faq),
                        desc = getString(R.string.faq_desc),
                        icon = R.drawable.ic_round_help_24,
                        onClick = {
                            startActivity(Intent(settings, FAQActivity::class.java))
                        },
                        hasTransition = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.devs),
                        desc = getString(R.string.devs_desc),
                        icon = R.drawable.ic_round_accessible_forward_24,
                        onClick = {
                            DevelopersDialogFragment().show(settings.supportFragmentManager, "dialog")
                        },
                        isActivity = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.forks),
                        desc = getString(R.string.forks_desc),
                        icon = R.drawable.ic_round_restaurant_24,
                        onClick = {
                            ForksDialogFragment().show(settings.supportFragmentManager, "dialog")
                        },
                        isActivity = true
                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.disclaimer),
                        desc = getString(R.string.disclaimer_desc),
                        icon = R.drawable.ic_round_info_24,
                        onClick = {
                            val text = TextView(settings)
                            text.setText(R.string.full_disclaimer)

                            CustomBottomDialog.newInstance().apply {
                                setTitleText(settings.getString(R.string.disclaimer))
                                addView(text)
                                setNegativeButton(settings.getString(R.string.close)) {
                                    dismiss()
                                }
                                show(settings.supportFragmentManager, "dialog")
                            }
                        },
                        isActivity = true
                    ),
                )
            )
            binding.settingsRecyclerView.layoutManager =
                LinearLayoutManager(settings, LinearLayoutManager.VERTICAL, false)

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
            loginGitlab.setOnClickListener {
                openLinkInBrowser(getString(R.string.gitlab, getString(R.string.repo_gl)))
            }
            binding.loginHimitsu.setOnClickListener {
                openLinkInBrowser(getString(R.string.dantotsu))
            }

            settingsVersion.apply {
                text = getString(R.string.version_current, BuildConfig.COMMIT)

                setOnLongClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    copyToClipboard(SettingsActivity.getDeviceInfo(), false)
                    toast(getString(R.string.copied_device_info))
                    return@setOnLongClickListener true
                }
            }
        }
    }
}
