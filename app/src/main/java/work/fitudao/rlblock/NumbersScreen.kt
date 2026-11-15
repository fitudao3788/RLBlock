package work.fitudao.rlblock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun NumbersScreen(
    items: SnapshotStateList<NumItem>,
    editable: Boolean,
    persist: () -> Unit,
    onSelectingChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var showAdd by remember { mutableStateOf(false) }
    val selecting = selected.isNotEmpty()

    BackHandler(enabled = selecting) { selected = emptySet() }
    LaunchedEffect(selecting) { onSelectingChange(selecting) }
    DisposableEffect(Unit) { onDispose { onSelectingChange(false) } }

    fun toggle(number: String) {
        val i = items.indexOfFirst { it.number == number }
        if (i >= 0) items[i] = items[i].copy(enabled = !items[i].enabled)
        persist()
    }

    fun delete(number: String) {
        items.removeAll { it.number == number }
        selected = selected - number
        persist()
    }

    Scaffold(
        topBar = {
            Crossfade(selecting, label = "bar") { sel ->
            if (sel) {
                TopAppBar(
                    title = { Text("${selected.size}件選択") },
                    navigationIcon = {
                        IconButton({ selected = emptySet() }) { Icon(Icons.Filled.Close, "選択を解除") }
                    },
                    actions = {
                        IconButton({ selected = items.map { it.number }.toSet() }) {
                            Icon(Icons.AutoMirrored.Filled.List, "すべて選択")
                        }
                        val enableNext = items.filter { it.number in selected }.any { !it.enabled }
                        IconButton({
                            val targets = items.filter { it.number in selected }
                            targets.forEach { t ->
                                val i = items.indexOfFirst { it.number == t.number }
                                if (i >= 0) items[i] = items[i].copy(enabled = enableNext)
                            }
                            persist()
                        }) {
                            Icon(
                                if (enableNext) Icons.Filled.CheckCircle else Icons.Filled.Clear,
                                if (enableNext) "有効にする" else "無効にする",
                            )
                        }
                        IconButton({
                            items.removeAll { it.number in selected }
                            selected = emptySet()
                            persist()
                        }) { Icon(Icons.Filled.Delete, "削除") }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text("個別の番号") },
                    navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
                )
            }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !selecting && editable,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("番号を追加")
                }
            }
        },
    ) { pad ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        Icons.Filled.Phone, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("拒否する番号はありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.number }) { item ->
                    val number = item.number
                    val dismiss = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismiss.currentValue) {
                        when (dismiss.currentValue) {
                            SwipeToDismissBoxValue.StartToEnd -> { toggle(number); dismiss.reset() }
                            SwipeToDismissBoxValue.EndToStart -> delete(number)
                            SwipeToDismissBoxValue.Settled -> {}
                        }
                    }
                    val shape = RoundedCornerShape(20.dp)
                    SwipeToDismissBox(
                        modifier = Modifier.animateItem(),
                        state = dismiss,
                        enableDismissFromStartToEnd = !selecting && editable,
                        enableDismissFromEndToStart = !selecting && editable,
                        backgroundContent = {
                            val dir = dismiss.dismissDirection
                            val toStart = dir == SwipeToDismissBoxValue.StartToEnd
                            val toEnd = dir == SwipeToDismissBoxValue.EndToStart
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        when {
                                            toEnd -> MaterialTheme.colorScheme.errorContainer
                                            toStart -> MaterialTheme.colorScheme.tertiaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                                        },
                                        shape,
                                    )
                                    .padding(horizontal = 24.dp),
                                contentAlignment = if (toEnd) Alignment.CenterEnd else Alignment.CenterStart,
                            ) {
                                if (toStart) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(if (item.enabled) Icons.Filled.Clear else Icons.Filled.CheckCircle, null)
                                        Text(if (item.enabled) "無効化" else "有効化")
                                    }
                                }
                                if (toEnd) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("削除")
                                        Icon(Icons.Filled.Delete, null)
                                    }
                                }
                            }
                        },
                    ) {
                        val isSel = number in selected
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
                                    onClick = { if (selecting) selected = if (isSel) selected - number else selected + number },
                                    onLongClick = { selected = if (isSel) selected - number else selected + number },
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Avatar(
                                    icon = if (isSel) Icons.Filled.Done else Icons.Filled.Phone,
                                    active = isSel || item.enabled,
                                    selected = isSel,
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        number,
                                        style = MaterialTheme.typography.titleMedium,
                                        textDecoration = if (item.enabled) null else TextDecoration.LineThrough,
                                        color = if (item.enabled) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        if (item.enabled) "拒否中" else "無効",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (!selecting) {
                                    Switch(checked = item.enabled, onCheckedChange = { toggle(number) }, enabled = editable)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var input by remember { mutableStateOf("") }
        val normalized = Blocklist.normalize(input)
        val valid = Blocklist.hasDigits(input) && items.none { it.number == normalized }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            icon = { Icon(Icons.Filled.Add, null) },
            title = { Text("番号を追加") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    label = { Text("電話番号") },
                    placeholder = { Text("09012345678") },
                    supportingText = {
                        if (input.isNotEmpty() && !valid) Text(if (Blocklist.hasDigits(input)) "登録済みです" else "数字を入力してください")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        items.add(0, NumItem(normalized))
                        persist()
                        showAdd = false
                    },
                    enabled = valid,
                ) { Text("追加") }
            },
            dismissButton = { TextButton({ showAdd = false }) { Text("キャンセル") } },
        )
    }
}

@Composable
private fun Avatar(icon: ImageVector, active: Boolean, selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        when {
            selected -> scheme.primary
            active -> scheme.primaryContainer
            else -> scheme.surfaceVariant
        },
        label = "avatarBg",
    )
    val fg by animateColorAsState(
        when {
            selected -> scheme.onPrimary
            active -> scheme.onPrimaryContainer
            else -> scheme.onSurfaceVariant
        },
        label = "avatarFg",
    )
    Box(Modifier.size(40.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
        Crossfade(icon, label = "avatarIcon") { Icon(it, null, tint = fg) }
    }
}
