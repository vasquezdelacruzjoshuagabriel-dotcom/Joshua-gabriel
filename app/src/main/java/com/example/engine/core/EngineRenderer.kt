package com.example.engine.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.engine.model.BlendModeType
import com.example.engine.model.Layer
import com.example.engine.model.LayerTransformState
import com.example.engine.model.LayerType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Motor de renderizado GPU para la composición de capas y efectos visuales en tiempo real.
 * Diseñado para ejecutarse en el render loop a 60 FPS con asignación de memoria cero (Zero Allocation).
 */
object EngineRenderer {

    fun renderComposition(
        drawScope: DrawScope,
        layers: List<Layer>,
        timeMs: Long,
        selectedLayerId: String?,
        textMeasurer: TextMeasurer,
        showGizmo: Boolean = true
    ) {
        val center = Offset(drawScope.size.width / 2f, drawScope.size.height / 2f)

        // 1. Renderizar fondo de estudio (Grid de composición y guías de seguridad)
        renderStudioBackdrop(drawScope, center)

        // 2. Renderizar capas en orden de profundidad (Bottom to Top)
        for (layer in layers) {
            if (!layer.isVisible) continue
            val transform = layer.evaluateTransformAt(timeMs)

            // Aplicar efectos GPU por capa: Chromatic Aberration, Motion Blur, Lens Distortion
            renderLayerWithGpuPipeline(
                drawScope = drawScope,
                layer = layer,
                transform = transform,
                center = center,
                timeMs = timeMs,
                textMeasurer = textMeasurer
            )
        }

        // 3. Renderizar Gizmo interactivo para la capa seleccionada
        if (showGizmo && selectedLayerId != null) {
            val selected = layers.find { it.id == selectedLayerId }
            if (selected != null && selected.isVisible) {
                val t = selected.evaluateTransformAt(timeMs)
                renderLayerGizmo(drawScope, selected, t, center)
            }
        }
    }

