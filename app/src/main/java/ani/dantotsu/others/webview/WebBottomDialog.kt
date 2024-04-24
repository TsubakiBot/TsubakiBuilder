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
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import ani.dantotsu.R
import ani.dantotsu.databinding.BottomSheetWebBinding
import ani.dantotsu.view.dialog.BottomSheetDialogFragment
import ani.himitsu.os.Version
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
        configureWebView(mWebView)
    }

    @SuppressLint("AddJavascriptInterface", "SetJavaScriptEnabled")
    private fun configureWebView(webView: WebView?) {
        if (null == webView) return
        val webViewSettings = webView.settings
        webView.isScrollbarFadingEnabled = true
        webViewSettings.loadWithOverviewMode = true
        webViewSettings.useWideViewPort = true
        webViewSettings.allowFileAccess = true
        webViewSettings.allowContentAccess = false
        webViewSettings.javaScriptEnabled = true
        webViewSettings.domStorageEnabled = true
        webViewSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        if (Version.isLollipop) {
            val assetLoader = WebViewAssetLoader.Builder().addPathHandler(
                "/assets/",
                AssetsPathHandler(requireContext())
            ).build()
            webView.webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(
                    view: WebView, request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onReceivedSslError(
                    view: WebView?, handler: SslErrorHandler, error: SslError?
                ) {
                    // handler.proceed()
                    handler.cancel()
                }
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

    fun hasGoneBack() : Boolean {
        return if (mWebView?.canGoBack() == true) {
            mWebView?.goBack()
            true
        } else {
            false
        }
    }

    @Suppress("unused")
    private inner class JavaScriptInterface {
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