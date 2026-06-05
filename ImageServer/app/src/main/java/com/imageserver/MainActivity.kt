package com.imageserver

import android.Manifest
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.imageserver.databinding.ActivityMainBinding
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var folderUri: Uri? = null
    private val prefs by lazy { getSharedPreferences("prefs", MODE_PRIVATE) }

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        folderUri = uri
        prefs.edit().putString("folder_uri", uri.toString()).apply()
        updateUI()
        Toast.makeText(this, "Folder selected", Toast.LENGTH_SHORT).show()
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { updateUI() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs.getString("folder_uri", null)?.let { folderUri = Uri.parse(it) }
        binding.etPort.setText(prefs.getInt("port", 8080).toString())

        binding.btnSelectFolder.setOnClickListener { folderPicker.launch(null) }
        binding.btnToggle.setOnClickListener { toggleServer() }
        binding.tvUrl.setOnClickListener { copyUrl() }
        binding.btnCopy.setOnClickListener { copyUrl() }

        requestRequiredPermissions()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun toggleServer() {
        if (ImageServerService.isRunning) {
            stopService(Intent(this, ImageServerService::class.java))
            ImageServerService.isRunning = false
        } else {
            if (folderUri == null) {
                Toast.makeText(this, "Please select an image folder first", Toast.LENGTH_SHORT).show()
                return
            }
            val port = binding.etPort.text.toString().toIntOrNull()?.coerceIn(1024, 65535) ?: 8080
            prefs.edit().putInt("port", port).apply()

            val intent = Intent(this, ImageServerService::class.java).apply {
                putExtra(ImageServerService.EXTRA_FOLDER_URI, folderUri.toString())
                putExtra(ImageServerService.EXTRA_PORT, port)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
        // Give the service a moment to start/stop
        binding.btnToggle.postDelayed({ updateUI() }, 400)
    }

    private fun updateUI() {
        val running = ImageServerService.isRunning
        val port = if (running) ImageServerService.activePort
                   else binding.etPort.text.toString().toIntOrNull() ?: 8080

        binding.statusDot.setImageResource(
            if (running) R.drawable.dot_green else R.drawable.dot_red
        )
        binding.tvStatus.text = if (running) "Running" else "Stopped"

        binding.btnToggle.text = if (running) "Stop Server" else "Start Server"
        binding.btnToggle.backgroundTintList = getColorStateList(
            if (running) R.color.btn_stop else R.color.accent
        )

        binding.etPort.isEnabled = !running

        val url = if (running) {
            val ip = getLocalIp() ?: "localhost"
            "http://$ip:$port"
        } else "—"
        binding.tvUrl.text = url
        binding.tvUrl.isClickable = running

        binding.tvFolder.text = folderUri?.let { friendlyPath(it) } ?: "No folder selected"
    }

    private fun copyUrl() {
        val url = binding.tvUrl.text.toString().takeIf { it != "—" } ?: return
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Server URL", url))
        Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun getLocalIp(): String? = try {
        NetworkInterface.getNetworkInterfaces()?.toList()
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress
    } catch (_: Exception) { null }

    private fun friendlyPath(uri: Uri): String {
        val raw = uri.lastPathSegment ?: uri.toString()
        return raw.substringAfterLast(':').substringAfterLast('/')
            .ifEmpty { raw }
    }

    private fun requestRequiredPermissions() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (perms.isNotEmpty()) permissionsLauncher.launch(perms.toTypedArray())
    }
}
