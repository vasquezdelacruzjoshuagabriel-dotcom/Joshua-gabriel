package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.core.EngineRenderer
import com.example.engine.model.Layer

/**
 * Lienzo principal de previsualización GPU del motor de motion graphics.
 * Permite renderizar a 60 FPS con compositing en tiempo real y soporte para arrastrar capas (On-Screen Gizmo Drag).
 */
@Composable
fun ViewportCanvas(
    layers: List<Layer>,
    currentTimeMs: Long,
    selectedLayerId: String?,
    showGizmo: Boolean,
    fps: Float,
    frameTimeMs: Float,
    onMoveSelectedLayer: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D0F15))
            .testTag("viewport_canvas_container")
            .pointerInput(selectedLayerId) {
                if (selectedLayerId != null) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onMoveSelectedLayer(dragAmount.x, dragAmount.y)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            EngineRenderer.renderComposition(
                drawScope = this,
                layers = layers,
                timeMs = currentTimeMs,
                selectedLayerId = selectedLayerId,
                textMeasurer = textMeasurer,
                showGizmo = showGizmo
            )
        }

        // Overlay de Telemetría de FPS y GPU (Esquina superior izquierda)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xCC08090E))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${"%.1f".format(fps)} FPS • ${"%.2f".format(frameTimeMs)}ms • 1080x1080 GPU",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (fps >= 55f) Color(0xFF00FF66) else Color(0xFFFFB703)
            )
        }

        // Indicador de Formato y Resolución (Esquina superior derecha)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xCC08090E))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "ALIGHT ENGINE CORE",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00F2FE)
            )
        }
    }
}
