package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.model.AnimatedPropertyType
import com.example.ui.components.ArchitectureViewer
import com.example.ui.components.BezierCurveEditor
import com.example.ui.components.LayerInspector
import com.example.ui.components.TimelineScrubber
import com.example.ui.components.ViewportCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionStudioApp(
    viewModel: MotionStudioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val selectedLayer = uiState.composition.layers.find { it.id == uiState.selectedLayerId }
    val hasKeyframeOnProp = selectedLayer?.let { layer ->
        when (uiState.selectedProperty) {
            AnimatedPropertyType.SCALE -> layer.scale.hasKeyframeAt(uiState.currentTimeMs)
            AnimatedPropertyType.ROTATION -> layer.rotation.hasKeyframeAt(uiState.currentTimeMs)
            AnimatedPropertyType.POS_X -> layer.posX.hasKeyframeAt(uiState.currentTimeMs)
            AnimatedPropertyType.POS_Y -> layer.posY.hasKeyframeAt(uiState.currentTimeMs)
            AnimatedPropertyType.OPACITY -> layer.opacity.hasKeyframeAt(uiState.currentTimeMs)
            AnimatedPropertyType.CHROMATIC_ABERRATION -> layer.effects.chromaticAberration.hasKeyframeAt(uiState.currentTimeMs)
            AnimatedPropertyType.MOTION_BLUR -> layer.effects.motionBlur.hasKeyframeAt(uiState.currentTimeMs)
            AnimatedPropertyType.LENS_DISTORTION -> layer.effects.lensDistortion.hasKeyframeAt(uiState.currentTimeMs)
        }
    } ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MotionStudio",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ALIGHT ENGINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00F2FE),
                            modifier = Modifier
                                .background(Color(0xFF142433), shape = MaterialTheme.shapes.extraSmall)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0F15)
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.togglePlayback() },
                        modifier = Modifier.testTag("top_bar_play_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Configuración",
                            tint = Color.LightGray
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0B0D13),
                contentColor = Color(0xFF00F2FE)
            ) {
                NavigationBarItem(
                    selected = uiState.activeTab == 0,
                    onClick = { viewModel.setActiveTab(0) },
                    icon = { Icon(Icons.Default.Brush, contentDescription = null) },
                    label = { Text("Estudio & Timeline", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00F2FE),
                        selectedTextColor = Color(0xFF00F2FE),
                        indicatorColor = Color(0x2200F2FE),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = uiState.activeTab == 1,
                    onClick = { viewModel.setActiveTab(1) },
                    icon = { Icon(Icons.Default.Architecture, contentDescription = null) },
                    label = { Text("Arquitectura & GPU", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF9D4EDD),
                        selectedTextColor = Color(0xFF9D4EDD),
                        indicatorColor = Color(0x229D4EDD),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        },
        containerColor = Color(0xFF0A0C12)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.activeTab == 0) {
                // Pantalla Principal: Viewport + Inspector + Timeline
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Lienzo Viewport (mitad superior interactiva con Gizmo y 60 FPS)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(8.dp)
                    ) {
                        ViewportCanvas(
                            layers = uiState.composition.layers,
                            currentTimeMs = uiState.currentTimeMs,
                            selectedLayerId = uiState.selectedLayerId,
                            showGizmo = uiState.showGizmo,
                            fps = uiState.fps,
                            frameTimeMs = uiState.frameTimeMs,
                            onMoveSelectedLayer = { dx, dy ->
                                viewModel.moveSelectedLayer(dx, dy)
                            }
                        )
                    }

                    // 2. Inspector de la Capa Seleccionada (Transformadas, Curvas y Sliders)
                    if (selectedLayer != null) {
                        LayerInspector(
                            layer = selectedLayer,
                            currentTimeMs = uiState.currentTimeMs,
                            selectedProperty = uiState.selectedProperty,
                            onSelectProperty = { viewModel.selectProperty(it) },
                            onValueChange = { prop, newVal ->
                                viewModel.updatePropertyValue(prop, newVal)
                            },
                            onApplyPreset = { preset ->
                                viewModel.applyPreset(preset)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Timeline Scrubber (Regla de tiempo, Playhead y Pistas con Rombos)
                    TimelineScrubber(
                        currentTimeMs = uiState.currentTimeMs,
                        durationMs = uiState.composition.durationMs,
                        fps = uiState.composition.fps,
                        isPlaying = uiState.isPlaying,
                        layers = uiState.composition.layers,
                        selectedLayerId = uiState.selectedLayerId,
                        selectedProperty = uiState.selectedProperty,
                        hasKeyframeOnCurrentProp = hasKeyframeOnProp,
                        onTimeChange = { viewModel.setTime(it) },
                        onTogglePlay = { viewModel.togglePlayback() },
                        onStepFrame = { viewModel.stepFrame(it) },
                        onSelectLayer = { viewModel.selectLayer(it) },
                        onToggleLayerVisibility = { viewModel.toggleLayerVisibility(it) },
                        onToggleLayerLock = { viewModel.toggleLayerLock(it) },
                        onToggleKeyframe = { viewModel.toggleKeyframeOnCurrent() },
                        onOpenCurveEditor = { viewModel.toggleCurveEditor() }
                    )
                }

                // Modal Deslizable: Editor de Curva de Bézier Cúbica
                AnimatedVisibility(
                    visible = uiState.isCurveEditorOpen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    BezierCurveEditor(
                        curve = uiState.currentCurve,
                        onCurveChanged = { newCurve ->
                            viewModel.updateCurve(newCurve)
                        },
                        onClose = { viewModel.toggleCurveEditor() }
                    )
                }
            } else {
                // Pestaña de Arquitectura del Motor y Shaders
                ArchitectureViewer(
                    fps = uiState.fps,
                    frameTimeMs = uiState.frameTimeMs,
                    activeLayerCount = uiState.composition.layers.count { it.isVisible }
                )
            }
        }
    }
}
