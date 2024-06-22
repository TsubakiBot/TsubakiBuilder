package ani.dantotsu.settings

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import ani.dantotsu.MainActivity
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.BottomSheetSettingsBinding
import ani.dantotsu.download.anime.OfflineAnimeFragment
import ani.dantotsu.download.manga.OfflineMangaFragment
import ani.dantotsu.home.AnimeFragment
import ani.dantotsu.home.HomeFragment
import ani.dantotsu.home.LoginFragment
import ani.dantotsu.home.MangaFragment
import ani.dantotsu.home.NoInternet
import ani.dantotsu.incognitoNotification
import ani.dantotsu.loadImage
import ani.dantotsu.notifications.NotificationActivity
import ani.dantotsu.offline.OfflineFragment
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.activity.FeedActivity
import ani.dantotsu.refresh
import ani.dantotsu.setNavigationTheme
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.customAlertDialog
import ani.dantotsu.view.dialog.BottomSheetDialogFragment
import bit.himitsu.onCompletedAction
import bit.himitsu.search.ReverseSearchDialogFragment
import bit.himitsu.update.MatagiUpdater
import eu.kanade.tachiyomi.util.system.getSerializableCompat
import java.util.Timer
import kotlin.concurrent.schedule

class SettingsDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSettingsBinding? = null
    private val binding by lazy { _binding!! }

    private lateinit var pageType: PageType
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageType = arguments?.getSerializableCompat("pageType") as? PageType ?: PageType.HOME
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window = dialog?.window
        window?.statusBarColor = Color.CYAN
        window?.setNavigationTheme(requireContext())
        val notificationIcon = if (Anilist.unreadNotificationCount > 0) {
            R.drawable.ic_round_notifications_active_24
        } else {
            R.drawable.ic_round_notifications_none_24
        }
        binding.settingsNotification.setImageResource(notificationIcon)

        if (Anilist.token != null) {
            binding.settingsLogin.setText(R.string.logout)
            binding.settingsLogin.setOnClickListener {
                requireContext().customAlertDialog().apply{
                    setTitle(R.string.logout)
                    setMessage(R.string.logout_confirm)
                    setPosButton(R.string.yes) {
                        Anilist.removeSavedToken()
                        dismiss()
                       requireActivity().refresh()
                    }
                    setNegButton(R.string.no)
                    show()
                }
            }
            binding.settingsUsername.text = Anilist.username
            binding.settingsUserAvatar.loadImage(Anilist.avatar)
        } else {
            binding.settingsUsername.visibility = View.GONE
            binding.settingsLogin.setText(R.string.login)
            binding.settingsLogin.setOnClickListener {
                dismiss()
                Anilist.loginIntent(requireActivity())
            }
        }

        val count = Anilist.unreadNotificationCount + MatagiUpdater.hasUpdate
        binding.settingsNotificationCount.isVisible = count > 0
        binding.settingsNotificationCount.text = count.toString()
        binding.settingsUserAvatar.setOnClickListener {
            ContextCompat.startActivity(
                requireContext(), Intent(requireContext(), ProfileActivity::class.java)
                    .putExtra("userId", Anilist.userid), null
            )
        }

        fun search(query: String) {
            ReverseSearchDialogFragment(query).show(
                requireActivity().supportFragmentManager, null
            )
            dismiss()
        }

        binding.searchView.setEndIconOnClickListener {
            search(binding.searchViewText.text.toString())
        }

        binding.searchViewText.setOnEditorActionListener(onCompletedAction {
            search(binding.searchViewText.text.toString())
        })

        binding.settingsHiddenOption.setSafeOnClickListener {
            dismiss()
        }

        binding.settingsActivity.setSafeOnClickListener {
            startActivity(Intent(activity, FeedActivity::class.java))
            dismiss()
        }

        binding.settingsExtensionSettings.setSafeOnClickListener {
            startActivity(Intent(activity, ExtensionsActivity::class.java))
            dismiss()
        }

        binding.settingsExtensionSettings.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            binding.searchView.isVisible = binding.searchView.isGone
            true
        }

        binding.settingsSettings.setSafeOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
            dismiss()
        }

        binding.settingsNotification.setOnClickListener {
            startActivity(Intent(activity, NotificationActivity::class.java))
            dismiss()
        }
        binding.settingsNotification.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            binding.settingsHiddenOption.isVisible = binding.settingsHiddenOption.isGone
            true
        }

        binding.settingsIncognito.isChecked = PrefManager.getVal(PrefName.Incognito)
        binding.settingsIncognito.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.Incognito, isChecked)
            incognitoNotification(requireContext())
        }

        binding.settingsDownloads.isChecked = PrefManager.getVal(PrefName.OfflineMode)
        binding.settingsDownloads.setOnCheckedChangeListener { _, isChecked ->
            Timer().schedule(300) {
                when (pageType) {
                    PageType.MANGA -> {
                        val intent = Intent(activity, NoInternet::class.java)
                        intent.putExtra(
                            "FRAGMENT_CLASS_NAME",
                            OfflineMangaFragment::class.java.name
                        )
                        startActivity(intent)
                    }

                    PageType.ANIME -> {
                        val intent = Intent(activity, NoInternet::class.java)
                        intent.putExtra(
                            "FRAGMENT_CLASS_NAME",
                            OfflineAnimeFragment::class.java.name
                        )
                        startActivity(intent)
                    }

                    PageType.HOME -> {
                        val intent = Intent(activity, NoInternet::class.java)
                        intent.putExtra("FRAGMENT_CLASS_NAME", OfflineFragment::class.java.name)
                        startActivity(intent)
                    }

                    PageType.OfflineMANGA -> {
                        val intent = Intent(activity, MainActivity::class.java)
                        intent.putExtra("FRAGMENT_CLASS_NAME", MangaFragment::class.java.name)
                        startActivity(intent)
                    }

                    PageType.OfflineHOME -> {
                        val intent = Intent(activity, MainActivity::class.java)
                        intent.putExtra(
                            "FRAGMENT_CLASS_NAME",
                            if (Anilist.token != null) HomeFragment::class.java.name else LoginFragment::class.java.name
                        )
                        startActivity(intent)
                    }

                    PageType.OfflineANIME -> {
                        val intent = Intent(activity, MainActivity::class.java)
                        intent.putExtra("FRAGMENT_CLASS_NAME", AnimeFragment::class.java.name)
                        startActivity(intent)
                    }
                }

                dismiss()
                PrefManager.setVal(PrefName.OfflineMode, isChecked)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        enum class PageType {
            MANGA, ANIME, HOME, OfflineMANGA, OfflineANIME, OfflineHOME
        }

        fun newInstance(pageType: PageType): SettingsDialogFragment {
            val fragment = SettingsDialogFragment()
            val args = Bundle()
            args.putSerializable("pageType", pageType)
            fragment.arguments = args
            return fragment
        }
    }
}
