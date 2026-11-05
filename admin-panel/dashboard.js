import { auth, db, usernameToEmail } from "./firebase-config.js";
import { signOut, createUserWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";
import { ref, onValue, set, get } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-database.js";

const role = sessionStorage.getItem("cv_role");
const uid = sessionStorage.getItem("cv_uid");
if (!uid) window.location.href = "index.html";

document.getElementById("roleBadge").textContent = role === "owner" ? "OWNER" : "ADMIN";
if (role === "owner") {
  document.getElementById("ownerOnlySection").style.display = "block";
}

document.getElementById("logoutBtn").addEventListener("click", async () => {
  await signOut(auth);
  sessionStorage.clear();
  window.location.href = "index.html";
});

// ---- Device list ----
const deviceGrid = document.getElementById("deviceGrid");
onValue(ref(db, "devices"), (snap) => {
  deviceGrid.innerHTML = "";
  if (!snap.exists()) {
    deviceGrid.innerHTML = `<p style="color:var(--muted)">Belum ada device terhubung.</p>`;
    return;
  }
  snap.forEach((child) => {
    const d = child.val();
    const id = child.key;
    const card = document.createElement("div");
    card.className = "device-card";
    card.innerHTML = `
      <h3><span class="status-dot ${d.status === 'online' ? 'online' : 'offline'}"></span>${d.child_name || 'Tanpa nama'}</h3>
      <div class="device-meta">Baterai: ${d.battery ?? '-'}%</div>
      <div class="device-meta">Status: ${d.status || 'offline'}</div>
    `;
    card.addEventListener("click", () => {
      window.location.href = `device.html?id=${id}`;
    });
    deviceGrid.appendChild(card);
  });
});

// ---- Owner: manage admins ----
if (role === "owner") {
  const adminList = document.getElementById("adminList");
  onValue(ref(db, "users"), (snap) => {
    adminList.innerHTML = "";
    snap.forEach((child) => {
      const u = child.val();
      const li = document.createElement("li");
      li.innerHTML = `<span>${u.username || child.key}</span><span class="role-badge">${u.role}</span>`;
      adminList.appendChild(li);
    });
  });

  document.getElementById("addAdminForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const username = document.getElementById("newAdminUser").value.trim();
    const password = document.getElementById("newAdminPass").value;
    const email = usernameToEmail(username);
    try {
      // Catatan: membuat user baru dari client akan otomatis login sebagai user itu.
      // Untuk produksi, pindahkan proses ini ke Firebase Cloud Function (Admin SDK)
      // supaya sesi owner tidak ter-switch. Di sini contoh dasar untuk pengembangan.
      const cred = await createUserWithEmailAndPassword(auth, email, password);
      await set(ref(db, `users/${cred.user.uid}`), {
        username,
        role: "admin",
        created_by: sessionStorage.getItem("cv_username"),
        created_at: Date.now()
      });
      alert("Admin berhasil ditambahkan. Silakan login ulang sebagai owner.");
      await signOut(auth);
      sessionStorage.clear();
      window.location.href = "index.html";
    } catch (err) {
      alert("Gagal menambah admin: " + err.message);
    }
  });
}
