import { auth, db, usernameToEmail } from "./firebase-config.js";
import { signOut, createUserWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";
import { ref, onValue, set, get } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-database.js";

const role = sessionStorage.getItem("cv_role");
const uid = sessionStorage.getItem("cv_uid");
const username = sessionStorage.getItem("cv_username");
if (!uid) window.location.href = "index.html";

document.getElementById("roleBadge").textContent = role === "owner" ? "OWNER" : "ADMIN";
document.getElementById("menuUsername").textContent = `Masuk sebagai: ${username}`;
if (role === "owner") {
  document.getElementById("menuAddAdmin").style.display = "block";
}

// ---- Profile dropdown ----
const profileBtn = document.getElementById("profileBtn");
const profileDropdown = document.getElementById("profileDropdown");
profileBtn.addEventListener("click", (e) => {
  e.stopPropagation();
  profileDropdown.classList.toggle("open");
});
document.addEventListener("click", () => profileDropdown.classList.remove("open"));

document.getElementById("menuLogout").addEventListener("click", async () => {
  await signOut(auth);
  sessionStorage.clear();
  window.location.href = "index.html";
});

// ---- Modal: Add Child (owner & admin) ----
const addChildModal = document.getElementById("addChildModal");
document.getElementById("menuAddChild").addEventListener("click", () => {
  profileDropdown.classList.remove("open");
  addChildModal.classList.add("open");
});
document.getElementById("cancelAddChild").addEventListener("click", () => {
  addChildModal.classList.remove("open");
});
document.getElementById("submitAddChild").addEventListener("click", async () => {
  const label = document.getElementById("childLabel").value.trim();
  const childUsername = document.getElementById("childUsername").value.trim();
  const childPassword = document.getElementById("childPassword").value;
  const errEl = document.getElementById("addChildError");
  if (!label || !childUsername || !childPassword) {
    errEl.textContent = "Semua kolom wajib diisi.";
    return;
  }
  const email = `${childUsername.toLowerCase()}@noir-child.local`;
  try {
    const cred = await createUserWithEmailAndPassword(auth, email, childPassword);
    await set(ref(db, `children/${cred.user.uid}`), {
      label,
      username: childUsername,
      added_by: username,
      added_at: Date.now()
    });
    alert("Akun anak berhasil dibuat. Silakan login ulang sebagai admin/owner.");
    await signOut(auth);
    sessionStorage.clear();
    window.location.href = "index.html";
  } catch (err) {
    errEl.textContent = "Gagal membuat akun: " + err.message;
  }
});

// ---- Modal: Add Admin (owner only) ----
const addAdminModal = document.getElementById("addAdminModal");
if (role === "owner") {
  document.getElementById("menuAddAdmin").addEventListener("click", () => {
    profileDropdown.classList.remove("open");
    addAdminModal.classList.add("open");
  });
  document.getElementById("cancelAddAdmin").addEventListener("click", () => {
    addAdminModal.classList.remove("open");
  });
  document.getElementById("submitAddAdmin").addEventListener("click", async () => {
    const newUsername = document.getElementById("newAdminUser").value.trim();
    const password = document.getElementById("newAdminPass").value;
    const errEl = document.getElementById("addAdminError");
    if (!newUsername || !password) {
      errEl.textContent = "Semua kolom wajib diisi.";
      return;
    }
    const email = usernameToEmail(newUsername);
    try {
      const cred = await createUserWithEmailAndPassword(auth, email, password);
      await set(ref(db, `users/${cred.user.uid}`), {
        username: newUsername,
        role: "admin",
        created_by: username,
        created_at: Date.now()
      });
      alert("Admin berhasil ditambahkan. Silakan login ulang sebagai owner.");
      await signOut(auth);
      sessionStorage.clear();
      window.location.href = "index.html";
    } catch (err) {
      errEl.textContent = "Gagal menambah admin: " + err.message;
    }
  });
}

// ---- Device list, rendered as rounded "island" bars, no emoji ----
const phoneIconSvg = `
<svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect x="7" y="2" width="10" height="20" rx="2" stroke="#C0392B" stroke-width="1.6"/>
  <line x1="10" y1="18" x2="14" y2="18" stroke="#C0392B" stroke-width="1.6" stroke-linecap="round"/>
</svg>`;

const deviceList = document.getElementById("deviceIslandList");
onValue(ref(db, "devices"), (snap) => {
  deviceList.innerHTML = "";
  if (!snap.exists()) {
    deviceList.innerHTML = `<p style="color:var(--muted)">Belum ada device terhubung.</p>`;
    return;
  }
  snap.forEach((child) => {
    const d = child.val();
    const id = child.key;
    const isOnline = d.status === "online";
    const island = document.createElement("div");
    island.className = "device-island";
    island.innerHTML = `
      <div class="phone-icon-wrap">${phoneIconSvg}</div>
      <div class="island-info">
        <div class="island-name">${d.child_name || "Tanpa nama"}</div>
        <div class="island-meta">Baterai ${d.battery ?? "-"}% &middot; ${isOnline ? "Online" : "Offline"}</div>
      </div>
      <div class="island-status" style="background:${isOnline ? "var(--online)" : "var(--offline)"}"></div>
    `;
    island.addEventListener("click", () => {
      window.location.href = `device.html?id=${id}`;
    });
    deviceList.appendChild(island);
  });
});
