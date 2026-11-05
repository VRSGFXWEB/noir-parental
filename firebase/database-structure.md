# Firebase Realtime Database — Struktur Data

## Struktur

```
users/
  {uid}/
    username: "centralvex"
    role: "owner" | "admin"
    created_by: "centralvex" | null
    created_at: timestamp

devices/
  {device_id}/
    child_name: "Nama Anak"
    owner_uid: "{uid pemilik}"
    status: "online" | "offline"
    battery: 85
    last_seen: timestamp
    location:
      lat: 0.0
      lng: 0.0
      timestamp: 0
    uninstall_protected: true
    flashlight: "off" | "on" | "strobe"

commands/
  {device_id}/
    {command_id}/
      type: "message" | "flashlight" | "wallpaper_suggestion" | "lock_request"
      payload: "..."
      created_by: "{uid}"
      created_at: timestamp
      acknowledged: false

alerts/
  {device_id}/
    {alert_id}/
      type: "sos" | "checkin_missed" | "geofence_exit"
      location: { lat, lng }
      timestamp: 0
      resolved: false

checkins/
  {device_id}/
    schedule: ["08:00", "12:00", "18:00", "21:00"]
    last_checkin: timestamp

geofences/
  {device_id}/
    {fence_id}/
      label: "Rumah"
      lat: 0.0
      lng: 0.0
      radius_meters: 200
```

## Security Rules (database.rules.json)

Prinsip: hanya user ter-autentikasi (owner/admin) yang bisa baca semua device;
device (app anak) hanya bisa menulis ke node miliknya sendiri menggunakan custom
token / anonymous auth yang di-link ke device_id.

```json
{
  "rules": {
    "users": {
      ".read": "auth != null",
      "$uid": {
        ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'owner' || auth.uid === $uid)"
      }
    },
    "devices": {
      ".read": "auth != null",
      "$device_id": {
        ".write": "auth != null && (root.child('users').child(auth.uid).exists() || auth.uid === $device_id)"
      }
    },
    "commands": {
      "$device_id": {
        ".read": "auth != null && (root.child('users').child(auth.uid).exists() || auth.uid === $device_id)",
        ".write": "auth != null && root.child('users').child(auth.uid).exists()"
      }
    },
    "alerts": {
      "$device_id": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "checkins": {
      "$device_id": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "geofences": {
      "$device_id": {
        ".read": "auth != null",
        ".write": "auth != null && root.child('users').child(auth.uid).exists()"
      }
    }
  }
}
```

## Catatan penting

- Password owner/admin TIDAK disimpan plaintext. Gunakan Firebase Authentication
  (Email/Password atau Custom Auth), bukan field `password` di Realtime DB.
  Username `centralvex` dipetakan ke email internal, mis. `centralvex@noir.local`,
  lalu login pakai Firebase Auth SDK.
- Device (app anak) login menggunakan Firebase Anonymous Auth, UID anonymous-nya
  dipakai sebagai `device_id` agar device hanya bisa menulis datanya sendiri.
- Fitur kamera TIDAK ada di struktur data manapun — sengaja tidak disediakan.
