package com.sutikshan.scorecore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sutikshan.scorecore.AppViewModel
import com.sutikshan.scorecore.data.Teams
import com.sutikshan.scorecore.data.UiMatch
import com.sutikshan.scorecore.data.fmt2
import com.sutikshan.scorecore.engine.BallInput
import com.sutikshan.scorecore.engine.BallView
import com.sutikshan.scorecore.engine.Dismissal
import com.sutikshan.scorecore.engine.ExtraType
import com.sutikshan.scorecore.engine.InningsState
import com.sutikshan.scorecore.engine.OverSummary
import com.sutikshan.scorecore.engine.Phase
import com.sutikshan.scorecore.engine.allowedDismissals
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MatchScreen(vm: AppViewModel, matchId: Long, onBack: () -> Unit) {
    val flow = remember(matchId) { vm.observeMatch(matchId) }
    val match by flow.collectAsStateWithLifecycle(initialValue = null)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    val m = match

    Column(
        Modifier
            .fillMaxSize()
            .background(Ios.Black),
    ) {
        NavBar(
            title = if (m != null) "${Teams.code(m.teamA)} v ${Teams.code(m.teamB)}" else "",
            backLabel = "Matches",
            onBack = onBack,
            trailing = {
                if (m != null) {
                    Label(
                        text = "Delete",
                        color = Ios.Red,
                        modifier = Modifier
                            .iosClickable { confirmDelete = true }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            },
        )
        if (m != null) {
            SegmentedControl(
                options = listOf("Score", "Balls", "Summary"),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when (tab) {
                0 -> ScoreTab(vm, m, Modifier.weight(1f))
                1 -> BallsTab(m, Modifier.weight(1f))
                else -> SummaryTab(m, Modifier.weight(1f))
            }
        }
    }

    if (confirmDelete && m != null) {
        IosDialog(title = "Delete this match?", onDismiss = { confirmDelete = false }) {
            Label(
                text = "The score and every ball will be removed.",
                style = IosType.subhead,
                color = Ios.LabelSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Cancel", { confirmDelete = false }, Modifier.weight(1f), Ios.LabelSecondary)
                SecondaryButton(
                    text = "Delete",
                    onClick = {
                        confirmDelete = false
                        vm.deleteMatch(m.id)
                        onBack()
                    },
                    modifier = Modifier.weight(1f),
                    color = Ios.Red,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Score tab

@Composable
private fun ScoreTab(vm: AppViewModel, m: UiMatch, modifier: Modifier = Modifier) {
    val s = m.state
    val inn = s.current
    val haptic = LocalHapticFeedback.current
    var extra by remember { mutableStateOf(ExtraType.NONE) }
    var showOut by remember { mutableStateOf(false) }
    var chaseStarted by remember(m.id) { mutableStateOf(false) }

    // If the last ball of the first innings is undone, the break card should come back next time.
    LaunchedEffect(s.phase) {
        if (s.phase == Phase.FIRST_INNINGS) chaseStarted = false
    }

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Scoreboard(m)
        Spacer(Modifier.height(12.dp))

        when {
            s.phase == Phase.COMPLETE -> {
                ResultCard(m)
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Undo last ball", { vm.undo(m.id) })
            }

            s.phase == Phase.INNINGS_BREAK && !chaseStarted -> {
                BreakCard(
                    m = m,
                    onStart = { chaseStarted = true },
                    onUndo = { vm.undo(m.id) },
                )
            }

            else -> {
                ThisOver(inn)
                Spacer(Modifier.height(16.dp))
                Keypad(
                    extra = extra,
                    onExtra = { extra = it },
                    canUndo = inn.balls.isNotEmpty() || s.phase != Phase.FIRST_INNINGS,
                    onRun = { runs ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vm.addBall(m.id, BallInput(extra, runs))
                        extra = ExtraType.NONE
                    },
                    onOut = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showOut = true
                    },
                    onUndo = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        extra = ExtraType.NONE
                        vm.undo(m.id)
                    },
                )
            }
        }
        Spacer(
            Modifier
                .navigationBarsPadding()
                .height(24.dp),
        )
    }

    if (showOut) {
        DismissalDialog(
            extra = extra,
            freeHit = inn.freeHitNext,
            onDismiss = { showOut = false },
            onConfirm = { dismissal, runs ->
                vm.addBall(m.id, BallInput(extra, runs, dismissal))
                extra = ExtraType.NONE
                showOut = false
            },
        )
    }
}

@Composable
private fun Scoreboard(m: UiMatch) {
    val s = m.state
    val inn = s.current
    val chasing = s.phase != Phase.FIRST_INNINGS
    val batting = if (chasing) m.battingSecond else m.battingFirst

    GroupCard {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(batting, Modifier.weight(1f), IosType.headline, maxLines = 1)
                Label(
                    text = (if (chasing) "2nd innings" else "1st innings") + " · " + m.formatLabel,
                    style = IosType.footnote,
                    color = Ios.LabelSecondary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Label("${inn.runs}/${inn.wickets}", style = IosType.score)
                Spacer(Modifier.width(10.dp))
                Label(
                    text = "(${inn.oversText} ov)",
                    style = IosType.title3,
                    color = Ios.LabelSecondary,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat("CRR", fmt2(inn.runRate))
                if (chasing) {
                    Stat("Target", s.target.toString())
                    val rrr = s.requiredRate
                    if (rrr != null) Stat("RRR", fmt2(rrr))
                }
            }
            if (chasing && s.phase != Phase.COMPLETE) {
                Spacer(Modifier.height(12.dp))
                Label(m.statusText, style = IosType.subheadSemibold, color = Ios.Blue)
            }
            if (inn.freeHitNext && s.canScore) {
                Spacer(Modifier.height(12.dp))
                Pill("FREE HIT", Ios.Orange.copy(alpha = 0.2f), Ios.Orange)
            }
        }
    }
}

@Composable
private fun Stat(title: String, value: String) {
    Column {
        Label(title, style = IosType.footnote, color = Ios.LabelSecondary)
        Label(value, style = IosType.headline)
    }
}

@Composable
private fun ThisOver(inn: InningsState) {
    Column {
        Label(
            text = "THIS OVER",
            style = IosType.footnote,
            color = Ios.LabelSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (inn.thisOver.isEmpty()) {
                Label("No balls yet", style = IosType.subhead, color = Ios.LabelTertiary)
            } else {
                inn.thisOver.forEach { BallChip(it) }
            }
        }
    }
}

@Composable
private fun BreakCard(m: UiMatch, onStart: () -> Unit, onUndo: () -> Unit) {
    val first = m.state.first
    GroupCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label("Innings break", style = IosType.headline)
            Label(
                text = "${m.battingFirst} ${first.runs}/${first.wickets} (${first.oversText} ov)",
                style = IosType.subhead,
            )
            Label(
                text = "${m.battingSecond} need ${m.state.target} to win from ${m.maxOvers} overs.",
                style = IosType.subhead,
                color = Ios.LabelSecondary,
            )
            PrimaryButton("Start 2nd innings", onStart)
            SecondaryButton("Undo last ball", onUndo)
        }
    }
}

@Composable
private fun ResultCard(m: UiMatch) {
    GroupCard {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Label("RESULT", style = IosType.footnote, color = Ios.LabelSecondary)
            Label(
                text = m.statusText,
                style = IosType.title3.copy(fontWeight = FontWeight.SemiBold),
                color = Ios.Blue,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Keypad(
    extra: ExtraType,
    onExtra: (ExtraType) -> Unit,
    canUndo: Boolean,
    onRun: (Int) -> Unit,
    onOut: () -> Unit,
    onUndo: () -> Unit,
) {
    val hint = when (extra) {
        ExtraType.NONE -> "Tap the runs scored off the bat"
        ExtraType.WIDE -> "Wide: tap extra runs run (0 = just the wide)"
        ExtraType.NO_BALL -> "No ball: tap runs off the bat (+1 for the no ball)"
        ExtraType.BYE -> "Byes: tap how many were run"
        ExtraType.LEG_BYE -> "Leg byes: tap how many were run"
    }
    val noZero = extra == ExtraType.BYE || extra == ExtraType.LEG_BYE

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExtraToggle("Wide", ExtraType.WIDE, extra, onExtra, Modifier.weight(1f))
            ExtraToggle("No ball", ExtraType.NO_BALL, extra, onExtra, Modifier.weight(1f))
            ExtraToggle("Bye", ExtraType.BYE, extra, onExtra, Modifier.weight(1f))
            ExtraToggle("Leg bye", ExtraType.LEG_BYE, extra, onExtra, Modifier.weight(1f))
        }
        Label(
            text = hint,
            style = IosType.footnote,
            color = Ios.LabelSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(0, 1, 2, 3).forEach { n ->
                KeyButton(
                    label = n.toString(),
                    modifier = Modifier.weight(1f),
                    background = Ios.SurfaceHigh,
                    foreground = Ios.Label,
                    enabled = !(n == 0 && noZero),
                    onClick = { onRun(n) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KeyButton("4", Modifier.weight(1f), Ios.Blue, Color.White, true) { onRun(4) }
            KeyButton("6", Modifier.weight(1f), Ios.Indigo, Color.White, true) { onRun(6) }
            KeyButton("OUT", Modifier.weight(1f), Ios.Red.copy(alpha = 0.2f), Ios.Red, true) { onOut() }
            KeyButton("Undo", Modifier.weight(1f), Ios.SurfaceHigh, Ios.LabelSecondary, canUndo) { onUndo() }
        }
    }
}

@Composable
private fun ExtraToggle(
    label: String,
    type: ExtraType,
    selected: ExtraType,
    onSelect: (ExtraType) -> Unit,
    modifier: Modifier,
) {
    val on = selected == type
    Box(
        modifier
            .height(40.dp)
            .iosClickable { onSelect(if (on) ExtraType.NONE else type) }
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) Ios.Orange else Ios.Surface),
        contentAlignment = Alignment.Center,
    ) {
        Label(
            text = label,
            style = IosType.footnoteSemibold,
            color = if (on) Color.Black else Ios.LabelSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun KeyButton(
    label: String,
    modifier: Modifier,
    background: Color,
    foreground: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(64.dp)
            .iosClickable(enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Label(
            text = label,
            style = IosType.keypad,
            color = if (enabled) foreground else Ios.LabelTertiary,
            maxLines = 1,
        )
    }
}

@Composable
private fun DismissalDialog(
    extra: ExtraType,
    freeHit: Boolean,
    onConfirm: (Dismissal, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = remember(extra, freeHit) { allowedDismissals(extra, freeHit) }
    var selected by remember { mutableStateOf(options.singleOrNull()) }
    var runs by remember { mutableIntStateOf(0) }

    IosDialog(title = "How was the batter out?", onDismiss = onDismiss) {
        if (freeHit || extra != ExtraType.NONE) {
            Label(
                text = if (freeHit && extra == ExtraType.NONE) "Free hit: only a run out counts." else "Only these are possible on this ball.",
                style = IosType.footnote,
                color = Ios.LabelSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        options.forEach { option ->
            val on = option == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .iosClickable { selected = option }
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) Ios.Blue.copy(alpha = 0.22f) else Ios.Surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Label(option.label, Modifier.weight(1f), color = if (on) Ios.Blue else Ios.Label)
                if (on) Label("✓", style = IosType.headline, color = Ios.Blue)
            }
        }
        if (selected == Dismissal.RUN_OUT) {
            Label("Runs completed before the run out", style = IosType.footnote, color = Ios.LabelSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..3).forEach { n ->
                    val chosen = runs == n
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .iosClickable { runs = n }
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (chosen) Ios.Blue else Ios.Surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Label(n.toString(), style = IosType.headline, color = if (chosen) Color.White else Ios.Label)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Cancel", onDismiss, Modifier.weight(1f), Ios.LabelSecondary)
            PrimaryButton(
                text = "Out",
                onClick = {
                    val chosen = selected
                    if (chosen != null) onConfirm(chosen, if (chosen == Dismissal.RUN_OUT) runs else 0)
                },
                modifier = Modifier.weight(1f),
                enabled = selected != null,
            )
        }
    }
}

// ---------------------------------------------------------------- Balls tab

@Composable
private fun BallsTab(m: UiMatch, modifier: Modifier = Modifier) {
    val s = m.state
    val second = s.second
    val hasSecond = second != null && second.balls.isNotEmpty()
    var selected by remember(m.id) { mutableIntStateOf(if (hasSecond) 1 else 0) }
    val index = if (selected == 1 && hasSecond) 1 else 0
    val inn = if (index == 1 && second != null) second else s.first

    Column(modifier) {
        if (hasSecond) {
            SegmentedControl(
                options = listOf(
                    "1st · ${Teams.code(m.battingFirst)}",
                    "2nd · ${Teams.code(m.battingSecond)}",
                ),
                selected = index,
                onSelect = { selected = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (inn.balls.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Label("No balls bowled yet", style = IosType.subhead, color = Ios.LabelSecondary)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                inn.overs.asReversed().forEach { over ->
                    item(key = "header-${inn.innings}-${over.over}") { OverHeader(over) }
                    item(key = "over-${inn.innings}-${over.over}") {
                        GroupCard {
                            val rows = over.balls.asReversed()
                            rows.forEachIndexed { i, view ->
                                BallRow(view)
                                if (i < rows.lastIndex) RowDivider(inset = 16.dp)
                            }
                        }
                    }
                }
                item(key = "bottom-space") {
                    Spacer(
                        Modifier
                            .navigationBarsPadding()
                            .height(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OverHeader(over: OverSummary) {
    val last = over.balls.last()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(
            text = "OVER ${over.over + 1}",
            modifier = Modifier.weight(1f),
            style = IosType.footnote,
            color = Ios.LabelSecondary,
        )
        val wickets = if (over.wickets == 0) "" else " · ${over.wickets} wkt"
        Label(
            text = "${over.runs} runs$wickets · ${last.scoreAfter}/${last.wicketsAfter}",
            style = IosType.footnote,
            color = Ios.LabelSecondary,
        )
    }
}

@Composable
private fun BallRow(view: BallView) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(view.label, Modifier.width(38.dp), IosType.subhead, Ios.LabelSecondary)
        BallChip(view, 30.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Label(view.description, style = IosType.subhead, maxLines = 2)
            if (view.isFreeHit) Label("Free hit", style = IosType.footnote, color = Ios.Orange)
        }
        Label("${view.scoreAfter}/${view.wicketsAfter}", style = IosType.subhead, color = Ios.LabelSecondary)
    }
}

// ---------------------------------------------------------------- Summary tab

@Composable
private fun SummaryTab(m: UiMatch, modifier: Modifier = Modifier) {
    val s = m.state
    val finished = s.phase == Phase.COMPLETE
    val date = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(m.createdAt))

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        SectionHeader("Match")
        GroupCard {
            ValueRow("Format", "${m.formatLabel} · ${m.maxOvers} overs")
            RowDivider()
            ValueRow("Toss", "${m.tossWinner} chose to ${m.electedTo}")
            RowDivider()
            ValueRow("Date", date)
            RowDivider()
            ValueRow(
                title = if (finished) "Result" else "Status",
                value = m.statusText,
                valueColor = if (finished) Ios.Blue else Ios.Orange,
            )
        }

        InningsSummary(m.battingFirst, s.first)
        val second = s.second
        if (second != null && second.balls.isNotEmpty()) InningsSummary(m.battingSecond, second)

        Spacer(
            Modifier
                .navigationBarsPadding()
                .height(24.dp),
        )
    }
}

@Composable
private fun InningsSummary(team: String, inn: InningsState) {
    SectionHeader("$team · ${if (inn.innings == 1) "1st" else "2nd"} innings")
    GroupCard {
        ValueRow("Score", "${inn.runs}/${inn.wickets} (${inn.oversText} ov)", valueColor = Ios.Label)
        RowDivider()
        ValueRow("Run rate", fmt2(inn.runRate), valueColor = Ios.Label)
        RowDivider()
        ValueRow("Extras", inn.extras.toString(), valueColor = Ios.Label)
        if (inn.extras > 0) {
            val parts = buildList {
                if (inn.wides > 0) add("Wides ${inn.wides}")
                if (inn.noBalls > 0) add("No balls ${inn.noBalls}")
                if (inn.byes > 0) add("Byes ${inn.byes}")
                if (inn.legByes > 0) add("Leg byes ${inn.legByes}")
            }
            Label(
                text = parts.joinToString(" · "),
                style = IosType.footnote,
                color = Ios.LabelSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }
        if (inn.fallOfWickets.isNotEmpty()) {
            RowDivider()
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Label("Fall of wickets", style = IosType.footnote, color = Ios.LabelSecondary)
                Spacer(Modifier.height(4.dp))
                Label(
                    text = inn.fallOfWickets.joinToString("   ") { "${it.wicketNo}-${it.score} (${it.label})" },
                    style = IosType.subhead,
                )
            }
        }
    }
}
