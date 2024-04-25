package ani.dantotsu.others.webview

import android.net.Uri
import android.text.TextUtils
import android.webkit.WebResourceResponse
import ani.dantotsu.currContext
import ani.dantotsu.tryWithSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.net.MalformedURLException


object AdBlocker {
    private val AD_HOSTS: MutableList<String> = mutableListOf()
    private const val AD_HOSTS_FILE = "pgl.yoyo.org.txt"
    init {
        CoroutineScope(Dispatchers.IO).launch {
            tryWithSuspend {
                currContext().assets.open(AD_HOSTS_FILE).use { stream ->
                    stream.source().buffer().use { buffer ->
                        do {
                            val line = buffer.readUtf8Line()?.also { AD_HOSTS.add(it) }
                        } while (line !== null)
                    }
                }
            }
        }
    }

    fun isAd(url: Uri): Boolean {
        return try {
            getHost(url)?.let { isAdHost(it) } ?: false
        } catch (e: MalformedURLException) { false }
    }

    private fun isAdHost(host: String): Boolean {
        if (TextUtils.isEmpty(host)) return false
        val index = host.indexOf(".")
        return index >= 0 && (AD_HOSTS.contains(host) ||
                index + 1 < host.length && isAdHost(host.substring(index + 1)))
    }

    fun createEmptyResource(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain", "utf-8", ByteArrayInputStream("".toByteArray())
        )
    }

    @Throws(MalformedURLException::class)
    fun getHost(url: Uri): String? {
        return url.host
    }
}