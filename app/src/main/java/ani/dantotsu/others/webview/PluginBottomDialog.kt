/*
 * ====================================================================
 * Copyright (c) 2023 AbandonedCart.  All rights reserved.
 *
 * https://github.com/AbandonedCart/AbandonedCart/blob/main/LICENSE#L4
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */
package ani.dantotsu.others.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.addCallback
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import ani.dantotsu.R
import ani.dantotsu.databinding.BottomSheetWebBinding
import bit.himitsu.lineSeparator
import ani.dantotsu.others.webview.AdBlocker.createEmptyResource
import ani.dantotsu.others.webview.AdBlocker.isAd
import bit.himitsu.sanitized
import ani.dantotsu.view.dialog.BottomSheetDialogFragment
import bit.himitsu.os.Version
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


class PluginBottomDialog(val location: String) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetWebBinding? = null
    val binding get() = _binding!!
    private val webHandler = Handler(Looper.getMainLooper())
    private var mWebView: WebView? = null

    private var novelTitle = ""

    val cookies: CookieManager? = Injekt.get<NetworkHelper>().cookieJar.manager
    val cfTag = "cf_clearance"
    var cfCookie: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetWebBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mWebView = binding.webviewContent
        with (dialog as androidx.activity.ComponentDialog) {
            onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                if (mWebView?.canGoBack() == true) {
                    mWebView?.goBack()
                } else {
                    this@PluginBottomDialog.dismiss()
                }
            }
        }
        configureWebView(mWebView)
    }

    @SuppressLint("AddJavascriptInterface", "SetJavaScriptEnabled")
    private fun configureWebView(webView: WebView?) {
        if (null == webView) return
        val webViewSettings = webView.settings
        webView.isScrollbarFadingEnabled = true
        webViewSettings.loadWithOverviewMode = true
        webViewSettings.useWideViewPort = true
        webViewSettings.allowContentAccess = false
        webViewSettings.javaScriptEnabled = true
        webViewSettings.domStorageEnabled = true
        webViewSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        if (Version.isLollipop) {
            val assetLoader = WebViewAssetLoader.Builder().addPathHandler(
                "/assets/", AssetsPathHandler(requireContext())
            ).build()
            webView.webViewClient = object : WebViewClientCompat() {
                private val loadedUrls: HashMap<Uri, Boolean> = hashMapOf()
                override fun shouldInterceptRequest(
                    view: WebView, request: WebResourceRequest
                ): WebResourceResponse? {
                    val ad: Boolean =
                        if (!loadedUrls.containsKey(request.url)) {
                            isAd(request.url).also { loadedUrls[request.url] = it }
                        } else {
                            loadedUrls[request.url] == true
                        }
                    return if (ad) {
                        createEmptyResource()
                    } else {
                        assetLoader.shouldInterceptRequest(request.url)
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    val address = url?.substringAfter("/novel/") ?: ""
                    val slashes = address.split("/").size - 1
                    if (slashes == 0) {
                        webView.keepScreenOn = false
                        webView.clearHistory()
                    } else if (slashes == 1) {
                        val cookie = cookies?.getCookie(url)
                        if (cookie?.contains(cfTag) == true) {
                            cfCookie = cookie.substringAfter("$cfTag=").substringBefore(";")
                        }
                    }
                    super.onPageStarted(view, url, favicon)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    val address = url?.substringAfter("/novel/") ?: ""
                    val slashes = address.split("/").size - 1
                    when {
                        slashes == 1 -> {
                            webView.loadUrl("javascript:window.Android.handleNovel" +
                                    "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
                        }
                        slashes > 1 -> {
                            webView.keepScreenOn = true
                            webView.loadUrl("javascript:window.Android.handleChapter" +
                                    "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
                        }
                        else -> {
                            super.onPageFinished(view, url)
                        }
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?, handler: SslErrorHandler, error: SslError?
                ) { handler.cancel() }
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
                ServiceWorkerControllerCompat.getInstance().setServiceWorkerClient(
                    object : ServiceWorkerClientCompat() {
                        override fun shouldInterceptRequest(
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            return assetLoader.shouldInterceptRequest(request.url)
                        }
                    })
            }
        } else {
            @Suppress("deprecation")
            webViewSettings.allowFileAccessFromFileURLs = true
            @Suppress("deprecation")
            webViewSettings.allowUniversalAccessFromFileURLs = true
        }
        val download = JavaScriptInterface()
        webView.addJavascriptInterface(download, "Android")
        webView.setDownloadListener { url: String, _: String?, _: String?, mimeType: String, _: Long ->
            if (url.startsWith("blob") || url.startsWith("data")) {
                webView.loadUrl(download.getBase64StringFromBlob(url, mimeType))
            }
        }
        loadWebsite(location)
    }

    private fun loadWebsite(address: String) {
        mWebView?.let {
            val webViewSettings = it.settings
            webViewSettings.setSupportZoom(true)
            webViewSettings.builtInZoomControls = true
            it.loadUrl(address)
        } ?: webHandler.postDelayed({ loadWebsite(address) }, 50L)
    }

    private fun downloadCover(url: String, directory: File) {
        CoroutineScope(Dispatchers.IO).launch {
            val urlConnection = URL(url).openConnection() as HttpURLConnection
            urlConnection.setRequestProperty(
                "User-Agent", NetworkHelper.defaultUserAgentProvider()
            )
            cfCookie?.let {
                urlConnection.setRequestProperty("Cookie", "${cfTag}=${it};")
            }
            urlConnection.connect()
            if (urlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                val bitmap = BitmapFactory.decodeStream(urlConnection.inputStream)
                FileOutputStream(File(directory, "cover.png")).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }

    @Suppress("unused")
    private inner class JavaScriptInterface {

        @JavascriptInterface
        fun handleNovel(html: String) {
            val doc = Jsoup.parse(html)
            doc.selectFirst("div.post-title")?.selectFirst("h1")?.text()?.let { novel ->
                novelTitle = novel
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Dantotsu/Novel/${novel.sanitized}"
                ).apply { if (!exists()) mkdirs() }
                val img =  doc.selectFirst("div.summary_image")?.selectFirst("img")
                img?.absUrl("data-src")?.ifBlank { img.absUrl("src") }
                    ?.also { url -> downloadCover(url, directory) }
            }
        }

        @JavascriptInterface
        fun handleChapter(html: String) {
            val doc = Jsoup.parse(html)
            doc.selectFirst("div.nav-next.premium")?.let {
                doc.selectFirst("a.prev_page")?.attr("href")?.let {
                    mWebView?.postDelayed( {
                        mWebView?.loadUrl("${it.substringBefore("/novel/")}/novel/")
                    }, 500L)
                }
            }
            doc.selectFirst("a.next_page")?.attr("href")?.let { page ->
                doc.selectFirst("h1#chapter-heading")?.text()?.let { novel ->
                    novelTitle = novelTitle.ifBlank { novel.substringBefore(" - Ch") }
                    val directory = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "Dantotsu/Novel/${novelTitle.sanitized}"
                    ).apply { if (!exists()) mkdirs() }
                    val content = doc.selectFirst("div.reading-content")
                    val text = content?.selectFirst("div.text-left")
                    val title = text?.selectFirst("h1, h2, h3, h4")?.text()
                        ?: novel.substringAfter("$novelTitle - ")
                    val chapter = StringBuilder(title).append(lineSeparator)
                    text?.select("p")?.forEach { paragraph ->
                        chapter.append(lineSeparator)
                        if (paragraph.text() == "&nbsp;") {
                            chapter.append(" ")
                        } else {
                            val span = paragraph.select("span")
                            if (span.isEmpty()) {
                                chapter.append(paragraph.text())
                            } else {
                                span.forEach { chapter.append(it.text()) }
                            }
                        }
                        chapter.append(lineSeparator)
                    }
                    chapter.append(lineSeparator).append(lineSeparator)
                    novelTitle.let { file ->
                        FileOutputStream(File(directory, file.sanitized), true).use {
                            it.write(chapter.toString().toByteArray())
                        }
                    }
                }
                mWebView?.postDelayed( { mWebView?.loadUrl(page) }, 500L)
            } ?: doc.selectFirst("a.prev_page")?.attr("href")?.let {
                mWebView?.postDelayed( {
                    mWebView?.loadUrl("${it.substringBefore("/novel/")}/novel/")
                }, 500L)
            }
        }

        @JavascriptInterface
        @Throws(IOException::class)
        fun getBase64FromBlobData(base64Data: String) {
            convertBase64StringSave(base64Data)
        }

        fun getBase64StringFromBlob(blobUrl: String, mimeType: String): String {
            return if (blobUrl.startsWith("blob") || blobUrl.startsWith("data")) {
                "javascript: var xhr = new XMLHttpRequest();" +
                        "xhr.open('GET', '" + blobUrl + "', true);" +
                        "xhr.setRequestHeader('Content-type','" + mimeType + "');" +
                        "xhr.responseType = 'blob';" +
                        "xhr.onload = function(e) {" +
                        "  if (this.status == 200) {" +
                        "    var blobFile = this.response;" +
                        "    var reader = new FileReader();" +
                        "    reader.readAsDataURL(blobFile);" +
                        "    reader.onloadend = function() {" +
                        "      base64data = reader.result;" +
                        "      Android.getBase64FromBlobData(base64data);" +
                        "    }" +
                        "  }" +
                        "};" +
                        "xhr.send();"
            } else "javascript: console.log('Not a valid blob URL');"
        }

        @Throws(IOException::class)
        private fun convertBase64StringSave(base64File: String) {
            val zipType = getString(R.string.mimetype_zip)
            if (base64File.contains("data:$zipType;")) {
                val filePath = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "download.zip"
                )
                FileOutputStream(filePath, false).use {
                    it.write(Base64.decode(base64File.replaceFirst(
                        "^data:$zipType;base64,".toRegex(), ""
                    ), 0))
                    it.flush()
                }
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    companion object {
        fun newInstance(url: String) = PluginBottomDialog(url)
    }
}
