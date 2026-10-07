package com.example.roopswap

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private var faceUri: Uri? = null
    private var videoUri: Uri? = null
    private var resultFile: File? = null

    private lateinit var serverUrl: EditText
    private lateinit var apiKey: EditText
    private lateinit var facePreview: ImageView
    private lateinit var videoLabel: TextView
    private lateinit var enhance: CheckBox
    private lateinit var consent: CheckBox
    private lateinit var start: Button
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private lateinit var result: VideoView
    private lateinit var save: Button

    private val pickFace = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let { uri -> faceUri = uri; facePreview.setImageURI(uri) }
    }
    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let { uri -> videoUri = uri; videoLabel.text = "Video: ${uri.lastPathSegment}" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serverUrl = findViewById(R.id.serverUrl)
        apiKey = findViewById(R.id.apiKey)
        facePreview = findViewById(R.id.facePreview)
        videoLabel = findViewById(R.id.videoLabel)
        enhance = findViewById(R.id.enhance)
        consent = findViewById(R.id.consentCheck)
        start = findViewById(R.id.start)
        progress = findViewById(R.id.progress)
        status = findViewById(R.id.status)
        result = findViewById(R.id.result)
        save = findViewById(R.id.save)

        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        serverUrl.setText(prefs.getString("url", ""))
        apiKey.setText(prefs.getString("key", ""))

        findViewById<Button>(R.id.pickFace).setOnClickListener { pickFace.launch("image/*") }
        findViewById<Button>(R.id.pickVideo).setOnClickListener { pickVideo.launch("video/*") }
        save.setOnClickListener { saveToGallery() }

        start.setOnClickListener {
            val url = serverUrl.text.toString()
            when {
                url.isBlank() -> toast("Enter your server URL")
                faceUri == null || videoUri == null -> toast("Pick a face image and a video")
                !consent.isChecked -> toast("Please confirm you have permission")
                else -> {
                    prefs.edit().putString("url", url).putString("key", apiKey.text.toString()).apply()
                    runSwap(url)
                }
            }
        }
    }

    private fun runSwap(url: String) {
        val client = ApiClient(this, url, apiKey.text.toString())
        setBusy(true, "Uploading…")
        result.visibility = View.GONE
        save.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val out = withContext(Dispatchers.IO) {
                    val id = client.submit(faceUri!!, videoUri!!, enhance.isChecked)
                    while (true) {
                        val (st, msg) = client.status(id)
                        withContext(Dispatchers.Main) { status.text = "Status: $st $msg" }
                        if (st == "done") break
                        if (st == "error") error(msg)
                        delay(3000)
                    }
                    File(cacheDir, "swapped_$id.mp4").also { client.download(id, it) }
                }
                resultFile = out
                status.text = "Done!"
                result.visibility = View.VISIBLE
                result.setVideoPath(out.absolutePath)
                result.setMediaController(MediaController(this@MainActivity))
                result.start()
                save.visibility = View.VISIBLE
            } catch (e: Exception) {
                status.text = "Failed: ${e.message}"
            } finally {
                setBusy(false, null)
            }
        }
    }

    private fun saveToGallery() {
        val f = resultFile ?: return
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, f.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= 29)
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/RoopSwap")
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) { toast("Could not save"); return }
        contentResolver.openOutputStream(uri)?.use { o -> f.inputStream().use { it.copyTo(o) } }
        toast("Saved to Movies/RoopSwap")
    }

    private fun setBusy(busy: Boolean, msg: String?) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        start.isEnabled = !busy
        msg?.let { status.text = it }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
