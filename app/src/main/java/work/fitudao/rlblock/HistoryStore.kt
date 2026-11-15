package work.fitudao.rlblock

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HistoryEntry(val time: Long, val number: String, val reason: String)

object HistoryStore {
    private const val MAX = 500
    val entries = mutableStateListOf<HistoryEntry>()
    private var loaded = false

    private fun file(ctx: Context) = File(ctx.filesDir, "history.json")

    @Synchronized
    fun ensureLoaded(ctx: Context) {
        if (loaded) return
        loaded = true
        try {
            val a = JSONArray(file(ctx).readText())
            entries.addAll((0 until a.length()).map {
                val o = a.getJSONObject(it)
                HistoryEntry(o.getLong("t"), o.getString("n"), o.getString("r"))
            })
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    fun add(ctx: Context, e: HistoryEntry) {
        ensureLoaded(ctx)
        entries.add(0, e)
        while (entries.size > MAX) entries.removeAt(entries.lastIndex)
        save(ctx)
    }

    @Synchronized
    fun remove(ctx: Context, targets: Set<HistoryEntry>) {
        ensureLoaded(ctx)
        entries.removeAll { it in targets }
        save(ctx)
    }

    @Synchronized
    fun clear(ctx: Context) {
        ensureLoaded(ctx)
        entries.clear()
        save(ctx)
    }

    private fun save(ctx: Context) {
        val a = JSONArray()
        entries.forEach { a.put(JSONObject().put("t", it.time).put("n", it.number).put("r", it.reason)) }
        val tmp = File(ctx.filesDir, "history.json.tmp")
        tmp.writeText(a.toString())
        tmp.renameTo(file(ctx))
    }
}
