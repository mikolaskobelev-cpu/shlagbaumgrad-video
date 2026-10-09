package com.example.livecamera

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.text.InputType
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.ArrayList

class MainActivity : Activity() {
    private lateinit var vlcLayout: VLCVideoLayout
    private lateinit var status: TextView
    private lateinit var urlInput: EditText
    private lateinit var connectButton: Button
    private lateinit var controls: LinearLayout
    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var retryDelayMs = 1500L
    private var retryRunnable: Runnable? = null
    private var manualStop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        vlcLayout = VLCVideoLayout(this)
        root.addView(vlcLayout, FrameLayout.LayoutParams(-1, -1))

        controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            setBackgroundColor(0xE6000000.toInt())
        }
        urlInput = EditText(this).apply {
            hint = "rtsp://host:554/path (add credentials only on your device)"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 14f
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_URI or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            maxLines = 1
        }
        connectButton = Button(this).apply {
            text = "Подключить / переподключить"
            setBackgroundColor(0xFFFF7900.toInt())
            setTextColor(Color.WHITE)
            setOnClickListener {
                manualStop = false
                retryDelayMs = 1500L
                connect(urlInput.text.toString().trim())
            }
        }
        status = TextView(this).apply {
            text = "Enter the camera RTSP address. For internet access, use a VPN or secure relay."
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, 4, 0, 0)
        }
        controls.addView(urlInput, LinearLayout.LayoutParams(-1, -2))
        controls.addView(connectButton, LinearLayout.LayoutParams(-1, -2))
        controls.addView(status, LinearLayout.LayoutParams(-1, -2))

        val panelParams = FrameLayout.LayoutParams(-1, -2, Gravity.TOP)
        root.addView(controls, panelParams)
        root.setOnClickListener {
            controls.visibility = if (controls.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        controls.setOnClickListener { /* Keep taps on controls from toggling panel */ }
        setContentView(root)

        val options = ArrayList<String>().apply {
            add("--rtsp-tcp")
            add("--network-caching=150")
            add("--live-caching=150")
            add("--clock-jitter=0")
            add("--clock-synchro=0")
        }
        libVLC = LibVLC(this, options)
        mediaPlayer = MediaPlayer(libVLC).also { player ->
            player.attachViews(vlcLayout, null, false, false)
            player.setEventListener { event ->
                runOnUiThread {
                    when (event.type) {
                        MediaPlayer.Event.Opening -> status.text = "Подключение…"
                        MediaPlayer.Event.Buffering -> status.text = "Буферизация ${event.buffering.toInt()}%"
                        MediaPlayer.Event.Playing -> {
                            status.text = "● В ЭФИРЕ • Подключено"
                            retryDelayMs = 1500L
                            cancelRetry()
                        }
                        MediaPlayer.Event.Stopped -> {
                            status.text = "Поток остановлен"
                            scheduleReconnect()
                        }
                        MediaPlayer.Event.EncounteredError -> {
                            status.text = "Ошибка соединения • повторное подключение…"
                            scheduleReconnect()
                        }
                        MediaPlayer.Event.EndReached -> {
                            status.text = "Поток завершён • повторное подключение…"
                            scheduleReconnect()
                        }
                    }
                }
            }
        }
    }

    private fun connect(url: String) {
        if (url.isBlank() || !(url.startsWith("rtsp://") || url.startsWith("rtsps://"))) {
            status.text = "Введите корректный адрес rtsp:// или rtsps://."
            return
        }
        cancelRetry()
        try {
            mediaPlayer?.stop()
            val media = Media(libVLC, android.net.Uri.parse(url))
            media.setHWDecoderEnabled(true, false)
            media.addOption(":rtsp-tcp")
            media.addOption(":network-caching=150")
            media.addOption(":live-caching=150")
            mediaPlayer?.media = media
            media.release()
            mediaPlayer?.play()
            status.text = "Подключение…"
        } catch (e: Exception) {
            status.text = "Не удалось запустить поток • повторная попытка…"
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (manualStop || isFinishing || isDestroyed) return
        cancelRetry()
        val delay = retryDelayMs
        retryDelayMs = (retryDelayMs * 2).coerceAtMost(15000L)
        retryRunnable = Runnable {
            val url = urlInput.text.toString().trim()
            if (url.isNotBlank()) connect(url)
        }
        handler.postDelayed(retryRunnable!!, delay)
    }

    private fun cancelRetry() {
        retryRunnable?.let { handler.removeCallbacks(it) }
        retryRunnable = null
    }

    override fun onPause() {
        super.onPause()
        // Keep stream lifecycle predictable when the app is backgrounded.
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (urlInput.text.isNotBlank()) {
            // User can tap Connect if their camera/server doesn't resume automatically.
        }
    }

    override fun onDestroy() {
        manualStop = true
        cancelRetry()
        mediaPlayer?.let {
            it.stop()
            it.detachViews()
            it.release()
        }
        mediaPlayer = null
        libVLC?.release()
        libVLC = null
        super.onDestroy()
    }
}
