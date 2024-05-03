package ani.dantotsu.settings

import ani.dantotsu.databinding.ItemSettingsBinding
import ani.dantotsu.databinding.ItemSettingsSwitchBinding
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName

data class Settings(
    val type: SettingsView,
    val name: String,
    val desc: String,
    val icon: Int,
    val pref: PrefName? = null,
    val onClick: ((ItemSettingsBinding) -> Unit)? = null,
    val onLongClick: (() -> Unit)? = null,
    val switch: ((isChecked: Boolean, view: ItemSettingsSwitchBinding) -> Unit)? = pref?.let {
        { isChecked, _ -> PrefManager.setVal(PrefName.SettingsPreferDub, isChecked) } },
    val attach: ((ItemSettingsBinding) -> Unit)? = null,
    val attachToSwitch: ((ItemSettingsSwitchBinding) -> Unit)? = null,
    val isVisible: Boolean = true,
    val isActivity: Boolean = false,
    var isChecked: Boolean = pref?.let { PrefManager.getVal(it) } ?: false
)
enum class SettingsView {
    BUTTON,
    SWITCH
}