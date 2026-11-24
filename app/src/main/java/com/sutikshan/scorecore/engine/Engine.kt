package com.sutikshan.scorecore.engine

/**
 * Pure-Kotlin scoring engine. No Android dependencies, so it is trivially unit-testable.
 *
 * The single source of truth for a match is its list of [Ball] events. Everything else
 * (score, overs, run rate, free hits, result, ball-by-ball view) is derived from that list.
 */

enum class ExtraType { NONE, WIDE, NO_BALL, BYE, LEG_BYE }

enum class Dismissal(val label: String) {
    BOWLED("Bowled"),
    CAUGHT("Caught"),
    LBW("LBW"),
    RUN_OUT("Run out"),
    STUMPED("Stumped"),
    HIT_WICKET("Hit wicket"),
    OTHER("Other"),
}

/** One delivery. [extraRuns] already includes the automatic 1 run for a wide / no-ball. */
data class Ball(
    val innings: Int,
    val seq: Int,
    val batRuns: Int,
    val extra: ExtraType,
    val extraRuns: Int,
    val dismissal: Dismissal?,
) {
    val isLegal: Boolean get() = extra != ExtraType.WIDE && extra != ExtraType.NO_BALL
    val totalRuns: Int get() = batRuns + extraRuns
    val isWicket: Boolean get() = dismissal != null
}

/**
 * What the scorer taps. [runs] means:
 *  - NONE: runs off the bat
 *  - WIDE: runs run in addition to the wide (total = 1 + runs)
 *  - NO_BALL: runs off the bat (the no-ball adds 1 more)
 *  - BYE / LEG_BYE: number of byes / leg-byes
 */
data class BallInput(
    val extra: ExtraType = ExtraType.NONE,
    val runs: Int = 0,
    val dismissal: Dismissal? = null,
) {
    fun toBall(innings: Int, seq: Int): Ball = when (extra) {
        ExtraType.NONE -> Ball(innings, seq, runs, ExtraType.NONE, 0, dismissal)
        ExtraType.WIDE -> Ball(innings, seq, 0, ExtraType.WIDE, 1 + runs, dismissal)
        ExtraType.NO_BALL -> Ball(innings, seq, runs, ExtraType.NO_BALL, 1, dismissal)
        ExtraType.BYE -> Ball(innings, seq, 0, ExtraType.BYE, runs, dismissal)
        ExtraType.LEG_BYE -> Ball(innings, seq, 0, ExtraType.LEG_BYE, runs, dismissal)
    }
}

/** Which ways of getting out make sense for the delivery being scored. */
fun allowedDismissals(extra: ExtraType, freeHit: Boolean): List<Dismissal> = when {
    extra == ExtraType.WIDE -> listOf(Dismissal.STUMPED, Dismissal.RUN_OUT, Dismissal.HIT_WICKET)
    extra == ExtraType.NO_BALL || extra == ExtraType.BYE || extra == ExtraType.LEG_BYE ->
        listOf(Dismissal.RUN_OUT)
    freeHit -> listOf(Dismissal.RUN_OUT)
    else -> Dismissal.entries.toList()
}

data class MatchRules(val maxOvers: Int, val maxWickets: Int = 10) {
    val maxLegalBalls: Int get() = maxOvers * 6
}

