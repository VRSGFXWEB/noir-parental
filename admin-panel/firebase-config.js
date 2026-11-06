// Firebase config — ganti dengan project kamu kalau perlu
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";
import { getDatabase } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-database.js";

const firebaseConfig = {
  apiKey: "AIzaSyAk-IHBRJaeVmQl1fS5bz2pCE_5rVyOBlE",
  authDomain: "xrpl-database-67cec.firebaseapp.com",
  databaseURL: "https://xrpl-database-67cec-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "xrpl-database-67cec",
  storageBucket: "xrpl-database-67cec.firebasestorage.app",
  messagingSenderId: "705054439033",
  appId: "1:705054439033:web:82dcaa95562e81796e46a8"
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getDatabase(app);

// PENTING: username "centralvex" dipetakan ke email internal untuk Firebase Auth.
// Sebelum dipakai, buat akun ini di Firebase Console > Authentication > Add user:
//   email: centralvex@noir.local
//   password: (ganti VRSHACK dengan password yang lebih kuat di Firebase Console)
// Realtime DB tidak pernah menyimpan password dalam bentuk apapun — hanya role.
export function usernameToEmail(username) {
  return `${username.trim().toLowerCase()}@noir.local`;
}
