package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.model.AnimatedPropertyType
import com.example.engine.model.Layer

/**
 * Inspector de propiedades y transformaciones de capa estilo Alight Motion.
 * Permite cambiar la propiedad activa para la curva de keyframes y modificar valores con feedback háptico/numérico.
 */
@Composable
fun LayerInspector(
    layer: Layer,
    currentTimeMs: Long,
    selectedProperty: AnimatedPropertyType,
    onSelectProperty: (AnimatedPropertyType) -> Unit,
    onValueChange: (AnimatedPropertyType, Float) -> Unit,
    onApplyPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val transform = layer.evaluateTransformAt(currentTimeMs)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("layer_inspector_panel"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141724)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Selector de Propiedad Activa (Chips horizontales con iconos)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PropertyChip(
                    type = AnimatedPropertyType.SCALE,
                    icon = Icons.Default.Transform,
                    isSelected = selectedProperty == AnimatedPropertyType.SCALE,
                    onSelect = { onSelectProperty(AnimatedPropertyType.SCALE) }
                )
                PropertyChip(
                    type = AnimatedPropertyType.ROTATION,
                    icon = Icons.Default.RotateRight,
                    isSelected = selectedProperty == AnimatedPropertyType.ROTATION,
                    onSelect = { onSelectProperty(AnimatedPropertyType.ROTATION) }
                )
                PropertyChip(
                    type = AnimatedPropertyType.POS_X,
                    icon = Icons.Default.CropFree,
                    isSelected = selectedProperty == AnimatedPropertyType.POS_X,
                    onSelect = { onSelectProperty(AnimatedPropertyType.POS_X) }
                )
                PropertyChip(
                    type = AnimatedPropertyType.CHROMATIC_ABERRATION,
                    icon = Icons.Default.ColorLens,
                    isSelected = selectedProperty == AnimatedPropertyType.CHROMATIC_ABERRATION,
                    onSelect = { onSelectProperty(AnimatedPropertyType.CHROMATIC_ABERRATION) }
                )
                PropertyChip(
                    type = AnimatedPropertyType.MOTION_BLUR,
                    icon = Icons.Default.BlurOn,
                    isSelected = selectedProperty == AnimatedPropertyType.MOTION_BLUR,
                    onSelect = { onSelectProperty(AnimatedPropertyType.MOTION_BLUR) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slider del parámetro activo con valor numérico en tiempo real
            val (currentVal, minVal, maxVal, unitLabel) = when (selectedProperty) {
                AnimatedPropertyType.SCALE -> Quadruple(transform.scale, 0.1f, 3.0f, "x")
                AnimatedPropertyType.ROTATION -> Quadruple(transform.rotation, -360f, 720f, "°")
                AnimatedPropertyType.POS_X -> Quadruple(transform.posX, -250f, 250f, "px")
                AnimatedPropertyType.POS_Y -> Quadruple(transform.posY, -250f, 250f, "px")
                AnimatedPropertyType.OPACITY -> Quadruple(transform.opacity, 0f, 1f, "%")
                AnimatedPropertyType.CHROMATIC_ABERRATION -> Quadruple(transform.chromaticAberration, 0f, 25f, "px")
                AnimatedPropertyType.MOTION_BLUR -> Quadruple(transform.motionBlur, 0f, 15f, "lvl")
                AnimatedPropertyType.LENS_DISTORTION -> Quadruple(transform.lensDistortion, -1f, 1f, "")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedProperty.label}:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "${"%.2f".format(currentVal)} $unitLabel",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00F2FE)
                )
            }

            Slider(
                value = currentVal.coerceIn(minVal, maxVal),
                onValueChange = { newVal ->
                    onValueChange(selectedProperty, newVal)
                },
                valueRange = minVal..maxVal,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00F2FE),
                    activeTrackColor = Color(0xFF00F2FE),
                    inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("property_value_slider")
            )

            // Fila de Animaciones Preconfiguradas (Presets de Motion Graphics)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFB703),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Presets:",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                PresetButton("Punch In") { onApplyPreset("PUNCH_IN") }
                PresetButton("Whip Spin") { onApplyPreset("WHIP_SPIN") }
                PresetButton("Glitch Hop") { onApplyPreset("GLITCH_HOP") }
            }
        }
    }
}

@Composable
private fun PropertyChip(
    type: AnimatedPropertyType,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onSelect,
        label = {
            Text(
                text = type.label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isSelected) Color(0xFF00F2FE) else Color.Gray
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF00F2FE).copy(alpha = 0.2f),
            selectedLabelColor = Color(0xFF00F2FE),
            containerColor = Color(0xFF1B1E2B),
            labelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = Color(0x33FFFFFF),
            selectedBorderColor = Color(0xFF00F2FE)
        )
    )
}

@Composable
private fun PresetButton(
    title: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color(0xFFFFB703)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color(0xFF231F2D)
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = Color(0x40FFB703)
        )
    )
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
