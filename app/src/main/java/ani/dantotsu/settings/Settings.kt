package ani.dantotsu.settings

import ani.dantotsu.databinding.ItemSettingsBinding
import ani.dantotsu.databinding.ItemSettingsSliderBinding
import ani.dantotsu.databinding.ItemSettingsSwitchBinding
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName

data class Settings(
    val type: SettingsView,
    val name: String,
    val desc: String = "",
    val icon: Int,
    val pref: PrefName? = null,
    val onClick: ((ItemSettingsBinding) -> Unit)? = null,
    val onLongClick: (() -> Unit)? = null,
    val switch: ((isChecked: Boolean, view: ItemSettingsSwitchBinding) -> Unit)? = pref?.let {
        { isChecked, _ -> PrefManager.setVal(pref, isChecked) } },
    val slider: ((value: Float, view: ItemSettingsSliderBinding) -> Unit)? = pref?.let {
        { value, _ -> PrefManager.setVal(pref, value) } },
    val attach: ((ItemSettingsBinding) -> Unit)? = null,
    val attachToSwitch: ((ItemSettingsSwitchBinding) -> Unit)? = null,
    val attachToSlider: ((ItemSettingsSliderBinding) -> Unit)? = null,
    val isVisible: Boolean = true,
    val hasTransition: Boolean = false,
    var isChecked: Boolean = pref?.let {
        item -> PrefManager.getVal<Any>(item).takeIf { it is Boolean } as? Boolean
    } ?: false,
    val stepSize: Float = 1f,
    val valueFrom: Float = 0f,
    val valueTo: Float = 10f,
    var value: Float = pref?.let {
        item -> PrefManager.getVal<Any>(item).takeIf { it is Float } as? Float
    } ?: valueFrom
)
enum class SettingsView {
    BUTTON,
    SWITCH,
    SLIDER
}