import { auth, db, usernameToEmail } from "./firebase-config.js";
import { signInWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";
import { ref, get } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-database.js";

const form = document.getElementById("loginForm");
const errorEl = document.getElementById("loginError");

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  errorEl.textContent = "";
  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;
  const email = usernameToEmail(username);

  try {
    const cred = await signInWithEmailAndPassword(auth, email, password);
    const uid = cred.user.uid;

    // Ambil role dari Realtime DB
    const snap = await get(ref(db, `users/${uid}`));
    if (!snap.exists()) {
      errorEl.textContent = "Akun belum terdaftar sebagai admin/owner.";
      return;
    }
    const userData = snap.val();
    sessionStorage.setItem("cv_uid", uid);
    sessionStorage.setItem("cv_role", userData.role);
    sessionStorage.setItem("cv_username", username);
    window.location.href = "dashboard.html";
  } catch (err) {
    errorEl.textContent = "Username atau password salah.";
    console.error(err);
  }
});
