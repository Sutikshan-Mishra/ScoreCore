package com.sutikshan.scorecore.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sutikshan.scorecore.data.MatchFormat
import com.sutikshan.scorecore.data.Team
import com.sutikshan.scorecore.data.Teams

@Composable
fun NewMatchScreen(
    onBack: () -> Unit,
    onStart: (teamA: String, teamB: String, format: MatchFormat, tossWinner: String, electedToBat: Boolean) -> Unit,
) {
    var teamA by rememberSaveable { mutableStateOf<String?>(null) }
    var teamB by rememberSaveable { mutableStateOf<String?>(null) }
    var formatIndex by rememberSaveable { mutableIntStateOf(0) }
    var tossIndex by rememberSaveable { mutableIntStateOf(0) }
    var electedIndex by rememberSaveable { mutableIntStateOf(0) }
    /** 0 = closed, 1 = picking team 1, 2 = picking team 2. */
    var picking by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = picking != 0) { picking = 0 }

    val a = teamA
    val b = teamB
    val format = MatchFormat.entries[formatIndex]
    val ready = a != null && b != null && a != b

    Box(
        Modifier
            .fillMaxSize()
            .background(Ios.Black),
    ) {
        Column(Modifier.fillMaxSize()) {
            NavBar(title = "New Match", backLabel = "Matches", onBack = onBack)

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                SectionHeader("Teams")
                GroupCard {
                    ValueRow(
                        title = "Team 1",
                        value = a ?: "Select",
                        valueColor = if (a == null) Ios.LabelSecondary else Ios.Label,
                        onClick = { picking = 1 },
                    )
                    RowDivider()
                    ValueRow(
                        title = "Team 2",
                        value = b ?: "Select",
                        valueColor = if (b == null) Ios.LabelSecondary else Ios.Label,
                        onClick = { picking = 2 },
                    )
                }

                SectionHeader("Format")
                SegmentedControl(
                    options = MatchFormat.entries.map { it.label },
                    selected = formatIndex,
                    onSelect = { formatIndex = it },
                )
                Label(
                    text = "${format.overs} overs per side",
                    style = IosType.footnote,
                    color = Ios.LabelSecondary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                )

                if (a != null && b != null) {
                    SectionHeader("Toss won by")
                    SegmentedControl(
                        options = listOf(a, b),
                        selected = tossIndex,
                        onSelect = { tossIndex = it },
                    )
                    SectionHeader("Elected to")
                    SegmentedControl(
                        options = listOf("Bat", "Bowl"),
                        selected = electedIndex,
                        onSelect = { electedIndex = it },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                if (a != null && b != null && ready) {
                    val tossWinner = if (tossIndex == 0) a else b
                    val other = if (tossIndex == 0) b else a
                    val battingFirst = if (electedIndex == 0) tossWinner else other
                    Label(
                        text = "$battingFirst bat first",
                        style = IosType.footnote,
                        color = Ios.LabelSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                    )
                }
                PrimaryButton(
                    text = "Start Match",
                    enabled = ready,
                    onClick = {
                        if (a != null && b != null) {
                            val tossWinner = if (tossIndex == 0) a else b
                            onStart(a, b, format, tossWinner, electedIndex == 0)
                        }
                    },
                )
            }
        }

        if (picking != 0) {
            TeamPicker(
                title = if (picking == 1) "Team 1" else "Team 2",
                selected = if (picking == 1) a else b,
                unavailable = if (picking == 1) b else a,
                onPick = { name ->
                    if (picking == 1) teamA = name else teamB = name
                    picking = 0
                },
                onClose = { picking = 0 },
            )
        }
    }
}

@Composable
private fun TeamPicker(
    title: String,
    selected: String?,
    unavailable: String?,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ios.Black),
    ) {
        NavBar(
            title = title,
            backLabel = null,
            onBack = onClose,
            trailing = {
                Label(
                    text = "Cancel",
                    color = Ios.Blue,
                    modifier = Modifier
                        .iosClickable(onClick = onClose)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            },
        )
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            item { SectionHeader("Full members") }
            item { TeamGroup(Teams.fullMembers, selected, unavailable, onPick) }
            item { SectionHeader("Associate members") }
            item { TeamGroup(Teams.associates, selected, unavailable, onPick) }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun TeamGroup(
    teams: List<Team>,
    selected: String?,
    unavailable: String?,
    onPick: (String) -> Unit,
) {
    GroupCard {
        teams.forEachIndexed { index, team ->
            val blocked = team.name == unavailable
            val base = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
            Row(
                (if (blocked) base else base.iosClickable { onPick(team.name) })
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Label(
                    text = team.name,
                    modifier = Modifier.weight(1f),
                    color = if (blocked) Ios.LabelTertiary else Ios.Label,
                    maxLines = 1,
                )
                Label(team.code, style = IosType.footnote, color = Ios.LabelSecondary)
                if (team.name == selected) {
                    Spacer(Modifier.width(10.dp))
                    Label("✓", style = IosType.headline, color = Ios.Blue)
                }
            }
            if (index < teams.lastIndex) RowDivider()
        }
    }
}
