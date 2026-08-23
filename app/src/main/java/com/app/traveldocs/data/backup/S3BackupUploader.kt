package com.app.traveldocs.data.backup

import com.app.traveldocs.debug.DebugLogger
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class S3Config(val endpoint: String, val bucket: String, val accessKey: String, val secretKey: String, val region: String = "us-east-1")

object S3BackupUploader {
    fun upload(zipFile: File, fileName: String, config: S3Config): Result<String> {
        return try {
            DebugLogger.i("S3Backup", "Uploading $fileName (${zipFile.length()/1024}KB) to ${config.endpoint}/${config.bucket}")
            val bytes = zipFile.readBytes()
            val contentHash = sha256Hex(bytes)
            val now = Date()
            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(now)
            val amzDate = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(now)
            val host = URL(config.endpoint).host
            val path = "/${config.bucket}/$fileName"
            val url = "${config.endpoint}$path"
            val headers = "content-type:application/zip\nhost:$host\nx-amz-content-sha256:$contentHash\nx-amz-date:$amzDate\n"
            val signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date"
            val canonical = "PUT\n$path\n\n$headers\n$signedHeaders\n$contentHash"
            val scope = "$dateStamp/${config.region}/s3/aws4_request"
            val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$scope\n${sha256Hex(canonical.toByteArray())}"
            val kDate = hmac("AWS4${config.secretKey}".toByteArray(), dateStamp)
            val kRegion = hmac(kDate, config.region)
            val kService = hmac(kRegion, "s3")
            val kSigning = hmac(kService, "aws4_request")
            val signature = hmac(kSigning, stringToSign).joinToString("") { "%02x".format(it) }
            val authorization = "AWS4-HMAC-SHA256 Credential=${config.accessKey}/$scope, SignedHeaders=$signedHeaders, Signature=$signature"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"; doOutput = true
                setRequestProperty("Content-Type", "application/zip")
                setRequestProperty("Host", host)
                setRequestProperty("x-amz-content-sha256", contentHash)
                setRequestProperty("x-amz-date", amzDate)
                setRequestProperty("Authorization", authorization)
                setRequestProperty("Content-Length", bytes.size.toString())
            }
            conn.outputStream.use { it.write(bytes) }
            val code = conn.responseCode
            if (code in 200..299) { DebugLogger.i("S3Backup", "Success: HTTP $code"); Result.success("Uploaded to S3: HTTP $code") }
            else { val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"; DebugLogger.e("S3Backup", err); Result.failure(Exception(err)) }
        } catch (e: Exception) { DebugLogger.e("S3Backup", "Failed", e); Result.failure(e) }
    }
    private fun sha256Hex(data: ByteArray) = MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
    private fun hmac(key: ByteArray, data: String): ByteArray { val m = Mac.getInstance("HmacSHA256"); m.init(SecretKeySpec(key, "HmacSHA256")); return m.doFinal(data.toByteArray()) }
}
