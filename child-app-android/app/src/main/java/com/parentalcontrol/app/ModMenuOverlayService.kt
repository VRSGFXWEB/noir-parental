package com.parentalcontrol.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat

/**
 * Overlay bulat mengambang (pakai icon app sebagai tombol), mirip mod-menu
 * game booster. Begitu di-tap, muncul menu kecil dengan: FPS, Boost, Screenshot,
 * Rekam Layar. Bisa di-drag dan di-minimize, tidak pernah mengunci layar atau
 * menghalangi interaksi dengan game di baliknya.
 *
 * CATATAN JUJUR soal FPS: Android tidak mengizinkan app pihak ketiga membaca
 * frame-rate render app LAIN tanpa root. Angka FPS di sini adalah ESTIMASI dari
 * refresh-rate sistem (Choreographer), bukan FPS asli dari game yang sedang
 * dimainkan. Ini batasan platform Android, bukan bug.
 */
class ModMenuOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var menuView: View? = null
    private var fpsCounter = 0
    private var lastFpsUpdate = 0L
    private var currentFps = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(3001, buildNotification())
        showBubble()
        startFpsEstimate()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val channelId = "mod_menu"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Mod Menu", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Mod Menu aktif")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun showBubble() {
        val bubble = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = ViewGroup.LayoutParams(140, 140)
        }

        val params = WindowManager.LayoutParams(
            140, 140,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20
        params.y = 300

        // Drag untuk mindahin posisi bubble, tap singkat untuk buka/tutup menu
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) isDragging = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(bubble, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) toggleMenu()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubble, params)
        bubbleView = bubble
    }

    private fun toggleMenu() {
        if (menuView != null) {
            windowManager.removeView(menuView)
            menuView = null
            return
        }
        showMenu()
    }

    private fun showMenu() {
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE1A1412.toInt())
            setPadding(24, 24, 24, 24)
        }

        val tvFps = TextView(this).apply {
            text = "FPS (estimasi): --"
            setTextColor(0xFFF2E9E4.toInt())
            textSize = 13f
            setPadding(0, 0, 0, 12)
        }
        fpsLabelRef = tvFps

        val btnBoost = Button(this).apply {
            text = "BOOST"
            setOnClickListener { runBoost() }
        }
        val btnScreenshot = Button(this).apply {
            text = "SCREENSHOT"
            setOnClickListener { requestScreenCapture("screenshot") }
        }
        val btnRecord = Button(this).apply {
            text = "REKAM LAYAR"
            setOnClickListener { requestScreenCapture("record") }
        }
        val btnClose = Button(this).apply {
            text = "TUTUP OVERLAY"
            setOnClickListener { stopSelf() }
        }

        menu.addView(tvFps)
        menu.addView(btnBoost)
        menu.addView(btnScreenshot)
        menu.addView(btnRecord)
        menu.addView(btnClose)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 180
        params.y = 300

        windowManager.addView(menu, params)
        menuView = menu
    }

    private var fpsLabelRef: TextView? = null

    /**
     * Estimasi FPS dari Choreographer (vsync sistem). Ini BUKAN FPS render
     * asli dari game yang sedang berjalan -- Android tidak expose data itu
     * ke app pihak ketiga tanpa root.
     */
    private fun startFpsEstimate() {
        val choreographer = Choreographer.getInstance()
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                fpsCounter++
                val now = System.currentTimeMillis()
                if (now - lastFpsUpdate >= 1000) {
                    currentFps = fpsCounter
                    fpsCounter = 0
                    lastFpsUpdate = now
                    fpsLabelRef?.text = "FPS (estimasi sistem): $currentFps"
                }
                choreographer.postFrameCallback(this)
            }
        }
        choreographer.postFrameCallback(callback)
    }

    /** BOOST yang jujur: trim memori proses sendiri + saran ke sistem. */
    private fun runBoost() {
        Runtime.getRuntime().gc()
        Toast.makeText(this, "Boost selesai.", Toast.LENGTH_SHORT).show()
    }

    /**
     * Screenshot & screen record BUTUH persetujuan sistem (MediaProjection)
     * setiap sesi -- ini dialog resmi Android, tidak bisa dilewati/disembunyikan.
     * Diarahkan ke MainActivity yang memegang launcher untuk permission ini.
     */
    private fun requestScreenCapture(mode: String) {
        val intent = Intent(this, ScreenCaptureActivity::class.java)
        intent.putExtra("mode", mode)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let { windowManager.removeView(it) }
        menuView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
