package com.parentalcontrol.app

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.*

object LocationHelper {

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context, onResult: (Double, Double) -> Unit) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) onResult(loc.latitude, loc.longitude)
        }
    }

    @SuppressLint("MissingPermission")
    fun startPeriodicUpdates(context: Context, deviceId: String) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000L)
            .setMinUpdateIntervalMillis(30_000L)
            .build()

        client.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val db = FirebaseDatabase.getInstance().reference
                db.child("devices").child(deviceId).child("location").setValue(
                    mapOf("lat" to loc.latitude, "lng" to loc.longitude, "timestamp" to System.currentTimeMillis())
                )
                checkGeofences(context, deviceId, loc.latitude, loc.longitude)
            }
        }, context.mainLooper)
    }

    private fun checkGeofences(context: Context, deviceId: String, lat: Double, lng: Double) {
        val db = FirebaseDatabase.getInstance().reference
        db.child("geofences").child(deviceId).get().addOnSuccessListener { snap ->
            snap.children.forEach { fence ->
                val fLat = fence.child("lat").getValue(Double::class.java) ?: return@forEach
                val fLng = fence.child("lng").getValue(Double::class.java) ?: return@forEach
                val radius = fence.child("radius_meters").getValue(Double::class.java) ?: 200.0
                val label = fence.child("label").getValue(String::class.java) ?: "Area"
                val distance = haversineMeters(lat, lng, fLat, fLng)
                if (distance > radius) {
                    val alert = mapOf(
                        "type" to "geofence_exit",
                        "location" to mapOf("lat" to lat, "lng" to lng),
                        "timestamp" to System.currentTimeMillis(),
                        "resolved" to false
                    )
                    db.child("alerts").child(deviceId).push().setValue(alert)
                }
            }
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
