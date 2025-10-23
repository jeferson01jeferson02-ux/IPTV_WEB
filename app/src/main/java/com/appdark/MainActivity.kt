package com.appdark

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var infoTextView: TextView

    // URL do arquivo de credenciais no GitHub raw (substitua pelos seus dados)
    private val credentialsUrl = "https://raw.githubusercontent.com/seu_usuario/seu_repositorio/main/credentials.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        infoTextView = findViewById(R.id.footer_text)

        val webSettings: WebSettings = webView.settings
        with(webSettings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowContentAccess = true
            allowFileAccess = true
            setSupportZoom(true)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            loadWithOverviewMode = true
            useWideViewPort = true
            textZoom = 60
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectCSS()
                fetchCredentialsAndLogin()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            private var customView: View? = null
            private var customViewCallback: CustomViewCallback? = null

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                webView.visibility = View.GONE

                val decorView = window.decorView as ViewGroup
                decorView.addView(view, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ))

                enterFullscreen()
            }

            override fun onHideCustomView() {
                customView?.let {
                    window.decorView.apply {
                        (this as ViewGroup).removeView(it)
                    }
                }
                customView = null
                webView.visibility = View.VISIBLE
                customViewCallback?.onCustomViewHidden()
                exitFullscreen()
            }
        }

        // Carrega o site inicial
        webView.loadUrl("http://atmos.webplayer.one/")

        infoTextView.text = """
            Telegram - @Anonimofrio - webView - atmos.webplayer.one
        """.trimIndent()
    }

    private fun injectCSS() {
        val css = """
            var style = document.createElement('style');
            style.type = 'text/css';
            style.innerHTML = `
                body, html {
                    margin: 0 !important;
                    padding: 0 !important;
                    max-width: 100vw !important;
                    overflow-x: hidden !important;
                }
                img, video {
                    max-width: 100% !important;
                    height: auto !important;
                }
                .container, .content {
                    padding-left: 4px !important;
                    padding-right: 4px !important;
                }
            `;
            document.head.appendChild(style);
        """
        webView.evaluateJavascript(css, null)
    }

    private fun fetchCredentialsAndLogin() {
        val client = OkHttpClient()
        val request = Request.Builder().url(credentialsUrl).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Falha ao obter credenciais - opcional: mostrar mensagem para usuário
            }
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val json = JSONObject(body)
                    val usuario = json.getString("usuario")  // Ajuste as chaves se necessário
                    val senha = json.getString("senha")      // Ajuste as chaves se necessário

                    val js = """
                        (function() {
                            var u = document.getElementById('usuario'); // id do campo usuário
                            var p = document.getElementById('senha');   // id do campo senha
                            if(u) u.value = '601738542';
                            if(p) p.value = '297335743';
                            var form = document.querySelector('form');
                            if(form) form.submit();
                        })();
                    """
                    runOnUiThread {
                        webView.evaluateJavascript(js, null)
                    }
                }
            }
        })
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private fun enterFullscreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun exitFullscreen() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}