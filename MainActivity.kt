package com.smarttoolbd.app

import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.speech.RecognizerIntent
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SafeBrowsingResponse
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import java.net.URI
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var fileCallback: android.webkit.ValueCallback<Array<Uri>>? = null
    private val fileRequestCode = 4101
    private val speechRequestCode = 4102
    private val siteHost = "hmabumusa17-rgb.github.io"
    private val startUrl = "https://hmabumusa17-rgb.github.io/Smart-Tool-BD/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11, 20, 37)
        window.navigationBarColor = android.graphics.Color.rgb(11, 20, 37)
        webView = WebView(this)
        webView.layoutParams = FrameLayout.LayoutParams(-1, -1)
        webView.setBackgroundColor(android.graphics.Color.rgb(11, 20, 37))
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadsImagesAutomatically = true
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.mediaPlaybackRequiresUserGesture = true
        settings.setSupportMultipleWindows(false)
        CookieManager.getInstance().setAcceptCookie(true)
        webView.addJavascriptInterface(NativeBridge(), "SmartToolBDNative")
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "https" && uri.host == siteHost) return false
                if (uri.scheme == "http" || uri.scheme == "https") {
                    try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { }
                    return true
                }
                return true
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) Toast.makeText(this@MainActivity, "ইন্টারনেট সংযোগ পরীক্ষা করুন।", Toast.LENGTH_LONG).show()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView, callback: android.webkit.ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    startActivityForResult(params.createIntent(), fileRequestCode)
                    true
                } catch (_: ActivityNotFoundException) {
                    fileCallback = null
                    callback.onReceiveValue(null)
                    false
                }
            }
        }
        setContentView(webView)
        if (savedInstanceState == null) webView.loadUrl(startUrl) else webView.restoreState(savedInstanceState)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                    .setMimeType(mimeType)
                    .addRequestHeader("User-Agent", userAgent)
                    .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                    .setTitle("SmartToolBD download")
                    .setDescription("Downloading file")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SmartToolBD-" + System.currentTimeMillis())
                (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this, "ডাউনলোড শুরু হয়েছে।", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this, "ডাউনলোড করা যায়নি।", Toast.LENGTH_SHORT).show() }
        }
    }

    inner class NativeBridge {
        @JavascriptInterface fun startVoiceInput() {
            runOnUiThread {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "রোগীর সমস্যাটি বলুন")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                try { startActivityForResult(intent, speechRequestCode) }
                catch (_: ActivityNotFoundException) { webView.evaluateJavascript("window.stbOnSpeechError && window.stbOnSpeechError('এই ফোনে ভয়েস সার্ভিস পাওয়া যায়নি।');", null) }
            }
        }
    }

    @Deprecated("Deprecated in Android API; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileRequestCode) {
            val result = if (resultCode == RESULT_OK && data != null) WebChromeClient.FileChooserParams.parseResult(resultCode, data) else null
            fileCallback?.onReceiveValue(result)
            fileCallback = null
        } else if (requestCode == speechRequestCode) {
            if (resultCode == RESULT_OK) {
                val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
                val quoted = org.json.JSONObject.quote(text)
                webView.evaluateJavascript("window.stbOnSpeechResult && window.stbOnSpeechResult($quoted);", null)
            } else {
                webView.evaluateJavascript("window.stbOnSpeechError && window.stbOnSpeechError('ভয়েস ইনপুট বাতিল হয়েছে।');", null)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) { webView.saveState(outState); super.onSaveInstanceState(outState) }
    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
    override fun onDestroy() { if (::webView.isInitialized) webView.destroy(); super.onDestroy() }
}
