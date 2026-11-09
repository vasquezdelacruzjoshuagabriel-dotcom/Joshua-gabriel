package com.example.engine.model

import kotlin.math.abs

/**
 * Representa una curva de Bézier Cúbica para interpolación de aceleración/desaceleración
 * idéntica a la utilizada en Alight Motion y After Effects.
 * P0 = (0,0), P3 = (1,1) fijos.
 * P1 = (x1, y1), P2 = (x2, y2) son las tangentes configurables por el usuario.
 */
data class CubicBezierCurve(
    val x1: Float = 0.25f,
    val y1: Float = 0.1f,
    val x2: Float = 0.25f,
    val y2: Float = 1.0f,
    val name: String = "Custom"
) {
    /**
     * Resuelve el valor Y para un tiempo normalizado X in [0..1].
     * Utiliza el algoritmo Newton-Raphson de convergencia rápida en ~4-8 iteraciones.
     */
    fun solve(t: Float): Float {
        if (t <= 0f) return 0f
        if (t >= 1f) return 1f
        if (x1 == y1 && x2 == y2) return t // Linear

        // Encontrar parámetro 'u' tal que Bx(u) == t
        var u = t
        for (i in 0 until 8) {
            val currentX = sampleCurveX(u) - t
            if (abs(currentX) < 1e-5f) break
            val dX = sampleCurveDerivativeX(u)
            if (abs(dX) < 1e-5f) break
            u -= currentX / dX
        }
        u = u.coerceIn(0f, 1f)
        return sampleCurveY(u)
    }

    private fun sampleCurveX(u: Float): Float {
        // (1-u)^3*0 + 3(1-u)^2*u*x1 + 3(1-u)*u^2*x2 + u^3*1
        val oneMinusU = 1f - u
        return 3f * oneMinusU * oneMinusU * u * x1 + 3f * oneMinusU * u * u * x2 + u * u * u
    }

    private fun sampleCurveY(u: Float): Float {
        val oneMinusU = 1f - u
        return 3f * oneMinusU * oneMinusU * u * y1 + 3f * oneMinusU * u * u * y2 + u * u * u
    }

    private fun sampleCurveDerivativeX(u: Float): Float {
        val oneMinusU = 1f - u
        return 3f * (1f - 3f * u + 2f * u * u) * x1 + 3f * (2f * u - 3f * u * u) * x2 + 3f * u * u
    }

    companion object {
        val Linear = CubicBezierCurve(0f, 0f, 1f, 1f, "Linear")
        val EaseIn = CubicBezierCurve(0.42f, 0.0f, 1.0f, 1.0f, "Ease In")
        val EaseOut = CubicBezierCurve(0.0f, 0.0f, 0.58f, 1.0f, "Ease Out")
        val EaseInOut = CubicBezierCurve(0.42f, 0.0f, 0.58f, 1.0f, "Ease In-Out")
        val Overshoot = CubicBezierCurve(0.34f, 1.56f, 0.64f, 1.0f, "Overshoot / Bounce")
        val SmoothPunch = CubicBezierCurve(0.16f, 1.0f, 0.3f, 1.0f, "Snappy Punch")
        val DramaticEase = CubicBezierCurve(0.7f, 0.0f, 0.1f, 1.0f, "Dramatic")
    }
}

/**
 * Keyframe genérico con marca de tiempo en milisegundos, valor numérico y curva de salida hacia el siguiente keyframe.
 */
data class Keyframe(
    val timeMs: Long,
    val value: Float,
    val easing: CubicBezierCurve = CubicBezierCurve.EaseInOut
)

/**
 * Propiedad animada con lista ordenada de keyframes.
 * Optimizado para búsqueda binaria O(log N) durante el scrubbing del timeline.
 */
data class AnimatedFloat(
    val defaultValue: Float,
    val keyframes: List<Keyframe> = emptyList()
) {
    fun evaluate(timeMs: Long): Float {
        if (keyframes.isEmpty()) return defaultValue
        if (keyframes.size == 1) return keyframes.first().value
        if (timeMs <= keyframes.first().timeMs) return keyframes.first().value
        if (timeMs >= keyframes.last().timeMs) return keyframes.last().value

        // Búsqueda binaria para localizar los keyframes adyacentes
        var low = 0
        var high = keyframes.size - 1
        var idx = 0
        while (low <= high) {
            val mid = (low + high).ushr(1)
            if (keyframes[mid].timeMs <= timeMs) {
                idx = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        val k0 = keyframes[idx]
        val k1 = keyframes[(idx + 1).coerceAtMost(keyframes.size - 1)]

        val duration = (k1.timeMs - k0.timeMs).toFloat()
        if (duration <= 0f) return k0.value

        val progress = ((timeMs - k0.timeMs) / duration).coerceIn(0f, 1f)
        val easedProgress = k0.easing.solve(progress)

        return k0.value + (k1.value - k0.value) * easedProgress
    }

    fun hasKeyframeAt(timeMs: Long, toleranceMs: Long = 40L): Boolean {
        return keyframes.any { abs(it.timeMs - timeMs) <= toleranceMs }
    }

    fun findKeyframeAt(timeMs: Long, toleranceMs: Long = 40L): Keyframe? {
        return keyframes.firstOrNull { abs(it.timeMs - timeMs) <= toleranceMs }
    }

    fun withKeyframe(timeMs: Long, value: Float, easing: CubicBezierCurve = CubicBezierCurve.EaseInOut): AnimatedFloat {
        val filtered = keyframes.filterNot { abs(it.timeMs - timeMs) <= 30L }
        val updated = (filtered + Keyframe(timeMs, value, easing)).sortedBy { it.timeMs }
        return copy(keyframes = updated)
    }

    fun withoutKeyframe(timeMs: Long): AnimatedFloat {
        val updated = keyframes.filterNot { abs(it.timeMs - timeMs) <= 40L }
        return copy(keyframes = updated)
    }

    fun updateEasingAt(timeMs: Long, easing: CubicBezierCurve): AnimatedFloat {
        val updated = keyframes.map {
            if (abs(it.timeMs - timeMs) <= 40L) it.copy(easing = easing) else it
        }
        return copy(keyframes = updated)
    }
}
