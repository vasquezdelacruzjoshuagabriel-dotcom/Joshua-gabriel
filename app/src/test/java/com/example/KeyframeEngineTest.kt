package com.example

import com.example.engine.model.AnimatedFloat
import com.example.engine.model.CubicBezierCurve
import com.example.engine.model.Keyframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyframeEngineTest {

    @Test
    fun bezierSolver_boundaryValuesArePrecise() {
        val easeInOut = CubicBezierCurve.EaseInOut
        assertEquals(0f, easeInOut.solve(0f), 0.001f)
        assertEquals(1f, easeInOut.solve(1f), 0.001f)

        val linear = CubicBezierCurve.Linear
        assertEquals(0.5f, linear.solve(0.5f), 0.001f)
    }

    @Test
    fun bezierSolver_overshootExceedsOne() {
        val overshoot = CubicBezierCurve.Overshoot
        val peak = overshoot.solve(0.7f)
        assertTrue("Overshoot should bounce higher than 1.0f", peak > 1.0f)
    }

    @Test
    fun animatedFloat_evaluatesKeyframesWithEasing() {
        val anim = AnimatedFloat(
            defaultValue = 10f,
            keyframes = listOf(
                Keyframe(0L, 0f, CubicBezierCurve.Linear),
                Keyframe(1000L, 100f, CubicBezierCurve.Linear)
            )
        )

        assertEquals(0f, anim.evaluate(0L), 0.001f)
        assertEquals(50f, anim.evaluate(500L), 0.5f)
        assertEquals(100f, anim.evaluate(1000L), 0.001f)
        assertEquals(100f, anim.evaluate(1500L), 0.001f)
    }
}
