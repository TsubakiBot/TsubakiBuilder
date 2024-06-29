package ani.dantotsu.connections.discord

import android.content.Context
import android.content.Intent
import android.widget.TextView
import ani.dantotsu.R
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.toast
import ani.dantotsu.tryWith
import ani.dantotsu.view.dialog.CustomBottomDialog
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import java.io.File

object Discord {

    var token: String? = null
    var userid: String? = null
    var avatar: String? = null

    fun getSavedToken(): Boolean {
        token = PrefManager.getVal(
            PrefName.DiscordToken, null as String?
        )
        return token != null
    }

    fun saveToken(token: String) {
        PrefManager.setVal(PrefName.DiscordToken, token)
    }

    fun removeSavedToken(context: Context) {
        PrefManager.removeVal(PrefName.DiscordToken)

        tryWith(true) {
            val dir = File(context.filesDir?.parentFile, "app_webview")
            if (dir.deleteRecursively())
                toast(context.getString(R.string.discord_logout_success))
        }
    }

    private var rpc: RPC? = null


    fun warning(context: Context) = CustomBottomDialog().apply {
        title = context.getString(R.string.warning)
        val md = context.getString(R.string.discord_warning)
        addView(TextView(context).apply {
            val markWon =
                Markwon.builder(context).usePlugin(SoftBreakAddsNewLinePlugin.create()).build()
            markWon.setMarkdown(this, md)
        })

        setNegativeButton(context.getString(R.string.cancel)) {
            dismiss()
        }

        setPositiveButton(context.getString(R.string.login)) {
            dismiss()
            loginIntent(context)
        }
    }

    private fun loginIntent(context: Context) {
        val intent = Intent(context, Login::class.java)
        context.startActivity(intent)
    }

    enum class MODE {
        HIMITSU,
        ANILIST,
        NOTHING
    }

    const val application_Id = "1243752355502227517"
    const val small_Image: String =
        "mp:external/Hb3tbwkuWo6-CxOCTV4QtWTFB0ibQmOfpfTm40NrOqM/https/cdn.discordapp.com/app-icons/1243752355502227517/521614c3e2484797248c45e7438fea4e.png"
    const val small_Image_AniList: String =
        "mp:external/rHOIjjChluqQtGyL_UHk6Z4oAqiVYlo_B7HSGPLSoUg/%3Fsize%3D128/https/cdn.discordapp.com/icons/210521487378087947/a_f54f910e2add364a3da3bb2f2fce0c72.webp"
}