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
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import ani.dantotsu.others.webview.AdBlocker.createEmptyResource
import ani.dantotsu.others.webview.AdBlocker.isAd
import ani.dantotsu.sanitized
import ani.dantotsu.view.dialog.BottomSheetDialogFragment
import ani.himitsu.os.Version
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class WebBottomDialog(val location: String) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetWebBinding? = null
    val binding get() = _binding!!
    private val webHandler = Handler(Looper.getMainLooper())
    private var mWebView: WebView? = null

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
                    this@WebBottomDialog.dismiss()
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
                    return if (ad) createEmptyResource() else assetLoader.shouldInterceptRequest(request.url)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    val address = url?.substringAfter("/novel/") ?: ""
                    if (address.split("/").size - 1 < 1) {
                        webView.keepScreenOn = false
                        webView.clearHistory()
                    }
                    super.onPageStarted(view, url, favicon)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    val address = url?.substringAfter("/novel/") ?: ""
                    if (address.split("/").size - 1 > 1) {
                        webView.keepScreenOn = true
                        webView.loadUrl("javascript:window.Android.handleHtml" +
                                "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
                    } else {
                        super.onPageFinished(view, url)
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

    @Suppress("unused")
    private inner class JavaScriptInterface {
        @JavascriptInterface
        fun handleHtml(html: String) {
            val doc = Jsoup.parse(html)
            val novel = doc.selectFirst("h1#chapter-heading")?.text()
            doc.selectFirst("div.nav-next.premium")?.let {
                doc.selectFirst("a.prev_page")?.attr("href")?.let {
                    mWebView?.postDelayed( {
                        mWebView?.loadUrl("${it.substringBefore("/novel/")}/novel/")
                    }, 1000L)
                }
            }
            doc.selectFirst("a.next_page")?.attr("href")?.let { page ->
                novel?.let { name ->
                    val directory = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "Dantotsu/Novel/${name.substringBefore(" - Ch").sanitized}"
                    ).apply { if (!exists()) mkdirs() }
                    val content = doc.selectFirst("div.reading-content")
                    val text = content?.selectFirst("div.text-left")
                    val title = text?.selectFirst("h1, h2, h3, h4")?.text()
                        ?: "Ch${name.substringAfter(" - Ch", )}"
                    var chapter = (title ?: "") + "\n"
                    text?.select("p")?.forEach { paragraph ->
                        if (paragraph.text() == "&nbsp;") {
                            chapter += "\n"
                        } else {
                            val span = paragraph.select("span")
                            if (span.isEmpty()) {
                                chapter += "\n${paragraph.text()}"
                            } else {
                                span.forEach { chapter += "\n${it.text()}" }
                            }
                        }
                    }
                    title?.let { chap ->
                        FileOutputStream(File(directory, chap.sanitized)).use {
                            it.write(chapter.toByteArray())
                        }
                    }
                }
                mWebView?.postDelayed( { mWebView?.loadUrl(page) }, 1000L)
            } ?: doc.selectFirst("a.prev_page")?.attr("href")?.let {
                mWebView?.postDelayed( {
                    mWebView?.loadUrl("${it.substringBefore("/novel/")}/novel/")
                }, 1000L)
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
        fun newInstance(url: String) = WebBottomDialog(url)
    }
}