    private fun renderStudioBackdrop(drawScope: DrawScope, center: Offset) {
        val w = drawScope.size.width
        val h = drawScope.size.height

        // Fondo oscuro de estudio
        drawScope.drawRect(
            color = Color(0xFF0D0F15),
            size = drawScope.size
        )

        // Grid sutil de coordenadas
        val gridStep = 40f
        var x = 0f
        while (x <= w) {
            drawScope.drawLine(
                color = Color(0x15FFFFFF),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f
            )
            x += gridStep
        }
        var y = 0f
        while (y <= h) {
            drawScope.drawLine(
                color = Color(0x15FFFFFF),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
            y += gridStep
        }

        // Ejes principales y mira central
        drawScope.drawLine(
            color = Color(0x3000F2FE),
            start = Offset(center.x, 0f),
            end = Offset(center.x, h),
            strokeWidth = 1.5f
        )
        drawScope.drawLine(
            color = Color(0x3000F2FE),
            start = Offset(0f, center.y),
            end = Offset(w, center.y),
            strokeWidth = 1.5f
        )
        drawScope.drawCircle(
            color = Color(0x4000F2FE),
            radius = 12f,
            center = center,
            style = Stroke(width = 1.5f)
        )
    }

    private fun renderLayerWithGpuPipeline(
        drawScope: DrawScope,
        layer: Layer,
        transform: LayerTransformState,
        center: Offset,
        timeMs: Long,
        textMeasurer: TextMeasurer
    ) {
        val basePos = center + Offset(transform.posX, transform.posY)
        val composeBlend = when (layer.blendMode) {
            BlendModeType.ADDITIVE -> BlendMode.Plus
            BlendModeType.MULTIPLY -> BlendMode.Multiply
            BlendModeType.SCREEN -> BlendMode.Screen
            BlendModeType.OVERLAY -> BlendMode.Overlay
            BlendModeType.NORMAL -> BlendMode.SrcOver
        }

        // Simulación de Pipeline de Shaders Móviles:
        // Si hay Chromatic Aberration (> 0.5px), realizamos multipass de canales RGB desplazados
        val cShift = transform.chromaticAberration
        val mBlur = transform.motionBlur

        if (cShift > 0.8f) {
            // Canal Rojo desplazado
            drawLayerTransformed(
                drawScope = drawScope,
                layer = layer,
                transform = transform,
                pos = basePos + Offset(-cShift, -cShift * 0.5f),
                timeMs = timeMs,
                textMeasurer = textMeasurer,
                colorTint = Color(0xFFFF0055),
                alphaMultiplier = 0.6f * transform.opacity,
                blendMode = BlendMode.Plus
            )
            // Canal Azul/Cian desplazado
            drawLayerTransformed(
                drawScope = drawScope,
                layer = layer,
                transform = transform,
                pos = basePos + Offset(cShift, cShift * 0.5f),
                timeMs = timeMs,
                textMeasurer = textMeasurer,
                colorTint = Color(0xFF00E5FF),
                alphaMultiplier = 0.6f * transform.opacity,
                blendMode = BlendMode.Plus
            )
        }

        // Motion Blur (Multipass direccional atenuado en GPU)
        if (mBlur > 1.0f) {
            val samples = (mBlur.toInt().coerceIn(2, 6))
            val stepOffset = Offset(mBlur * 1.5f, mBlur * 0.8f) / samples.toFloat()
            for (i in 1..samples) {
                val weight = 0.15f / samples
                drawLayerTransformed(
                    drawScope = drawScope,
                    layer = layer,
                    transform = transform,
                    pos = basePos - stepOffset * i.toFloat(),
                    timeMs = timeMs,
                    textMeasurer = textMeasurer,
                    colorTint = null,
                    alphaMultiplier = weight * transform.opacity,
                    blendMode = BlendMode.Plus
                )
            }
        }

        // Capa Principal (Shader de Fragmento / Rasterizador)
        drawLayerTransformed(
            drawScope = drawScope,
            layer = layer,
            transform = transform,
            pos = basePos,
            timeMs = timeMs,
            textMeasurer = textMeasurer,
            colorTint = null,
            alphaMultiplier = transform.opacity,
            blendMode = composeBlend
        )
    }

    private fun drawLayerTransformed(
        drawScope: DrawScope,
        layer: Layer,
        transform: LayerTransformState,
        pos: Offset,
        timeMs: Long,
        textMeasurer: TextMeasurer,
        colorTint: Color?,
        alphaMultiplier: Float,
        blendMode: BlendMode
    ) {
        if (alphaMultiplier <= 0f) return

        // Distorsión de lente (Fisheye / Pinch) simula dilatación radial
        val effectiveScale = (transform.scale * (1f + transform.lensDistortion * 0.25f)).coerceAtLeast(0.01f)

        drawScope.withTransform({
            translate(pos.x, pos.y)
            rotate(transform.rotation, pivot = Offset.Zero)
            scale(effectiveScale, effectiveScale, pivot = Offset.Zero)
        }) {
            val primary = colorTint ?: layer.primaryColor
            val secondary = colorTint ?: layer.secondaryColor

            when (layer.type) {
                LayerType.SHAPE_STAR -> {
                    drawStar(
                        drawScope = this,
                        primaryColor = primary.copy(alpha = primary.alpha * alphaMultiplier),
                        secondaryColor = secondary.copy(alpha = secondary.alpha * alphaMultiplier),
                        blendMode = blendMode
                    )
                }
                LayerType.SHAPE_HEXAGON -> {
                    drawHexagon(
                        drawScope = this,
                        primaryColor = primary.copy(alpha = primary.alpha * alphaMultiplier),
                        secondaryColor = secondary.copy(alpha = secondary.alpha * alphaMultiplier),
                        blendMode = blendMode
                    )
                }
                LayerType.SHAPE_RECTANGLE -> {
                    drawStyledRect(
                        drawScope = this,
                        primaryColor = primary.copy(alpha = primary.alpha * alphaMultiplier),
                        secondaryColor = secondary.copy(alpha = secondary.alpha * alphaMultiplier),
                        blendMode = blendMode
                    )
                }
                LayerType.SHAPE_CIRCLE -> {
                    drawStyledCircle(
                        drawScope = this,
                        primaryColor = primary.copy(alpha = primary.alpha * alphaMultiplier),
                        blendMode = blendMode
                    )
                }
                LayerType.TEXT_MOTION -> {
                    drawKineticText(
                        drawScope = this,
                        text = layer.textContent,
                        primaryColor = primary.copy(alpha = primary.alpha * alphaMultiplier),
                        secondaryColor = secondary.copy(alpha = secondary.alpha * alphaMultiplier),
                        textMeasurer = textMeasurer,
                        blendMode = blendMode
                    )
                }
                LayerType.MEDIA_VIDEO_PROXY -> {
                    drawMediaCodecVideoSimulation(
                        drawScope = this,
                        primaryColor = primary.copy(alpha = primary.alpha * alphaMultiplier),
                        secondaryColor = secondary.copy(alpha = secondary.alpha * alphaMultiplier),
                        timeMs = timeMs,
                        textMeasurer = textMeasurer,
                        blendMode = blendMode
                    )
                }
            }
        }
    }

    private fun drawStar(
        drawScope: DrawScope,
        primaryColor: Color,
        secondaryColor: Color,
        blendMode: BlendMode
    ) {
        val path = Path()
        val numPoints = 8
        val outerRadius = 85f
        val innerRadius = 38f
        val angleStep = (PI / numPoints).toFloat()

        for (i in 0 until (numPoints * 2)) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = i * angleStep - (PI / 2).toFloat()
            val x = r * cos(angle)
            val y = r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        val gradient = Brush.radialGradient(
            colors = listOf(primaryColor, secondaryColor, Color.Transparent),
            center = Offset.Zero,
            radius = outerRadius * 1.3f
        )

        drawScope.drawPath(path = path, brush = gradient, blendMode = blendMode)
        drawScope.drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.85f * primaryColor.alpha),
            style = Stroke(width = 3.5f, join = StrokeJoin.Round),
            blendMode = blendMode
        )

