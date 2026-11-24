package com.sutikshan.scorecore.data

import androidx.room.withTransaction
import com.sutikshan.scorecore.engine.BallInput
import com.sutikshan.scorecore.engine.MatchRules
import com.sutikshan.scorecore.engine.MatchState
import com.sutikshan.scorecore.engine.Phase
import com.sutikshan.scorecore.engine.allowedDismissals
import com.sutikshan.scorecore.engine.computeMatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

/** A match as the UI sees it: the stored header plus everything derived from its ball events. */
data class UiMatch(
    val id: Long,
    val teamA: String,
    val teamB: String,
    val formatLabel: String,
    val maxOvers: Int,
    val tossWinner: String,
    val battingFirst: String,
    val battingSecond: String,
    val createdAt: Long,
    val state: MatchState,
) {
    val electedTo: String get() = if (tossWinner == battingFirst) "bat" else "bowl"

    val resultText: String? get() = state.resultText(battingFirst, battingSecond)

    /** One-line status for the match list and the scoreboard. */
    val statusText: String
        get() {
            resultText?.let { return it }
            return when (state.phase) {
                Phase.FIRST_INNINGS -> "$battingFirst batting"
                Phase.INNINGS_BREAK -> "Innings break · target ${state.target}"
                Phase.SECOND_INNINGS -> {
                    val s = state.second
                    val balls = s?.ballsRemaining ?: 0
                    "$battingSecond need ${state.runsNeeded} off $balls ${if (balls == 1) "ball" else "balls"}"
                }
                Phase.COMPLETE -> ""
            }
        }
}

class MatchRepository(private val db: AppDatabase) {
    private val dao = db.matchDao()

    fun observeMatches(): Flow<List<UiMatch>> = dao.observeAll().map { list -> list.map { it.toUi() } }

    fun observeMatch(id: Long): Flow<UiMatch?> = dao.observe(id).map { it?.toUi() }

    suspend fun createMatch(
        teamA: String,
        teamB: String,
        format: MatchFormat,
        tossWinner: String,
        electedToBat: Boolean,
    ): Long {
        val other = if (tossWinner == teamA) teamB else teamA
        val battingFirst = if (electedToBat) tossWinner else other
        return dao.insertMatch(
            MatchEntity(
                teamA = teamA,
                teamB = teamB,
                formatLabel = format.label,
                maxOvers = format.overs,
                tossWinner = tossWinner,
                battingFirst = battingFirst,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Appends one delivery. The match is re-derived inside the transaction so a rapid double tap
     * can never push an innings past its end.
     */
    suspend fun addBall(matchId: Long, input: BallInput) {
        db.withTransaction<Unit> {
            val stored = dao.get(matchId) ?: return@withTransaction
            val state = computeMatch(MatchRules(stored.match.maxOvers), stored.balls.map { it.toBall() })
            if (!state.canScore) return@withTransaction

            val current = state.current
            val dismissal = input.dismissal
            if (dismissal != null && dismissal !in allowedDismissals(input.extra, current.freeHitNext)) {
                return@withTransaction
            }

            val ball = input.toBall(state.currentInningsNo, current.balls.size)
            dao.insertBall(
                BallEntity(
                    matchId = matchId,
                    innings = ball.innings,
                    seq = ball.seq,
                    batRuns = ball.batRuns,
                    extra = ball.extra.name,
                    extraRuns = ball.extraRuns,
                    dismissal = ball.dismissal?.name,
                ),
            )
        }
    }

    suspend fun undoLastBall(matchId: Long) = dao.deleteLastBall(matchId)

    suspend fun deleteMatch(id: Long) = dao.deleteMatch(id)
}

private fun MatchWithBalls.toUi(): UiMatch {
    val state = computeMatch(MatchRules(match.maxOvers), balls.map { it.toBall() })
    val battingSecond = if (match.battingFirst == match.teamA) match.teamB else match.teamA
    return UiMatch(
        id = match.id,
        teamA = match.teamA,
        teamB = match.teamB,
        formatLabel = match.formatLabel,
        maxOvers = match.maxOvers,
        tossWinner = match.tossWinner,
        battingFirst = match.battingFirst,
        battingSecond = battingSecond,
        createdAt = match.createdAt,
        state = state,
    )
}

/** Two-decimal formatting that ignores the device locale (no decimal commas in a score). */
fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)
