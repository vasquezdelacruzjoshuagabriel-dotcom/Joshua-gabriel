package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.model.AnimatedPropertyType
import com.example.engine.model.Layer
import kotlin.math.abs

/**
 * Timeline interactivo con regla de tiempo, cabezal de reproducción (playhead) táctil,
 * pistas de capas con nodos de keyframe y controles de transporte estilo Alight Motion.
 */
@Composable
fun TimelineScrubber(
    currentTimeMs: Long,
    durationMs: Long,
    fps: Int,
    isPlaying: Boolean,
    layers: List<Layer>,
    selectedLayerId: String?,
    selectedProperty: AnimatedPropertyType,
    hasKeyframeOnCurrentProp: Boolean,
    onTimeChange: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onSelectLayer: (String) -> Unit,
    onToggleLayerVisibility: (String) -> Unit,
    onToggleLayerLock: (String) -> Unit,
    onToggleKeyframe: () -> Unit,
    onOpenCurveEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timeline_scrubber_container"),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF10121B)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            // 1. Barra de Controles de Transporte y Timecode
            TimelineControlHeader(
                currentTimeMs = currentTimeMs,
                durationMs = durationMs,
                fps = fps,
                isPlaying = isPlaying,
                hasKeyframe = hasKeyframeOnCurrentProp,
                selectedProperty = selectedProperty,
                onTogglePlay = onTogglePlay,
                onStepFrame = onStepFrame,
                onToggleKeyframe = onToggleKeyframe,
                onOpenCurveEditor = onOpenCurveEditor
            )

            // 2. Regla de Tiempo y Cursor de Scrubbing
            TimelineRuler(
                currentTimeMs = currentTimeMs,
                durationMs = durationMs,
                fps = fps,
                onTimeChange = onTimeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )

            // 3. Pistas de Capas con Nodos de Keyframe
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(layers, key = { it.id }) { layer ->
                    LayerTrackItem(
                        layer = layer,
                        isSelected = layer.id == selectedLayerId,
                        currentTimeMs = currentTimeMs,
                        durationMs = durationMs,
                        onSelect = { onSelectLayer(layer.id) },
                        onToggleVisibility = { onToggleLayerVisibility(layer.id) },
                        onToggleLock = { onToggleLayerLock(layer.id) },
                        onSeekToKeyframe = onTimeChange
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineControlHeader(
    currentTimeMs: Long,
    durationMs: Long,
    fps: Int,
    isPlaying: Boolean,
    hasKeyframe: Boolean,
    selectedProperty: AnimatedPropertyType,
    onTogglePlay: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onToggleKeyframe: () -> Unit,
    onOpenCurveEditor: () -> Unit
) {
    val currentFrame = (currentTimeMs * fps / 1000).toInt()
    val totalFrames = (durationMs * fps / 1000).toInt()
    val seconds = currentTimeMs / 1000f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timecode
        Column {
            Text(
                text = "${"%02d".format(currentTimeMs / 1000)}:${"%02d".format((currentTimeMs % 1000) / 10)}f ($currentFrame / $totalFrames)",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF00F2FE)
            )
            Text(
                text = "Editando: ${selectedProperty.label}",
                fontSize = 11.sp,
                color = Color.LightGray
            )
        }

        // Botones de Play / Step Frame
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onStepFrame(-1) },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("step_prev_frame_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Frame Anterior",
                    tint = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) Color(0xFFFFB703) else Color(0xFF00F2FE))
                    .clickable { onTogglePlay() }
                    .testTag("play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = { onStepFrame(1) },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("step_next_frame_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Frame Siguiente",
                    tint = Color.White
                )
            }
        }

        // Botón de Rombo de Keyframe (+ / -) y Botón de Curva
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Botón de Rombo Alight Motion
            FilledTonalButton(
                onClick = onToggleKeyframe,
                modifier = Modifier
                    .height(34.dp)
                    .testTag("toggle_keyframe_button")
            ) {
                Icon(
                    imageVector = if (hasKeyframe) Icons.Default.Remove else Icons.Default.Add,
                    contentDescription = "Modificar Keyframe",
                    tint = if (hasKeyframe) Color(0xFFFF0055) else Color(0xFFFFB703),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Rombo
                Canvas(modifier = Modifier.size(12.dp)) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height / 2f)
                        lineTo(size.width / 2f, size.height)
                        lineTo(0f, size.height / 2f)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = if (hasKeyframe) Color(0xFFFFB703) else Color.White
                    )
                }
            }

            // Botón de Curva
            FilledTonalButton(
                onClick = onOpenCurveEditor,
                modifier = Modifier
                    .height(34.dp)
                    .testTag("open_curve_editor_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "Abrir Curva",
                    tint = Color(0xFF00F2FE),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineRuler(
    currentTimeMs: Long,
    durationMs: Long,
    fps: Int,
    onTimeChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF090A0F))
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onTimeChange((fraction * durationMs).toLong())
                }
            }
            .pointerInput(durationMs) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onTimeChange((fraction * durationMs).toLong())
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Marcas de tiempo cada 500ms y 1000ms
            val numSeconds = (durationMs / 1000).toInt()
            for (sec in 0..numSeconds) {
                val secX = (sec * 1000f / durationMs) * w
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(secX, 0f),
                    end = Offset(secX, h * 0.7f),
                    strokeWidth = 1.5f
                )

                // Submarcas (medio segundo)
                if (sec < numSeconds) {
                    val halfSecX = ((sec * 1000f + 500f) / durationMs) * w
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(halfSecX, 0f),
                        end = Offset(halfSecX, h * 0.4f),
                        strokeWidth = 1f
                    )
                }
            }

            // Cursor del Playhead (Aguja de reproducción)
            val playheadX = (currentTimeMs.toFloat() / durationMs.toFloat()) * w

            // Línea roja/cian brillante
            drawLine(
                color = Color(0xFFFF0055),
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, h),
                strokeWidth = 2.5f
            )

            // Cabezal superior (Punta de flecha invertida)
            val headPath = Path().apply {
                moveTo(playheadX - 6f, 0f)
                lineTo(playheadX + 6f, 0f)
                lineTo(playheadX, 10f)
                close()
            }
            drawPath(path = headPath, color = Color(0xFFFF0055))
        }
    }
}

