package com.sutikshan.scorecore

import com.sutikshan.scorecore.engine.Ball
import com.sutikshan.scorecore.engine.BallInput
import com.sutikshan.scorecore.engine.Dismissal
import com.sutikshan.scorecore.engine.ExtraType
import com.sutikshan.scorecore.engine.MatchRules
import com.sutikshan.scorecore.engine.Phase
import com.sutikshan.scorecore.engine.Winner
import com.sutikshan.scorecore.engine.allowedDismissals
import com.sutikshan.scorecore.engine.computeInnings
import com.sutikshan.scorecore.engine.computeMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {

    private val rules = MatchRules(maxOvers = 20)
    private val oneOver = MatchRules(maxOvers = 1)

    private fun balls(innings: Int, vararg inputs: BallInput): List<Ball> =
        inputs.mapIndexed { i, input -> input.toBall(innings, i) }

    private fun run(n: Int) = BallInput(ExtraType.NONE, n)
    private val dot = run(0)
    private val wide = BallInput(ExtraType.WIDE, 0)
    private val noBall = BallInput(ExtraType.NO_BALL, 0)
    private fun wicket(d: Dismissal = Dismissal.BOWLED) = BallInput(ExtraType.NONE, 0, d)

    @Test
    fun legalBallsAdvanceOversAndRunRate() {
        val list = balls(1, run(1), run(4), dot, run(6), run(2), dot, run(1))
        val s = computeInnings(1, list, rules)
        assertEquals(14, s.runs)
        assertEquals(7, s.legalBalls)
        assertEquals("1.1", s.oversText)
        assertEquals(14 * 6.0 / 7, s.runRate, 1e-9)
    }

    @Test
    fun widesAndNoBallsDoNotAdvanceTheOver() {
        val list = balls(1, dot, dot, dot, dot, wide, dot)
        val s = computeInnings(1, list, rules)
        assertEquals(5, s.legalBalls)
        assertEquals(1, s.runs)
        assertEquals(1, s.wides)
        // The wide and the legal ball after it are both the fifth delivery of the over.
        assertEquals("0.5", s.balls[4].label)
        assertEquals("0.5", s.balls[5].label)
    }

    @Test
    fun extraRunsAreCountedCorrectly() {
        val list = balls(
            1,
            BallInput(ExtraType.WIDE, 3),
            BallInput(ExtraType.NO_BALL, 4),
            BallInput(ExtraType.BYE, 2),
            BallInput(ExtraType.LEG_BYE, 1),
            run(6),
        )
        val s = computeInnings(1, list, rules)
        assertEquals(4 + 5 + 2 + 1 + 6, s.runs)
        assertEquals(4, s.wides)
        assertEquals(1, s.noBalls)
        assertEquals(2, s.byes)
        assertEquals(1, s.legByes)
        assertEquals(8, s.extras)
        assertEquals(3, s.legalBalls)
    }

    @Test
    fun freeHitFollowsNoBallAndSurvivesAWide() {
        val afterNoBall = computeInnings(1, balls(1, dot, noBall), rules)
        assertTrue(afterNoBall.freeHitNext)

        val afterWide = computeInnings(1, balls(1, noBall, wide), rules)
        assertTrue(afterWide.freeHitNext)
        assertTrue(afterWide.balls[1].isFreeHit)

        val afterLegal = computeInnings(1, balls(1, noBall, wide, run(1)), rules)
        assertFalse(afterLegal.freeHitNext)
        assertTrue(afterLegal.balls[2].isFreeHit)

        val plain = computeInnings(1, balls(1, dot, wide), rules)
        assertFalse(plain.freeHitNext)
    }

    @Test
    fun inningsEndsOnTenWickets() {
        val list = balls(1, *Array(10) { wicket() })
        val s = computeInnings(1, list, rules)
        assertEquals(10, s.wickets)
        assertTrue(s.isComplete)
        assertEquals(10, s.fallOfWickets.size)
        assertEquals("0.1", s.fallOfWickets.first().label)
    }

    @Test
    fun inningsEndsWhenOversAreBowled() {
        val notYet = computeInnings(1, balls(1, dot, dot, dot, dot, dot, wide), oneOver)
        assertFalse(notYet.isComplete)
        val done = computeInnings(1, balls(1, dot, dot, dot, dot, dot, dot), oneOver)
        assertTrue(done.isComplete)
        assertEquals(0, done.ballsRemaining)
    }

    @Test
    fun runOutCanCarryRuns() {
        val s = computeInnings(1, balls(1, BallInput(ExtraType.NONE, 2, Dismissal.RUN_OUT)), rules)
        assertEquals(2, s.runs)
        assertEquals(1, s.wickets)
        assertEquals("2W", s.balls[0].chip)
        assertEquals("2 runs · OUT · Run out", s.balls[0].description)
    }

    @Test
    fun overSummariesGroupExtrasWithTheirOver() {
        val list = balls(1, run(1), run(1), run(1), run(1), run(1), run(1), wide, run(4))
        val s = computeInnings(1, list, rules)
        assertEquals(2, s.overs.size)
        assertEquals(6, s.overs[0].runs)
        assertTrue(s.overs[0].isComplete)
        // The wide is the first delivery of the second over.
        assertEquals(5, s.overs[1].runs)
        assertEquals(2, s.overs[1].balls.size)
        assertFalse(s.overs[1].isComplete)
        assertEquals("1.1", s.overs[1].balls[0].label)
    }

    @Test
    fun thisOverKeepsAFinishedOverVisible() {
        val s = computeInnings(1, balls(1, run(1), run(1), run(1), run(1), run(1), run(1)), rules)
        assertEquals(6, s.thisOver.size)
        val next = computeInnings(1, balls(1, run(1), run(1), run(1), run(1), run(1), run(1), dot), rules)
        assertEquals(1, next.thisOver.size)
    }

    @Test
    fun phasesFollowTheMatch() {
        val firstDone = balls(1, run(4), run(4), run(4), run(4), run(4), run(4)) // 24 off 1 over
        assertEquals(Phase.FIRST_INNINGS, computeMatch(oneOver, firstDone.take(3)).phase)
        assertEquals(Phase.INNINGS_BREAK, computeMatch(oneOver, firstDone).phase)
        assertTrue(computeMatch(oneOver, firstDone).canScore)

        val chasing = firstDone + balls(2, run(1))
        val m = computeMatch(oneOver, chasing)
        assertEquals(Phase.SECOND_INNINGS, m.phase)
        assertEquals(25, m.target)
        assertEquals(24, m.runsNeeded)
        assertEquals(2, m.currentInningsNo)
        assertTrue(m.canScore)
    }

    @Test
    fun firstTeamWinsByRuns() {
        val first = balls(1, run(4), run(4), run(4), run(4), run(4), run(4))
        val second = balls(2, run(1), run(1), run(1), run(1), run(1), run(1))
        val m = computeMatch(oneOver, first + second)
        assertEquals(Phase.COMPLETE, m.phase)
        assertEquals(Winner.FIRST, m.result?.winner)
        assertEquals("India won by 18 runs", m.resultText("India", "Pakistan"))
        assertFalse(m.canScore)
    }

    @Test
    fun secondTeamWinsByWicketsAndStopsAtTarget() {
        val first = balls(1, run(4), run(4), run(4), run(4), run(4), run(4)) // 24
        val second = balls(2, run(4), wicket(), run(6), run(6), run(6), run(4)) // 26 for 1
        val m = computeMatch(oneOver, first + second)
        assertEquals(Phase.COMPLETE, m.phase)
        assertEquals("Pakistan won by 9 wickets", m.resultText("India", "Pakistan"))
        assertNull(m.requiredRate)
    }

    @Test
    fun chaseEndsTheMomentTargetIsPassed() {
        val first = balls(1, run(4), run(4), run(4), run(4), run(4), run(4)) // 24
        val second = balls(2, run(6), run(6), run(6), run(6), run(1))        // 25 after 5 balls
        val m = computeMatch(oneOver, first + second)
        assertEquals(Phase.COMPLETE, m.phase)
        assertEquals(Winner.SECOND, m.result?.winner)
        assertEquals(1, m.second?.ballsRemaining)
    }

    @Test
    fun tieIsDetected() {
        val first = balls(1, run(4), run(4), run(4), run(4), run(4), run(4)) // 24
        val second = balls(2, run(4), run(4), run(4), run(4), run(4), run(4))
        val m = computeMatch(oneOver, first + second)
        assertEquals(Winner.TIE, m.result?.winner)
        assertEquals("Match tied", m.resultText("India", "Pakistan"))
    }

    @Test
    fun requiredRateIsRunsNeededPerOverRemaining() {
        val first = balls(1, run(6), run(6), run(6), run(6), run(6), run(6)) // 36
        val second = balls(2, run(1), run(1))                                  // 2 needs 35 more off 4
        val m = computeMatch(oneOver, first + second)
        assertEquals(35, m.runsNeeded)
        assertEquals(35 * 6.0 / 4, m.requiredRate!!, 1e-9)
    }

    @Test
    fun removingTheLastBallIsAFullUndo() {
        val list = balls(1, run(4), wide, wicket(), run(6))
        val before = computeInnings(1, list.dropLast(1), rules)
        val after = computeInnings(1, list, rules)
        assertEquals(6, after.runs - before.runs)
        assertEquals(1 + 4, before.runs)
        assertEquals(1, before.wickets)
    }

    @Test
    fun dismissalRulesDependOnTheDelivery() {
        assertEquals(Dismissal.entries.toList(), allowedDismissals(ExtraType.NONE, freeHit = false))
        assertEquals(listOf(Dismissal.RUN_OUT), allowedDismissals(ExtraType.NONE, freeHit = true))
        assertEquals(listOf(Dismissal.RUN_OUT), allowedDismissals(ExtraType.NO_BALL, freeHit = false))
        assertTrue(Dismissal.STUMPED in allowedDismissals(ExtraType.WIDE, freeHit = false))
        assertFalse(Dismissal.BOWLED in allowedDismissals(ExtraType.WIDE, freeHit = false))
    }

    @Test
    fun descriptionsAndChipsAreReadable() {
        val s = computeInnings(
            1,
            balls(
                1,
                run(4), run(6), dot, wide, BallInput(ExtraType.NO_BALL, 4), BallInput(ExtraType.BYE, 2),
                BallInput(ExtraType.WIDE, 2, Dismissal.STUMPED), wicket(Dismissal.CAUGHT),
            ),
            rules,
        )
        assertEquals(
            listOf("4", "6", "0", "Wd", "5Nb", "2B", "3Wd·W", "W"),
            s.balls.map { it.chip },
        )
        assertEquals("FOUR", s.balls[0].description)
        assertEquals("SIX", s.balls[1].description)
        assertEquals("No run", s.balls[2].description)
        assertEquals("Wide", s.balls[3].description)
        assertEquals("No ball + 4", s.balls[4].description)
        assertEquals("2 byes", s.balls[5].description)
        assertEquals("Wide + 2 · OUT · Stumped", s.balls[6].description)
        assertEquals("OUT · Caught", s.balls[7].description)
    }
}
