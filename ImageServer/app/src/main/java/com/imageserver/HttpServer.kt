package com.imageserver

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import kotlin.concurrent.thread

class HttpServer(private val port: Int, private val context: Context) {

    @Volatile var folderUri: Uri? = null
    private var serverSocket: ServerSocket? = null
    @Volatile private var running = false

    fun start() {
        serverSocket = ServerSocket(port)
        running = true
        thread(name = "http-accept", isDaemon = true) {
            while (running) {
                try {
                    val client = serverSocket!!.accept()
                    thread(isDaemon = true) { handleClient(client) }
                } catch (_: SocketException) {
                    // socket closed on stop()
                } catch (e: IOException) {
                    if (running) e.printStackTrace()
                }
            }
        }
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: IOException) {}
        serverSocket = null
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 5000
            socket.use {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val out = socket.getOutputStream()

                val requestLine = reader.readLine() ?: return
                // drain remaining headers
                var line = reader.readLine()
                while (!line.isNullOrEmpty()) line = reader.readLine()

                val parts = requestLine.split(" ")
                if (parts.size < 2 || parts[0] != "GET") {
                    sendError(out, 405, "Method Not Allowed")
                    return
                }

                val path = URLDecoder.decode(parts[1].substringBefore("?").trimStart('/'), "UTF-8")

                when (path) {
                    "", "index.html" -> sendIndex(out)
                    "list"          -> sendFileList(out)
                    "health"        -> sendText(out, 200, "text/plain", "OK")
                    else            -> serveImage(out, path)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun serveImage(out: OutputStream, filename: String) {
        val uri = folderUri ?: run { sendError(out, 503, "No folder configured"); return }

        val treeDocId = DocumentsContract.getTreeDocumentId(uri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, treeDocId)

        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0) ?: continue
                val name  = cursor.getString(1) ?: continue
                val mime  = cursor.getString(2) ?: "application/octet-stream"

                if (name.equals(filename, ignoreCase = true)) {
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                    context.contentResolver.openInputStream(fileUri)?.use { stream ->
                        val bytes = stream.readBytes()
                        val header = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: $mime\r\n" +
                                "Content-Length: ${bytes.size}\r\n" +
                                "Cache-Control: max-age=60\r\n" +
                                "Access-Control-Allow-Origin: *\r\n" +
                                "Connection: close\r\n\r\n"
                        out.write(header.toByteArray())
                        out.write(bytes)
                        out.flush()
                    } ?: sendError(out, 500, "Cannot open file")
                    return
                }
            }
        } ?: run { sendError(out, 500, "Cannot read folder"); return }

        sendError(out, 404, "Not Found: $filename")
    }

    private fun sendIndex(out: OutputStream) {
        val images = getImageList()
        val html = buildString {
            append("<!DOCTYPE html><html><head>")
            append("<meta charset='utf-8'>")
            append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
            append("<title>Image Server</title>")
            append("<style>")
            append("*{box-sizing:border-box}body{font-family:sans-serif;margin:0;padding:16px;")
            append("background:#0f0e17;color:#eee}h1{color:#e94560;margin:0 0 4px}p{color:#888;")
            append("margin:0 0 16px;font-size:13px}.grid{display:flex;flex-wrap:wrap;gap:10px}")
            append(".card{background:#1a1a2e;border-radius:10px;overflow:hidden;width:150px}")
            append(".card img{width:100%;height:110px;object-fit:cover;display:block}")
            append(".card span{display:block;padding:6px 8px;font-size:11px;color:#aaa;")
            append("white-space:nowrap;overflow:hidden;text-overflow:ellipsis}")
            append("a{color:#e94560;text-decoration:none}")
            append("</style></head><body>")
            append("<h1>Image Server</h1>")
            append("<p>Port $port &nbsp;|&nbsp; <a href='/list'>JSON list</a> &nbsp;|&nbsp; ")
            append("${images.size} image${if (images.size != 1) "s" else ""}</p>")
            append("<div class='grid'>")
            if (images.isEmpty()) {
                append("<p style='color:#666'>No images found. Select a folder in the app.</p>")
            }
            images.forEach { name ->
                append("<div class='card'><a href='/$name'>")
                append("<img src='/$name' loading='lazy' alt='$name'>")
                append("</a><span>$name</span></div>")
            }
            append("</div></body></html>")
        }
        sendText(out, 200, "text/html; charset=utf-8", html)
    }

    private fun sendFileList(out: OutputStream) {
        val images = getImageList()
        val json = images.joinToString(",\n  ", "[\n  ", "\n]") { "\"$it\"" }
        sendText(out, 200, "application/json", json)
    }

    private fun getImageList(): List<String> {
        val uri = folderUri ?: return emptyList()
        val treeDocId = DocumentsContract.getTreeDocumentId(uri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, treeDocId)
        val result = mutableListOf<String>()

        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0) ?: continue
                val mime = cursor.getString(1) ?: continue
                if (mime.startsWith("image/")) result.add(name)
            }
        }
        return result
    }

    private fun sendText(out: OutputStream, code: Int, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code OK\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray())
        out.write(bytes)
        out.flush()
    }

    private fun sendError(out: OutputStream, code: Int, message: String) {
        val body = "<h1>$code — $message</h1>"
        val bytes = body.toByteArray()
        val header = "HTTP/1.1 $code $message\r\nContent-Type: text/html\r\n" +
                "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray())
        out.write(bytes)
        out.flush()
    }
}
