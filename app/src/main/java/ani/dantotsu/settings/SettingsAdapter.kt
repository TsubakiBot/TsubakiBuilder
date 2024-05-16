package ani.dantotsu.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.databinding.ItemSettingsBinding
import ani.dantotsu.databinding.ItemSettingsHeaderBinding
import ani.dantotsu.databinding.ItemSettingsSliderBinding
import ani.dantotsu.databinding.ItemSettingsSwitchBinding
import ani.dantotsu.setAnimation

class SettingsAdapter(private val settings: ArrayList<Settings>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class SettingsViewHolder(val binding: ItemSettingsBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class SettingsSwitchViewHolder(val binding: ItemSettingsSwitchBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class SettingsSliderViewHolder(val binding: ItemSettingsSliderBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class SettingsHeaderViewHolder(val binding: ItemSettingsHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            SettingsView.BUTTON.ordinal -> SettingsViewHolder(
                ItemSettingsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

            SettingsView.SWITCH.ordinal -> SettingsSwitchViewHolder(
                ItemSettingsSwitchBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

            SettingsView.SLIDER.ordinal -> SettingsSliderViewHolder(
                ItemSettingsSliderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

            SettingsView.HEADER.ordinal -> SettingsHeaderViewHolder(
                ItemSettingsHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

            else -> SettingsViewHolder(
                ItemSettingsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val settings = settings[position]
        when (settings.type) {
            SettingsView.BUTTON -> {
                val b = (holder as SettingsViewHolder).binding
                setAnimation(b.root.context, b.root)

                b.settingsTitle.text = settings.name
                b.settingsTitle.isVisible = settings.name.isNotBlank()
                b.settingsDesc.text = settings.desc
                b.settingsDesc.isVisible = settings.desc.isNotBlank()
                b.settingsIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        b.root.context, settings.icon
                    )
                )
                b.settingsLayout.setOnClickListener {
                    settings.onClick?.invoke(b)
                }
                b.settingsLayout.setOnLongClickListener {
                    settings.onLongClick?.invoke()
                    false
                }
                b.settingsLayout.isVisible = settings.isVisible
                b.settingsIconRight.isVisible = settings.hasTransition
                b.attachView.isVisible = settings.attach != null
                settings.attach?.invoke(b)
            }

            SettingsView.SWITCH -> {
                val b = (holder as SettingsSwitchViewHolder).binding
                setAnimation(b.root.context, b.root)

                b.settingsButton.text = settings.name
                b.settingsDesc.text = settings.desc
                b.settingsDesc.isVisible = settings.desc.isNotBlank()
                b.settingsIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        b.root.context, settings.icon
                    )
                )
                b.settingsButton.isChecked = settings.isChecked
                b.settingsButton.setOnCheckedChangeListener { _, isChecked ->
                    settings.switch?.invoke(isChecked, b)
                }
                b.settingsLayout.setOnLongClickListener {
                    settings.onLongClick?.invoke()
                    false
                }
                b.settingsLayout.isVisible = settings.isVisible
                settings.attachToSwitch?.invoke(b)
            }

            SettingsView.SLIDER -> {
                val b = (holder as SettingsSliderViewHolder).binding
                setAnimation(b.root.context, b.root)

                b.settingsButton.text = settings.name
                b.settingsButton.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(
                        b.root.context, settings.icon
                    ), null, null, null
                )
                b.settingSlider.stepSize = settings.stepSize
                b.settingSlider.valueFrom = settings.valueFrom
                b.settingSlider.valueTo = settings.valueTo
                b.settingSlider.value = settings.value
                b.settingSlider.addOnChangeListener { _, value, _ ->
                    settings.slider?.invoke(value, b)
                }
                b.settingsLayout.setOnLongClickListener {
                    settings.onLongClick?.invoke()
                    false
                }
                b.settingsLayout.isVisible = settings.isVisible
                settings.attachToSlider?.invoke(b)
            }

            SettingsView.HEADER -> {
                val b = (holder as SettingsHeaderViewHolder).binding
                setAnimation(b.root.context, b.root)

                b.settingsTitle.text = settings.name
                b.settingsTitle.isVisible = settings.name.isNotBlank()
            }
        }
    }

    override fun getItemCount(): Int = settings.size

    override fun getItemViewType(position: Int): Int {
        return settings[position].type.ordinal
    }
}