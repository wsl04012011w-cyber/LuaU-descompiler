package app.luau.readable

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MainActivity : Activity() {
    private external fun nativeStatus(): String
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val fileChooserRequestCode = 4107

    companion object {
        init { System.loadLibrary("luaureadable") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val web = WebView(this)
        web.webViewClient = WebViewClient()
        web.settings.javaScriptEnabled = true
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                        "application/octet-stream",
                        "application/x-luau",
                        "text/plain"
                    ))
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

                return try {
                    startActivityForResult(intent, fileChooserRequestCode)
                    true
                } catch (error: Exception) {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = null
                    Toast.makeText(this@MainActivity, "Não foi possível abrir os arquivos.", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
        Toast.makeText(this, nativeStatus(), Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Android, but supported for this activity's file chooser flow")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != fileChooserRequestCode) return

        val callback = fileChooserCallback ?: return
        fileChooserCallback = null

        if (resultCode != RESULT_OK || data == null) {
            callback.onReceiveValue(null)
            return
        }

        val results = mutableListOf<Uri>()
        data.clipData?.let { clip ->
            for (index in 0 until clip.itemCount) {
                results.add(clip.getItemAt(index).uri)
            }
        } ?: data.data?.let { results.add(it) }

        callback.onReceiveValue(results.toTypedArray())
    }

    override fun onDestroy() {
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = null
        super.onDestroy()
    }
}
