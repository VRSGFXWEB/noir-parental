package com.parentalcontrol.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class InstalledApp(val label: String, val packageName: String, val icon: Drawable)

/**
 * Daftar game/app yang terdeteksi di HP, mirip referensi "Game Space" yang diminta.
 * Anak pilih "Main" -> game asli kebuka + overlay mod-menu muncul.
 */
class GameLibraryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_library)

        val recycler = findViewById<RecyclerView>(R.id.recyclerGames)
        recycler.layoutManager = LinearLayoutManager(this)

        val apps = getLaunchableApps()
        recycler.adapter = GameAdapter(apps) { app ->
            launchGameWithOverlay(app)
        }
    }

    private fun getLaunchableApps(): List<InstalledApp> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved
            .filter { it.activityInfo.packageName != packageName } // jangan tampilkan diri sendiri
            .map {
                InstalledApp(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun launchGameWithOverlay(app: InstalledApp) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Tidak bisa membuka aplikasi ini.", Toast.LENGTH_SHORT).show()
            return
        }

        if (android.provider.Settings.canDrawOverlays(this)) {
            val serviceIntent = Intent(this, ModMenuOverlayService::class.java)
            serviceIntent.putExtra("game_label", app.label)
            startService(serviceIntent)
        } else {
            Toast.makeText(this, "Izinkan 'Tampil di atas aplikasi lain' dulu di Pengaturan.", Toast.LENGTH_LONG).show()
        }
    }
}

class GameAdapter(
    private val apps: List<InstalledApp>,
    private val onPlay: (InstalledApp) -> Unit
) : RecyclerView.Adapter<GameAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgGameIcon)
        val name: TextView = view.findViewById(R.id.tvGameName)
        val playBtn: Button = view.findViewById(R.id.btnPlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.label
        holder.playBtn.setOnClickListener { onPlay(app) }
    }

    override fun getItemCount() = apps.size
}
