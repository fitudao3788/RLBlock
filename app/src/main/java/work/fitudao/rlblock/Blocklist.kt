package work.fitudao.rlblock

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class NumItem(val number: String, val enabled: Boolean = true)

object Blocklist {
    const val GROUP = "config"
    const val KEY_ITEMS = "items"
    const val KEY_NUMBERS = "blocklist" // 旧形式(1行1件テキスト)
    const val KEY_ANONYMOUS = "block_anonymous"
    const val KEY_UNKNOWN = "block_unknown"
    const val KEY_FOREIGN = "block_foreign"

    // "sip:+8190...@host;..." のユーザー部のみ取り出す(ドメイン側の数字で誤判定しない)
    private fun userPart(raw: String): String {
        val s = raw.trim().lowercase()
        return s.substringAfter(':', s).substringBefore('@').substringBefore(';')
    }

    fun hasDigits(raw: String): Boolean = userPart(raw).any { it.isDigit() }

    // 国内は 0 始まり、国外は +国番号 始まりに正規化
    fun normalize(raw: String): String {
        val s = userPart(raw)
        val digits = s.filter { it.isDigit() }
        if (digits.isEmpty()) return s
        return when {
            digits.startsWith("0") && !s.startsWith("+") -> digits
            digits.startsWith("81") -> "0" + digits.substring(2)
            else -> "+$digits"
        }
    }

    fun isForeign(normalized: String): Boolean = normalized.startsWith("+")

    fun encode(items: List<NumItem>): String {
        val a = JSONArray()
        items.forEach { a.put(JSONObject().put("n", it.number).put("e", it.enabled)) }
        return a.toString()
    }

    private fun decode(json: String): List<NumItem> = try {
        val a = JSONArray(json)
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            NumItem(o.getString("n"), o.optBoolean("e", true))
        }
    } catch (_: Throwable) {
        emptyList()
    }

    fun load(prefs: SharedPreferences): List<NumItem> {
        prefs.getString(KEY_ITEMS, null)?.let { return decode(it) }
        return (prefs.getString(KEY_NUMBERS, "") ?: "").lineSequence()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .map { NumItem(normalize(it)) }
            .distinct()
            .toList()
    }

    fun enabledSet(prefs: SharedPreferences): Set<String> =
        load(prefs).filter { it.enabled }.map { it.number }.toSet()
}
