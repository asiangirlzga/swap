package com.example.roopswap

import android.content.Context
import android.net.Uri
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class ApiClient(private val ctx: Context, baseUrl: String, private val apiKey: String) {

    private val base = baseUrl.trim().trimEnd('/')
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .build()

    private fun Request.Builder.auth(): Request.Builder =
        if (apiKey.isNotBlank()) header("X-API-Key", apiKey) else this

    private fun uriBody(uri: Uri, mime: String) = object : RequestBody() {
        override fun contentType() = mime.toMediaType()
        override fun writeTo(sink: BufferedSink) {
            ctx.contentResolver.openInputStream(uri)!!.use { sink.writeAll(it.source()) }
        }
    }

    /** Uploads the files and returns the job id. */
    fun submit(face: Uri, video: Uri, enhance: Boolean): String {
        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("enhance", enhance.toString())
            .addFormDataPart("source", "face.jpg", uriBody(face, "image/jpeg"))
            .addFormDataPart("target", "video.mp4", uriBody(video, "video/mp4"))
            .build()
        val req = Request.Builder().url("$base/swap").auth().post(form).build()
        http.newCall(req).execute().use { r ->
            val text = r.body?.string().orEmpty()
            if (!r.isSuccessful) error("Server error ${r.code}: $text")
            return JSONObject(text).getString("job_id")
        }
    }

    /** Returns Pair(status, message). status: queued | running | done | error */
    fun status(jobId: String): Pair<String, String> {
        val req = Request.Builder().url("$base/status/$jobId").auth().get().build()
        http.newCall(req).execute().use { r ->
            val text = r.body?.string().orEmpty()
            if (!r.isSuccessful) error("Server error ${r.code}: $text")
            val j = JSONObject(text)
            return j.getString("status") to j.optString("message", "")
        }
    }

    fun download(jobId: String, dest: File) {
        val req = Request.Builder().url("$base/result/$jobId").auth().get().build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("Download failed: ${r.code}")
            dest.outputStream().use { out -> r.body!!.byteStream().copyTo(out) }
        }
    }
}
