package com.kapi.ledgerroast.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(tx: TransactionEntity): Long

    @Update
    suspend fun update(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions ORDER BY timestampMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE timestampMs BETWEEN :startMs AND :endMs")
    suspend fun countBetween(startMs: Long, endMs: Long): Long

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = 'EXPENSE' AND category = :category AND timestampMs BETWEEN :startMs AND :endMs")
    suspend fun sumExpenseByCategoryBetween(category: String, startMs: Long, endMs: Long): Long

    @Query("SELECT category AS category, COALESCE(SUM(amountCents), 0) AS amountCents FROM transactions WHERE type = 'EXPENSE' AND timestampMs BETWEEN :startMs AND :endMs GROUP BY category ORDER BY amountCents DESC")
    suspend fun sumExpenseGroupByCategoryBetween(startMs: Long, endMs: Long): List<CategorySumRow>
}

data class CategorySumRow(
    val category: String,
    val amountCents: Long
)

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun delete(category: String)

    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun get(category: String): BudgetEntity?
}

@Dao
interface GoalDao {
    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM goals ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<GoalEntity>>
}
