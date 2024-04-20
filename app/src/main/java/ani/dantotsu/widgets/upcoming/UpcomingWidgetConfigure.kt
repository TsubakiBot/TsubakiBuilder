package ani.dantotsu.widgets.upcoming

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import ani.dantotsu.databinding.UpcomingWidgetConfigureBinding
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.widgets.ColorDialog
import com.google.android.material.button.MaterialButton
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.color.SimpleColorDialog
import eu.kanade.tachiyomi.util.system.getThemeColor

/**
 * The configuration screen for the [UpcomingWidget] AppWidget.
 */
class UpcomingWidgetConfigure : AppCompatActivity(),
    SimpleDialog.OnDialogResultListener {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isMonetEnabled = false
    private var onClickListener = View.OnClickListener {
        val context = this@UpcomingWidgetConfigure
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
    private lateinit var binding: UpcomingWidgetConfigureBinding

    public override fun onCreate(icicle: Bundle?) {
        ThemeManager(this).applyTheme()
        super.onCreate(icicle)
        setResult(RESULT_CANCELED)

        binding = UpcomingWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val prefs =
            getSharedPreferences(UpcomingWidget.getPrefsName(appWidgetId), Context.MODE_PRIVATE)

        val topBackground =
            prefs.getInt(UpcomingWidget.PREF_BACKGROUND_COLOR, Color.parseColor("#80000000"))
        (binding.topBackgroundButton as MaterialButton).iconTint =
            ColorStateList.valueOf(topBackground)
        binding.topBackgroundButton.setOnClickListener {
            ColorDialog.showColorDialog(
                this@UpcomingWidgetConfigure,
                topBackground,
                UpcomingWidget.PREF_BACKGROUND_COLOR
            )
        }
        val bottomBackground =
            prefs.getInt(UpcomingWidget.PREF_BACKGROUND_FADE, Color.parseColor("#00000000"))
        (binding.bottomBackgroundButton as MaterialButton).iconTint =
            ColorStateList.valueOf(bottomBackground)
        binding.bottomBackgroundButton.setOnClickListener {
            ColorDialog.showColorDialog(
                this@UpcomingWidgetConfigure,
                bottomBackground,
                UpcomingWidget.PREF_BACKGROUND_FADE
            )
        }
        val titleTextColor = prefs.getInt(UpcomingWidget.PREF_TITLE_TEXT_COLOR, Color.WHITE)
        (binding.titleColorButton as MaterialButton).iconTint =
            ColorStateList.valueOf(titleTextColor)
        binding.titleColorButton.setOnClickListener {
            ColorDialog.showColorDialog(
                this@UpcomingWidgetConfigure,
                titleTextColor,
                UpcomingWidget.PREF_TITLE_TEXT_COLOR
            )
        }
        val countdownTextColor = prefs.getInt(UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR, Color.WHITE)
        (binding.countdownColorButton as MaterialButton).iconTint =
            ColorStateList.valueOf(countdownTextColor)
        binding.countdownColorButton.setOnClickListener {
            ColorDialog.showColorDialog(
                this@UpcomingWidgetConfigure,
                countdownTextColor,
                UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR
            )
        }
        binding.useAppTheme.setOnCheckedChangeListener { _, isChecked ->
            isMonetEnabled = isChecked
            binding.topBackgroundButton.isGone = isChecked
            binding.bottomBackgroundButton.isGone = isChecked
            binding.titleColorButton.isGone = isChecked
            binding.countdownColorButton.isGone = isChecked
            if (isChecked) themeColors()
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

    private fun themeColors() {
        val backgroundColor = getThemeColor(com.google.android.material.R.attr.colorSurface)
        val textColor = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val subTextColor = getThemeColor(com.google.android.material.R.attr.colorOutline)

        getSharedPreferences(UpcomingWidget.getPrefsName(appWidgetId), Context.MODE_PRIVATE).edit()
            .apply {
                putInt(UpcomingWidget.PREF_BACKGROUND_COLOR, backgroundColor)
                putInt(UpcomingWidget.PREF_BACKGROUND_FADE, backgroundColor)
                putInt(UpcomingWidget.PREF_TITLE_TEXT_COLOR, textColor)
                putInt(UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR, subTextColor)
                apply()
            }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
            val prefs = getSharedPreferences(
                UpcomingWidget.getPrefsName(appWidgetId),
                Context.MODE_PRIVATE
            )
            if (!isMonetEnabled) {
                when (dialogTag) {
                    UpcomingWidget.PREF_BACKGROUND_COLOR -> {
                        prefs.edit()
                            .putInt(
                                UpcomingWidget.PREF_BACKGROUND_COLOR,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                        (binding.topBackgroundButton as MaterialButton).iconTint =
                            ColorStateList.valueOf(extras.getInt(SimpleColorDialog.COLOR))
                    }

                    UpcomingWidget.PREF_BACKGROUND_FADE -> {
                        prefs.edit()
                            .putInt(
                                UpcomingWidget.PREF_BACKGROUND_FADE,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                        (binding.bottomBackgroundButton as MaterialButton).iconTint =
                            ColorStateList.valueOf(extras.getInt(SimpleColorDialog.COLOR))
                    }

                    UpcomingWidget.PREF_TITLE_TEXT_COLOR -> {
                        prefs.edit()
                            .putInt(
                                UpcomingWidget.PREF_TITLE_TEXT_COLOR,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                        (binding.titleColorButton as MaterialButton).iconTint =
                            ColorStateList.valueOf(extras.getInt(SimpleColorDialog.COLOR))
                    }

                    UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR -> {
                        prefs.edit()
                            .putInt(
                                UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                        (binding.countdownColorButton as MaterialButton).iconTint =
                            ColorStateList.valueOf(extras.getInt(SimpleColorDialog.COLOR))
                    }

                }
            }
        }
        return true
    }
}