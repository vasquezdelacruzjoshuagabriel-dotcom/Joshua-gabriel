package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.model.CubicBezierCurve
import kotlinx.coroutines.delay

/**
 * Editor Interactivo de Curvas de Bézier Cúbicas (Estilo Alight Motion / After Effects).
 * Permite ajustar los puntos de control P1 y P2 mediante gestos táctiles directos,
 * evaluar la física de la animación y seleccionar curvas predefinidas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BezierCurveEditor(
    curve: CubicBezierCurve,
    onCurveChanged: (CubicBezierCurve) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var p1x by remember(curve) { mutableFloatStateOf(curve.x1) }
    var p1y by remember(curve) { mutableFloatStateOf(curve.y1) }
    var p2x by remember(curve) { mutableFloatStateOf(curve.x2) }
    var p2y by remember(curve) { mutableFloatStateOf(curve.y2) }

    // Animación de prueba interactiva para verificar la aceleración
    var testProgress by remember { mutableFloatStateOf(0f) }
    var isTesting by remember { mutableStateOf(true) }

    LaunchedEffect(isTesting) {
        while (isTesting) {
            val startTime = System.currentTimeMillis()
            val duration = 1200L
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val raw = (elapsed % duration) / duration.toFloat()
                testProgress = raw
                delay(16)
                if (elapsed >= duration) break
            }
            delay(300)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bezier_curve_editor_card"),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF141721)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabecera del Editor de Curvas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Editor de Curva",
                        tint = Color(0xFF00F2FE),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Curva de Interpolación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${curve.name})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9D4EDD)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_curve_editor_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar Editor de Curvas",
                        tint = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gráfico de Bézier y Preview en Vivo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Lienzo 2D interactivo de Bézier
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0A0C12))
                        .pointerInput(Unit) {
                            var activeHandle: Int? = null // 1 for P1, 2 for P2
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    val p1Pos = Offset(p1x * w, (1f - p1y) * h)
                                    val p2Pos = Offset(p2x * w, (1f - p2y) * h)

                                    val dist1 = (offset - p1Pos).getDistance()
                                    val dist2 = (offset - p2Pos).getDistance()

                                    activeHandle = if (dist1 < dist2 && dist1 < 60f) {
                                        1
                                    } else if (dist2 < 60f) {
                                        2
                                    } else if (dist1 < 90f) {
                                        1
                                    } else {
                                        2
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()

                                    if (activeHandle == 1) {
                                        p1x = (p1x + dragAmount.x / w).coerceIn(0f, 1f)
                                        p1y = (p1y - dragAmount.y / h).coerceIn(-0.5f, 1.8f)
                                    } else if (activeHandle == 2) {
                                        p2x = (p2x + dragAmount.x / w).coerceIn(0f, 1f)
                                        p2y = (p2y - dragAmount.y / h).coerceIn(-0.5f, 1.8f)
                                    }
                                    onCurveChanged(
                                        CubicBezierCurve(
                                            x1 = p1x,
                                            y1 = p1y,
                                            x2 = p2x,
                                            y2 = p2y,
                                            name = "Personalizada"
                                        )
                                    )
                                },
                                onDragEnd = {
                                    activeHandle = null
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height

                        // Grid 4x4
                        for (i in 1..3) {
                            val step = i / 4f
                            drawLine(
                                color = Color(0x1AFFFFFF),
                                start = Offset(step * w, 0f),
                                end = Offset(step * w, h),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color(0x1AFFFFFF),
                                start = Offset(0f, step * h),
                                end = Offset(w, step * h),
                                strokeWidth = 1f
                            )
                        }

                        // Diagonal de referencia lineal
                        drawLine(
                            color = Color(0x33FFFFFF),
                            start = Offset(0f, h),
                            end = Offset(w, 0f),
                            strokeWidth = 1f
                        )

                        val p0 = Offset(0f, h)
                        val p3 = Offset(w, 0f)
                        val handle1 = Offset(p1x * w, (1f - p1y) * h)
                        val handle2 = Offset(p2x * w, (1f - p2y) * h)

                        // Varillas de control de tangentes
                        drawLine(
                            color = Color(0xFF00F2FE),
                            start = p0,
                            end = handle1,
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color(0xFF9D4EDD),
                            start = p3,
                            end = handle2,
                            strokeWidth = 2f
                        )

                        // Curva de Bézier trazada con muestras de precisión
                        val path = Path()
                        path.moveTo(p0.x, p0.y)
                        val samples = 60
                        for (i in 1..samples) {
                            val t = i / samples.toFloat()
                            val curX = curve.solve(t)
                            path.lineTo(t * w, (1f - curX) * h)
                        }

                        drawPath(
                            path = path,
                            color = Color.White,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )

                        // Nodos P1 y P2 (Manijas táctiles)
                        drawCircle(
                            color = Color(0xFF00F2FE),
                            radius = 9f,
                            center = handle1
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = handle1
                        )

                        drawCircle(
                            color = Color(0xFF9D4EDD),
                            radius = 9f,
                            center = handle2
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = handle2
                        )
                    }
                }

                // Panel lateral: Simulación de física y Valores numéricos
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(190.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Tangente 1 (In)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00F2FE)
                        )
                        Text(
                            text = "X: ${"%.2f".format(p1x)}  Y: ${"%.2f".format(p1y)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Tangente 2 (Out)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9D4EDD)
                        )
                        Text(
                            text = "X: ${"%.2f".format(p2x)}  Y: ${"%.2f".format(p2y)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    // Visualizador dinámico de aceleración
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0F16)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Preview Dinámica",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "${(testProgress * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00F2FE)
                                )
                            }

                            // Barra con el objeto acelerando según la curva actual
                            val easedVal = curve.solve(testProgress)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1B1E29)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val offsetFraction = easedVal.coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(offsetFraction.coerceAtLeast(0.06f))
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF00F2FE))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Presets Rápidos de Alight Motion
            Text(
                text = "Preajustes de Curva:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(6.dp))

            val presets = listOf(
                CubicBezierCurve.Linear,
                CubicBezierCurve.EaseIn,
                CubicBezierCurve.EaseOut,
                CubicBezierCurve.EaseInOut,
                CubicBezierCurve.Overshoot,
                CubicBezierCurve.SmoothPunch,
                CubicBezierCurve.DramaticEase
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (preset in presets) {
                    val isSelected = curve.name == preset.name
                    AssistChip(
                        onClick = {
                            p1x = preset.x1
                            p1y = preset.y1
                            p2x = preset.x2
                            p2y = preset.y2
                            onCurveChanged(preset)
                        },
                        label = {
                            Text(
                                text = preset.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) Color(0xFF00F2FE).copy(alpha = 0.2f) else Color(0xFF1B1E2B),
                            labelColor = if (isSelected) Color(0xFF00F2FE) else Color.White
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (isSelected) Color(0xFF00F2FE) else Color(0x33FFFFFF)
                        )
                    )
                }
            }
        }
    }
}
