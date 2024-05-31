package ani.dantotsu.offline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.databinding.FragmentOfflineBinding
import ani.dantotsu.isOnline
import ani.dantotsu.navBarHeight
import ani.dantotsu.refresh
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight

class OfflineFragment : Fragment() {
    private var offline = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentOfflineBinding.inflate(inflater, container, false)
        binding.refreshContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }
        offline = PrefManager.getVal(PrefName.OfflineMode)
        binding.noInternet.text =
            if (offline) getString(R.string.offline_mode) else getString(R.string.no_internet)
        binding.refreshButton.text = getString(if (offline) R.string.go_online else R.string.refresh)
        binding.refreshButton.setOnClickListener {
            if (offline && isOnline(requireContext())) {
                PrefManager.setVal(PrefName.OfflineMode, false)
                requireActivity().recreate()
                Refresh.all()
            } else {
                if (isOnline(requireContext()) ) {
                    requireActivity().refresh()
                }
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        offline = PrefManager.getVal(PrefName.OfflineMode)
    }
}