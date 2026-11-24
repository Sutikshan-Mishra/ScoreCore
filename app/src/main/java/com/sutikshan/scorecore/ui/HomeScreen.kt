package com.sutikshan.scorecore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sutikshan.scorecore.data.UiMatch
import com.sutikshan.scorecore.engine.InningsState
import com.sutikshan.scorecore.engine.Phase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    matches: List<UiMatch>?,
    onNewMatch: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ios.Black),
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
        ) {
            Label("ScoreCore", style = IosType.largeTitle)
            Label("International matches", style = IosType.subhead, color = Ios.LabelSecondary)
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                matches == null -> Unit
                matches.isEmpty() -> EmptyState()
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(matches, key = { it.id }) { match ->
                        MatchCard(match) { onOpen(match.id) }
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            PrimaryButton("New Match", onNewMatch)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Label("No matches yet", style = IosType.headline)
        Spacer(Modifier.height(6.dp))
        Label(
            "Start a match and score it ball by ball. Everything stays on your phone.",
            style = IosType.subhead,
            color = Ios.LabelSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MatchCard(match: UiMatch, onClick: () -> Unit) {
    val state = match.state
    Column(
        Modifier
            .fillMaxWidth()
            .iosClickable(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(Ios.Surface)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(match.formatLabel)
            Spacer(Modifier.width(8.dp))
            Label(dateText(match.createdAt), style = IosType.footnote, color = Ios.LabelSecondary)
        }
        Spacer(Modifier.height(14.dp))
        TeamScoreRow(match.battingFirst, state.first, yetToBat = false)
        Spacer(Modifier.height(8.dp))
        TeamScoreRow(
            name = match.battingSecond,
            innings = state.second,
            yetToBat = state.second == null || state.second.balls.isEmpty(),
        )
        Spacer(Modifier.height(14.dp))
        val finished = state.phase == Phase.COMPLETE
        Label(
            text = if (finished) match.statusText else "Live · ${match.statusText}",
            style = IosType.subheadSemibold,
            color = if (finished) Ios.Blue else Ios.Orange,
            maxLines = 2,
        )
    }
}

@Composable
private fun TeamScoreRow(name: String, innings: InningsState?, yetToBat: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Label(name, Modifier.weight(1f), style = IosType.headline, maxLines = 1)
        if (yetToBat || innings == null) {
            Label("Yet to bat", style = IosType.subhead, color = Ios.LabelSecondary)
        } else {
            Label("${innings.runs}/${innings.wickets}", style = IosType.headline)
            Spacer(Modifier.width(6.dp))
            Label("(${innings.oversText})", style = IosType.subhead, color = Ios.LabelSecondary)
        }
    }
}

private fun dateText(millis: Long): String =
    SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(millis))
