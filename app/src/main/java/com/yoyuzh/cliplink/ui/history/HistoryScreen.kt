package com.yoyuzh.cliplink.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yoyuzh.cliplink.data.remote.ws.SessionState
import com.yoyuzh.cliplink.domain.model.ClipboardKind
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.model.PublishState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DAY_RANGES = listOf(1, 3, 7, 15)

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Show snackbar for capture result
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.captureResult) {
        when (val result = state.captureResult) {
            is CaptureResult.Success -> {
                snackbarMessage = "✓ 已捕获到本地"
                delay(2000)
                snackbarMessage = null
                viewModel.dismissCaptureResult()
            }
            is CaptureResult.Failure -> {
                snackbarMessage = "✗ ${result.message}"
                delay(2500)
                snackbarMessage = null
                viewModel.dismissCaptureResult()
            }
            null -> {}
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // Connection header
                ConnectionHeader(
                    sessionState = state.sessionState,
                    onReconnect = viewModel::reconnect
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("搜索标题、预览、来源设备...") },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Filter chips — 6 tabs
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HistoryFilter.entries) { filter ->
                        FilterChip(
                            selected = state.selectedFilter == filter,
                            onClick = { viewModel.selectFilter(filter) },
                            label = { Text(filter.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Time range selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DAY_RANGES.forEach { days ->
                        val selected = state.selectedDayRange == days
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { viewModel.selectDayRange(days) }
                        ) {
                            Text(
                                text = "${days}天",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (state.records.isEmpty()) {
                item { EmptyHistory() }
            } else {
                items(state.records, key = { it.id }) { record ->
                    ClipboardRecordCard(
                        record = record,
                        onPublish = { viewModel.publish(record.id) },
                        onCopy = { copyToClipboard(context, record) }
                    )
                }
            }
        }

        // FAB — capture current clipboard
        ExtendedFloatingActionButton(
            onClick = viewModel::capture,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Text("↑ 捕获剪贴板")
        }

        // Snackbar
        AnimatedVisibility(
            visible = snackbarMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .padding(bottom = 72.dp)
        ) {
            snackbarMessage?.let {
                Snackbar { Text(it) }
            }
        }
    }
}

@Composable
private fun ConnectionHeader(sessionState: SessionState, onReconnect: () -> Unit) {
    val (dotColor, label) = when (sessionState) {
        is SessionState.Online -> MaterialTheme.colorScheme.primary to
            "已连接 · ${sessionState.onlineDeviceCount} 台设备在线"
        is SessionState.Connecting -> Color(0xFFFF9800) to "连接中..."
        is SessionState.Error -> MaterialTheme.colorScheme.error to "连接失败"
        is SessionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant to "未连接"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Connection status logo block
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("↗", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Column {
                    Text(
                        "ClipLink",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Status dot
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (sessionState is SessionState.Disconnected || sessionState is SessionState.Error) {
                TextButton(onClick = onReconnect) {
                    Text("重连", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📋", style = MaterialTheme.typography.headlineLarge)
            Text("暂无记录", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Text(
                "点击下方按钮捕获当前剪贴板内容，\n或从服务器同步历史记录。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ClipboardRecordCard(
    record: ClipboardRecord,
    onPublish: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row: kind + device + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${record.kind.label()} · ${record.sourceDeviceId}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (record.title.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            record.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(record.publishState)
            }

            // Content preview
            val preview = record.textPreview ?: record.textContent ?: record.title
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Action row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 复制 button — always shown
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("复制")
                }

                // 发布 button — only for LOCAL_ONLY and FAILED
                if (record.publishState == PublishState.LOCAL_ONLY ||
                    record.publishState == PublishState.FAILED
                ) {
                    Button(
                        onClick = onPublish,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("发布")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(state: PublishState) {
    val (text, bgColor) = when (state) {
        PublishState.LOCAL_ONLY -> "仅本地" to MaterialTheme.colorScheme.surfaceVariant
        PublishState.PENDING -> "待同步" to Color(0xFFFFF3CD)
        PublishState.PUBLISHED -> "已同步" to Color(0xFFD4EDDA)
        PublishState.FAILED -> "失败" to Color(0xFFF8D7DA)
    }
    Surface(color = bgColor, shape = RoundedCornerShape(6.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun ClipboardKind.label() = when (this) {
    ClipboardKind.TEXT -> "文本"
    ClipboardKind.IMAGE -> "图片"
    ClipboardKind.DOCUMENT -> "文件"
}

private fun copyToClipboard(context: Context, record: ClipboardRecord) {
    val text = record.textContent ?: record.textPreview ?: record.title
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("ClipLink", text))
    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
}
