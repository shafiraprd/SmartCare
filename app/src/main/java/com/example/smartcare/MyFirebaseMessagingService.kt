// Path: app/src/main/java/com/example/smartcare/MyFirebaseMessagingService.kt
package com.example.smartcare

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // fungsi ini dipanggil saat FCM memberikan token baru untuk device ini
    // token ini unik dan dipakai untuk kirim notifikasi ke device tertentu
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Refreshed token: $token")
        // token ini biasanya dikirim ke server agar bisa dipakai untuk kirim notifikasi
        // untuk aplikasi ini, pengiriman token ke Firestore dilakukan saat login
    }

    // fungsi ini akan dipanggil saat device menerima push notification
    // khususnya ketika aplikasi sedang aktif (foreground)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM_MESSAGE", "From: ${remoteMessage.from}")

        // cek apakah pesan notifikasi punya isi
        remoteMessage.notification?.let {
            Log.d("FCM_MESSAGE", "Notification Message Body: ${it.body}")
            // di sini bisa tambahkan logika untuk munculkan notifikasi custom
            // misalnya pakai NotificationManager atau tampilkan dialog
        }
    }
}
