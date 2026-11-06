package com.parentalcontrol.app

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast

/**
 * Activity transparan yang cuma mentrigger dialog izin screen-capture bawaan
 * Android (MediaProjection). Dialog ini SELALU muncul dan WAJIB di-approve
 * manual setiap sesi -- tidak ada cara untuk melewati atau menyembunyikannya,
 * ini pengamanan dari Android sendiri terhadap screen-recording diam-diam.
 */
class ScreenCaptureActivity : Activity() {

    private val REQUEST_CODE = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra("mode") ?: "screenshot"

        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE)
        pendingMode = mode
    }

    private var pendingMode: String = "screenshot"

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val serviceIntent = Intent(this, ScreenCaptureService::class.java)
                serviceIntent.putExtra("mode", pendingMode)
                serviceIntent.putExtra("resultCode", resultCode)
                serviceIntent.putExtra("resultData", data)
                startForegroundService(serviceIntent)
            } else {
                Toast.makeText(this, "Izin screen capture ditolak.", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
