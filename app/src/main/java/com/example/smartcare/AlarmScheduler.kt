package com.example.smartcare

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Ini adalah kelas 'pembantu' yang tugasnya khusus untuk mengurus semua hal
 * yang berkaitan dengan penjadwalan dan pembatalan alarm.
 * Dibuat terpisah agar kode di Activity (misalnya ElderlyDashboardActivity) lebih rapi dan bersih.
 * 'context' dibutuhkan agar kelas ini bisa 'berbicara' dengan sistem Android,
 * misalnya untuk memanggil layanan sistem seperti AlarmManager.
 */
class AlarmScheduler(private val context: Context) {

    // Dapatkan akses ke layanan sistem AlarmManager dari Android.
    // Ini adalah 'mesin' utama untuk semua penjadwalan yang akan di lakukan.
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Fungsi ini bertugas untuk MENJADWALKAN sebuah alarm untuk satu tugas spesifik.
     */
    fun schedule(task: Task) {
        // Cek dulu apakah tugas ini punya waktu pengingat. Jika tidak (null), hentikan fungsi.
        val reminderTime = task.reminderTime ?: return

        // --- Bagian Pengecekan Izin ---
        // Untuk Android 12 (S) ke atas, ada aturan ketat untuk alarm presisi.
        // Jadi, periksa dulu apakah aplikasi punya izin 'canScheduleExactAlarms'.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Jika tidak punya izin, catat di Log dan hentikan fungsi agar tidak crash.
                Log.w("AlarmScheduler", "Tidak memiliki izin untuk menjadwalkan alarm presisi.")
                return
            }
        }

        // Siapkan (Intent) yang akan dikirim saat alarm berbunyi.
        // Tujuannya adalah kelas AlarmReceiver kita.
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            // Lampirkan data penting seperti ID dan Judul Tugas ke dalam surat.
            putExtra("EXTRA_TASK_ID", task.id)
            putExtra("EXTRA_TASK_TITLE", task.title)
        }

        // Buat 'request code' yang unik untuk setiap alarm, agar tidak saling tumpang tindih.
        // Menggunakan hashCode dari ID tugas adalah cara yang baik untuk memastikan keunikan.
        val requestCode = task.id.hashCode()

        // Bungkus (Intent)  dengan '(PendingIntent).
        //  memberikan izin kepada sistem Android untuk mengirimkan intent  nanti.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            // FLAG_UPDATE_CURRENT memastikan jika ada alarm dengan ID yang sama, datanya akan diperbarui.
            // FLAG_IMMUTABLE adalah syarat keamanan untuk Android versi baru.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ini adalah perintah intinya: "Hai AlarmManager, tolong setel alarm yang akan membangunkan
        // perangkat (RTC_WAKEUP), pada waktu yang presisi (setExactAndAllowWhileIdle),
        // dan jalankan 'izin' (pendingIntent) ini saat waktunya tiba."
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            // reminderTime.toDate().time mengubah format waktu kita menjadi milidetik yang dimengerti sistem.
            reminderTime.toDate().time,
            pendingIntent
        )
        // Catat di Logcat untuk debugging, memastikan alarm sudah berhasil diatur.
        Log.d("AlarmScheduler", "Alarm diatur untuk tugas: ${task.title}")
    }

    /**
     * Fungsi ini bertugas untuk MEMBATALKAN alarm yang sudah dijadwalkan,
     * misalnya saat tugas sudah diselesaikan atau dihapus.
     */
    fun cancel(task: Task) {
        // Untuk membatalkan sebuah alarm, kita harus membuat PendingIntent yang SAMA PERSIS
        // dengan yang kita gunakan saat menjadwalkannya. Itu sebabnya kita membuat ulang Intent
        // dan PendingIntent dengan requestCode yang identik.
        val intent = Intent(context, AlarmReceiver::class.java)
        val requestCode = task.id.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Perintah untuk membatalkan alarm yang cocok dengan PendingIntent ini.
        alarmManager.cancel(pendingIntent)
        // Catat di Logcat untuk debugging.
        Log.d("AlarmScheduler", "Alarm dibatalkan untuk tugas: ${task.title}")
    }
}