@Composable
private fun LayerTrackItem(
    layer: Layer,
    isSelected: Boolean,
    currentTimeMs: Long,
    durationMs: Long,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onSeekToKeyframe: (Long) -> Unit
) {
    val trackBg = if (isSelected) Color(0xFF1E2233) else Color(0xFF131622)
    val borderColor = if (isSelected) Color(0xFF00F2FE) else Color(0x22FFFFFF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(trackBg)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable { onSelect() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Controles de capa (Visibilidad & Candado)
        IconButton(
            onClick = onToggleVisibility,
            modifier = Modifier.size(26.dp)
        ) {
            Icon(
                imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Visibilidad",
                tint = if (layer.isVisible) Color.White else Color.DarkGray,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onToggleLock,
            modifier = Modifier.size(26.dp)
        ) {
            Icon(
                imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Bloquear",
                tint = if (layer.isLocked) Color(0xFFFFB703) else Color.DarkGray,
                modifier = Modifier.size(16.dp)
            )
        }

        // Título de la capa
        Text(
            text = layer.name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF00F2FE) else Color.White,
            modifier = Modifier.width(90.dp),
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Franja de pista con Rombos de Keyframes dibujados en sus marcas de tiempo
        Box(
            modifier = Modifier
                .weight(1f)
                .height(26.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF0C0E14))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Franja activa de la capa
                val startX = (layer.startTimeMs.toFloat() / durationMs.toFloat()) * w
                val endX = (layer.endTimeMs.toFloat() / durationMs.toFloat()) * w
                drawRect(
                    color = layer.primaryColor.copy(alpha = 0.25f),
                    topLeft = Offset(startX, 2f),
                    size = Size(endX - startX, h - 4f)
                )

                // Cursor de tiempo en la pista
                val cursorX = (currentTimeMs.toFloat() / durationMs.toFloat()) * w
                drawLine(
                    color = Color(0xFFFF0055).copy(alpha = 0.7f),
                    start = Offset(cursorX, 0f),
                    end = Offset(cursorX, h),
                    strokeWidth = 1.5f
                )

                // Recopilar todos los keyframes de la capa
                val allKeyframes = (layer.scale.keyframes +
                        layer.rotation.keyframes +
                        layer.posX.keyframes +
                        layer.posY.keyframes +
                        layer.opacity.keyframes).distinctBy { it.timeMs }

                for (kf in allKeyframes) {
                    val kfX = (kf.timeMs.toFloat() / durationMs.toFloat()) * w
                    val isCurrent = abs(kf.timeMs - currentTimeMs) < 40L

                    val diamondPath = Path().apply {
                        val dSize = if (isCurrent) 7f else 5.5f
                        moveTo(kfX, h / 2f - dSize)
                        lineTo(kfX + dSize, h / 2f)
                        lineTo(kfX, h / 2f + dSize)
                        lineTo(kfX - dSize, h / 2f)
                        close()
                    }

                    drawPath(
                        path = diamondPath,
                        color = if (isCurrent) Color(0xFFFFB703) else Color(0xFF00F2FE)
                    )
                }
            }
        }
    }
}
