package ani.matagi.widgets.resumable

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
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
class ResumableWidgetConfigure : AppCompatActivity(),
    SimpleDialog.OnDialogResultListener {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private var isMonetEnabled = false

    private var onClickListener = View.OnClickListener {
        val context = this@ResumableWidgetConfigure
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
        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val prefs = getSharedPreferences(ResumableWidget.getPrefsName(appWidgetId), Context.MODE_PRIVATE)

        binding.useAppTheme.setOnCheckedChangeListener { _, isChecked ->
            isMonetEnabled = isChecked
            if (isChecked) {
                binding.topBackgroundButton.visibility = View.GONE
                binding.bottomBackgroundButton.visibility = View.GONE
                binding.titleColorButton.visibility = View.GONE
                themeColors()

            } else {
                binding.topBackgroundButton.visibility = View.VISIBLE
                binding.bottomBackgroundButton.visibility = View.VISIBLE
                binding.titleColorButton.visibility = View.VISIBLE
            }
        }

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
                    this@ResumableWidgetConfigure,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .setupColorWheelAlpha(true)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@ResumableWidgetConfigure, tag)
        }
        binding.bottomBackgroundButton.setOnClickListener {
            val tag = ResumableWidget.PREF_BACKGROUND_FADE
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(prefs.getInt(ResumableWidget.PREF_BACKGROUND_FADE, Color.parseColor("#00000000")))
                .colors(
                    this@ResumableWidgetConfigure,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .setupColorWheelAlpha(true)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@ResumableWidgetConfigure, tag)
        }
        binding.titleColorButton.setOnClickListener {
            val tag = ResumableWidget.PREF_TITLE_TEXT_COLOR
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(prefs.getInt(ResumableWidget.PREF_TITLE_TEXT_COLOR, Color.WHITE))
                .colors(
                    this@ResumableWidgetConfigure,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@ResumableWidgetConfigure, tag)
        }

        binding.useStackView.run {
            isChecked = prefs.getBoolean(ResumableWidget.PREF_USE_STACKVIEW, isChecked)
        }
        binding.useStackView.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putBoolean(ResumableWidget.PREF_USE_STACKVIEW, isChecked)
                .apply()
        }

        binding.widgetType.setText(
            ResumableType.entries[
            prefs.getInt(ResumableWidget.PREF_WIDGET_TYPE, 2)
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
            val prefs = getSharedPreferences(
                ResumableWidget.getPrefsName(appWidgetId),
                Context.MODE_PRIVATE
            )
            when (dialogTag) {
                ResumableWidget.PREF_BACKGROUND_COLOR -> {
                    prefs.edit()
                        .putInt(
                            ResumableWidget.PREF_BACKGROUND_COLOR,
                            extras.getInt(SimpleColorDialog.COLOR)
                        )
                        .apply()
                }

                ResumableWidget.PREF_BACKGROUND_FADE -> {
                    prefs.edit()
                        .putInt(
                            ResumableWidget.PREF_BACKGROUND_FADE,
                            extras.getInt(SimpleColorDialog.COLOR)
                        )
                        .apply()
                }

                ResumableWidget.PREF_TITLE_TEXT_COLOR -> {
                    prefs.edit()
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

    private fun themeColors() {
        val typedValueSurface = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValueSurface, true)
        val backgroundColor = typedValueSurface.data

        val typedValuePrimary = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValuePrimary, true)
        val textColor = typedValuePrimary.data

        getSharedPreferences(ResumableWidget.getPrefsName(appWidgetId), Context.MODE_PRIVATE).edit().apply {
            putInt(ResumableWidget.PREF_BACKGROUND_COLOR, backgroundColor)
            putInt(ResumableWidget.PREF_BACKGROUND_FADE, backgroundColor)
            putInt(ResumableWidget.PREF_TITLE_TEXT_COLOR, textColor)
            apply()
        }
    }
}