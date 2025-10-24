package com.iptvservidor

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
import java.io.InputStreamReader
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var infoTextView: TextView
    private var loginSubmetido = false
    private val credentialsUrl = "https://raw.githubusercontent.com/jeferson01jeferson02-ux/IPTV_WEB/main/credentials.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        infoTextView = findViewById(R.id.footer_text)

        setupWebSettings()
        setupCookiePolicy()
        setupWebClients()

        webView.loadUrl("http://web.turboxweb.com")
        infoTextView.text = "☞ Telegram - @Anonimofrio - webView ☜"

        hideSystemBarsImmersive()
    }

    private fun setupWebSettings() {
        val ws = webView.settings
        with(ws) {
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
            textZoom = 100
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        }
    }

    private fun setupCookiePolicy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }
    }

    private fun setupWebClients() {
        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectGlobalCss()
                injectScrollFixJs() // Scroll fix adicionado aqui
                if (!loginSubmetido) checkIfLoginFormPresentAndLogin()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                loginSubmetido = false
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            private var customView: View? = null
            private var customViewCallback: CustomViewCallback? = null

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) { callback?.onCustomViewHidden(); return }
                customView = view
                customViewCallback = callback
                webView.visibility = View.GONE
                val decorView = window.decorView as ViewGroup
                decorView.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                enterFullscreen()
            }

            override fun onHideCustomView() {
                customView?.let { (window.decorView as ViewGroup).removeView(it) }
                customView = null
                webView.visibility = View.VISIBLE
                customViewCallback?.onCustomViewHidden()
                exitFullscreen()
            }
        }
    }

    private fun injectGlobalCss() {
        val css = """
            (function(){
                try {
                    var style = document.getElementById('iptv-app-style');
                    if(!style) {
                        style = document.createElement('style');
                        style.id = 'iptv-app-style';
                        var rules='';
                        rules += 'html, body { height:100%; margin:0; padding:0; background:#000; color:#fff; overflow:auto; }';
                        rules += 'body > * { box-sizing:border-box; }';
                        rules += '#login-form, form { width:320px !important; max-width:90% !important; margin:30px auto !important; position:relative !important; }';
                        rules += 'input, button, select, textarea { font-size:16px !important; padding:10px !important; }';
                        rules += '.container, .main, #app { width:100% !important; margin:0 !important; padding:0 !important; }';
                        rules += '.header, .footer { display:none !important; }';
                        rules += '.player, video { width:100% !important; height:100% !important; }';
                        style.appendChild(document.createTextNode(rules));
                        document.head.appendChild(style);
                    }
                } catch(e){}
            })();
        """
        webView.evaluateJavascript(css, null)
    }

    // Nova função para corrigir scroll das categorias
    private fun injectScrollFixJs() {
        val js = """
            (function(){
                try {
                    // Todos os elementos que estavam fixos passam a rolar
                    var fixedEls = document.querySelectorAll('header, .header, .footer, .sticky, .navbar, .player');
                    for(var i=0;i<fixedEls.length;i++){
                        fixedEls[i].style.position='relative';
                        fixedEls[i].style.top='0';
                        fixedEls[i].style.left='0';
                        fixedEls[i].style.width='100%';
                    }
                    // Containers principais com overflow auto
                    var containers = document.querySelectorAll('.category-list, .channels-list, .main, #app');
                    for(var i=0;i<containers.length;i++){
                        containers[i].style.overflow='auto';
                        containers[i].style.maxHeight='100%';
                    }
                    document.body.style.overflow='auto';
                } catch(e){}
            })();
        """
        webView.evaluateJavascript(js, null)
    }

    private fun checkIfLoginFormPresentAndLogin() {
        val js = """
            (function(){
                var u=document.querySelector('input[name="username"], input[name="user"], input[type="email"], input[type="text"]');
                var p=document.querySelector('input[name="password"], input[type="password"]');
                return JSON.stringify({hasLogin: !!(u && p)});
            })();
        """
        webView.evaluateJavascript(js) { result ->
            try {
                if (result != null && result.contains("true") && !loginSubmetido) {
                    loginSubmetido = true
                    fetchCredentialsAndLogin()
                }
            } catch (_: Exception) { if (!loginSubmetido) { loginSubmetido = true; fetchCredentialsAndLogin() } }
        }
    }

    private fun fetchCredentialsAndLogin() {
        infoTextView.post { infoTextView.text = "Obtendo credenciais..." }
        try {
            assets.open("credentials.json").use { stream ->
                val bodyStr = InputStreamReader(stream).readText()
                applyCredentialsToWeb(bodyStr)
                return
            }
        } catch (_: Exception) { Log.i("MainActivity", "credentials.json não está em assets, usando URL remota") }

        val client = OkHttpClient()
        val request = Request.Builder().url(credentialsUrl).build()
        client.newCall(request).enqueue(object: Callback{
            override fun onFailure(call: Call, e: IOException) { e.printStackTrace(); runOnUiThread { infoTextView.text="Falha ao obter credenciais" }; loginSubmetido=false }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) { runOnUiThread { infoTextView.text="Erro HTTP: ${'$'}{response.code}" }; loginSubmetido=false; return }
                val bodyStr = response.body?.string() ?: run { runOnUiThread { infoTextView.text="Credenciais vazias" }; loginSubmetido=false; return }
                applyCredentialsToWeb(bodyStr)
            }
        })
    }

    private fun applyCredentialsToWeb(bodyStr: String) {
        try {
            val json = JSONObject(bodyStr)
            val usuario = json.optString("usuario","")
            val senha = json.optString("senha","")
            val js = """
                (function() {
                    try {
                        function dispatchInput(el,value){var ev=new Event('input',{bubbles:true}); el.value=value; el.dispatchEvent(ev);}
                        var u=document.querySelector('input[name="username"], input[name="user"], input[type="email"], input[type="text"]');
                        var p=document.querySelector('input[name="password"], input[type="password"]');
                        if(u){dispatchInput(u,'${escapeJs(usuario)}'); u.focus();}
                        if(p){dispatchInput(p,'${escapeJs(senha)}'); p.focus();}
                        var btns=['#login-button','button[type="submit"]','button.login','input[type="submit"]','button']; var clicked=false;
                        for(var i=0;i<btns.length && !clicked;i++){ var b=document.querySelector(btns[i]); if(b){b.click(); clicked=true; break;}}
                        if(!clicked){var frm=(u&&u.form)||(p&&p.form)||document.querySelector('form'); if(frm){try{frm.submit();}catch(e){var s=frm.querySelector('[type="submit"]'); if(s)s.click();}}}
                        return true;
                    }catch(e){return false;}
                })();
            """
            runOnUiThread { webView.evaluateJavascript(js,null); infoTextView.text="Login automático: tentando..." }
        } catch (e: Exception) { e.printStackTrace(); runOnUiThread { infoTextView.text="Erro ao analisar JSON" }; loginSubmetido=false }
    }

    private fun escapeJs(s:String) = s.replace("\\","\\\\").replace("'","\\'").replace("\n","\\n").replace("\r","")
    override fun onBackPressed() { if(webView.canGoBack()) webView.goBack() else super.onBackPressed() }
    override fun onResume() { super.onResume(); hideSystemBarsImmersive() }
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if(hasFocus) hideSystemBarsImmersive() }

    private fun hideSystemBarsImmersive() {
        try {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (_: Exception) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private fun enterFullscreen() { hideSystemBarsImmersive() }
    private fun exitFullscreen() {
        try {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
        } catch (_: Exception) { window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE }
    }
}
