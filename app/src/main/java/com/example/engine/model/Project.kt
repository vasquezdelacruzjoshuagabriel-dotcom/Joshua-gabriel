package com.example.engine.model

import androidx.compose.ui.graphics.Color

data class Composition(
    val title: String = "Alight Motion Core Project",
    val width: Int = 1080,
    val height: Int = 1080,
    val fps: Int = 60,
    val durationMs: Long = 4000L,
    val layers: List<Layer> = defaultDemoLayers()
)

fun defaultDemoLayers(): List<Layer> {
    // Layer 1: Core Vector Shape (Star with animated scale & rotation & overshoot)
    val layerStar = Layer(
        id = "layer_vector_star",
        name = "Vector Geometry (Estrella)",
        type = LayerType.SHAPE_STAR,
        primaryColor = Color(0xFF00F2FE),
        secondaryColor = Color(0xFF4FACFE),
        scale = AnimatedFloat(
            defaultValue = 1.0f,
            keyframes = listOf(
                Keyframe(0L, 0.2f, CubicBezierCurve.Overshoot),
                Keyframe(1000L, 1.45f, CubicBezierCurve.DramaticEase),
                Keyframe(2000L, 0.95f, CubicBezierCurve.SmoothPunch),
                Keyframe(3200L, 1.6f, CubicBezierCurve.EaseInOut),
                Keyframe(4000L, 1.0f, CubicBezierCurve.EaseOut)
            )
        ),
        rotation = AnimatedFloat(
            defaultValue = 0f,
            keyframes = listOf(
                Keyframe(0L, 0f, CubicBezierCurve.SmoothPunch),
                Keyframe(1200L, 180f, CubicBezierCurve.EaseInOut),
                Keyframe(2500L, 360f, CubicBezierCurve.DramaticEase),
                Keyframe(4000L, 720f, CubicBezierCurve.EaseOut)
            )
        ),
        posX = AnimatedFloat(
            defaultValue = 0f,
            keyframes = listOf(
                Keyframe(0L, -50f, CubicBezierCurve.Overshoot),
                Keyframe(1500L, 40f, CubicBezierCurve.EaseInOut),
                Keyframe(3000L, -30f, CubicBezierCurve.EaseInOut),
                Keyframe(4000L, 0f, CubicBezierCurve.EaseOut)
            )
        ),
        effects = EffectsConfig(
            chromaticAberration = AnimatedFloat(
                defaultValue = 0f,
                keyframes = listOf(
                    Keyframe(0L, 14f, CubicBezierCurve.EaseOut),
                    Keyframe(800L, 0f, CubicBezierCurve.EaseIn),
                    Keyframe(2000L, 18f, CubicBezierCurve.DramaticEase),
                    Keyframe(2800L, 0f, CubicBezierCurve.EaseOut)
                )
            ),
            motionBlur = AnimatedFloat(
                defaultValue = 0f,
                keyframes = listOf(
                    Keyframe(0L, 8f, CubicBezierCurve.EaseOut),
                    Keyframe(1200L, 1f, CubicBezierCurve.EaseInOut),
                    Keyframe(2500L, 10f, CubicBezierCurve.DramaticEase),
                    Keyframe(3500L, 0f, CubicBezierCurve.EaseOut)
                )
            ),
            lensDistortion = AnimatedFloat(
                defaultValue = 0f,
                keyframes = listOf(
                    Keyframe(0L, 0.35f, CubicBezierCurve.EaseOut),
                    Keyframe(1500L, -0.2f, CubicBezierCurve.EaseInOut),
                    Keyframe(3000L, 0.4f, CubicBezierCurve.EaseInOut),
                    Keyframe(4000L, 0f, CubicBezierCurve.Linear)
                )
            )
        )
    )

    // Layer 2: Kinetic Typography ("ALIGHT MOTION CORE")
    val layerText = Layer(
        id = "layer_kinetic_text",
        name = "Kinetic Typo (Motion Title)",
        type = LayerType.TEXT_MOTION,
        textContent = "ALIGHT CORE 60FPS",
        primaryColor = Color(0xFFF72585),
        secondaryColor = Color(0xFF7209B7),
        posY = AnimatedFloat(
            defaultValue = 160f,
            keyframes = listOf(
                Keyframe(0L, 350f, CubicBezierCurve.Overshoot),
                Keyframe(900L, 170f, CubicBezierCurve.SmoothPunch),
                Keyframe(2200L, 150f, CubicBezierCurve.EaseInOut),
                Keyframe(3500L, 170f, CubicBezierCurve.EaseOut)
            )
        ),
        scale = AnimatedFloat(
            defaultValue = 1.0f,
            keyframes = listOf(
                Keyframe(0L, 0.6f, CubicBezierCurve.Overshoot),
                Keyframe(900L, 1.25f, CubicBezierCurve.SmoothPunch),
                Keyframe(2400L, 1.0f, CubicBezierCurve.EaseInOut),
                Keyframe(3800L, 1.15f, CubicBezierCurve.EaseOut)
            )
        ),
        opacity = AnimatedFloat(
            defaultValue = 1.0f,
            keyframes = listOf(
                Keyframe(0L, 0f, CubicBezierCurve.EaseIn),
                Keyframe(500L, 1.0f, CubicBezierCurve.EaseOut),
                Keyframe(3200L, 1.0f, CubicBezierCurve.EaseInOut),
                Keyframe(4000L, 0.85f, CubicBezierCurve.EaseOut)
            )
        )
    )

    // Layer 3: Simulated Video/Media Texture Proxy (MediaCodec Hardware Stream)
    val layerVideo = Layer(
        id = "layer_video_proxy",
        name = "Video Layer (MediaCodec HW)",
        type = LayerType.MEDIA_VIDEO_PROXY,
        primaryColor = Color(0xFF4361EE),
        secondaryColor = Color(0xFF4CC9F0),
        blendMode = BlendModeType.NORMAL,
        opacity = AnimatedFloat(0.85f),
        scale = AnimatedFloat(
            defaultValue = 0.9f,
            keyframes = listOf(
                Keyframe(0L, 0.8f, CubicBezierCurve.EaseInOut),
                Keyframe(2000L, 1.05f, CubicBezierCurve.EaseInOut),
                Keyframe(4000L, 0.8f, CubicBezierCurve.EaseInOut)
            )
        )
    )

    // Layer 4: Geometric Hexagon background element
    val layerHexagon = Layer(
        id = "layer_hexagon_bg",
        name = "Outer Shield (Hexágono)",
        type = LayerType.SHAPE_HEXAGON,
        primaryColor = Color(0xFFFFB703),
        secondaryColor = Color(0xFFFB8500),
        blendMode = BlendModeType.ADDITIVE,
        scale = AnimatedFloat(
            defaultValue = 1.8f,
            keyframes = listOf(
                Keyframe(0L, 1.2f, CubicBezierCurve.EaseInOut),
                Keyframe(2000L, 2.1f, CubicBezierCurve.DramaticEase),
                Keyframe(4000L, 1.2f, CubicBezierCurve.EaseInOut)
            )
        ),
        rotation = AnimatedFloat(
            defaultValue = 0f,
            keyframes = listOf(
                Keyframe(0L, 0f, CubicBezierCurve.Linear),
                Keyframe(4000L, -360f, CubicBezierCurve.Linear)
            )
        ),
        opacity = AnimatedFloat(0.5f)
    )

    return listOf(layerHexagon, layerVideo, layerStar, layerText)
}
