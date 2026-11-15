package work.fitudao.rlblock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class App : Application(), XposedServiceHelper.OnServiceListener {
    companion object {
        var service: XposedService? by mutableStateOf(null)
            private set
    }

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        App.service = service
    }

    override fun onServiceDied(service: XposedService) {
        App.service = null
    }
}
