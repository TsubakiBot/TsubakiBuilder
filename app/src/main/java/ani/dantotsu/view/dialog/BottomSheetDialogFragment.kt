package ani.dantotsu.view.dialog

import android.content.res.Configuration
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentManager
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import eu.kanade.tachiyomi.util.system.getThemeColor

open class BottomSheetDialogFragment : BottomSheetDialogFragment() {
    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val immersiveMode: Boolean = PrefManager.getVal(PrefName.ImmersiveMode)
            if (immersiveMode) {
                WindowInsetsControllerCompat(
                    window, window.decorView
                ).hide(WindowInsetsCompat.Type.statusBars())
            }
            if (this.resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
                val behavior = BottomSheetBehavior.from(requireView().parent as View)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            window.navigationBarColor = requireContext().getThemeColor(
                com.google.android.material.R.attr.colorSurface
            )
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }
}