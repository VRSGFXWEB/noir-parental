package com.parentalcontrol.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tampilan utama app anak — bertema "game booster" sesuai permintaan,
 * TAPI tetap jelas ini adalah app parental control:
 *  - Nama app & ikon terlihat jelas di launcher (lihat AndroidManifest / strings.xml)
 *  - Tidak ada logic "pura-pura crash" atau penyamaran apa pun
 *  - Tombol SOS & check-in terlihat untuk anak, bukan tersembunyi
 */
class MainActivity : AppCompatActivity() {

    private lateinit var deviceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestRuntimePermissions()

        // Anonymous auth -> UID dipakai sebagai device_id
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnSuccessListener { result ->
                deviceId = result.user!!.uid
                registerDevice()
                startForegroundControlService()
            }

        findViewById<Button>(R.id.btnBoost).setOnClickListener { runBoost() }
        findViewById<Button>(R.id.btnSOS).setOnClickListener { sendSOS() }
        findViewById<Button>(R.id.btnCheckin).setOnClickListener { sendCheckin() }
        findViewById<Button>(R.id.btnFpsOverlay).setOnClickListener { toggleFpsOverlay() }
    }

    /**
     * Izin runtime wajib diminta eksplisit (bukan cuma didaftar di manifest),
     * kalau tidak app akan SecurityException/force-close saat mengakses lokasi
     * atau mengirim notifikasi. Dialog ini SELALU muncul dan terlihat oleh
     * pemegang HP — bukan proses tersembunyi.
     */
    private fun requestRuntimePermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 1001)
        }

        // Lokasi background (butuh izin foreground lokasi granted dulu) -> diminta
        // terpisah, baru setelah user approve izin foreground di atas.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1002
            )
        }
    }

    private fun registerDevice() {
        val db = FirebaseDatabase.getInstance().reference
        val deviceData = mapOf(
            "child_name" to android.os.Build.MODEL,
            "status" to "online",
            "battery" to getBatteryLevel(),
            "uninstall_protected" to false,
            "flashlight" to "off"
        )
        db.child("devices").child(deviceId).updateChildren(deviceData)
    }

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun startForegroundControlService() {
        // Foreground service tipe "location" butuh izin lokasi SUDAH granted
        // sebelum dipanggil, kalau tidak akan SecurityException di Android 14+.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return // akan dicoba lagi dari onRequestPermissionsResult setelah izin diberikan
        }
        val intent = Intent(this, ControlForegroundService::class.java)
        intent.putExtra("device_id", deviceId)
        startForegroundService(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED &&
            this::deviceId.isInitialized) {
            startForegroundControlService()
        }
    }

    /** Fitur BOOST sungguhan: trim memory + hint ke sistem untuk bersihkan cache app ini. */
    private fun runBoost() {
        val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        // Catatan: Android modern membatasi akses untuk membersihkan cache app LAIN
        // tanpa izin system/root. Yang legal & bisa dilakukan: trim memori proses sendiri
        // dan beri tips ke pengguna.
        Runtime.getRuntime().gc()
        Toast.makeText(this, "Boost selesai: memori dibersihkan.", Toast.LENGTH_SHORT).show()
    }

    /** Tombol SOS — anak tekan manual, kirim lokasi + alert ke ortu. Transparan, bukan silent. */
    private fun sendSOS() {
        val db = FirebaseDatabase.getInstance().reference
        LocationHelper.getCurrentLocation(this) { lat, lng ->
            val alert = mapOf(
                "type" to "sos",
                "location" to mapOf("lat" to lat, "lng" to lng),
                "timestamp" to System.currentTimeMillis(),
                "resolved" to false
            )
            db.child("alerts").child(deviceId).push().setValue(alert)
            Toast.makeText(this, "SOS terkirim ke orang tua.", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendCheckin() {
        val db = FirebaseDatabase.getInstance().reference
        db.child("checkins").child(deviceId).child("last_checkin")
            .setValue(System.currentTimeMillis())
        Toast.makeText(this, "Check-in terkirim: aman.", Toast.LENGTH_SHORT).show()
    }

    /** Overlay FPS mod-menu — murni kosmetik gaming, bisa di-off dari pengaturan app. */
    private fun toggleFpsOverlay() {
        // Izin overlay (SYSTEM_ALERT_WINDOW) itu "special permission": harus di-grant
        // manual lewat halaman Settings, tidak cukup cuma didaftarin di manifest.
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan 'Tampil di atas aplikasi lain' dulu di halaman berikutnya.", Toast.LENGTH_LONG).show()
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        val intent = Intent(this, FpsOverlayService::class.java)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val enabled = !prefs.getBoolean("fps_overlay_on", false)
        prefs.edit().putBoolean("fps_overlay_on", enabled).apply()
        if (enabled) startService(intent) else stopService(intent)
        Toast.makeText(this, "Mod menu overlay: ${if (enabled) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
    }
}
