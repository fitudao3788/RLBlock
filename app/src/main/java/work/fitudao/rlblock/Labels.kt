package work.fitudao.rlblock

fun reasonLabel(r: String) = when (r) {
    "number" -> "個別の番号"
    "anonymous" -> "非通知"
    "foreign" -> "国外"
    "unknown" -> "電話帳未登録"
    else -> r
}
