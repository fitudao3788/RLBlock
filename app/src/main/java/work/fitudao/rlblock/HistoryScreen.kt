package work.fitudao.rlblock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun reasonIcon(r: String): ImageVector = when (r) {
    "number" -> Icons.Filled.Phone
    "anonymous" -> Icons.Filled.Lock
    "foreign" -> Icons.Filled.Place
    "unknown" -> Icons.Filled.Person
    else -> Icons.Filled.Warning
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(onSelectingChange: (Boolean) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current.applicationContext
    val entries = HistoryStore.entries
    var selected by remember { mutableStateOf(setOf<HistoryEntry>()) }
    var confirmClear by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("M/d HH:mm:ss", Locale.JAPAN) }
    val selecting = selected.isNotEmpty()

    BackHandler(enabled = selecting) { selected = emptySet() }
    LaunchedEffect(selecting) { onSelectingChange(selecting) }
    DisposableEffect(Unit) { onDispose { onSelectingChange(false) } }

    Scaffold(
        topBar = {
            Crossfade(selecting, label = "bar") { sel ->
            if (sel) {
                TopAppBar(
                    title = { Text("${selected.size}件選択") },
                    navigationIcon = { IconButton({ selected = emptySet() }) { Icon(Icons.Filled.Close, "選択を解除") } },
                    actions = {
                        IconButton({ selected = entries.toSet() }) { Icon(Icons.AutoMirrored.Filled.List, "すべて選択") }
                        IconButton({
                            HistoryStore.remove(ctx, selected)
                            selected = emptySet()
                        }) { Icon(Icons.Filled.Delete, "削除") }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text("ブロック履歴") },
                    navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
                    actions = {
                        if (entries.isNotEmpty()) {
                            IconButton({ confirmClear = true }) { Icon(Icons.Filled.Delete, "履歴をすべて消去") }
                        }
                    },
                )
            }
            }
        },
    ) { pad ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        Icons.Filled.DateRange, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("履歴はありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(entries, key = { "${it.time}/${it.number}/${it.reason}" }) { e ->
                    val shape = RoundedCornerShape(20.dp)
                    val dismiss = rememberSwipeToDismissBoxState(
                        confirmValueChange = { v ->
                            if (v == SwipeToDismissBoxValue.Settled) false
                            else {
                                HistoryStore.remove(ctx, setOf(e))
                                selected = selected - e
                                true
                            }
                        },
                    )
                    SwipeToDismissBox(
                        modifier = Modifier.animateItem(),
                        state = dismiss,
                        enableDismissFromStartToEnd = !selecting,
                        enableDismissFromEndToStart = !selecting,
                        backgroundContent = {
                            val dir = dismiss.dismissDirection
                            val active = dir != SwipeToDismissBoxValue.Settled
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (active) MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape,
                                    )
                                    .padding(horizontal = 24.dp),
                                contentAlignment = if (dir == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd
                                else Alignment.CenterStart,
                            ) {
                                if (active) Icon(Icons.Filled.Delete, "削除")
                            }
                        },
                    ) {
                        val isSel = e in selected
                        Surface(
                            shape = shape,
                            color = animateColorAsState(
                                if (isSel) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                label = "row",
                            ).value,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { if (selecting) selected = if (isSel) selected - e else selected + e },
                                    onLongClick = { selected = if (isSel) selected - e else selected + e },
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .background(
                                            animateColorAsState(
                                                if (isSel) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.primaryContainer,
                                                label = "avatarBg",
                                            ).value,
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Crossfade(
                                        if (isSel) Icons.Filled.Done else reasonIcon(e.reason),
                                        label = "avatarIcon",
                                    ) {
                                        Icon(
                                            it,
                                            null,
                                            tint = if (isSel) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(e.number, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${reasonLabel(e.reason)} ・ ${fmt.format(Date(e.time))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Filled.Delete, null) },
            title = { Text("履歴をすべて消去しますか") },
            confirmButton = {
                TextButton({
                    HistoryStore.clear(ctx)
                    confirmClear = false
                }) { Text("消去") }
            },
            dismissButton = { TextButton({ confirmClear = false }) { Text("キャンセル") } },
        )
    }
}
