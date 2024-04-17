package ani.dantotsu.settings

import android.view.ViewGroup
import ani.dantotsu.databinding.ItemSettingsBinding
import ani.dantotsu.databinding.ItemSettingsSwitchBinding

data class Settings(
    val type: SettingsView,
    val name : String,
    val desc: String,
    val icon : Int,
    val onClick: ((ItemSettingsBinding) -> Unit)? = null,
    val onLongClick: (() -> Unit)? = null,
    val switch: ((isChecked:Boolean , view: ItemSettingsSwitchBinding ) -> Unit)? = null,
    val attach:((ItemSettingsBinding) -> Unit)? = null,
    val isVisible: Boolean = true,
    val isActivity: Boolean = false,
    var isChecked : Boolean = false
)

enum class SettingsView {
    BUTTON,
    SWITCH
}