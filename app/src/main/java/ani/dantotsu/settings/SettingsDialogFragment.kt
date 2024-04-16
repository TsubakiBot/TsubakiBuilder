package ani.dantotsu.settings

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ani.dantotsu.MainActivity
import ani.dantotsu.R
import ani.dantotsu.Refresh
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
import ani.dantotsu.offline.OfflineFragment
import ani.dantotsu.others.BottomSheetDialogFragment
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.activity.FeedActivity
import ani.dantotsu.profile.activity.NotificationActivity
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.matagi.update.MatagiUpdater
import com.bumptech.glide.Glide
import eu.kanade.tachiyomi.util.system.getSerializableCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.schedule

class SettingsDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!

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
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window?.navigationBarColor = typedValue.data
        val notificationIcon = if (Anilist.unreadNotificationCount > 0) {
            R.drawable.ic_round_notifications_active_24
        } else {
            R.drawable.ic_round_notifications_none_24
        }
        binding.settingsNotification.setImageResource(notificationIcon)

        if (Anilist.token != null) {
            binding.settingsLogin.setText(R.string.logout)
            binding.settingsLogin.setOnClickListener {
                val alertDialog = AlertDialog.Builder(requireContext(), R.style.MyPopup)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    Anilist.removeSavedToken()
                    dismiss()
                    requireActivity().recreate()
                }
                .setNegativeButton("No") { _, _ -> }
                .create()
                alertDialog.window?.setDimAmount(0.8f)
                alertDialog.show()
            }
            binding.settingsUsername.text = Anilist.username
            binding.settingsUserAvatar.loadImage(Anilist.avatar)
        } else {
            binding.settingsUsername.visibility = View.GONE
            binding.settingsLogin.setText(R.string.sign_in)
            binding.settingsLogin.setOnClickListener {
                dismiss()
                Anilist.loginIntent(requireActivity())
            }
        }

        val count = Anilist.unreadNotificationCount + MatagiUpdater.hasUpdate
        binding.settingsNotificationCount.isVisible = count > 0
        binding.settingsNotificationCount.text = count.toString()
        binding.settingsUserAvatar.setOnClickListener{
            ContextCompat.startActivity(
                requireContext(), Intent(requireContext(), ProfileActivity::class.java)
                    .putExtra("userId", Anilist.userid), null
            )
        }

        binding.settingsIncognito.isChecked = PrefManager.getVal(PrefName.Incognito)
        binding.settingsIncognito.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.Incognito, isChecked)
            incognitoNotification(requireContext())
        }

        binding.hidePreventInjection.isChecked = PrefManager.getVal(PrefName.DisableMitM)
        binding.hidePreventInjection.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.DisableMitM, isChecked)
            if (isChecked) {
                PrefManager.removeVal(PrefName.ImageUrl)
                activity?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        Glide.get(it).clearDiskCache()
                    }
                    Glide.get(it).clearMemory()
                }
            }
            Refresh.all()
            requireActivity().recreate()
        }

        binding.settingsExtensionSettings.setSafeOnClickListener {
            startActivity(Intent(activity, ExtensionsActivity::class.java))
            dismiss()
        }

        binding.settingsSettings.setSafeOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
            dismiss()
        }

        binding.settingsActivity.setSafeOnClickListener {
            startActivity(Intent(activity, FeedActivity::class.java))
            dismiss()
        }

        binding.settingsNotification.setOnClickListener {
            startActivity(Intent(activity, NotificationActivity::class.java))
            dismiss()
        }
        binding.settingsNotification.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            binding.hidePreventInjection.isVisible = true
            true
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