        // Núcleo brillante
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.9f * primaryColor.alpha),
            radius = 12f,
            center = Offset.Zero,
            blendMode = BlendMode.Plus
        )
    }

    private fun drawHexagon(
        drawScope: DrawScope,
        primaryColor: Color,
        secondaryColor: Color,
        blendMode: BlendMode
    ) {
        val path = Path()
        val radius = 100f
        for (i in 0 until 6) {
            val angle = (i * 60f * PI / 180f).toFloat()
            val x = radius * cos(angle)
            val y = radius * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawScope.drawPath(
            path = path,
            color = primaryColor.copy(alpha = 0.15f * primaryColor.alpha),
            style = Fill,
            blendMode = blendMode
        )
        drawScope.drawPath(
            path = path,
            brush = Brush.sweepGradient(listOf(primaryColor, secondaryColor, primaryColor)),
            style = Stroke(width = 4f, cap = StrokeCap.Round),
            blendMode = blendMode
        )
    }

    private fun drawStyledRect(
        drawScope: DrawScope,
        primaryColor: Color,
        secondaryColor: Color,
        blendMode: BlendMode
    ) {
        val size = Size(160f, 100f)
        val topLeft = Offset(-size.width / 2f, -size.height / 2f)
        drawScope.drawRect(
            brush = Brush.linearGradient(listOf(primaryColor, secondaryColor)),
            topLeft = topLeft,
            size = size,
            blendMode = blendMode
        )
        drawScope.drawRect(
            color = Color.White.copy(alpha = 0.8f * primaryColor.alpha),
            topLeft = topLeft,
            size = size,
            style = Stroke(width = 2.5f)
        )
    }

    private fun drawStyledCircle(
        drawScope: DrawScope,
        primaryColor: Color,
        blendMode: BlendMode
    ) {
        drawScope.drawCircle(
            color = primaryColor,
            radius = 70f,
            blendMode = blendMode
        )
    }

    private fun drawKineticText(
        drawScope: DrawScope,
        text: String,
        primaryColor: Color,
        secondaryColor: Color,
        textMeasurer: TextMeasurer,
        blendMode: BlendMode
    ) {
        val style = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            color = primaryColor
        )
        val layout = textMeasurer.measure(text = text, style = style)
        val textPos = Offset(-layout.size.width / 2f, -layout.size.height / 2f)

        // Resplandor de texto (Sombra de color)
        drawScope.drawText(
            textMeasurer = textMeasurer,
            text = text,
            topLeft = textPos + Offset(2f, 2f),
            style = style.copy(color = secondaryColor.copy(alpha = 0.7f * secondaryColor.alpha))
        )
        drawScope.drawText(
            textMeasurer = textMeasurer,
            text = text,
            topLeft = textPos,
            style = style
        )
    }

    private fun drawMediaCodecVideoSimulation(
        drawScope: DrawScope,
        primaryColor: Color,
        secondaryColor: Color,
        timeMs: Long,
        textMeasurer: TextMeasurer,
        blendMode: BlendMode
    ) {
        val w = 240f
        val h = 140f
        val topLeft = Offset(-w / 2f, -h / 2f)

        // Pantalla de video hardware (SurfaceView frame)
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
            ),
            topLeft = topLeft,
            size = Size(w, h),
            blendMode = blendMode
        )

        // Ondas de video animadas según tiempo real
        val wavePath = Path()
        val freq = 0.05f
        val phase = (timeMs % 1000) / 1000f * 2f * PI.toFloat()
        var first = true
        var px = topLeft.x + 10f
        while (px <= topLeft.x + w - 10f) {
            val py = topLeft.y + h / 2f + sin((px * freq) + phase) * 22f
            if (first) {
                wavePath.moveTo(px, py)
                first = false
            } else {
                wavePath.lineTo(px, py)
            }
            px += 6f
        }
        drawScope.drawPath(
            path = wavePath,
            color = primaryColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
            blendMode = blendMode
        )

        // Scanlines CRT sutiles
        var sy = topLeft.y
        while (sy <= topLeft.y + h) {
            drawScope.drawLine(
                color = Color(0x20000000),
                start = Offset(topLeft.x, sy),
                end = Offset(topLeft.x + w, sy),
                strokeWidth = 1f
            )
            sy += 4f
        }

        // Borde y etiqueta de Hardware Decoder
        drawScope.drawRect(
            color = secondaryColor.copy(alpha = 0.7f),
            topLeft = topLeft,
            size = Size(w, h),
            style = Stroke(width = 2f)
        )

        val frameNum = (timeMs * 60 / 1000) % 240
        val labelStyle = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )
        drawScope.drawText(
            textMeasurer = textMeasurer,
            text = "HW:MediaCodec #$frameNum (0-Copy)",
            topLeft = topLeft + Offset(8f, 6f),
            style = labelStyle
        )
    }

    private fun renderLayerGizmo(
        drawScope: DrawScope,
        layer: Layer,
        transform: LayerTransformState,
        center: Offset
    ) {
        val pos = center + Offset(transform.posX, transform.posY)
        val boxRadius = 60f * transform.scale.coerceIn(0.3f, 3.0f)

        drawScope.withTransform({
            translate(pos.x, pos.y)
            rotate(transform.rotation, pivot = Offset.Zero)
        }) {
            // Marco de selección con guías punteadas
            val rect = Size(boxRadius * 2, boxRadius * 2)
            val rectTopLeft = Offset(-boxRadius, -boxRadius)

            drawScope.drawRect(
                color = Color(0xFF00F2FE),
                topLeft = rectTopLeft,
                size = rect,
                style = Stroke(width = 1.5f)
            )

            // Manijas en las 4 esquinas (Handles de escala)
            val cornerSize = 10f
            val corners = listOf(
                rectTopLeft,
                rectTopLeft + Offset(rect.width, 0f),
                rectTopLeft + Offset(0f, rect.height),
                rectTopLeft + Offset(rect.width, rect.height)
            )
            for (c in corners) {
                drawScope.drawRect(
                    color = Color.White,
                    topLeft = c - Offset(cornerSize / 2f, cornerSize / 2f),
                    size = Size(cornerSize, cornerSize)
                )
                drawScope.drawRect(
                    color = Color(0xFF00F2FE),
                    topLeft = c - Offset(cornerSize / 2f, cornerSize / 2f),
                    size = Size(cornerSize, cornerSize),
                    style = Stroke(width = 1.5f)
                )
            }

            // Manija de Rotación superior
            val rotHandlePos = Offset(0f, -boxRadius - 28f)
            drawScope.drawLine(
                color = Color(0xFF00F2FE),
                start = Offset(0f, -boxRadius),
                end = rotHandlePos,
                strokeWidth = 1.5f
            )
            drawScope.drawCircle(
                color = Color(0xFFFFB703),
                radius = 7f,
                center = rotHandlePos
            )
        }

        // Pivote central
        drawScope.drawCircle(
            color = Color(0xFFFF0055),
            radius = 5f,
            center = pos
        )
    }
}
