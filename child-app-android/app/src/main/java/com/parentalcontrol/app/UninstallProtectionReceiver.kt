package com.parentalcontrol.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Proteksi uninstall yang LEGAL:
 * - App didaftarkan sebagai Device Admin (butuh persetujuan eksplisit saat setup,
 *   anak/pemegang HP MELIHAT dialog sistem Android yang menjelaskan izin ini).
 * - Untuk uninstall, Android akan meminta nonaktifkan Device Admin dulu lewat
 *   Settings > Security > Device Admin Apps — dan activity di app ini akan
 *   meminta password ortu sebelum mengizinkan nonaktifkan.
 * - INI BUKAN "tidak bisa dihapus sama sekali": pemegang HP tetap bisa uninstall
 *   kalau tahu password ortu, atau lewat factory reset dalam keadaan darurat.
 *   Tidak ada mekanisme silent/hidden di sini.
 */
class UninstallProtectionReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Menonaktifkan ini akan memungkinkan aplikasi parental control dihapus. " +
               "Dibutuhkan password orang tua di aplikasi untuk melanjutkan."
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        // Device admin aktif -> pemegang HP sudah melihat & menyetujui dialog sistem ini.
    }
}
