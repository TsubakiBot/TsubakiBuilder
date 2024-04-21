package ani.matagi.update

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
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import ani.dantotsu.BuildConfig
import ani.dantotsu.Mapper
import ani.dantotsu.R
import ani.dantotsu.client
import ani.dantotsu.currContext
import ani.dantotsu.databinding.ItemAppUpdateBinding
import ani.dantotsu.logError
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.snackString
import ani.dantotsu.toast
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

object MatagiUpdater {

    private var buildHash: String? = null
    var hasUpdate = 0
    @SuppressLint("DiscouragedApi")
    val tokenRes = currContext().resources.getIdentifier(
        "token", "raw", currContext().packageName
    )
    val headers = if (tokenRes != 0) {
        val token = currContext().resources.openRawResource(tokenRes).bufferedReader().use {
            it.readText()
        }
        mapOf("Authorization" to "Bearer $token")
        mapOf("Authorization" to "Token $token")
    } else emptyMap()

    private fun getUpdateDialog(activity: FragmentActivity, version: String): CustomBottomDialog {
        return CustomBottomDialog.newInstance().apply {
            setTitleText(activity.getString(R.string.install_update, version))
            setCheck(activity.getString(R.string.matagi_dont_show, version), false) { isChecked ->
                if (isChecked) PrefManager.setCustomVal("dont_ask_for_update_$version", true)
            }
        }
    }

    suspend fun check(activity: FragmentActivity, post: Boolean = false) {
        if (post) snackString(currContext().getString(R.string.checking_for_update))
        val repo = if (tokenRes != 0)
            activity.getString(R.string.token_repo)
        else activity.getString(R.string.repo)
        tryWithSuspend {
            val res = client.get("https://api.github.com/repos/$repo/releases", headers)
                .parsed<JsonArray>().map {
                    Mapper.json.decodeFromJsonElement<GithubResponse>(it)
                }
            val r = res.filter { it.prerelease }.maxByOrNull {
                it.timeStamp()
            } ?: throw Exception("No Prerelease Found")
            val v = r.tagName
            val (_, version) = (r.body ?: "") to v.ifEmpty {
                throw Exception("Unexpected Tag : ${r.tagName}")
            }

            Logger.log("Release Hash : $version")
            val dontShow = PrefManager.getCustomVal("dont_ask_for_update_$version", false)
            if (compareVersion(version) && !dontShow && !activity.isDestroyed) activity.runOnUiThread {
                if (post) {
                    getUpdateDialog(activity, version).apply {
                        setPositiveButton(activity.getString(R.string.lets_go)) {
                            requestUpdate(activity, version)
                            dismiss()
                        }
                        setNegativeButton(activity.getString(R.string.cope)) {
                            dismiss()
                        }
                        show(activity.supportFragmentManager, "dialog")
                    }
                } else {
                    hasUpdate = 1
                    buildHash = version
                }
            }
            else {
                if (post) snackString(currContext().getString(R.string.no_update_found))
            }
        }
    }

    private suspend fun installUpdate(activity: FragmentActivity, version: String) =
        withContext(Dispatchers.IO) {
            val repo = if (tokenRes != 0)
                activity.getString(R.string.token_repo)
            else activity.getString(R.string.repo)
            try {
                val res = client.get("https://api.github.com/repos/$repo/releases/tags/$version", headers)
                    .parsed<GithubResponse>().assets?.find {
                        it.browserDownloadURL.contains(
                            "-${Build.SUPPORTED_ABIS.firstOrNull() ?: "universal"}-", true
                        )
                    }
                val downloadUrl = res?.assetUrl?.takeIf { tokenRes != 0 } ?: res?.browserDownloadURL
                downloadUrl?.let {
                    activity.downloadUpdate(version, it)
                } ?: openLinkInBrowser("https://github.com/$repo/releases/tag/$version")
            } catch (e: Exception) {
                logError(e)
            }
        }

    private fun requestUpdate(activity: FragmentActivity, version: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.packageManager.canRequestPackageInstalls()) {
                val onInstallResult = activity.registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { _: ActivityResult ->
                    if (activity.packageManager.canRequestPackageInstalls()) {
                        MainScope().launch(Dispatchers.IO) {
                            installUpdate(activity, version)
                        }
                    } else {
                        snackString(R.string.update_blocked)
                    }
                }
                onInstallResult.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse(String.format("package:%s", activity.packageName))
                })
            } else {
                MainScope().launch(Dispatchers.IO) {
                    installUpdate(activity, version)
                }
            }
        }
    }

    fun notifyOnUpdate(activity: AppCompatActivity, parent: ViewGroup) {
        buildHash?.let { version ->
            val view = ItemAppUpdateBinding.inflate(
                LayoutInflater.from(parent.context), parent, true
            )
            Glide.with(activity)
                .load(activity.getString(R.string.update_banner))
                .into(view.notificationBannerImage)
            Glide.with(activity)
                .load(activity.getString(R.string.update_icon))
                .into(view.notificationCover)
            view.notificationText.text = activity.getString(R.string.matagi_update, version)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            view.notificationDate.text = dateFormat.format(System.currentTimeMillis())
            view.root.setOnClickListener {
                getUpdateDialog(activity, version).apply {
                    setPositiveButton(activity.getString(R.string.lets_go)) {
                        buildHash = null
                        requestUpdate(activity, version)
                        dismiss()
                        parent.removeView(view.root)
                    }
                    setNegativeButton(activity.getString(R.string.cope)) {
                        buildHash = null
                        dismiss()
                        parent.removeView(view.root)
                    }
                    show(activity.supportFragmentManager, "dialog")
                }
            }
            hasUpdate = 0
        }
    }

    private fun compareVersion(version: String): Boolean {
        return BuildConfig.COMMIT != version
    }

    private fun Activity.downloadUpdate(version: String, url: String) {

        val downloadManager = this.getSystemService<DownloadManager>()!!

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(getString(R.string.matagi_downloading, version))
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Mr.Matagi-$version.apk"
            )
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
        if (tokenRes != 0) {
            request.addRequestHeader(headers.keys.first(), headers.values.first())
            request.addRequestHeader("Accept", "application/octet-stream")
        } else {
            request.setMimeType("application/vnd.android.package-archive")
        }

        val id = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            logError(e)
            -1
        }
        if (id == -1L) return
        toast(getString(R.string.matagi_downloading, version))
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
                    val session = with(context.packageManager.packageInstaller) {
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
            @SerialName("url")
            val assetUrl: String,
            @SerialName("browser_download_url")
            val browserDownloadURL: String
        )

        fun timeStamp(): Long {
            return dateFormat.parse(createdAt)!!.time
        }
    }

    private const val INSTALL_ACTION = "AppUpdater.INSTALL_ACTION"
}
