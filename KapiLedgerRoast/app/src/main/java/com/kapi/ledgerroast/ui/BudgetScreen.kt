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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kapi.ledgerroast.data.AppRepository
import com.kapi.ledgerroast.data.BudgetEntity
import kotlinx.coroutines.launch

@Composable
fun BudgetScreen(repo: AppRepository) {
    val budgets by repo.observeBudgets().collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = rememberSnackbarHostState()
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Text("+") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("分类预算（按月）", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("提示：预算会影响记账后的“咔皮锐评”强度", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(4.dp))

            if (budgets.isEmpty()) {
                Text("还没设预算，先来一条规则管管钱吧。", color = MaterialTheme.colorScheme.secondary)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(budgets) { b ->
                        BudgetRow(
                            budget = b,
                            onDelete = {
                                scope.launch {
                                    repo.deleteBudget(b.category)
                                    snackbarHostState.showSnackbar("已删除：${b.category}")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddBudgetDialog(
            onDismiss = { showAdd = false },
            onSave = { category, amountText ->
                val cents = Money.parseToCents(amountText)
                if (cents == null || cents <= 0) {
                    scope.launch { snackbarHostState.showSnackbar("预算金额不太对哦") }
                    return@AddBudgetDialog
                }
                scope.launch {
                    repo.upsertBudget(category, cents)
                    showAdd = false
                    snackbarHostState.showSnackbar("已保存预算：$category ${Money.formatCents(cents)}")
                }
            }
        )
    }
}

@Composable
private fun BudgetRow(budget: BudgetEntity, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(budget.category, fontWeight = FontWeight.Medium)
            Text("月上限：${Money.formatCents(budget.monthlyLimitCents)}", color = MaterialTheme.colorScheme.secondary)
        }
        TextButton(onClick = onDelete) { Text("删除") }
    }
}

@Composable
private fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var category by remember { mutableStateOf(Categories.expense.first()) }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增预算") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("月上限金额") },
                    placeholder = { Text("比如 1500") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "常用分类：${Categories.expense.take(8).joinToString("、")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(category, amount) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

