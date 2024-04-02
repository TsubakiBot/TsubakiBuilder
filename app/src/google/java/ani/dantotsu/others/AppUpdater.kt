package ani.dantotsu.others

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import ani.dantotsu.BuildConfig
import ani.dantotsu.Mapper
import ani.dantotsu.R
import ani.dantotsu.buildMarkwon
import ani.dantotsu.client
import ani.dantotsu.currContext
import ani.dantotsu.logError
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.snackString
import ani.dantotsu.toast
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.util.system.getParcelableExtraCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random

object AppUpdater {

    suspend fun check(activity: FragmentActivity, post: Boolean = false) {
        if (post) snackString(currContext()?.getString(R.string.checking_for_update))
        val repo = activity.getString(R.string.repo)
        tryWithSuspend {
            val res = client.get("https://api.github.com/repos/$repo/releases")
                .parsed<JsonArray>().map {
                    Mapper.json.decodeFromJsonElement<GithubResponse>(it)
                }
            val r = res.filter { it.prerelease }.maxByOrNull {
                it.timeStamp()
            } ?: throw Exception("No Prerelease Found")
            val v = r.tagName
            val (md, version) = (r.body ?: "") to v.ifEmpty { throw Exception("Unexpected Tag : ${r.tagName}") }

            Logger.log("Release Hash : $version")
            val dontShow = PrefManager.getCustomVal("dont_ask_for_update_$version", false)
            if (compareVersion(version) && !dontShow && !activity.isDestroyed) activity.runOnUiThread {
                CustomBottomDialog.newInstance().apply {
                    setTitleText(
                        "${if (BuildConfig.DEBUG) "Beta " else ""}Update " + currContext()!!.getString(
                            R.string.available
                        )
                    )
                    setCheck(
                        currContext()!!.getString(R.string.dont_show_again, version),
                        false
                    ) { isChecked ->
                        if (isChecked) {
                            PrefManager.setCustomVal("dont_ask_for_update_$version", true)
                        }
                    }
                    setPositiveButton(currContext()!!.getString(R.string.lets_go)) {
                        MainScope().launch(Dispatchers.IO) {
                            try {
                                client.get("https://api.github.com/repos/$repo/releases/tags/$version")
                                    .parsed<GithubResponse>().assets?.find {
                                        it.browserDownloadURL.endsWith("apk")
                                    }?.browserDownloadURL.apply {
                                        if (this != null) activity.downloadUpdate(version, this)
                                        else openLinkInBrowser("https://github.com/repos/$repo/releases/tag/$version")
                                    }
                            } catch (e: Exception) {
                                logError(e)
                            }
                        }
                        dismiss()
                    }
                    setNegativeButton(currContext()!!.getString(R.string.cope)) {
                        dismiss()
                    }
                    show(activity.supportFragmentManager, "dialog")
                }
            }
            else {
                if (post) snackString(currContext()?.getString(R.string.no_update_found))
            }
        }
    }

    private fun compareVersion(version: String): Boolean {
        return BuildConfig.COMMIT != version
    }


    // https://github.com/LagradOst/CloudStream-3/blob/master/app/src/main/java/com/lagradost/cloudstream3/utils/InAppUpdater.kt
    private fun Activity.downloadUpdate(version: String, url: String) {

        toast(getString(R.string.downloading_update, version))

        val downloadManager = this.getSystemService<DownloadManager>()!!

        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType("application/vnd.android.package-archive")
            .setTitle("Downloading Dantotsu $version")
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Dantotsu-$version.apk"
            )
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)

        val id = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            logError(e)
            -1
        }
        if (id == -1L) return
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                @SuppressLint("Range")
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        val downloadId = intent?.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, id
                        ) ?: id

                        downloadManager.getUriForDownloadedFile(downloadId)?.let {
                            openApk(this@downloadUpdate, it)
                        }
                    } catch (e: Exception) {
                        logError(e)
                    }
                }
            }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun openApk(context: Context, uri: Uri) {
        try {
            uri.path?.let {
                context.contentResolver.openInputStream(uri).use { apkStream ->
                    val session = with (context.packageManager.packageInstaller) {
                        val params = PackageInstaller.SessionParams(
                            PackageInstaller.SessionParams.MODE_FULL_INSTALL
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            params.setRequireUserAction(
                                PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
                            )
                        }
                        openSession(createSession(params))
                    }
                    val document = DocumentFile.fromSingleUri(context, uri)
                        ?: throw IOException("Invalid document file size!")
                    session.openWrite("NAME", 0, document.length()).use { sessionStream ->
                        apkStream?.copyTo(sessionStream)
                        session.fsync(sessionStream)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context, Random.nextInt(),
                        Intent(INSTALL_ACTION),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                                    or PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                        else
                            PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    session.commit(pi.intentSender)
                }
            }
        } catch (e: Exception) {
            logError(e)
        }
    }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    @Serializable
    data class GithubResponse(
        @SerialName("html_url")
        val htmlUrl: String,
        @SerialName("tag_name")
        val tagName: String,
        val prerelease: Boolean,
        @SerialName("created_at")
        val createdAt: String,
        val body: String? = null,
        val assets: List<Asset>? = null
    ) {
        @Serializable
        data class Asset(
            @SerialName("browser_download_url")
            val browserDownloadURL: String
        )

        fun timeStamp(): Long {
            return dateFormat.parse(createdAt)!!.time
        }
    }

    private const val INSTALL_ACTION = "AppUpdater.INSTALL_ACTION"
}