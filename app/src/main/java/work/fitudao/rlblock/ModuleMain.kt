package work.fitudao.rlblock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.lang.reflect.Method

class ModuleMain : XposedModule() {
    private companion object {
        const val TAG = "RLBlock"
        const val TARGET = "jp.co.rakuten.mobile.rcs"
        const val INTERACTOR = "jp.co.rakuten.mobile.rcs.call.common.CallSlInteractor"
        const val API_SERVICE = "com.mavenir.appservice.api.ApiServiceMain"
        const val REJECT_DECLINE = 603
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(Log.INFO, TAG, "loaded in ${param.processName}, API $apiVersion")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != TARGET || !param.isFirstPackage) return
        try {
            install(param.classLoader)
        } catch (t: Throwable) {
            log(Log.ERROR, TAG, "install failed", t)
        }
    }

    private fun install(cl: ClassLoader) {
        val interactor = Class.forName(INTERACTOR, false, cl)
        val apiClass = Class.forName(API_SERVICE, false, cl)

        // 難読化名に依存しないよう、型・シグネチャで特定
        val onIncoming: Method = interactor.declaredMethods.single {
            it.returnType == Void.TYPE && it.parameterTypes.contentEquals(
                arrayOf(
                    String::class.java, String::class.java, String::class.java, String::class.java,
                    java.lang.Boolean.TYPE, java.lang.Boolean.TYPE
                )
            )
        }
        val apiField = interactor.declaredFields.single { it.type == apiClass }.apply { isAccessible = true }
        val ctxField = interactor.declaredFields.single { it.type == Context::class.java }.apply { isAccessible = true }
        val reject = apiClass.getMethod("rejectCall", String::class.java, Integer.TYPE)

        val prefs = getRemotePreferences(Blocklist.GROUP)
        log(Log.INFO, TAG, "hook ${onIncoming.name} / api ${apiField.name} / ctx ${ctxField.name}")

        hook(onIncoming).intercept { chain ->
            val callId = chain.getArg(0) as String?
            val remote = chain.getArg(1) as String?
            val isNative = chain.getArg(5) as Boolean
            if (!isNative && callId != null && remote != null) {
                val ctx = ctxField.get(chain.thisObject) as Context
                val reason = try {
                    judge(prefs, ctx, remote)
                } catch (t: Throwable) {
                    log(Log.ERROR, TAG, "judge failed, passing through", t)
                    null
                }
                if (reason != null) {
                    try {
                        reject.invoke(apiField.get(chain.thisObject), callId, REJECT_DECLINE)
                        log(Log.INFO, TAG, "rejected($reason): $remote")
                        report(ctx, if (Blocklist.hasDigits(remote)) Blocklist.normalize(remote) else remote, reason)
                        return@intercept null
                    } catch (t: Throwable) {
                        log(Log.ERROR, TAG, "reject failed, passing through", t)
                    }
                } else {
                    log(Log.DEBUG, TAG, "incoming (allowed): $remote")
                }
            }
            chain.proceed()
        }
    }

    private fun report(ctx: Context, number: String, reason: String) {
        try {
            ctx.sendBroadcast(
                Intent(Ipc.ACTION)
                    .setClassName(Ipc.MODULE_PKG, Ipc.RECEIVER)
                    .putExtra(Ipc.EXTRA_NUMBER, number)
                    .putExtra(Ipc.EXTRA_REASON, reason)
                    .putExtra(Ipc.EXTRA_TIME, System.currentTimeMillis())
            )
        } catch (t: Throwable) {
            log(Log.WARN, TAG, "history report failed", t)
        }
    }

    // 拒否理由を返す。許可なら null
    private fun judge(prefs: android.content.SharedPreferences, ctx: Context, remote: String): String? {
        // 非通知
        if (!Blocklist.hasDigits(remote)) {
            return if (prefs.getBoolean(Blocklist.KEY_ANONYMOUS, false)) "anonymous" else null
        }
        val n = Blocklist.normalize(remote)

        // 個別番号
        if (n in Blocklist.enabledSet(prefs)) return "number"

        val blockForeign = prefs.getBoolean(Blocklist.KEY_FOREIGN, false)
        val blockUnknown = prefs.getBoolean(Blocklist.KEY_UNKNOWN, false)
        if (!blockForeign && !blockUnknown) return null

        // 電話帳登録済みは国外・未登録ルールの対象外
        if (inContacts(ctx, n)) return null
        if (blockForeign && Blocklist.isForeign(n)) return "foreign"
        if (blockUnknown) return "unknown"
        return null
    }

    private fun inContacts(ctx: Context, number: String): Boolean {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use {
            return it.count > 0
        }
        throw IllegalStateException("contacts query returned null")
    }
}
