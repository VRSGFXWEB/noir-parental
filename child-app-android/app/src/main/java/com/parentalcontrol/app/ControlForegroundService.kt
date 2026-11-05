package com.parentalcontrol.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*

/**
 * Service ini SELALU menampilkan notifikasi persistent "Parental Control aktif"
 * selama berjalan — ini wajib di Android untuk foreground service, dan juga
 * memastikan anak selalu tahu bahwa app pemantauan sedang berjalan.
 *
 * Tugas:
 *  - Dengarkan perintah dari Firebase (commands/{device_id})
 *  - Senter ON/OFF
 *  - Pesan dari ortu -> tampil sebagai notifikasi normal (BUKAN overlay)
 *  - Saran wallpaper -> tampil sebagai notifikasi, anak klik untuk approve
 *  - Update lokasi berkala
 */
class ControlForegroundService : Service() {

    private lateinit var deviceId: String
    private val db get() = FirebaseDatabase.getInstance().reference

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        deviceId = intent?.getStringExtra("device_id") ?: return START_NOT_STICKY

        startForeground(1, buildPersistentNotification())
        listenForCommands()
        LocationHelper.startPeriodicUpdates(this, deviceId)

        return START_STICKY
    }

    private fun buildPersistentNotification(): Notification {
        val channelId = "control_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Layanan Kontrol", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Parental control aktif")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun listenForCommands() {
        db.child("commands").child(deviceId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snap: DataSnapshot, prev: String?) {
                    val type = snap.child("type").getValue(String::class.java) ?: return
                    val payload = snap.child("payload").getValue(String::class.java) ?: ""
                    when (type) {
                        "flashlight" -> setFlashlight(payload == "on")
                        "message" -> showMessageNotification(payload)
                        "wallpaper_suggestion" -> showWallpaperApprovalNotification(payload)
                    }
                    snap.ref.child("acknowledged").setValue(true)
                }
                override fun onChildChanged(s: DataSnapshot, p: String?) {}
                override fun onChildRemoved(s: DataSnapshot) {}
                override fun onChildMoved(s: DataSnapshot, p: String?) {}
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun setFlashlight(on: Boolean) {
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            cm.setTorchMode(id, on)
        } catch (_: Exception) { }
    }

    /** Pesan dari ortu selalu tampil sebagai notifikasi biasa — anak sadar sepenuhnya. */
    private fun showMessageNotification(text: String) {
        val channelId = "parent_messages"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Pesan dari Orang Tua", NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pesan dari Orang Tua")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(2001, notif)
    }

    /** Wallpaper tidak pernah di-set otomatis diam-diam — selalu lewat approval anak. */
    private fun showWallpaperApprovalNotification(url: String) {
        val channelId = "wallpaper_requests"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Permintaan Wallpaper", NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val intent = Intent(this, WallpaperApprovalActivity::class.java)
        intent.putExtra("wallpaper_url", url)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Orang tua mengirim wallpaper baru")
            .setContentText("Ketuk untuk melihat dan menyetujui")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(2002, notif)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
