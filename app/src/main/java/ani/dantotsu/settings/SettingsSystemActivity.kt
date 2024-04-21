package ani.dantotsu.settings

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivitySettingsSystemBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.restartApp
import ani.dantotsu.savePrefsToDownloads
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.internal.Location
import ani.dantotsu.settings.saving.internal.PreferenceKeystore
import ani.dantotsu.settings.saving.internal.PreferencePackager
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.dantotsu.util.Logger
import ani.dantotsu.util.StoragePermissions
import ani.matagi.update.MatagiUpdater
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsSystemActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsSystemBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)

        binding = ActivitySettingsSystemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val openDocumentLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    try {
                        val jsonString = contentResolver.openInputStream(uri)?.readBytes()
                            ?: throw Exception("Error reading file")
                        val name = DocumentFile.fromSingleUri(this, uri)?.name ?: "settings"
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
                                        restartApp()
                                } else {
                                    toast(getString(R.string.password_cannot_be_empty))
                                }
                            }
                        } else if (name.endsWith(".ani")) {
                            val decryptedJson = jsonString.toString(Charsets.UTF_8)
                            if (PreferencePackager.unpack(decryptedJson))
                                restartApp()
                        } else {
                            toast(getString(R.string.unknown_file_type))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toast(getString(R.string.error_importing_settings))
                    }
                }
            }

        binding.apply {
            settingsSystemLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            systemSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            var hasFoldingFeature = false
            CoroutineScope(Dispatchers.IO).launch {
                WindowInfoTracker.getOrCreate(this@SettingsSystemActivity)
                    .windowLayoutInfo(this@SettingsSystemActivity)
                    .collect { newLayoutInfo ->
                        hasFoldingFeature = newLayoutInfo.displayFeatures.find {
                            it is FoldingFeature
                        } != null
                    }
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = SettingsView.BUTTON,
                        name = getString(R.string.backup_restore),
                        desc = getString(R.string.backup_restore_desc),
                        icon = R.drawable.backup_restore,
                        onClick = {
                            StoragePermissions.downloadsPermission(this@SettingsSystemActivity)
                            val selectedArray = mutableListOf(false)
                            var filteredLocations = Location.entries.filter { it.exportable }
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                filteredLocations = filteredLocations.filter {
                                    it.location != Location.Protected.location
                                }
                            }
                            selectedArray.addAll(List(filteredLocations.size - 1) { false })
                            val dialog =
                                AlertDialog.Builder(this@SettingsSystemActivity, R.style.MyPopup)
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
                                                        this@SettingsSystemActivity,
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
                                                this@SettingsSystemActivity,
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
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.use_foldable),
                        desc = getString(R.string.use_foldable_desc),
                        icon = R.drawable.ic_devices_fold_24,
                        isChecked = PrefManager.getVal(PrefName.UseFoldable),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.UseFoldable, isChecked)
                        },
                        isVisible = hasFoldingFeature
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.add_shortcuts),
                        desc = getString(R.string.add_shortcuts_desc),
                        icon = R.drawable.ic_app_shortcut_24,
                        isChecked = PrefManager.getVal(PrefName.UseShortcuts),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.UseShortcuts, isChecked)
                            restartApp()
                        }
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
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
                                MatagiUpdater.check(this@SettingsSystemActivity, true)
                            }
                        },
                        isVisible = !BuildConfig.FLAVOR.contains("fdroid")
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.share_username_in_logs),
                        desc = getString(R.string.share_username_in_logs_desc),
                        icon = R.drawable.ic_round_search_24,
                        isChecked = PrefManager.getVal(PrefName.SharedUserID),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.SharedUserID, isChecked)
                        },
                        isVisible = !BuildConfig.FLAVOR.contains("fdroid")
                    ),
                    Settings(
                        type = SettingsView.SWITCH,
                        name = getString(R.string.log_to_file),
                        desc = getString(R.string.logging_warning),
                        icon = R.drawable.ic_round_edit_note_24,
                        isChecked = PrefManager.getVal(PrefName.LogToFile),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.LogToFile, isChecked)
                            restartApp()
                        }

                    ),
                    Settings(
                        type = SettingsView.BUTTON,
                        name = "",
                        desc = getString(R.string.share_log),
                        icon = R.drawable.ic_round_share_24,
                        onClick = {
                            Logger.shareLog(this@SettingsSystemActivity)
                        }
                    )
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }
        }
    }

    private fun passwordAlertDialog(isExporting: Boolean, callback: (CharArray?) -> Unit) {
        val password = CharArray(16).apply { fill('0') }

        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_agent, null)
        val box = dialogView.findViewById<TextInputEditText>(R.id.userAgentTextBox)
        box?.hint = getString(R.string.password)
        box?.setSingleLine()

        val dialog = AlertDialog.Builder(this, R.style.MyPopup)
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

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}