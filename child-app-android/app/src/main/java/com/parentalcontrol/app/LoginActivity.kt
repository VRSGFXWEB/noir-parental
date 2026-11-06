package com.parentalcontrol.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * Login anak: akun (username + password) dibuat oleh admin/owner di CentralVex,
 * BUKAN oleh anak sendiri. Anak cuma masukin apa yang dikasih ortu.
 * Setelah login sukses, device terhubung ke akun anak itu di Firebase.
 */
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvError = findViewById<TextView>(R.id.tvLoginError)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()
            if (username.isEmpty() || password.isEmpty()) {
                tvError.text = "Username dan password wajib diisi."
                return@setOnClickListener
            }

            val email = "$username@noir-child.local"
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user!!.uid
                    val db = FirebaseDatabase.getInstance().reference
                    db.child("children").child(uid).get().addOnSuccessListener { snap ->
                        if (!snap.exists()) {
                            tvError.text = "Akun ini belum terdaftar sebagai anak. Hubungi orang tua."
                            FirebaseAuth.getInstance().signOut()
                            return@addOnSuccessListener
                        }
                        getSharedPreferences("session", MODE_PRIVATE).edit()
                            .putString("child_uid", uid)
                            .putString("username", username)
                            .apply()
                        startActivity(Intent(this, PermissionSetupActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener {
                    tvError.text = "Username atau password salah."
                }
        }
    }
}
