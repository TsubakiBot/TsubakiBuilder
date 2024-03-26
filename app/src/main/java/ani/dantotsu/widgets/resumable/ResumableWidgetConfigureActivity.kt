package ani.dantotsu.widgets.resumable

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import ani.dantotsu.R
import ani.dantotsu.databinding.ResumableWidgetConfigureBinding
import ani.dantotsu.themes.ThemeManager
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.color.SimpleColorDialog

/**
 * The configuration screen for the [ResumableWidget] AppWidget.
 */
class ResumableWidgetConfigureActivity : AppCompatActivity(),
    SimpleDialog.OnDialogResultListener {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private var onClickListener = View.OnClickListener {
        val context = this@ResumableWidgetConfigureActivity
        val appWidgetManager = AppWidgetManager.getInstance(context)

        updateAppWidget(
            context,
            appWidgetManager,
            appWidgetId,
        )

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
    private lateinit var binding: ResumableWidgetConfigureBinding

    public override fun onCreate(icicle: Bundle?) {
        ThemeManager(this).applyTheme()
        super.onCreate(icicle)
        setResult(RESULT_CANCELED)

        binding = ResumableWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val prefs = getSharedPreferences(ResumableWidget.PREFS_NAME, Context.MODE_PRIVATE)

        binding.topBackgroundButton.setOnClickListener {
            val tag = ResumableWidget.PREF_BACKGROUND_COLOR
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(
                    prefs.getInt(
                        ResumableWidget.PREF_BACKGROUND_COLOR,
                        ContextCompat.getColor(this, R.color.theme)
                    )
                )
                .colors(
                    this@ResumableWidgetConfigureActivity,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .setupColorWheelAlpha(true)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@ResumableWidgetConfigureActivity, tag)
        }
        binding.bottomBackgroundButton.setOnClickListener {
            val tag = ResumableWidget.PREF_BACKGROUND_FADE
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(prefs.getInt(ResumableWidget.PREF_BACKGROUND_FADE, Color.GRAY))
                .colors(
                    this@ResumableWidgetConfigureActivity,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .setupColorWheelAlpha(true)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@ResumableWidgetConfigureActivity, tag)
        }
        binding.titleColorButton.setOnClickListener {
            val tag = ResumableWidget.PREF_TITLE_TEXT_COLOR
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(prefs.getInt(ResumableWidget.PREF_TITLE_TEXT_COLOR, Color.WHITE))
                .colors(
                    this@ResumableWidgetConfigureActivity,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@ResumableWidgetConfigureActivity, tag)
        }

        binding.widgetType.setText(ResumableType.entries[
            prefs.getInt(ResumableWidget.PREF_WIDGET_TYPE, 0)
        ].type)
        binding.widgetType.setAdapter(
            ArrayAdapter(
                this,
                R.layout.item_dropdown,
                ResumableType.entries .map { it.type }
            )
        )

        binding.widgetType.setOnItemClickListener { _, _, i, _ ->
            prefs.edit()
                .putInt(ResumableWidget.PREF_WIDGET_TYPE, i)
                .apply()
            binding.widgetType.clearFocus()
        }

        binding.useStackView.run {
            isChecked = prefs.getBoolean(ResumableWidget.PREF_USE_STACKVIEW, isChecked)
        }
        binding.useStackView.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putBoolean(ResumableWidget.PREF_USE_STACKVIEW, isChecked)
                .apply()
        }

        binding.addButton.setOnClickListener(onClickListener)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
            when (dialogTag) {
                ResumableWidget.PREF_BACKGROUND_COLOR -> {
                    getSharedPreferences(
                        ResumableWidget.PREFS_NAME,
                        Context.MODE_PRIVATE
                    ).edit()
                        .putInt(
                            ResumableWidget.PREF_BACKGROUND_COLOR,
                            extras.getInt(SimpleColorDialog.COLOR)
                        )
                        .apply()
                }

                ResumableWidget.PREF_BACKGROUND_FADE -> {
                    getSharedPreferences(
                        ResumableWidget.PREFS_NAME,
                        Context.MODE_PRIVATE
                    ).edit()
                        .putInt(
                            ResumableWidget.PREF_BACKGROUND_FADE,
                            extras.getInt(SimpleColorDialog.COLOR)
                        )
                        .apply()
                }

                ResumableWidget.PREF_TITLE_TEXT_COLOR -> {
                    getSharedPreferences(
                        ResumableWidget.PREFS_NAME,
                        Context.MODE_PRIVATE
                    ).edit()
                        .putInt(
                            ResumableWidget.PREF_TITLE_TEXT_COLOR,
                            extras.getInt(SimpleColorDialog.COLOR)
                        )
                        .apply()
                }
            }
        }
        return true
    }

}