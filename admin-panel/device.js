import { db } from "./firebase-config.js";
import { ref, onValue, set, push, update } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-database.js";

const uid = sessionStorage.getItem("cv_uid");
if (!uid) window.location.href = "index.html";

document.getElementById("backBtn").addEventListener("click", () => {
  window.location.href = "dashboard.html";
});

const params = new URLSearchParams(window.location.search);
const deviceId = params.get("id");
if (!deviceId) window.location.href = "dashboard.html";

const deviceRef = ref(db, `devices/${deviceId}`);
const username = sessionStorage.getItem("cv_username");

// ---- Load device info ----
onValue(deviceRef, (snap) => {
  const d = snap.val();
  if (!d) return;
  document.getElementById("deviceName").textContent = d.child_name || "Tanpa nama";
  document.getElementById("deviceStatus").textContent =
    `Status: ${d.status || 'offline'} · Baterai: ${d.battery ?? '-'}%`;

  document.getElementById("flashlightToggle").checked = d.flashlight === "on";
  document.getElementById("uninstallToggle").checked = !!d.uninstall_protected;

  if (d.location) {
    document.getElementById("mapBox").innerHTML =
      `Lat: ${d.location.lat.toFixed(5)}, Lng: ${d.location.lng.toFixed(5)}<br>
       <a href="https://maps.google.com/?q=${d.location.lat},${d.location.lng}" target="_blank" style="color:var(--accent)">Buka di Google Maps</a>`;
  }
});

// ---- Alerts (SOS, checkin missed, geofence) ----
onValue(ref(db, `alerts/${deviceId}`), (snap) => {
  const list = document.getElementById("alertList");
  list.innerHTML = "";
  if (!snap.exists()) {
    list.innerHTML = `<p style="color:var(--muted);font-size:13px">Tidak ada peringatan.</p>`;
    return;
  }
  const entries = [];
  snap.forEach((c) => entries.push({ id: c.key, ...c.val() }));
  entries.sort((a, b) => b.timestamp - a.timestamp);
  entries.slice(0, 10).forEach((a) => {
    const div = document.createElement("div");
    div.className = "alert-item" + (a.type === "sos" ? " sos" : "");
    const label = { sos: "SOS DITEKAN", checkin_missed: "Check-in terlewat", geofence_exit: "Keluar dari area aman" }[a.type] || a.type;
    div.textContent = `${label} — ${new Date(a.timestamp).toLocaleString('id-ID')}`;
    list.appendChild(div);
  });
});

// ---- Flashlight toggle ----
document.getElementById("flashlightToggle").addEventListener("change", (e) => {
  update(deviceRef, { flashlight: e.target.checked ? "on" : "off" });
  push(ref(db, `commands/${deviceId}`), {
    type: "flashlight",
    payload: e.target.checked ? "on" : "off",
    created_by: username,
    created_at: Date.now(),
    acknowledged: false
  });
});

// ---- Uninstall protection toggle ----
document.getElementById("uninstallToggle").addEventListener("change", (e) => {
  update(deviceRef, { uninstall_protected: e.target.checked });
});

// ---- Send message (as notification) ----
document.getElementById("sendMessageBtn").addEventListener("click", () => {
  const text = document.getElementById("messageText").value.trim();
  if (!text) return;
  push(ref(db, `commands/${deviceId}`), {
    type: "message",
    payload: text,
    created_by: username,
    created_at: Date.now(),
    acknowledged: false
  });
  document.getElementById("messageText").value = "";
  alert("Notifikasi dikirim ke HP anak.");
});

// ---- Wallpaper suggestion ----
document.getElementById("sendWallpaperBtn").addEventListener("click", () => {
  const url = document.getElementById("wallpaperUrl").value.trim();
  if (!url) return;
  push(ref(db, `commands/${deviceId}`), {
    type: "wallpaper_suggestion",
    payload: url,
    created_by: username,
    created_at: Date.now(),
    acknowledged: false
  });
  alert("Saran wallpaper dikirim. Anak akan menerima notifikasi untuk menyetujui.");
});

// ---- Check-in schedule ----
document.getElementById("saveCheckinBtn").addEventListener("click", () => {
  const raw = document.getElementById("checkinSchedule").value.trim();
  if (!raw) return;
  const schedule = raw.split(",").map(s => s.trim()).filter(Boolean);
  set(ref(db, `checkins/${deviceId}/schedule`), schedule);
  alert("Jadwal check-in disimpan.");
});

// ---- Geofence (uses device's last known location as center) ----
document.getElementById("addGeofenceBtn").addEventListener("click", () => {
  const label = document.getElementById("geofenceLabel").value.trim() || "Area";
  onValue(deviceRef, (snap) => {
    const d = snap.val();
    if (!d || !d.location) {
      alert("Lokasi device belum tersedia, tidak bisa menandai area.");
      return;
    }
    push(ref(db, `geofences/${deviceId}`), {
      label,
      lat: d.location.lat,
      lng: d.location.lng,
      radius_meters: 200
    });
    alert(`Area "${label}" ditambahkan berdasarkan lokasi device saat ini.`);
  }, { onlyOnce: true });
});
