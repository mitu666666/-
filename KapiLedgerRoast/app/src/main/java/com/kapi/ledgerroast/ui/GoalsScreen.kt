package com.kapi.ledgerroast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kapi.ledgerroast.data.AppRepository
import com.kapi.ledgerroast.data.GoalEntity
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun GoalsScreen(repo: AppRepository) {
    val goals by repo.observeGoals().collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = rememberSnackbarHostState()
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) { Text("+") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("存钱目标", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (goals.isEmpty()) {
                Text("先立个小目标，咔皮才有地方夸你。", color = MaterialTheme.colorScheme.secondary)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(goals) { g ->
                        GoalCard(
                            goal = g,
                            onAdd = { deltaText ->
                                val delta = Money.parseToCents(deltaText)
                                if (delta == null || delta <= 0) {
                                    scope.launch { snackbarHostState.showSnackbar("加多少？先写个正数呀") }
                                    return@GoalCard
                                }
                                scope.launch {
                                    val before = g
                                    val after = max(0, before.currentCents + delta)
                                    repo.addGoalProgress(before, delta)
                                    val roast = repo.buildGoalRoast(before, after.toLong())
                                    if (roast != null) {
                                        val msg = (roast.roastLines + listOf("建议：${roast.suggestion}")).joinToString("\n")
                                        snackbarHostState.showSnackbar(msg)
                                    } else {
                                        snackbarHostState.showSnackbar("已加进度 +${Money.formatCents(delta)}")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateGoalDialog(
            onDismiss = { showCreate = false },
            onSave = { name, targetText ->
                val target = Money.parseToCents(targetText)
                if (name.isBlank()) {
                    scope.launch { snackbarHostState.showSnackbar("目标名字不能为空") }
                    return@CreateGoalDialog
                }
                if (target == null || target <= 0) {
                    scope.launch { snackbarHostState.showSnackbar("目标金额不太对哦") }
                    return@CreateGoalDialog
                }
                scope.launch {
                    repo.createGoal(name.trim(), target)
                    showCreate = false
                    snackbarHostState.showSnackbar("目标创建成功：$name")
                }
            }
        )
    }
}

@Composable
private fun GoalCard(goal: GoalEntity, onAdd: (String) -> Unit) {
    val target = goal.targetCents.coerceAtLeast(1)
    val ratio = (goal.currentCents.toFloat() / target.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(goal.name, fontWeight = FontWeight.SemiBold)
        Text(
            "进度：${Money.formatCents(goal.currentCents)} / ${Money.formatCents(goal.targetCents)}",
            color = MaterialTheme.colorScheme.secondary
        )
        LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { onAdd("10") }, modifier = Modifier.weight(1f)) { Text("+10") }
            Button(onClick = { onAdd("50") }, modifier = Modifier.weight(1f)) { Text("+50") }
            Button(onClick = { onAdd("100") }, modifier = Modifier.weight(1f)) { Text("+100") }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建目标") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("目标名称") },
                    placeholder = { Text("比如 应急金 / 旅行基金") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("目标金额") },
                    placeholder = { Text("比如 3000") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, target) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

