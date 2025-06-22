const {onDocumentCreated, onDocumentUpdated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

// FUNGSI 1: Mengirim notifikasi ke LANSIA saat tugas BARU dibuat.
// Menggunakan sintaksis baru onDocumentCreated
exports.sendTaskNotification = onDocumentCreated("users/{userId}/tasks/{taskId}", async (event) => {
  // Data dari tugas yang baru dibuat.
  const taskData = event.data.data();
  const title = taskData.title;
  const assignedToId = taskData.assignedTo;

  // Ambil data pengguna lansia untuk mendapatkan FCM token mereka.
  const userDoc = await getFirestore()
      .collection("users")
      .doc(assignedToId)
      .get();

  const fcmToken = userDoc.data().fcmToken;

  if (fcmToken) {
    const payload = {
      notification: {
        title: "Tugas Baru Ditambahkan!",
        body: `Anda memiliki tugas baru: ${title}`,
        sound: "default",
      },
    };

    try {
      const response = await getMessaging().sendToDevice(fcmToken, payload);
      console.log("Notifikasi berhasil dikirim:", response);
    } catch (error) {
      console.log("Error mengirim notifikasi:", error);
    }
  } else {
    console.log("Tidak ada FCM token untuk pengguna:", assignedToId);
  }
});

// FUNGSI 2: Mengirim notifikasi ke KELUARGA saat tugas SELESAI.
// Menggunakan sintaksis baru onDocumentUpdated
exports.sendCompletionNotification = onDocumentUpdated("users/{userId}/tasks/{taskId}", async (event) => {
  // Dapatkan data sebelum dan sesudah update.
  const beforeData = event.data.before.data();
  const afterData = event.data.after.data();

  // Cek apakah status berubah dari 'pending' menjadi 'completed'.
  if (beforeData.status === "pending" &&
      afterData.status === "completed") {
    const taskTitle = afterData.title;
    const familyId = afterData.createdBy; // ID pengguna keluarga
    const elderlyId = afterData.assignedTo; // ID pengguna lansia

    // Ambil nama pengguna lansia untuk ditampilkan di notifikasi.
    const elderlyUserDoc = await getFirestore()
        .collection("users")
        .doc(elderlyId)
        .get();
    const elderlyName = elderlyUserDoc.data().name || "Lansia";

    // Ambil FCM token milik pengguna keluarga.
    const familyUserDoc = await getFirestore()
        .collection("users")
        .doc(familyId)
        .get();
    const fcmToken = familyUserDoc.data().fcmToken;

    if (fcmToken) {
      const payload = {
        notification: {
          title: "Tugas Telah Diselesaikan!",
          body: `${elderlyName} telah menyelesaikan tugas: ${taskTitle}`,
          sound: "default",
        },
      };

      try {
        const response = await getMessaging()
            .sendToDevice(fcmToken, payload);
        console.log("Notifikasi selesai berhasil dikirim:", response);
      } catch (error) {
        console.log("Error mengirim notifikasi selesai:", error);
      }
    } else {
      console.log("Tidak ada FCM token untuk pengguna keluarga:", familyId);
    }
  }
  return null;
});