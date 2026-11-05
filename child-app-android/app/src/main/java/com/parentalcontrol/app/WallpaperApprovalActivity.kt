package com.parentalcontrol.app

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.URL

/**
 * Wallpaper TIDAK PERNAH otomatis ter-set. Anak harus menekan "Setujui" di sini.
 */
class WallpaperApprovalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("wallpaper_url") ?: return finish()

        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val imageView = ImageView(this)
        val approveBtn = Button(this).apply { text = "Setujui & Pasang Wallpaper" }
        val declineBtn = Button(this).apply { text = "Tolak" }

        layout.addView(imageView)
        layout.addView(approveBtn)
        layout.addView(declineBtn)
        setContentView(layout)

        Thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL(url).openStream())
                runOnUiThread { imageView.setImageBitmap(bitmap) }

                approveBtn.setOnClickListener {
                    WallpaperManager.getInstance(this).setBitmap(bitmap)
                    Toast.makeText(this, "Wallpaper dipasang.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Gagal memuat gambar.", Toast.LENGTH_SHORT).show() }
            }
        }.start()

        declineBtn.setOnClickListener { finish() }
    }
}
