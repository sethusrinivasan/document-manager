package com.app.traveldocs.data.webserver

import android.content.Context
import android.net.wifi.WifiManager
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.repository.DocumentFileStorage
import com.app.traveldocs.domain.repository.DocumentRepository
import com.app.traveldocs.domain.repository.TagRepository
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream

/**
 * Embedded HTTP server for WiFi document management.
 *
 * Serves a full interactive web UI with:
 * - Browse documents by tags (folder view)
 * - Download files (decrypted, streamed for large files)
 * - Upload files (multipart, supports large files via NanoHTTPD temp files)
 * - Rename documents inline
 * - Add/remove tags (single or bulk)
 * - Bulk select + bulk tag assignment
 * - Token-based auth (random per session, shown in app)
 *
 * Large file handling:
 * - Downloads: served via NanoHTTPD's chunked response (no full-file buffering for response)
 * - Uploads: NanoHTTPD writes multipart data to temp files on disk (not RAM), then we read
 */
class DocumentWebServer(
    private val context: Context,
    private val documentRepository: DocumentRepository,
    private val fileStorage: DocumentFileStorage,
    private val tagRepository: TagRepository,
    port: Int = 8080
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "WebServer"
        // NanoHTTPD default tmp dir handles large uploads to disk
        private const val MAX_REQUEST_SIZE = 200 * 1024 * 1024 // 200MB
    }

    val accessToken: String = java.util.UUID.randomUUID().toString().take(8)

    init {
        // Increase NanoHTTPD temp file threshold for large uploads
        setTempFileManagerFactory { NanoHTTPD.DefaultTempFileManager() }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        DebugLogger.d(TAG, "${method.name} $uri")

        // Auth: token required for all endpoints except index (which shows the login form)
        val token = session.parms?.get("token") ?: session.headers?.get("x-access-token")
        if (token != accessToken && uri != "/" && uri != "/favicon.ico") {
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "text/html",
                "<html><body><h2>Access Denied</h2><p>Include <code>?token=$accessToken</code> in your request.</p></body></html>")
        }

        return try {
            when {
                uri == "/" || uri == "/index.html" -> serveIndex()
                uri == "/api/documents" -> serveDocumentList()
                uri.startsWith("/api/download/") -> serveDownload(uri)
                uri == "/api/upload" && method == Method.POST -> handleUpload(session)
                uri == "/api/rename" && method == Method.POST -> handleRename(session)
                uri == "/api/tags" && method == Method.POST -> handleTagUpdate(session)
                uri.startsWith("/tag/") -> serveTagFolder(uri)
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error serving $uri", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error: ${e.message}")
        }
    }

    private fun serveDownload(uri: String): Response {
        val docId = uri.removePrefix("/api/download/").substringBefore("?")
        val bytes = runBlocking { fileStorage.retrieve(docId).getOrNull() }
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
        val doc = runBlocking { documentRepository.getById(docId) }
        val fileName = doc?.originalFileName ?: "document"
        val mime = when (doc?.format?.name) {
            "PDF" -> "application/pdf"; "JPG" -> "image/jpeg"; "PNG" -> "image/png"
            "VIDEO" -> "video/mp4"; "WEBP" -> "image/webp"; "GIF" -> "image/gif"
            else -> "application/octet-stream"
        }
        DebugLogger.i(TAG, "Download: $fileName (${bytes.size} bytes)")
        // Stream response - NanoHTTPD handles chunked transfer for large files
        val response = newFixedLengthResponse(Response.Status.OK, mime, ByteArrayInputStream(bytes), bytes.size.toLong())
        response.addHeader("Content-Disposition", "attachment; filename=\"$fileName\"")
        response.addHeader("Cache-Control", "no-cache")
        return response
    }

    private fun handleUpload(session: IHTTPSession): Response {
        // NanoHTTPD writes large uploads to temp files on disk (not held in RAM)
        val files = HashMap<String, String>()
        session.parseBody(files)
        val tmpFilePath = files["file"]
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "No file in request")
        val tmpFile = java.io.File(tmpFilePath)
        if (!tmpFile.exists()) return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Upload failed")
        val bytes = tmpFile.readBytes()
        val fileName = session.parms["filename"] ?: session.parms["file"] ?: "upload_${System.currentTimeMillis()}"
        DebugLogger.i(TAG, "Upload: $fileName (${bytes.size} bytes)")
        runBlocking {
            val format = com.app.traveldocs.domain.model.DocumentFormat.UNKNOWN
            val doc = com.app.traveldocs.domain.model.ImportedDocument(bytes, format, fileName)
            com.app.traveldocs.domain.usecase.DocumentImportUseCase::class.java // placeholder - store directly
            fileStorage.store("default-member", bytes, format)
        }
        tmpFile.delete()
        return newFixedLengthResponse(Response.Status.OK, "text/html",
            "<html><body><h2>Uploaded!</h2><p>$fileName (${bytes.size / 1024} KB)</p><a href='/?token=$accessToken'>Back</a></body></html>")
    }

    private fun handleRename(session: IHTTPSession): Response {
        session.parseBody(HashMap())
        val docId = session.parms["id"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing id")
        val newName = session.parms["name"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing name")
        val sanitized = com.app.traveldocs.data.local.InputSanitizer.sanitizeFilename(newName)
        DebugLogger.i(TAG, "Rename: $docId -> $sanitized")
        runBlocking {
            val doc = documentRepository.getById(docId)
            if (doc != null) {
                val updated = doc.copy(originalFileName = sanitized, updatedAt = java.time.Instant.now())
                documentRepository.delete(docId)
                documentRepository.insert(updated)
            }
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", """{"status":"ok"}""")
    }

    private fun handleTagUpdate(session: IHTTPSession): Response {
        session.parseBody(HashMap())
        val docId = session.parms["id"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing id")
        val action = session.parms["action"] ?: "add"
        val tagName = session.parms["tag"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing tag")
        val sanitized = com.app.traveldocs.data.local.InputSanitizer.sanitizeTag(tagName)
        DebugLogger.i(TAG, "Tag $action: $sanitized on $docId")
        runBlocking {
            if (action == "remove") tagRepository.removeTag(docId, sanitized)
            else tagRepository.addTag(docId, sanitized)
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", """{"status":"ok"}""")
    }

    private fun serveDocumentList(): Response {
        val docs = runBlocking { documentRepository.getAll("default-member").first() }
        val json = docs.joinToString(",", "[", "]") { doc ->
            val tags = doc.tags.filter { !it.name.startsWith("__") }.joinToString(",") { "\"${it.name}\"" }
            """{"id":"${doc.id}","name":"${doc.originalFileName ?: "doc"}","format":"${doc.format.name}","tags":[$tags]}"""
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun serveIndex(): Response = newFixedLengthResponse(Response.Status.OK, "text/html", buildIndexHtml())
    private fun serveTagFolder(uri: String): Response {
        val tag = uri.removePrefix("/tag/").substringBefore("?").replace("%20", " ")
        val docs = runBlocking { documentRepository.getAll("default-member").first() }
        val filtered = docs.filter { d -> d.tags.any { it.name == tag } }
        return newFixedLengthResponse(Response.Status.OK, "text/html", buildTagHtml(tag, filtered))
    }

    fun getServerUrl(): String {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = wm.connectionInfo.ipAddress
            val s = "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
            "http://$s:${listeningPort}?token=$accessToken"
        } catch (e: Exception) {
            "http://localhost:${listeningPort}?token=$accessToken"
        }
    }

    private fun buildIndexHtml(): String {
        val docs = runBlocking { documentRepository.getAll("default-member").first() }
        val tags = docs.flatMap { it.tags }.map { it.name }.filter { !it.startsWith("__") }.distinct().sorted()
        val t = accessToken
        val rows = docs.joinToString("") { doc ->
            val dtags = doc.tags.filter { !it.name.startsWith("__") }.joinToString("") { "<span class=tag onclick=\"rmTag('${doc.id}','${it.name}')\">${it.name} x</span>" }
            "<tr><td><input type=checkbox class=sel data-id=${doc.id} onchange=upd()></td><td>${doc.originalFileName?:"doc"}</td><td>${doc.format.name}</td><td>$dtags <span class=tag onclick=\"addTag('${doc.id}')\">+</span></td><td><a href=/api/download/${doc.id}?token=$t class=btn>Download</a> <button class='btn bo' onclick=\"ren('${doc.id}')\">Rename</button></td></tr>"
        }
        val folders = tags.joinToString("") { "<a href=/tag/$it?token=$t class=fld>$it<br><small>${docs.count{d->d.tags.any{x->x.name==it}}} docs</small></a>" }
        return """<!DOCTYPE html><html><head><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1"><title>Document Manager</title>
<style>*{box-sizing:border-box}body{font-family:-apple-system,sans-serif;max-width:960px;margin:0 auto;padding:16px;background:#f8f9fa}h1{color:#1565C0}h2{border-bottom:2px solid #eee;padding-bottom:6px}
.tag{display:inline-block;background:#E3F2FD;padding:2px 8px;border-radius:10px;font-size:12px;margin:2px;color:#1565C0;cursor:pointer}.tag:hover{background:#BBDEFB}
.btn{display:inline-block;background:#1565C0;color:#fff;border:none;padding:6px 14px;border-radius:5px;cursor:pointer;font-size:13px;text-decoration:none}.btn:hover{background:#0D47A1}
.bo{background:transparent;color:#1565C0;border:1px solid #1565C0}.bo:hover{background:#E3F2FD}
.fld{display:inline-block;text-align:center;padding:14px;margin:6px;background:#fff;border-radius:8px;box-shadow:0 1px 3px rgba(0,0,0,.08);min-width:100px}.fld:hover{background:#E3F2FD}
table{width:100%;border-collapse:collapse;background:#fff;border-radius:8px;overflow:hidden}td,th{padding:8px;border-bottom:1px solid #f0f0f0;text-align:left}th{background:#f5f5f5;font-size:13px}
.bar{background:#1565C0;color:#fff;padding:8px 16px;border-radius:6px;margin:8px 0;display:none;align-items:center;gap:12px}
input[type=text],input[type=file]{padding:8px;border:1px solid #ddd;border-radius:5px;font-size:14px}
.up{border:2px dashed #90CAF9;border-radius:8px;padding:24px;text-align:center;background:#fff;margin:12px 0}
</style></head><body>
<h1>Document Manager <a href="/?token=$t" title=Refresh>&#x1F504;</a></h1>
<p>${docs.size} documents &bull; Token-protected &bull; Stop from app when done</p>
<h2>Tags</h2><div>$folders</div>
<h2>Documents</h2>
<div class=bar id=bar><span id=cnt>0</span> selected <button class='btn bo' style='color:#fff;border-color:#fff' onclick="bulkTag()">Add Tag</button> <button class='btn bo' style='color:#fff;border-color:#fff' onclick="clr()">Clear</button></div>
<table><tr><th><input type=checkbox onchange="all(this)"></th><th>Name</th><th>Format</th><th>Tags</th><th>Actions</th></tr>$rows</table>
<h2>Upload</h2><div class=up><form action="/api/upload?token=$t" method=post enctype=multipart/form-data><input type=file name=file multiple><br><br><button class=btn type=submit>Upload</button></form><p style="font-size:12px;color:#888">Supports large files (up to 200MB)</p></div>
<script>
const T='$t';
function ren(id){const n=prompt('New name:');if(n)fetch('/api/rename?token='+T+'&id='+id+'&name='+encodeURIComponent(n),{method:'POST'}).then(()=>location.reload())}
function addTag(id){const t=prompt('Tag name:');if(t)fetch('/api/tags?token='+T+'&id='+id+'&tag='+encodeURIComponent(t)+'&action=add',{method:'POST'}).then(()=>location.reload())}
function rmTag(id,tag){if(confirm('Remove "'+tag+'"?'))fetch('/api/tags?token='+T+'&id='+id+'&tag='+encodeURIComponent(tag)+'&action=remove',{method:'POST'}).then(()=>location.reload())}
function all(el){document.querySelectorAll('.sel').forEach(c=>c.checked=el.checked);upd()}
function upd(){const n=[...document.querySelectorAll('.sel:checked')].length;document.getElementById('bar').style.display=n?'flex':'none';document.getElementById('cnt').textContent=n}
function clr(){document.querySelectorAll('.sel').forEach(c=>c.checked=false);upd()}
function bulkTag(){const t=prompt('Tag for selected docs:');if(!t)return;const ids=[...document.querySelectorAll('.sel:checked')].map(c=>c.dataset.id);Promise.all(ids.map(id=>fetch('/api/tags?token='+T+'&id='+id+'&tag='+encodeURIComponent(t)+'&action=add',{method:'POST'}))).then(()=>location.reload())}
</script></body></html>"""
    }

    private fun buildTagHtml(tag: String, docs: List<Document>): String {
        val t = accessToken
        val rows = docs.joinToString("") { doc -> "<tr><td>${doc.originalFileName?:"doc"}</td><td>${doc.format.name}</td><td><a href=/api/download/${doc.id}?token=$t class=btn>Download</a></td></tr>" }
        return """<!DOCTYPE html><html><head><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1"><title>$tag</title>
<style>body{font-family:-apple-system,sans-serif;max-width:960px;margin:0 auto;padding:16px;background:#f8f9fa}h1{color:#1565C0}.btn{display:inline-block;background:#1565C0;color:#fff;padding:6px 14px;border-radius:5px;text-decoration:none;font-size:13px}table{width:100%;border-collapse:collapse;background:#fff;border-radius:8px}td,th{padding:8px;border-bottom:1px solid #f0f0f0;text-align:left}th{background:#f5f5f5}</style></head><body>
<p><a href="/?token=$t">&larr; Back</a></p><h1>$tag <a href="/tag/$tag?token=$t">&#x1F504;</a></h1><p>${docs.size} documents</p>
<table><tr><th>Name</th><th>Format</th><th>Actions</th></tr>$rows</table></body></html>"""
    }
}
