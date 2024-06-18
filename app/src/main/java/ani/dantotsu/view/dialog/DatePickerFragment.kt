package ani.dantotsu.view.dialog

import android.app.Activity
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.api.FuzzyDate
import java.util.Calendar

class DatePickerFragment(activity: Activity, var date: FuzzyDate = FuzzyDate().getToday()) :
    DialogFragment(),
    DatePickerDialog.OnDateSetListener {
    var dialog: DatePickerDialog

    init {
        val c = Calendar.getInstance()
        val year = date.year ?: c.get(Calendar.YEAR)
        val month = if (date.month != null) date.month!! - 1 else c.get(Calendar.MONTH)
        val day = date.day ?: c.get(Calendar.DAY_OF_MONTH)
        dialog = DatePickerDialog(activity, this, year, month, day)
        dialog.setButton(
            DialogInterface.BUTTON_NEUTRAL,
            activity.getString(R.string.remove)
        ) { _, which ->
            if (which == DialogInterface.BUTTON_NEUTRAL) {
                date = FuzzyDate()
            }
        }
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        date = FuzzyDate(year, month + 1, day)
    }
}