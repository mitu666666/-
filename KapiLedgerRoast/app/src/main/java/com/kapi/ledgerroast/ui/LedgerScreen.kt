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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kapi.ledgerroast.data.TransactionEntity
import com.kapi.ledgerroast.data.TxType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LedgerScreen(repo: AppRepository) {
    val items by repo.observeRecentTransactions().collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = rememberSnackbarHostState()
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("最近记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items) { tx ->
                    TxRow(tx)
                }
            }
        }
    }

    if (showAdd) {
        AddTxDialog(
            onDismiss = { showAdd = false },
            onSave = { type, category, amountText, note ->
                val cents = Money.parseToCents(amountText)
                if (cents == null || cents <= 0) {
                    scope.launch { snackbarHostState.showSnackbar("金额不太对哦，再检查一下") }
                    return@AddTxDialog
                }
                scope.launch {
                    val roast = repo.addTransaction(
                        type = type,
                        amountCents = cents,
                        category = category,
                        note = note,
                        timestampMs = System.currentTimeMillis()
                    )
                    showAdd = false
                    if (roast != null) {
                        val msg = (roast.roastLines + listOf("建议：${roast.suggestion}")).joinToString("\n")
                        snackbarHostState.showSnackbar(msg)
                    } else {
                        snackbarHostState.showSnackbar("已保存")
                    }
                }
            }
        )
    }
}

@Composable
private fun TxRow(tx: TransactionEntity) {
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(tx.timestampMs).atZone(zone)
    val fmt = remember { DateTimeFormatter.ofPattern("MM-dd HH:mm") }
    val sign = if (tx.type == TxType.EXPENSE) "-" else "+"
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${tx.category}  ${sign}${Money.formatCents(tx.amountCents)}", fontWeight = FontWeight.Medium)
            Text(time.format(fmt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        if (!tx.note.isNullOrBlank()) {
            Text(tx.note ?: "", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTxDialog(
    onDismiss: () -> Unit,
    onSave: (TxType, String, String, String) -> Unit
) {
    var type by remember { mutableStateOf(TxType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Categories.expense.first()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增一笔") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            type = TxType.EXPENSE
                            category = Categories.expense.first()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("支出") }
                    Button(
                        onClick = {
                            type = TxType.INCOME
                            category = Categories.income.first()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("收入") }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额") },
                    placeholder = { Text("比如 12.5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类") },
                    placeholder = { Text("比如 餐饮") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    if (type == TxType.EXPENSE) "常用分类：${Categories.expense.take(6).joinToString("、")}"
                    else "常用来源：${Categories.income.take(6).joinToString("、")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(type, category, amount, note) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
