package work.fitudao.rlblock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

private val VALID_REASONS = setOf("anonymous", "number", "foreign", "unknown")

class HistoryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Ipc.ACTION) return
        // exported のため他アプリからも送れる。最低限、値を検証して異常な入力を弾く
        val number = intent.getStringExtra(Ipc.EXTRA_NUMBER)?.takeIf { it.length in 1..64 } ?: return
        val reason = intent.getStringExtra(Ipc.EXTRA_REASON)?.takeIf { it in VALID_REASONS } ?: return
        val now = System.currentTimeMillis()
        val time = intent.getLongExtra(Ipc.EXTRA_TIME, now).coerceIn(now - 60_000, now)
        val app = context.applicationContext
        if (Settings.get(app, Settings.KEY_RECORD)) HistoryStore.add(app, HistoryEntry(time, number, reason))
        if (Settings.get(app, Settings.KEY_NOTIFY)) Notifier.show(app, number, reason, time)
    }
}
