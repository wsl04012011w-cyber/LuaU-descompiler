package app.luau.readable

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MainActivity : Activity() {
    private external fun nativeStatus(): String

    companion object {
        init { System.loadLibrary("luaureadable") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val web = WebView(this)
        web.webViewClient = WebViewClient()
        web.settings.javaScriptEnabled = false
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
        Toast.makeText(this, nativeStatus(), Toast.LENGTH_SHORT).show()
    }
}
