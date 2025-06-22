package com.example.smartcare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    /**
     * 'context' adalah informasi tentang keadaan aplikasi saat ini.
     * 'intent' adalah 'surat' yang dikirim oleh AlarmManager, berisi data tugas yang dilampirkan sebelumnya.
     */
    override fun onReceive(context: Context, intent: Intent) {
        // Ambil data yang sudah dikirimkan dari ElderlyDashboardActivity.
        // 'getStringExtra' digunakan untuk mengambil data berupa teks (String).
        val taskId = intent.getStringExtra("EXTRA_TASK_ID")
        val taskTitle = intent.getStringExtra("EXTRA_TASK_TITLE") ?: "Anda memiliki jadwal kegiatan!" // Teks default jika judulnya kosong

        // --- Bagian untuk membuat notifikasi bisa di-klik ---
        // Pertama, siapkan  (Intent) untuk membuka halaman ElderlyDashboardActivity saat notifikasi diklik.
        val activityIntent = Intent(context, ElderlyDashboardActivity::class.java).apply {
            // FLAG ini memastikan saat notifikasi diklik, aplikasi terbuka dengan benar dan tidak menumpuk halaman.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // 'PendingIntent' adalah bungkusan dari 'Intent' di atas.
        // seperti memberikan "izin" kepada sistem Android untuk menjalankan Intent kita
        // atas nama aplikasi, bahkan saat ditutup.
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(), // Gunakan ID unik untuk setiap PendingIntent
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        // Untuk Android versi 8.0 (Oreo) ke atas, setiap notifikasi harus punya 'channel' atau kategori.
        // Kode ini membuat channel jika belum ada. Jika sudah ada, tidak akan dibuat ulang.
        val channelId = "smartcare_channel"
        val channelName = "Pengingat SmartCare"

        // Dapatkan 'manajer notifikasi' dari sistem Android.
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Buat objek Channel dengan ID unik, nama yang akan muncul di setelan, dan tingkat kepentingan.
        // Tingkat kepentingan 'HIGH' membuat notifikasi muncul sebagai pop-up (heads-up notification).
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)



        // Mulai proses pembuatan notifikasi, menghubungkannya dengan channelId yang sudah dibuat.
        val notification = NotificationCompat.Builder(context, channelId)
            // .setSmallIcon: Menentukan ikon kecil yang akan muncul di status bar.
            .setSmallIcon(R.drawable.ic_notification)
            // .setContentTitle: Judul dari notifikasi.
            .setContentTitle("Pengingat Jadwal")
            // .setContentText: Isi atau deskripsi dari notifikasi.
            .setContentText(taskTitle)
            // .setPriority: Menentukan prioritas (penting agar muncul pop-up).
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // .setAutoCancel(true): Notifikasi akan otomatis hilang dari status bar saat di-klik.
            .setAutoCancel(true)
            // .setContentIntent(pendingIntent): Menghubungkan aksi klik dengan 'izin' (PendingIntent) yang sudah kita siapkan untuk membuka aplikasi.
            .setContentIntent(pendingIntent)
            // .build(): Selesaikan proses  dan objek notifikasi yang siap ditampilkan.
            .build()


        // Tampilkan notifikasi ke layar pengguna.
        // taskId.hashCode() digunakan sebagai ID unik untuk notifikasi ini,
        // agar setiap tugas memiliki notifikasinya sendiri-sendiri dan tidak saling menimpa.
        notificationManager.notify(taskId.hashCode(), notification)
    }
}