data class BallView(
    val ball: Ball,
    /** 0-based over index the delivery belongs to. */
    val over: Int,
    /** 1-based number of the delivery within the over (extras take the number of the ball they were bowled in). */
    val ballInOver: Int,
    val isFreeHit: Boolean,
    val scoreAfter: Int,
    val wicketsAfter: Int,
) {
    val label: String get() = "$over.$ballInOver"

    /** Short text for the little circular chips. */
    val chip: String
        get() {
            val b = ball
            val extraShort = when (b.extra) {
                ExtraType.NONE -> ""
                ExtraType.WIDE -> if (b.extraRuns == 1) "Wd" else "${b.extraRuns}Wd"
                ExtraType.NO_BALL -> if (b.totalRuns == 1) "Nb" else "${b.totalRuns}Nb"
                ExtraType.BYE -> "${b.extraRuns}B"
                ExtraType.LEG_BYE -> "${b.extraRuns}Lb"
            }
            return when {
                b.isWicket && b.extra == ExtraType.NONE -> if (b.batRuns == 0) "W" else "${b.batRuns}W"
                b.isWicket -> "$extraShort·W"
                b.extra != ExtraType.NONE -> extraShort
                else -> b.batRuns.toString()
            }
        }

    /** Longer description for the ball-by-ball list. */
    val description: String
        get() {
            val b = ball
            val runsPart = when (b.extra) {
                ExtraType.NONE -> when (b.batRuns) {
                    0 -> "No run"
                    4 -> "FOUR"
                    6 -> "SIX"
                    1 -> "1 run"
                    else -> "${b.batRuns} runs"
                }
                ExtraType.WIDE -> if (b.extraRuns == 1) "Wide" else "Wide + ${b.extraRuns - 1}"
                ExtraType.NO_BALL -> if (b.batRuns == 0) "No ball" else "No ball + ${b.batRuns}"
                ExtraType.BYE -> if (b.extraRuns == 1) "1 bye" else "${b.extraRuns} byes"
                ExtraType.LEG_BYE -> if (b.extraRuns == 1) "1 leg bye" else "${b.extraRuns} leg byes"
            }
            val d = b.dismissal ?: return runsPart
            return if (b.extra == ExtraType.NONE && b.batRuns == 0) "OUT · ${d.label}"
            else "$runsPart · OUT · ${d.label}"
        }
}

data class OverSummary(
    val over: Int,
    val balls: List<BallView>,
) {
    val runs: Int get() = balls.sumOf { it.ball.totalRuns }
    val wickets: Int get() = balls.count { it.ball.isWicket }
    val isComplete: Boolean get() = balls.count { it.ball.isLegal } == 6
}

data class FallOfWicket(val wicketNo: Int, val score: Int, val label: String)

data class InningsState(
    val innings: Int,
    val balls: List<BallView>,
    val runs: Int,
    val wickets: Int,
    val legalBalls: Int,
    val wides: Int,
    val noBalls: Int,
    val byes: Int,
    val legByes: Int,
    val isComplete: Boolean,
    val freeHitNext: Boolean,
    val fallOfWickets: List<FallOfWicket>,
    val rules: MatchRules,
) {
    val extras: Int get() = wides + noBalls + byes + legByes

    /** Cricket-style overs, e.g. 17.4 means 17 overs and 4 balls. */
    val oversText: String get() = "${legalBalls / 6}.${legalBalls % 6}"

    val ballsRemaining: Int get() = (rules.maxLegalBalls - legalBalls).coerceAtLeast(0)

    val runRate: Double get() = if (legalBalls == 0) 0.0 else runs * 6.0 / legalBalls

    /** Balls of the over the last delivery belonged to (so a finished over stays visible until the next ball). */
    val thisOver: List<BallView>
        get() {
            val last = balls.lastOrNull() ?: return emptyList()
            return balls.takeLastWhile { it.over == last.over }
        }

    /** Newest over first is a UI concern; this is chronological. */
    val overs: List<OverSummary>
        get() = balls.groupBy { it.over }.map { (o, b) -> OverSummary(o, b) }
}

