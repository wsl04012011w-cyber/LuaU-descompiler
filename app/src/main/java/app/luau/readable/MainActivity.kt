package app.luau.readable

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.database.Cursor
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : Activity() {
    private external fun nativeStatus(): String
    private external fun nativeDecompile(bytecode: ByteArray): String
    private lateinit var web: WebView
    private var sourceTree: Uri? = null
    private var destinationTree: Uri? = null
    private val sourceRequest = 4108
    private val destinationRequest = 4109
    private val sourceFiles = mutableListOf<FileEntry>()

    private data class FileEntry(val uri: Uri, val name: String, val size: Long, val relativePath: String)

    inner class AndroidBridge {
        @JavascriptInterface fun requestSourceFolder() {
            runOnUiThread { openTreePicker(sourceRequest) }
        }
        @JavascriptInterface fun requestDestinationFolder() {
            runOnUiThread { openTreePicker(destinationRequest) }
        }
        @JavascriptInterface fun getSourceFiles(): String {
            val array = JSONArray()
            synchronized(sourceFiles) {
                sourceFiles.forEachIndexed { index, file ->
                    array.put(JSONObject().put("id", index).put("name", file.name)
                        .put("path", file.relativePath).put("size", file.size))
                }
            }
            return array.toString()
        }
        @JavascriptInterface fun processFiles(selectedJson: String, makeZip: Boolean): String {
            val destination = destinationTree ?: return "ERRO: escolha uma pasta de destino primeiro."
            val ids = try { JSONArray(selectedJson) } catch (_: Exception) {
                return "ERRO: seleção de arquivos inválida."
            }
            val chosen = mutableListOf<FileEntry>()
            synchronized(sourceFiles) {
                for (i in 0 until ids.length()) {
                    val index = ids.optInt(i, -1)
                    if (index in sourceFiles.indices) chosen.add(sourceFiles[index])
                }
            }
            if (chosen.isEmpty()) return "ERRO: selecione pelo menos um arquivo."
            return try {
                val outputs = chosen.map { file ->
                    val bytes = contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                        ?: throw Exception("Não foi possível ler ${file.name}")
                    val result = nativeDecompile(bytes)
                    Pair(file.name.substringBeforeLast('.', file.name) + ".luau", result)
                }
                if (makeZip) {
                    val bytes = ByteArrayOutputStream()
                    ZipOutputStream(bytes).use { zip ->
                        outputs.forEach { (name, source) ->
                            zip.putNextEntry(ZipEntry(name))
                            zip.write(source.toByteArray(Charsets.UTF_8))
                            zip.closeEntry()
                        }
                    }
                    createOutput(destination, "luau-descompilado.zip", "application/zip", bytes.toByteArray())
                    "Concluído: ${outputs.size} arquivo(s) adicionados ao ZIP."
                } else {
                    outputs.forEach { (name, source) ->
                        createOutput(destination, name, "text/plain", source.toByteArray(Charsets.UTF_8))
                    }
                    "Concluído: ${outputs.size} arquivo(s) salvos na pasta escolhida."
                }
            } catch (error: Exception) {
                "ERRO: " + (error.message ?: "falha ao processar os arquivos")
            }
        }
    }

    companion object { init { System.loadLibrary("luaureadable") } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.webViewClient = WebViewClient()
        web.settings.javaScriptEnabled = true
        web.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?, filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                Toast.makeText(this@MainActivity, "Use a seleção de pasta dentro do app.", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
        Toast.makeText(this, nativeStatus(), Toast.LENGTH_SHORT).show()
    }

    private fun openTreePicker(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        try { startActivityForResult(intent, requestCode) }
        catch (_: Exception) { Toast.makeText(this, "Não foi possível abrir o seletor de pastas.", Toast.LENGTH_SHORT).show() }
    }

    @Deprecated("Deprecated API retained for the folder picker")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val tree = data.data!!
        try {
            contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } catch (_: Exception) { }
        when (requestCode) {
            sourceRequest -> {
                sourceTree = tree
                synchronized(sourceFiles) {
                    sourceFiles.clear()
                    scanTree(tree, DocumentsContract.getTreeDocumentId(tree), "")
                }
                web.evaluateJavascript("renderFilesFromAndroid()", null)
                Toast.makeText(this, "Pasta de origem carregada.", Toast.LENGTH_SHORT).show()
            }
            destinationRequest -> {
                destinationTree = tree
                web.evaluateJavascript("setDestinationChosen()", null)
                Toast.makeText(this, "Pasta de destino selecionada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scanTree(tree: Uri, documentId: String, parent: String) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(children, projection, null, null, null)
            while (cursor?.moveToNext() == true) {
                val id = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: "arquivo"
                val mime = cursor.getString(2) ?: ""
                val size = if (cursor.isNull(3)) 0L else cursor.getLong(3)
                val path = if (parent.isEmpty()) name else "$parent/$name"
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    scanTree(tree, id, path)
                } else if (name.endsWith(".luac", true) || name.endsWith(".luau", true)) {
                    sourceFiles.add(FileEntry(DocumentsContract.buildDocumentUriUsingTree(tree, id), name, size, path))
                }
            }
        } finally { cursor?.close() }
    }

    private fun createOutput(tree: Uri, name: String, mime: String, bytes: ByteArray) {
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
        val output = DocumentsContract.createDocument(contentResolver, root, mime, name)
            ?: throw Exception("Não foi possível criar $name na pasta de destino.")
        contentResolver.openOutputStream(output, "w")?.use { it.write(bytes) }
            ?: throw Exception("Não foi possível gravar $name.")
    }

    override fun onDestroy() {
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }
}
