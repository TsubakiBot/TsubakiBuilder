package ani.matagi.widgets.resumable

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import ani.dantotsu.R
import ani.dantotsu.databinding.ResumableWidgetConfigureBinding
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.widgets.ColorDialog
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.color.SimpleColorDialog
import eu.kanade.tachiyomi.util.system.getThemeColor

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
            binding.topBackgroundButton.isGone = isChecked
            binding.bottomBackgroundButton.isGone = isChecked
            binding.titleColorButton.isGone = isChecked
            binding.flipperColorButton.isGone = isChecked
            if (isChecked) themeColors()
        }

        binding.topBackgroundButton.setOnClickListener {
            ColorDialog.showColorDialog(
                this@ResumableWidgetConfigure,
                prefs.getInt(
                    ResumableWidget.PREF_BACKGROUND_COLOR,
                    ContextCompat.getColor(this, R.color.theme)
                ),
                ResumableWidget.PREF_BACKGROUND_COLOR)
        }
        binding.bottomBackgroundButton.setOnClickListener {
            ColorDialog.showColorDialog(
                this@ResumableWidgetConfigure,
                prefs.getInt(ResumableWidget.PREF_BACKGROUND_FADE,
                    Color.parseColor("#00000000")
                ),
                ResumableWidget.PREF_BACKGROUND_FADE)
        }
        binding.titleColorButton.setOnClickListener {
            ColorDialog.showColorDialog(
                this@ResumableWidgetConfigure,
                prefs.getInt(ResumableWidget.PREF_TITLE_TEXT_COLOR, Color.WHITE),
                ResumableWidget.PREF_TITLE_TEXT_COLOR)
        }
        binding.flipperColorButton.setOnClickListener {
            ColorDialog.showColorDialog(
                this@ResumableWidgetConfigure,
                prefs.getInt(ResumableWidget.PREF_FLIPPER_IMG_COLOR, Color.WHITE),
                ResumableWidget.PREF_FLIPPER_IMG_COLOR)
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

        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

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
            if (!isMonetEnabled) {
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

                    ResumableWidget.PREF_FLIPPER_IMG_COLOR -> {
                        prefs.edit()
                            .putInt(
                                ResumableWidget.PREF_FLIPPER_IMG_COLOR,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                    }
                }
            }
        }
        return true
    }

    private fun themeColors() {
        val backgroundColor = getThemeColor(com.google.android.material.R.attr.colorSurface)
        val textColor = getThemeColor(com.google.android.material.R.attr.colorOnBackground)
        val flipperColor = getThemeColor(com.google.android.material.R.attr.colorPrimary)

        getSharedPreferences(ResumableWidget.getPrefsName(appWidgetId), Context.MODE_PRIVATE).edit().apply {
            putInt(ResumableWidget.PREF_BACKGROUND_COLOR, backgroundColor)
            putInt(ResumableWidget.PREF_BACKGROUND_FADE, backgroundColor)
            putInt(ResumableWidget.PREF_TITLE_TEXT_COLOR, textColor)
            putInt(ResumableWidget.PREF_FLIPPER_IMG_COLOR, flipperColor)
            apply()
        }
    }
}
