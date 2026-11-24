package com.sutikshan.scorecore

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sutikshan.scorecore.ui.HomeScreen
import com.sutikshan.scorecore.ui.MatchScreen
import com.sutikshan.scorecore.ui.NewMatchScreen
import com.sutikshan.scorecore.ui.ScoreCoreTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            ScoreCoreTheme {
                ScoreCoreApp(vm)
            }
        }
    }
}

private enum class Route { HOME, NEW, MATCH }

@Composable
private fun ScoreCoreApp(vm: AppViewModel) {
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    var matchId by rememberSaveable { mutableLongStateOf(-1L) }
    val matches by vm.matches.collectAsStateWithLifecycle()

    BackHandler(enabled = route != Route.HOME) { route = Route.HOME }

    when (route) {
        Route.HOME -> HomeScreen(
            matches = matches,
            onNewMatch = { route = Route.NEW },
            onOpen = { id ->
                matchId = id
                route = Route.MATCH
            },
        )

        Route.NEW -> NewMatchScreen(
            onBack = { route = Route.HOME },
            onStart = { teamA, teamB, format, tossWinner, electedToBat ->
                vm.createMatch(teamA, teamB, format, tossWinner, electedToBat) { id ->
                    matchId = id
                    route = Route.MATCH
                }
            },
        )

        Route.MATCH -> MatchScreen(
            vm = vm,
            matchId = matchId,
            onBack = { route = Route.HOME },
        )
    }
}
