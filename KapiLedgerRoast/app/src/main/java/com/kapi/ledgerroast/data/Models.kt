package com.kapi.ledgerroast.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TxType {
    EXPENSE,
    INCOME
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amountCents: Long,
    val category: String,
    val note: String?,
    val timestampMs: Long
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String,
    val monthlyLimitCents: Long
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetCents: Long,
    val currentCents: Long,
    val createdAtMs: Long
)
