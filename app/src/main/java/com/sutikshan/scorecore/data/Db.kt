package com.sutikshan.scorecore.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.sutikshan.scorecore.engine.Ball
import com.sutikshan.scorecore.engine.Dismissal
import com.sutikshan.scorecore.engine.ExtraType
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamA: String,
    val teamB: String,
    val formatLabel: String,
    val maxOvers: Int,
    val tossWinner: String,
    val battingFirst: String,
    val createdAt: Long,
)

@Entity(
    tableName = "balls",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("matchId")],
)
data class BallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,
    val innings: Int,
    val seq: Int,
    val batRuns: Int,
    val extra: String,
    val extraRuns: Int,
    val dismissal: String?,
) {
    fun toBall(): Ball = Ball(
        innings = innings,
        seq = seq,
        batRuns = batRuns,
        extra = ExtraType.valueOf(extra),
        extraRuns = extraRuns,
        dismissal = dismissal?.let { Dismissal.valueOf(it) },
    )
}

data class MatchWithBalls(
    @Embedded val match: MatchEntity,
    @Relation(parentColumn = "id", entityColumn = "matchId")
    val balls: List<BallEntity>,
)

@Dao
interface MatchDao {
    @Transaction
    @Query("SELECT * FROM matches ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MatchWithBalls>>

    @Transaction
    @Query("SELECT * FROM matches WHERE id = :id")
    fun observe(id: Long): Flow<MatchWithBalls?>

    @Transaction
    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun get(id: Long): MatchWithBalls?

    @Insert
    suspend fun insertMatch(match: MatchEntity): Long

    @Insert
    suspend fun insertBall(ball: BallEntity): Long

    @Query(
        "DELETE FROM balls WHERE id = " +
            "(SELECT id FROM balls WHERE matchId = :matchId ORDER BY innings DESC, seq DESC LIMIT 1)",
    )
    suspend fun deleteLastBall(matchId: Long)

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun deleteMatch(id: Long)
}

@Database(entities = [MatchEntity::class, BallEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scorecore.db",
                ).build().also { instance = it }
            }
    }
}
