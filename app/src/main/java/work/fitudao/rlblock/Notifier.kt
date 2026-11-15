package work.fitudao.rlblock

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

object Notifier {
    private const val CHANNEL = "blocked"

    fun show(ctx: Context, number: String, reason: String, time: Long) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "ブロック通知", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle("着信を拒否しました")
            .setContentText("$number ・ ${reasonLabel(reason)}")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setWhen(time)
            .setShowWhen(true)
            .build()
        nm.notify((time and 0x7fffffff).toInt(), n)
    }
}
