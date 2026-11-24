package com.sutikshan.scorecore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sutikshan.scorecore.data.AppDatabase
import com.sutikshan.scorecore.data.MatchFormat
import com.sutikshan.scorecore.data.MatchRepository
import com.sutikshan.scorecore.data.UiMatch
import com.sutikshan.scorecore.engine.BallInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MatchRepository(AppDatabase.get(app))

    /** null while the first query is still loading. */
    val matches: StateFlow<List<UiMatch>?> =
        repo.observeMatches().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun observeMatch(id: Long): Flow<UiMatch?> = repo.observeMatch(id)

    fun createMatch(
        teamA: String,
        teamB: String,
        format: MatchFormat,
        tossWinner: String,
        electedToBat: Boolean,
        onCreated: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val id = repo.createMatch(teamA, teamB, format, tossWinner, electedToBat)
            onCreated(id)
        }
    }

    fun addBall(matchId: Long, input: BallInput) {
        viewModelScope.launch { repo.addBall(matchId, input) }
    }

    fun undo(matchId: Long) {
        viewModelScope.launch { repo.undoLastBall(matchId) }
    }

    fun deleteMatch(matchId: Long) {
        viewModelScope.launch { repo.deleteMatch(matchId) }
    }
}
