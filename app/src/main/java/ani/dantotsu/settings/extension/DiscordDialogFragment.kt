package ani.dantotsu.settings.extension

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ani.dantotsu.R
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.databinding.BottomSheetDiscordRpcBinding
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.view.dialog.BottomSheetDialogFragment

class DiscordDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetDiscordRpcBinding? = null
    private val binding by lazy { _binding!! }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDiscordRpcBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (PrefManager.getCustomVal("discord_mode", Discord.MODE.HIMITSU.name)) {
            Discord.MODE.NOTHING.name -> binding.radioNothing.isChecked = true
            Discord.MODE.HIMITSU.name -> binding.radioDantotsu.isChecked = true
            Discord.MODE.ANILIST.name -> binding.radioAnilist.isChecked = true
            else -> binding.radioAnilist.isChecked = true
        }
        binding.showIcon.isChecked = PrefManager.getVal(PrefName.ShowAniListIcon)
        binding.showIcon.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.ShowAniListIcon, isChecked)
        }
        binding.anilistLinkPreview.text =
            getString(R.string.anilist_link, PrefManager.getVal<String>(PrefName.AnilistUserName))

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                binding.radioNothing.id -> Discord.MODE.NOTHING.name
                binding.radioDantotsu.id -> Discord.MODE.HIMITSU.name
                binding.radioAnilist.id -> Discord.MODE.ANILIST.name
                else -> Discord.MODE.HIMITSU.name
            }
            PrefManager.setCustomVal("discord_mode", mode)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}