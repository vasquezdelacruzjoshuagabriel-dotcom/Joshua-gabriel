package com.example.engine.model

import androidx.compose.ui.graphics.Color

enum class LayerType {
    SHAPE_STAR,
    SHAPE_HEXAGON,
    SHAPE_RECTANGLE,
    SHAPE_CIRCLE,
    TEXT_MOTION,
    MEDIA_VIDEO_PROXY
}

enum class BlendModeType {
    NORMAL,
    ADDITIVE,
    MULTIPLY,
    SCREEN,
    OVERLAY
}

data class EffectsConfig(
    val chromaticAberration: AnimatedFloat = AnimatedFloat(0f), // 0..20 px
    val motionBlur: AnimatedFloat = AnimatedFloat(0f), // 0..10 intensity
    val lensDistortion: AnimatedFloat = AnimatedFloat(0f), // -1..1 fisheye/pinch
    val colorShift: AnimatedFloat = AnimatedFloat(0f) // -180..180 hue
)

data class Layer(
    val id: String,
    val name: String,
    val type: LayerType,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 4000L,
    // Animated Transformations
    val posX: AnimatedFloat = AnimatedFloat(0f),
    val posY: AnimatedFloat = AnimatedFloat(0f),
    val scale: AnimatedFloat = AnimatedFloat(1f),
    val rotation: AnimatedFloat = AnimatedFloat(0f),
    val opacity: AnimatedFloat = AnimatedFloat(1f),
    // Effects
    val effects: EffectsConfig = EffectsConfig(),
    // Content parameters
    val primaryColor: Color = Color(0xFF00F2FE),
    val secondaryColor: Color = Color(0xFF9D4EDD),
    val textContent: String = "MOTION GRAPHIC",
    val blendMode: BlendModeType = BlendModeType.NORMAL
) {
    /**
     * Calcula la transformada del layer en un instante temporal específico 't'
     */
    fun evaluateTransformAt(timeMs: Long): LayerTransformState {
        val x = posX.evaluate(timeMs)
        val y = posY.evaluate(timeMs)
        val s = scale.evaluate(timeMs)
        val r = rotation.evaluate(timeMs)
        val o = opacity.evaluate(timeMs).coerceIn(0f, 1f)

        val chrom = effects.chromaticAberration.evaluate(timeMs)
        val mBlur = effects.motionBlur.evaluate(timeMs)
        val dist = effects.lensDistortion.evaluate(timeMs)
        val hue = effects.colorShift.evaluate(timeMs)

        return LayerTransformState(
            posX = x,
            posY = y,
            scale = s,
            rotation = r,
            opacity = o,
            chromaticAberration = chrom,
            motionBlur = mBlur,
            lensDistortion = dist,
            colorShift = hue
        )
    }

    fun hasAnyKeyframeAt(timeMs: Long): Boolean {
        return posX.hasKeyframeAt(timeMs) ||
                posY.hasKeyframeAt(timeMs) ||
                scale.hasKeyframeAt(timeMs) ||
                rotation.hasKeyframeAt(timeMs) ||
                opacity.hasKeyframeAt(timeMs)
    }
}

data class LayerTransformState(
    val posX: Float,
    val posY: Float,
    val scale: Float,
    val rotation: Float,
    val opacity: Float,
    val chromaticAberration: Float,
    val motionBlur: Float,
    val lensDistortion: Float,
    val colorShift: Float
)

enum class AnimatedPropertyType(val label: String) {
    SCALE("Escala"),
    ROTATION("Rotación"),
    POS_X("Posición X"),
    POS_Y("Posición Y"),
    OPACITY("Opacidad"),
    CHROMATIC_ABERRATION("Croma RGB"),
    MOTION_BLUR("Motion Blur"),
    LENS_DISTORTION("Distorsión Lente")
}
