package work.fitudao.rlblock

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import java.util.concurrent.CancellationException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HistoryStore.ensureLoaded(applicationContext)
        setContent { RLTheme { Root() } }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RLTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Root() {
    val svc = App.service
    val prefs = remember(svc) { svc?.getRemotePreferences(Blocklist.GROUP) }
    val items = remember(prefs) {
        mutableStateListOf<NumItem>().apply { prefs?.let { addAll(Blocklist.load(it)) } }
    }
    var screen by remember { mutableStateOf("main") }
    var lastSub by remember { mutableStateOf("numbers") }
    var subSelecting by remember { mutableStateOf(false) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backFromRight by remember { mutableStateOf(false) }
    val sub = screen != "main"
    if (sub) lastSub = screen

    val persist: () -> Unit = {
        prefs?.edit()?.putString(Blocklist.KEY_ITEMS, Blocklist.encode(items.toList()))?.apply()
    }

    PredictiveBackHandler(enabled = sub && !subSelecting) { events ->
        try {
            events.collect {
                backProgress = it.progress
                backFromRight = it.swipeEdge == BackEventCompat.EDGE_RIGHT
            }
            screen = "main"
        } catch (e: CancellationException) {
            backProgress = 0f
        }
    }
    LaunchedEffect(sub) {
        if (!sub) {
            delay(400)
            backProgress = 0f
        }
    }

    Box(Modifier.fillMaxSize()) {
        MainScreen(
            svc != null, prefs, items.size, HistoryStore.entries.size,
            { screen = "numbers" }, { screen = "history" },
        )
        AnimatedVisibility(
            visible = sub,
            enter = slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 3 } + fadeIn(),
            exit = slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 3 } + fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = backProgress
                        val scale = 1f - 0.1f * p
                        scaleX = scale
                        scaleY = scale
                        translationX = (if (backFromRight) -1f else 1f) * 24.dp.toPx() * p
                        shape = RoundedCornerShape((28 * p).dp)
                        clip = p > 0f
                    },
            ) {
                when (lastSub) {
                    "numbers" -> NumbersScreen(items, prefs != null, persist, { subSelecting = it }) { screen = "main" }
                    else -> HistoryScreen({ subSelecting = it }) { screen = "main" }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainScreen(
    connected: Boolean,
    prefs: android.content.SharedPreferences?,
    numberCount: Int,
    historyCount: Int,
    onOpenNumbers: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val svc = App.service
    val ctx = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var notify by remember { mutableStateOf(Settings.get(ctx, Settings.KEY_NOTIFY)) }
    var record by remember { mutableStateOf(Settings.get(ctx, Settings.KEY_RECORD)) }
    LaunchedEffect(notify) {
        if (notify && Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    var anonymous by remember(prefs) { mutableStateOf(prefs?.getBoolean(Blocklist.KEY_ANONYMOUS, false) ?: false) }
    var unknown by remember(prefs) { mutableStateOf(prefs?.getBoolean(Blocklist.KEY_UNKNOWN, false) ?: false) }
    var foreign by remember(prefs) { mutableStateOf(prefs?.getBoolean(Blocklist.KEY_FOREIGN, false) ?: false) }

    fun putBool(key: String, v: Boolean) {
        prefs?.edit()?.putBoolean(key, v)?.apply()
    }

    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("RLBlock") },
                subtitle = {
                    Text(
                        if (svc == null) "LSPosed 未接続"
                        else "${svc.frameworkName} ${svc.frameworkVersion} / API ${svc.apiVersion}"
                    )
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("拒否ルール") {
                SwitchRow("非通知を拒否", "番号が通知されない着信", anonymous, connected) {
                    anonymous = it; putBool(Blocklist.KEY_ANONYMOUS, it)
                }
                SwitchRow("電話帳未登録を拒否", "連絡先に登録されていない番号", unknown, connected) {
                    unknown = it; putBool(Blocklist.KEY_UNKNOWN, it)
                }
                SwitchRow("国外からの着信を拒否", "+81 以外の国番号(電話帳登録済みは除く)", foreign, connected) {
                    foreign = it; putBool(Blocklist.KEY_FOREIGN, it)
                }
            }

            Section("通知と履歴") {
                SwitchRow("ブロック時に通知", "拒否した着信を通知で知らせる", notify, true) {
                    notify = it
                    Settings.set(ctx, Settings.KEY_NOTIFY, it)
                }
                SwitchRow("履歴を記録", "拒否した着信をブロック履歴に保存", record, true) {
                    record = it
                    Settings.set(ctx, Settings.KEY_RECORD, it)
                }
            }

            NavCard(Icons.Filled.Phone, "個別の番号", "${numberCount}件", onOpenNumbers)
            NavCard(Icons.Filled.DateRange, "ブロック履歴", "${historyCount}件", onOpenHistory)
        }
    }
}

private typealias ColumnScopeContent = @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit

@Composable
private fun Section(title: String, content: ColumnScopeContent) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun NavCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}
