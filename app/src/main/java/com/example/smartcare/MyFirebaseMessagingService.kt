// Path: app/src/main/java/com/example/smartcare/MyFirebaseMessagingService.kt
package com.example.smartcare

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Fungsi ini akan dipanggil saat aplikasi mendapatkan token baru dari Firebase
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Refreshed token: $token")
        // Di aplikasi nyata, Anda akan mengirim token ini ke server Anda.
        // Kita akan melakukannya di halaman login.
    }

    // Fungsi ini akan dipanggil saat ada push notification masuk KETIKA APLIKASI SEDANG DIBUKA
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM_MESSAGE", "From: ${remoteMessage.from}")

        // Cek apakah notifikasi punya data
        remoteMessage.notification?.let {
            Log.d("FCM_MESSAGE", "Notification Message Body: ${it.body}")
            // Di sini Anda bisa menampilkan dialog atau notifikasi custom jika aplikasi sedang dibuka
        }
    }
}