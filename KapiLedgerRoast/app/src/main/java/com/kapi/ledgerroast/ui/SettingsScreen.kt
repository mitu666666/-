package com.kapi.ledgerroast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kapi.ledgerroast.data.AppRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(repo: AppRepository) {
    val settings by repo.settingsFlow.collectAsStateWithLifecycle(initialValue = repo.settingsSnapshot())
    val snackbarHostState = rememberSnackbarHostState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            SettingRow(
                title = "开启咔皮锐评",
                subtitle = "记完账就来一句，可爱毒舌但不越界",
                checked = settings.roastEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { repo.setRoastEnabled(enabled) }
                }
            )

            SettingRow(
                title = "打破第四面墙",
                subtitle = "偶尔像NPC一样跟你互动",
                checked = settings.fourthWall,
                onCheckedChange = { enabled ->
                    scope.launch { repo.setFourthWall(enabled) }
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("毒舌强度：L${settings.intensity}", fontWeight = FontWeight.Medium)
                Text("L1轻戳｜L2标准｜L3软刀子", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = settings.intensity.toFloat(),
                    onValueChange = { v ->
                        scope.launch { repo.setIntensity(v.roundToInt().coerceIn(1, 3)) }
                    },
                    valueRange = 1f..3f,
                    steps = 1
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("提醒", fontWeight = FontWeight.Medium)
            SettingRow(
                title = "每日记账提醒",
                subtitle = "当天没记账就来敲门（通知）",
                checked = settings.reminderEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { repo.setReminderEnabled(enabled) }
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("提醒时间：${settings.dailyReminderHour}:00", color = MaterialTheme.colorScheme.secondary)
                Slider(
                    value = settings.dailyReminderHour.toFloat(),
                    onValueChange = { v ->
                        scope.launch { repo.setDailyReminderHour(v.roundToInt().coerceIn(0, 23)) }
                    },
                    valueRange = 0f..23f,
                    steps = 22
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("AI 提示词模板（可选）", fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = AiPromptTemplates.zhCuteToxic,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(AiPromptTemplates.zhCuteToxic))
                    scope.launch { snackbarHostState.showSnackbar("已复制提示词") }
                }) {
                    Text("复制")
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

