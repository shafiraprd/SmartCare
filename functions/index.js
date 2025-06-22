// Import function onDocumentCreated dan onDocumentUpdated dari Firebase Functions v2 untuk Firestore trigger
const {onDocumentCreated, onDocumentUpdated} = require("firebase-functions/v2/firestore");

// Import admin SDK untuk inisialisasi aplikasi Firebase
const {initializeApp} = require("firebase-admin/app");

// Import Firestore dari Firebase Admin untuk akses database
const {getFirestore} = require("firebase-admin/firestore");

// Import Firebase Cloud Messaging dari Admin SDK untuk kirim notifikasi
const {getMessaging} = require("firebase-admin/messaging");

// Inisialisasi aplikasi Firebase Admin
initializeApp();


// ==============================
// FUNGSI 1:
// Kirim notifikasi ke LANSIA ketika tugas baru ditambahkan oleh keluarga
// Trigger: dokumen baru dibuat di koleksi users/{userId}/tasks/{taskId}
// ==============================
exports.sendTaskNotification = onDocumentCreated("users/{userId}/tasks/{taskId}", async (event) => {
  // Ambil data dari dokumen tugas yang baru dibuat
  const taskData = event.data.data();
  const title = taskData.title;
  const assignedToId = taskData.assignedTo; // ID pengguna lansia yang ditugaskan

  // Ambil data pengguna lansia dari Firestore
  const userDoc = await getFirestore()
      .collection("users")
      .doc(assignedToId)
      .get();

  // Ambil FCM token dari pengguna lansia
  const fcmToken = userDoc.data().fcmToken;

  // Cek apakah token tersedia
  if (fcmToken) {
    // Siapkan payload notifikasi
    const payload = {
      notification: {
        title: "Tugas Baru Ditambahkan!",
        body: `Anda memiliki tugas baru: ${title}`,
        sound: "default",
      },
    };

    try {
      // Kirim notifikasi ke perangkat dengan token tersebut
      const response = await getMessaging().sendToDevice(fcmToken, payload);
      console.log("Notifikasi berhasil dikirim:", response);
    } catch (error) {
      console.log("Error mengirim notifikasi:", error);
    }
  } else {
    // Log jika pengguna tidak memiliki token
    console.log("Tidak ada FCM token untuk pengguna:", assignedToId);
  }
});


// ==============================
// FUNGSI 2:
// Kirim notifikasi ke KELUARGA ketika tugas selesai dikerjakan oleh lansia
// Trigger: dokumen di-update di koleksi users/{userId}/tasks/{taskId}
// ==============================
exports.sendCompletionNotification = onDocumentUpdated("users/{userId}/tasks/{taskId}", async (event) => {
  // Ambil data sebelum dan sesudah perubahan dokumen
  const beforeData = event.data.before.data();
  const afterData = event.data.after.data();

  // Cek apakah status berubah dari 'pending' menjadi 'completed'
  if (beforeData.status === "pending" &&
      afterData.status === "completed") {

    const taskTitle = afterData.title;
    const familyId = afterData.createdBy; // ID pengguna keluarga
    const elderlyId = afterData.assignedTo; // ID pengguna lansia

    // Ambil nama lansia dari database untuk ditampilkan di notifikasi
    const elderlyUserDoc = await getFirestore()
        .collection("users")
        .doc(elderlyId)
        .get();
    const elderlyName = elderlyUserDoc.data().name || "Lansia";

    // Ambil FCM token milik pengguna keluarga
    const familyUserDoc = await getFirestore()
        .collection("users")
        .doc(familyId)
        .get();
    const fcmToken = familyUserDoc.data().fcmToken;

    // Cek apakah token tersedia
    if (fcmToken) {
      // Siapkan payload notifikasi
      const payload = {
        notification: {
          title: "Tugas Telah Diselesaikan!",
          body: `${elderlyName} telah menyelesaikan tugas: ${taskTitle}`,
          sound: "default",
        },
      };

      try {
        // Kirim notifikasi ke perangkat keluarga
        const response = await getMessaging()
            .sendToDevice(fcmToken, payload);
        console.log("Notifikasi selesai berhasil dikirim:", response);
      } catch (error) {
        console.log("Error mengirim notifikasi selesai:", error);
      }
    } else {
      // Log jika token tidak tersedia
      console.log("Tidak ada FCM token untuk pengguna keluarga:", familyId);
    }
  }

  // Mengembalikan null sebagai nilai akhir fungsi
  return null;
});
