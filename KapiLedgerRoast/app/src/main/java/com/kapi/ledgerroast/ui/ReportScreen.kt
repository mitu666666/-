package com.kapi.ledgerroast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kapi.ledgerroast.data.AppRepository
import java.time.Instant
import java.time.ZoneId

@Composable
fun ReportScreen(repo: AppRepository) {
    val nowMs = remember { System.currentTimeMillis() }
    val report by repo.observeMonthlyReport(nowMs).collectAsStateWithLifecycle(
        initialValue = AppRepository.MonthlyReport(nowMs, nowMs, 0, emptyList())
    )
    val snackbarHostState = rememberSnackbarHostState()

    LaunchedEffect(Unit) {
        val roast = repo.buildReportRoast(System.currentTimeMillis())
        if (roast != null) {
            val msg = (roast.roastLines + listOf("建议：${roast.suggestion}")).joinToString("\n")
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val zone = ZoneId.systemDefault()
            val start = Instant.ofEpochMilli(report.monthStartMs).atZone(zone).toLocalDate()
            val title = "${start.year}年${start.monthValue}月"

            Text("本月报表（$title）", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "本月支出合计：${Money.formatCents(report.totalExpenseCents)}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (report.byCategory.isEmpty()) {
                Text("还没有支出记录，先记一笔再来让我锐评。", color = MaterialTheme.colorScheme.secondary)
            } else {
                Text("分类排行", fontWeight = FontWeight.Medium)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(report.byCategory.take(30)) { row ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(row.category, fontWeight = FontWeight.Medium)
                            Text(
                                Money.formatCents(row.amountCents),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
