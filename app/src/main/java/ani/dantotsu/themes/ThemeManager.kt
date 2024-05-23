package ani.dantotsu.themes

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.view.Window
import android.view.WindowManager
import ani.dantotsu.R
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions


class ThemeManager(private val context: Activity) {
    fun applyTheme(fromImage: Bitmap? = null) {
        val useOLED = PrefManager.getVal(PrefName.UseOLED) && isDarkThemeActive(context)
        val useCustomTheme: Boolean = PrefManager.getVal(PrefName.UseCustomTheme)
        val customTheme: Int = PrefManager.getVal(PrefName.CustomThemeInt)
        val useSource: Boolean = PrefManager.getVal(PrefName.UseSourceTheme)
        val useMaterial: Boolean = PrefManager.getVal(PrefName.UseMaterialYou)
        if (useSource) {
            val returnedEarly = applyDynamicColors(
                useMaterial,
                context,
                useOLED,
                fromImage,
                useCustom = if (useCustomTheme) customTheme else null
            )
            if (!returnedEarly) return
        } else if (useCustomTheme) {
            val returnedEarly =
                applyDynamicColors(useMaterial, context, useOLED, useCustom = customTheme)
            if (!returnedEarly) return
        } else {
            val returnedEarly = applyDynamicColors(useMaterial, context, useOLED, useCustom = null)
            if (!returnedEarly) return
        }
        val theme: String = PrefManager.getVal(PrefName.Theme)

        val themeToApply = when (theme) {
            Theme.BLUE.name -> if (useOLED) R.style.Theme_Dantotsu_BlueOLED else R.style.Theme_Dantotsu_Blue
            Theme.GREEN.name -> if (useOLED) R.style.Theme_Dantotsu_GreenOLED else R.style.Theme_Dantotsu_Green
            Theme.PURPLE.name -> if (useOLED) R.style.Theme_Dantotsu_PurpleOLED else R.style.Theme_Dantotsu_Purple
            Theme.PINK.name -> if (useOLED) R.style.Theme_Dantotsu_PinkOLED else R.style.Theme_Dantotsu_Pink
            Theme.SAIKOU.name -> if (useOLED) R.style.Theme_Dantotsu_SaikouOLED else R.style.Theme_Dantotsu_Saikou
            Theme.RED.name -> if (useOLED) R.style.Theme_Dantotsu_RedOLED else R.style.Theme_Dantotsu_Red
            Theme.LAVENDER.name -> if (useOLED) R.style.Theme_Dantotsu_LavenderOLED else R.style.Theme_Dantotsu_Lavender
            Theme.OCEAN.name -> if (useOLED) R.style.Theme_Dantotsu_OceanOLED else R.style.Theme_Dantotsu_Ocean
            Theme.ORIAX.name -> if (useOLED) R.style.Theme_Dantotsu_OriaxOLED else R.style.Theme_Dantotsu_Oriax
            Theme.MONOCHROME.name -> if (useOLED) R.style.Theme_Dantotsu_MonochromeOLED else R.style.Theme_Dantotsu_Monochrome
            else -> if (useOLED) R.style.Theme_Dantotsu_SaikouOLED else R.style.Theme_Dantotsu_Saikou
        }

        val window = context.window
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = 0x00000000
        context.setTheme(themeToApply)
    }

    fun setWindowFlag(activity: Activity, bits: Int, on: Boolean) {
        val win: Window = activity.window
        val winParams: WindowManager.LayoutParams = win.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }

    private fun applyDynamicColors(
        useMaterialYou: Boolean,
        context: Context,
        useOLED: Boolean,
        bitmap: Bitmap? = null,
        useCustom: Int? = null
    ): Boolean {
        val builder = DynamicColorsOptions.Builder()
        var needMaterial = true

        // Set content-based source if a bitmap is provided
        if (bitmap != null) {
            builder.setContentBasedSource(bitmap)
            needMaterial = false
        } else if (useCustom != null) {
            builder.setContentBasedSource(useCustom)
            needMaterial = false
        }

        if (useOLED) {
            builder.setThemeOverlay(R.style.AppTheme_Amoled)
        }
        if (needMaterial && !useMaterialYou) return true

        // Build the options
        val options = builder.build()

        // Apply the dynamic colors to the activity
        val activity = context as Activity
        DynamicColors.applyToActivityIfAvailable(activity, options)

        if (useOLED) {
            val options2 = DynamicColorsOptions.Builder()
                .setThemeOverlay(R.style.AppTheme_Amoled)
                .build()
            DynamicColors.applyToActivityIfAvailable(activity, options2)
        }

        return false
    }

    private fun isDarkThemeActive(context: Context): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

    companion object {
        enum class Theme(val theme: String) {
            BLUE("BLUE"),
            GREEN("GREEN"),
            PURPLE("PURPLE"),
            PINK("PINK"),
            SAIKOU("SAIKOU"),
            RED("RED"),
            LAVENDER("LAVENDER"),
            OCEAN("OCEAN"),
            ORIAX("ORIAX"),
            MONOCHROME("MONOCHROME");
        }
    }
}