fun computeInnings(
    inningsNo: Int,
    balls: List<Ball>,
    rules: MatchRules,
    target: Int? = null,
): InningsState {
    var runs = 0
    var wickets = 0
    var legal = 0
    var wides = 0
    var noBalls = 0
    var byes = 0
    var legByes = 0
    var freeHit = false
    val views = ArrayList<BallView>(balls.size)
    val fow = ArrayList<FallOfWicket>()

    for (b in balls) {
        val over = legal / 6
        val ballNo = legal % 6 + 1
        val wasFreeHit = freeHit

        runs += b.totalRuns
        when (b.extra) {
            ExtraType.WIDE -> wides += b.extraRuns
            ExtraType.NO_BALL -> noBalls += b.extraRuns
            ExtraType.BYE -> byes += b.extraRuns
            ExtraType.LEG_BYE -> legByes += b.extraRuns
            ExtraType.NONE -> Unit
        }
        if (b.isWicket) {
            wickets++
            fow += FallOfWicket(wickets, runs, "$over.$ballNo")
        }
        if (b.isLegal) legal++

        // A no-ball gives a free hit; a wide on a free hit keeps the free hit alive.
        freeHit = b.extra == ExtraType.NO_BALL || (wasFreeHit && b.extra == ExtraType.WIDE)

        views += BallView(b, over, ballNo, wasFreeHit, runs, wickets)
    }

    val complete = wickets >= rules.maxWickets ||
        legal >= rules.maxLegalBalls ||
        (target != null && runs >= target)

    return InningsState(
        innings = inningsNo,
        balls = views,
        runs = runs,
        wickets = wickets,
        legalBalls = legal,
        wides = wides,
        noBalls = noBalls,
        byes = byes,
        legByes = legByes,
        isComplete = complete,
        freeHitNext = freeHit && !complete,
        fallOfWickets = fow,
        rules = rules,
    )
}

enum class Phase { FIRST_INNINGS, INNINGS_BREAK, SECOND_INNINGS, COMPLETE }

enum class Winner { FIRST, SECOND, TIE }

data class MatchResult(val winner: Winner, val margin: Int, val byRuns: Boolean)

data class MatchState(
    val rules: MatchRules,
    val first: InningsState,
    val second: InningsState?,
) {
    val phase: Phase
        get() = when {
            !first.isComplete -> Phase.FIRST_INNINGS
            second == null || second.balls.isEmpty() -> Phase.INNINGS_BREAK
            !second.isComplete -> Phase.SECOND_INNINGS
            else -> Phase.COMPLETE
        }

    /** The innings the next ball will be added to (or the one just finished, when the match is over). */
    val current: InningsState
        get() = when (phase) {
            Phase.FIRST_INNINGS -> first
            else -> second ?: first
        }

    /** Innings number the next ball belongs to. */
    val currentInningsNo: Int get() = if (phase == Phase.FIRST_INNINGS) 1 else 2

    val target: Int get() = first.runs + 1

    val runsNeeded: Int get() = (target - (second?.runs ?: 0)).coerceAtLeast(0)

    val requiredRate: Double?
        get() {
            val s = second ?: return null
            if (s.ballsRemaining == 0) return null
            return runsNeeded * 6.0 / s.ballsRemaining
        }

    val result: MatchResult?
        get() {
            if (phase != Phase.COMPLETE) return null
            val s = second ?: return null
            return when {
                s.runs >= target -> MatchResult(Winner.SECOND, rules.maxWickets - s.wickets, byRuns = false)
                s.runs == first.runs -> MatchResult(Winner.TIE, 0, byRuns = false)
                else -> MatchResult(Winner.FIRST, first.runs - s.runs, byRuns = true)
            }
        }

    fun resultText(firstName: String, secondName: String): String? {
        val r = result ?: return null
        return when (r.winner) {
            Winner.TIE -> "Match tied"
            Winner.FIRST -> "$firstName won by ${r.margin} ${if (r.margin == 1) "run" else "runs"}"
            Winner.SECOND -> "$secondName won by ${r.margin} ${if (r.margin == 1) "wicket" else "wickets"}"
        }
    }

    /** Can the scorer record another delivery right now? (The first ball of the chase is allowed at the innings break.) */
    val canScore: Boolean get() = phase != Phase.COMPLETE
}

fun computeMatch(rules: MatchRules, balls: List<Ball>): MatchState {
    val first = computeInnings(1, balls.filter { it.innings == 1 }.sortedBy { it.seq }, rules)
    val second = if (first.isComplete) {
        computeInnings(2, balls.filter { it.innings == 2 }.sortedBy { it.seq }, rules, target = first.runs + 1)
    } else {
        null
    }
    return MatchState(rules, first, second)
}
