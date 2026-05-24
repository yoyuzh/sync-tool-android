package com.yoyuzh.cliplink.ui.settings

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yoyuzh.cliplink.accessibility.ClipLinkAccessibilityService
import com.yoyuzh.cliplink.ui.permissions.PermissionGuideScreen

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings

    // Live-observe accessibility state
    var accessibilityEnabled by remember { mutableStateOf(isClipLinkAccessibilityEnabled(context)) }
    DisposableEffect(context) {
        val observer = object : android.database.ContentObserver(
            android.os.Handler(android.os.Looper.getMainLooper())
        ) {
            override fun onChange(selfChange: Boolean) {
                accessibilityEnabled = isClipLinkAccessibilityEnabled(context)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            observer
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    // Local edit state for text fields
    var serverUrlDraft by remember(settings.serverUrl) { mutableStateOf(settings.serverUrl) }
    var deviceNameDraft by remember(settings.deviceName) { mutableStateOf(settings.deviceName) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        // ── 服务器配置 ────────────────────────────────────────────────────────────
        SectionCard(title = "服务器配置") {
            OutlinedTextField(
                value = serverUrlDraft,
                onValueChange = { serverUrlDraft = it },
                label = { Text("服务器地址") },
                placeholder = { Text("http://192.168.x.x:8787") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = deviceNameDraft,
                onValueChange = { deviceNameDraft = it },
                label = { Text("设备名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(4.dp))
            if (settings.deviceId.isNotBlank()) {
                Text(
                    "设备 ID: ${settings.deviceId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.saveAndRegisterDevice(serverUrlDraft, deviceNameDraft)
                },
                enabled = !uiState.isRegistering,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        uiState.isRegistering -> "注册中..."
                        settings.deviceId.isBlank() -> "注册设备"
                        else -> "重新注册"
                    }
                )
            }
            uiState.registrationResult?.let { result ->
                Spacer(Modifier.height(4.dp))
                Text(
                    result,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result.startsWith("注册成功")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }

        // ── 同步设置 ──────────────────────────────────────────────────────────────
        SectionCard(title = "同步设置") {
            SettingRow(
                title = "手动发布",
                description = "关闭后不会上传本地剪贴板到服务器。",
                checked = settings.manualPublishEnabled,
                onCheckedChange = viewModel::updateManualPublishEnabled
            )
            SettingRow(
                title = "通知内容预览",
                description = "在通知中显示剪贴板文本预览（敏感内容慎开）。",
                checked = settings.notificationPreviewEnabled,
                onCheckedChange = viewModel::updateNotificationPreviewEnabled
            )
        }

        // ── 历史记录 ──────────────────────────────────────────────────────────────
        SectionCard(title = "历史记录") {
            LabelRow(
                label = "本地最大条数",
                value = "${settings.maxLocalHistoryItems} 条"
            )
            LabelRow(
                label = "同步窗口",
                value = "${settings.syncWindowDays} 天"
            )
        }

        // ── 权限状态 ──────────────────────────────────────────────────────────────
        SectionCard(title = "权限与服务") {
            PermissionGuideScreen()
            Spacer(Modifier.height(8.dp))
            SettingRow(
                title = "Accessibility 辅助服务",
                description = if (accessibilityEnabled) {
                    "✓ 已启用。路由焦点事件，不读取密码字段。"
                } else {
                    "点击开关进入系统无障碍设置启用 ClipLink。"
                },
                checked = accessibilityEnabled,
                onCheckedChange = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
            SettingRow(
                title = "SMS 任务",
                description = "接收短信后保存到本地历史；真实短信发送仍未启用。",
                checked = false,
                onCheckedChange = { }
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun LabelRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun isClipLinkAccessibilityEnabled(context: android.content.Context): Boolean {
    val expected = ComponentName(context, ClipLinkAccessibilityService::class.java).flattenToString()
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
}
