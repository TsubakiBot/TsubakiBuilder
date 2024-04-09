package ani.dantotsu.widgets

import androidx.appcompat.app.AppCompatActivity
import ani.dantotsu.R
import eltos.simpledialogfragment.color.SimpleColorDialog

object ColorDialog {
    fun showColorDialog(context: AppCompatActivity, colorPreset: Int, tag: String) {
        SimpleColorDialog().title(R.string.custom_theme)
            .colorPreset(colorPreset)
            .colors(
                context,
                SimpleColorDialog.MATERIAL_COLOR_PALLET
            )
            .allowCustom(true)
            .showOutline(0x46000000)
            .gridNumColumn(5)
            .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
            .neg()
            .show(context, tag)
    }
}