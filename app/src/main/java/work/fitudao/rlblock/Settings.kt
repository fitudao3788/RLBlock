package work.fitudao.rlblock

import android.content.Context

object Settings {
    private const val FILE = "settings"
    const val KEY_NOTIFY = "notify"
    const val KEY_RECORD = "record"

    fun get(ctx: Context, key: String): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(key, true)

    fun set(ctx: Context, key: String, v: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(key, v).apply()
    }
}
