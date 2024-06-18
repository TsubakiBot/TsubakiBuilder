package ani.dantotsu.util

import android.text.InputFilter
import android.text.Spanned
import android.widget.AutoCompleteTextView
import ani.dantotsu.R
import ani.dantotsu.currContext

class InputFilterMinMax(
    private val min: Double,
    private val max: Double,
    private val status: AutoCompleteTextView? = null
) :
    InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        try {
            val input = (dest.toString() + source.toString()).toDouble()
            if (isInRange(min, max, input)) return null
        } catch (nfe: NumberFormatException) {
            Logger.log(nfe)
        }
        return ""
    }

    private fun isInRange(a: Double, b: Double, c: Double): Boolean {
        val statusStrings = currContext().resources.getStringArray(R.array.status_manga)[2]

        if (c == b) {
            status?.setText(statusStrings, false)
            status?.parent?.requestLayout()
        }
        return if (b > a) c in a..b else c in b..a
    }
}