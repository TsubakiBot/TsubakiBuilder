package ani.dantotsu.settings.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.BuildConfig
import ani.dantotsu.Himitsu
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.databinding.ActivitySettingsSystemBinding
import ani.dantotsu.restart
import ani.dantotsu.savePrefsToDownloads
import ani.dantotsu.settings.Settings
import ani.dantotsu.settings.SettingsActivity
import ani.dantotsu.settings.SettingsAdapter
import ani.dantotsu.settings.ViewType
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.internal.Location
import ani.dantotsu.settings.saving.internal.PreferenceKeystore
import ani.dantotsu.settings.saving.internal.PreferencePackager
import ani.dantotsu.snackString
import ani.dantotsu.toast
import ani.dantotsu.util.Logger
import ani.dantotsu.util.StoragePermissions
import bit.himitsu.update.MatagiUpdater
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsSystemFragment : Fragment() {
    private lateinit var binding: ActivitySettingsSystemBinding


    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val settings = requireActivity() as SettingsActivity
            val component = ComponentName(settings.packageName, settings::class.qualifiedName!!)
            if (uri != null) {
                try {
                    val jsonString = settings.contentResolver.openInputStream(uri)?.readBytes()
                        ?: throw Exception("Error reading file")
                    val name = DocumentFile.fromSingleUri(settings, uri)?.name ?: "settings"
                    //.sani is encrypted, .ani is not
                    if (name.endsWith(".sani")
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        passwordAlertDialog(false) { password ->
                            if (password != null) {
                                val salt = jsonString.copyOfRange(0, 16)
                                val encrypted = jsonString.copyOfRange(16, jsonString.size)
                                val decryptedJson = try {
                                    PreferenceKeystore.decryptWithPassword(
                                        password,
                                        encrypted,
                                        salt
                                    )
                                } catch (e: Exception) {
                                    toast(getString(R.string.incorrect_password))
                                    return@passwordAlertDialog
                                }
                                if (PreferencePackager.unpack(decryptedJson))
                                    settings.restart(component)
                            } else {
                                toast(getString(R.string.password_cannot_be_empty))
                            }
                        }
                    } else if (name.endsWith(".ani")) {
                        val decryptedJson = jsonString.toString(Charsets.UTF_8)
                        if (PreferencePackager.unpack(decryptedJson))
                            settings.restart(component)
                    } else {
                        toast(getString(R.string.unknown_file_type))
                    }
                } catch (e: Exception) {
                    Logger.log(e)
                    toast(getString(R.string.error_importing_settings))
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsSystemBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = requireActivity() as SettingsActivity
        val component = ComponentName(settings.packageName, settings::class.qualifiedName!!)

        binding.apply {
            systemSettingsBack.setOnClickListener {
                settings.backToMenu()
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = ViewType.BUTTON,
                        name = getString(R.string.backup_restore),
                        desc = getString(R.string.backup_restore_desc),
                        icon = R.drawable.backup_restore,
                        onClick = {
                            StoragePermissions.downloadsPermission(settings as AppCompatActivity)
                            val selectedArray = mutableListOf(false)
                            var filteredLocations = Location.entries.filter { it.exportable }
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                filteredLocations = filteredLocations.filter {
                                    it.location != Location.Protected.location
                                }
                            }
                            selectedArray.addAll(List(filteredLocations.size - 1) { false })
                            val dialog =
                                AlertDialog.Builder(settings, R.style.MyPopup)
                                    .setTitle(R.string.backup_restore)
                                    .setMultiChoiceItems(
                                        filteredLocations.map { it.name }.toTypedArray(),
                                        selectedArray.toBooleanArray()
                                    ) { _, which, isChecked ->
                                        selectedArray[which] = isChecked
                                    }
                                    .setPositiveButton(R.string.button_restore) { dialog, _ ->
                                        openDocumentLauncher.launch(arrayOf("*/*"))
                                        dialog.dismiss()
                                    }
                                    .setNegativeButton(R.string.button_backup) { dialog, _ ->
                                        if (!selectedArray.contains(true)) {
                                            toast(R.string.no_location_selected)
                                            return@setNegativeButton
                                        }
                                        dialog.dismiss()
                                        val selected =
                                            filteredLocations.filterIndexed { index, _ -> selectedArray[index] }
                                        if (selected.contains(Location.Protected)) {
                                            passwordAlertDialog(true) { password ->
                                                if (password != null) {
                                                    savePrefsToDownloads(
                                                        "DantotsuSettings",
                                                        PrefManager.exportAllPrefs(selected),
                                                        settings,
                                                        password
                                                    )
                                                } else {
                                                    toast(R.string.password_cannot_be_empty)
                                                }
                                            }
                                        } else {
                                            savePrefsToDownloads(
                                                "DantotsuSettings",
                                                PrefManager.exportAllPrefs(selected),
                                                settings,
                                                null
                                            )
                                        }
                                    }
                                    .setNeutralButton(R.string.cancel) { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .create()
                            dialog.window?.setDimAmount(0.8f)
                            dialog.show()
                        },
                        isDialog = true
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.check_app_updates),
                        desc = getString(R.string.check_app_updates_desc),
                        icon = R.drawable.ic_round_new_releases_24,
                        isChecked = PrefManager.getVal(PrefName.CheckUpdate),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.CheckUpdate, isChecked)
                            if (!isChecked) {
                                snackString(getString(R.string.long_click_to_check_update))
                            }
                        },
                        onLongClick = {
                            lifecycleScope.launch(Dispatchers.IO) {
                                MatagiUpdater.check(settings, true)
                            }
                        },
                        isVisible = !BuildConfig.FLAVOR.contains("fdroid")
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.biometric_title),
                        desc = getString(R.string.biometric_summary),
                        icon = R.drawable.ic_fingerprint_24,
                        pref = PrefName.SecureLock,
                        isVisible = canUseBiometrics()
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.add_shortcuts),
                        desc = getString(R.string.add_shortcuts_desc),
                        icon = R.drawable.ic_app_shortcut_24,
                        isChecked = PrefManager.getVal(PrefName.UseShortcuts),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.UseShortcuts, isChecked)
                            settings.restart(component)
                        }
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.comments_api),
                        desc = getString(R.string.comments_api_desc),
                        icon = R.drawable.ic_round_comment_24,
                        pref = PrefName.CommentsOptIn
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.disable_mitm),
                        desc = getString(R.string.disable_mitm_desc),
                        icon = R.drawable.ic_round_coronavirus_24,
                        pref = PrefName.DisableMitM,
                        switch = { isChecked, _ ->
                            if (isChecked) {
                                PrefManager.removeVal(PrefName.ImageUrl)
                                    CoroutineScope(Dispatchers.IO).launch {
                                        Glide.get(Himitsu.instance).clearDiskCache()
                                    }
                                    Glide.get(Himitsu.instance).clearMemory()
                            }
                            Refresh.all()
                        }
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.share_username_in_logs),
                        desc = getString(R.string.share_username_in_logs_desc),
                        icon = R.drawable.ic_round_search_24,
                        pref = PrefName.SharedUserID,
                        isVisible = !BuildConfig.FLAVOR.contains("fdroid")
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.log_to_file),
                        desc = getString(R.string.logging_warning),
                        icon = R.drawable.ic_round_edit_note_24,
                        isChecked = PrefManager.getVal(PrefName.LogToFile),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.LogToFile, isChecked)
                            Logger.clearLog()
                            settings.restart(component)
                        }

                    ),
                    Settings(
                        type = ViewType.BUTTON,
                        name = "",
                        desc = getString(R.string.share_log),
                        icon = R.drawable.ic_round_share_24,
                        onClick = {
                            Logger.shareLog(settings)
                        }
                    ),
                    Settings(
                        type = ViewType.SWITCH,
                        name = getString(R.string.disable_debug),
                        desc = getString(R.string.rogue_warning),
                        icon = R.drawable.ic_bug_report_24,
                        isChecked = PrefManager.getVal(PrefName.Lightspeed),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.Lightspeed, isChecked)
                            Logger.clearLog()
                            settings.restart(component)
                        }
                    )
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(settings, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }
        }
    }

    private fun canUseBiometrics() : Boolean {
        val biometricManager = BiometricManager.from(requireActivity())
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG
                or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> true
            else -> { false }
        }
    }

    private fun passwordAlertDialog(isExporting: Boolean, callback: (CharArray?) -> Unit) {
        val password = CharArray(16).apply { fill('0') }

        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_user_agent, null)
        val box = dialogView.findViewById<TextInputEditText>(R.id.userAgentTextBox)
        box?.hint = getString(R.string.password)
        box?.setSingleLine()

        val dialog = AlertDialog.Builder(context, R.style.MyPopup)
            .setTitle(getString(R.string.enter_password))
            .setView(dialogView)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                password.fill('0')
                dialog.dismiss()
                callback(null)
            }
            .create()

        fun handleOkAction() {
            val editText = dialog.findViewById<TextInputEditText>(R.id.userAgentTextBox)
            if (editText?.text?.isNotBlank() == true) {
                editText.text?.toString()?.trim()?.toCharArray(password)
                dialog.dismiss()
                callback(password)
            } else {
                toast(getString(R.string.password_cannot_be_empty))
            }
        }
        box?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleOkAction()
                true
            } else {
                false
            }
        }
        val subtitleTextView = dialogView.findViewById<TextView>(R.id.subtitle)
        subtitleTextView?.visibility = View.VISIBLE
        if (!isExporting)
            subtitleTextView?.text = getString(R.string.enter_password_to_decrypt_file)


        dialog.window?.setDimAmount(0.8f)
        dialog.show()

        // Override the positive button here
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            handleOkAction()
        }
    }
}