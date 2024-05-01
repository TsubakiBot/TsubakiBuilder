package ani.dantotsu.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import ani.dantotsu.BuildConfig
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import java.io.File
import java.util.Date
import java.util.concurrent.Executors

object Logger {
    var file: File? = null
    private val loggerExecutor = Executors.newSingleThreadExecutor()

    fun init(context: Context) {
        try {
            if (!PrefManager.getVal<Boolean>(PrefName.LogToFile) || file != null) return
            file = File(context.getExternalFilesDir(null), "log.txt")
            if (file?.exists() == true) {
                if (file!!.length() > 1024 * 1024 * 10) { // 10MB
                    file?.delete()
                    file?.createNewFile()
                }
            } else {
                file?.createNewFile()
            }
            file?.let {
                it.appendText("log started\n")
                it.appendText("date/time: ${Date()}\n")
                it.appendText("device: ${Build.MODEL}\n")
                it.appendText("os version: ${Build.VERSION.RELEASE}\n")
                it.appendText(
                    "app version: ${
                        context.packageManager.getPackageInfo(
                            context.packageName,
                            0
                        ).versionName
                    }\n"
                )
                it.appendText(
                    "app version code: ${
                        context.packageManager.getPackageInfo(
                            context.packageName,
                            0
                        ).run {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                                longVersionCode
                            else
                                @Suppress("DEPRECATION") versionCode

                        }
                    }\n"
                )
                it.appendText("sdk version: ${Build.VERSION.SDK_INT}\n")
                it.appendText("manufacturer: ${Build.MANUFACTURER}\n")
                it.appendText("brand: ${Build.BRAND}\n")
                it.appendText("product: ${Build.PRODUCT}\n")
                it.appendText("device: ${Build.DEVICE}\n")
                it.appendText("hardware: ${Build.HARDWARE}\n")
                it.appendText("host: ${Build.HOST}\n")
                it.appendText("id: ${Build.ID}\n")
                it.appendText("type: ${Build.TYPE}\n")
                it.appendText("user: ${Build.USER}\n")
                it.appendText("tags: ${Build.TAGS}\n")
                it.appendText("time: ${Build.TIME}\n")
                it.appendText("radio: ${Build.getRadioVersion()}\n")
                it.appendText("bootloader: ${Build.BOOTLOADER}\n")
                it.appendText("board: ${Build.BOARD}\n")
                it.appendText("fingerprint: ${Build.FINGERPRINT}\n")
                it.appendText("supported_abis: ${Build.SUPPORTED_ABIS.joinToString()}\n")
                it.appendText("supported_32_bit_abis: ${Build.SUPPORTED_32_BIT_ABIS.joinToString()}\n")
                it.appendText("supported_64_bit_abis: ${Build.SUPPORTED_64_BIT_ABIS.joinToString()}\n")
                it.appendText("is emulator: ${Build.FINGERPRINT.contains("generic")}\n")
                it.appendText("--------------------------------\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            file = null
        }
    }

    fun log(message: String) {
        val trace = Thread.currentThread().stackTrace[3]
        loggerExecutor.execute {
            if (file == null) Log.d("Internal Logger", message)
            else {
                val className = trace.className
                val methodName = trace.methodName
                val lineNumber = trace.lineNumber
                file?.appendText("date/time: ${Date()} | $className.$methodName($lineNumber)\n")
                file?.appendText("message: $message\n-\n")
            }
        }
    }

    fun log(e: Exception) {
        loggerExecutor.execute {
            file?.let {
                it.appendText("---------------------------Exception---------------------------\n")
                it.appendText("date/time: ${Date()} |  ${e.message}\n")
                it.appendText("trace: ${e.stackTraceToString()}\n")
            }
        }
        e.printStackTrace()
    }

    fun log(e: Throwable) {
        loggerExecutor.execute {
            file?.let {
                it.appendText("---------------------------Exception---------------------------\n")
                it.appendText("date/time: ${Date()} |  ${e.message}\n")
                it.appendText("trace: ${e.stackTraceToString()}\n")
            }
        }
        e.printStackTrace()
    }

    fun shareLog(context: Context) {
        if (file == null) {
            snackString("No log file found")
            return
        }
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.provider",
                file!!
            )
        )
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Log file")
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Log file")
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(shareIntent, "Share log file"))
    }

    fun clearLog() {
        file?.delete()
        file = null
    }
}

class FinalExceptionHandler : Thread.UncaughtExceptionHandler {
    private val defaultUEH: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        Logger.log(e)
        defaultUEH?.uncaughtException(t, e)
    }
}