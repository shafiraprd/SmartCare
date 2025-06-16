// Menggunakan sintaks v2 yang modern
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {logger} = require("firebase-functions");
const admin = require("firebase-admin");

// Inisialisasi Firebase Admin SDK
admin.initializeApp();

/**
 * Cloud Function ini akan terpicu setiap kali ada DOKUMEN BARU DIBUAT
 * di dalam koleksi 'tasks', menggunakan sintaks v2.
 */
exports.sendTaskNotification = onDocumentCreated("tasks/{taskId}", async (event) => {
  // Dapatkan snapshot dari data yang baru dibuat
  const snap = event.data;
  if (!snap) {
    logger.log("Tidak ada data, notifikasi dibatalkan.");
    return null;
  }

  // 1. Dapatkan data dari tugas yang baru dibuat
  const taskData = snap.data();
  const taskTitle = taskData.title;
  const assignedToUid = taskData.assignedTo;

  if (!assignedToUid) {
    logger.log("Tidak ada assignedToUid, notifikasi dibatalkan.");
    return null;
  }

  logger.log("Tugas baru '", taskTitle, "' dibuat untuk user ", assignedToUid);

  // 2. Dapatkan "alamat" notifikasi (FCM Token) dari profil user Lansia
  const userDocRef = admin.firestore().collection("users").doc(assignedToUid);
  const userDoc = await userDocRef.get();

  if (!userDoc.exists) {
    logger.log(`User dengan UID ${assignedToUid} tidak ditemukan.`);
    return null;
  }

  const fcmToken = userDoc.data().fcmToken;

  if (!fcmToken) {
    logger.log(`User ${assignedToUid} tidak memiliki FCM Token.`);
    return null;
  }

  // 3. Siapkan isi notifikasi (payload)
  const payload = {
    notification: {
      title: "Tugas Baru Untuk Anda!",
      body: `Keluarga Anda menambahkan tugas baru: "${taskTitle}"`,
    },
    token: fcmToken,
  };

  logger.log("Mengirim notifikasi ke token:", fcmToken);

  // 4. Kirim notifikasi menggunakan Firebase Cloud Messaging
  try {
    const response = await admin.messaging().send(payload);
    logger.log("Notifikasi berhasil dikirim:", response);
  } catch (error) {
    logger.error("Gagal mengirim notifikasi:", error);
  }

  return null;
});