package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.model.AnimatedPropertyType
import com.example.engine.model.Composition
import com.example.engine.model.CubicBezierCurve
import com.example.engine.model.Layer
import com.example.engine.model.defaultDemoLayers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

data class StudioUiState(
    val composition: Composition = Composition(),
    val currentTimeMs: Long = 650L,
    val isPlaying: Boolean = false,
    val selectedLayerId: String? = "layer_vector_star",
    val selectedProperty: AnimatedPropertyType = AnimatedPropertyType.SCALE,
    val isCurveEditorOpen: Boolean = false,
    val currentCurve: CubicBezierCurve = CubicBezierCurve.Overshoot,
    val fps: Float = 60.0f,
    val frameTimeMs: Float = 16.6f,
    val showGizmo: Boolean = true,
    val activeTab: Int = 0 // 0 = Studio Editor, 1 = Arquitectura & Shaders
)

class MotionStudioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var lastFrameTimeNs: Long = 0L

    init {
        updateActiveCurveForCurrentState()
    }

    fun setTime(timeMs: Long) {
        val bounded = timeMs.coerceIn(0L, _uiState.value.composition.durationMs)
        _uiState.value = _uiState.value.copy(currentTimeMs = bounded)
        updateActiveCurveForCurrentState()
    }

    fun togglePlayback() {
        if (_uiState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        playbackJob?.cancel()
        _uiState.value = _uiState.value.copy(isPlaying = true)
        playbackJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            val duration = _uiState.value.composition.durationMs
            while (isActive) {
                val now = System.nanoTime()
                val deltaMs = ((now - lastTime) / 1_000_000L).coerceAtLeast(1L)
                lastTime = now

                // Calcular FPS y FrameTime
                val currentFps = (1000f / deltaMs.toFloat()).coerceIn(30f, 60f)
                val currentFrameMs = deltaMs.toFloat()

                var nextTime = _uiState.value.currentTimeMs + deltaMs
                if (nextTime >= duration) {
                    nextTime = 0L // Loop
                }
                _uiState.value = _uiState.value.copy(
                    currentTimeMs = nextTime,
                    fps = currentFps,
                    frameTimeMs = currentFrameMs
                )
                delay(16) // ~60 FPS
            }
        }
    }

    fun pause() {
        playbackJob?.cancel()
        playbackJob = null
        _uiState.value = _uiState.value.copy(isPlaying = false)
        updateActiveCurveForCurrentState()
    }

    fun stepFrame(delta: Int) {
        pause()
        val frameMs = 1000L / _uiState.value.composition.fps
        val newTime = (_uiState.value.currentTimeMs + delta * frameMs)
            .coerceIn(0L, _uiState.value.composition.durationMs)
        setTime(newTime)
    }

    fun selectLayer(layerId: String) {
        _uiState.value = _uiState.value.copy(selectedLayerId = layerId)
        updateActiveCurveForCurrentState()
    }

    fun selectProperty(property: AnimatedPropertyType) {
        _uiState.value = _uiState.value.copy(selectedProperty = property)
        updateActiveCurveForCurrentState()
    }

    fun toggleCurveEditor() {
        val willOpen = !_uiState.value.isCurveEditorOpen
        _uiState.value = _uiState.value.copy(isCurveEditorOpen = willOpen)
        if (willOpen) {
            updateActiveCurveForCurrentState()
        }
    }

    fun setActiveTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tabIndex)
    }

    fun toggleLayerVisibility(layerId: String) {
        val updated = _uiState.value.composition.layers.map {
            if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
        }
        _uiState.value = _uiState.value.copy(
            composition = _uiState.value.composition.copy(layers = updated)
        )
    }

    fun toggleLayerLock(layerId: String) {
        val updated = _uiState.value.composition.layers.map {
            if (it.id == layerId) it.copy(isLocked = !it.isLocked) else it
        }
        _uiState.value = _uiState.value.copy(
            composition = _uiState.value.composition.copy(layers = updated)
        )
    }

    fun moveSelectedLayer(dx: Float, dy: Float) {
        val selectedId = _uiState.value.selectedLayerId ?: return
        val timeMs = _uiState.value.currentTimeMs
        val updated = _uiState.value.composition.layers.map { layer ->
            if (layer.id == selectedId && !layer.isLocked) {
                val currentX = layer.posX.evaluate(timeMs)
                val currentY = layer.posY.evaluate(timeMs)
                val newX = currentX + dx
                val newY = currentY + dy
                layer.copy(
                    posX = layer.posX.withKeyframe(timeMs, newX),
                    posY = layer.posY.withKeyframe(timeMs, newY)
                )
            } else layer
        }
        _uiState.value = _uiState.value.copy(
            composition = _uiState.value.composition.copy(layers = updated)
        )
    }

    fun updatePropertyValue(property: AnimatedPropertyType, value: Float) {
        val selectedId = _uiState.value.selectedLayerId ?: return
        val timeMs = _uiState.value.currentTimeMs
        val curve = _uiState.value.currentCurve

        val updated = _uiState.value.composition.layers.map { layer ->
            if (layer.id == selectedId) {
                when (property) {
                    AnimatedPropertyType.SCALE -> layer.copy(scale = layer.scale.withKeyframe(timeMs, value, curve))
                    AnimatedPropertyType.ROTATION -> layer.copy(rotation = layer.rotation.withKeyframe(timeMs, value, curve))
                    AnimatedPropertyType.POS_X -> layer.copy(posX = layer.posX.withKeyframe(timeMs, value, curve))
                    AnimatedPropertyType.POS_Y -> layer.copy(posY = layer.posY.withKeyframe(timeMs, value, curve))
                    AnimatedPropertyType.OPACITY -> layer.copy(opacity = layer.opacity.withKeyframe(timeMs, value, curve))
                    AnimatedPropertyType.CHROMATIC_ABERRATION -> layer.copy(
                        effects = layer.effects.copy(
                            chromaticAberration = layer.effects.chromaticAberration.withKeyframe(timeMs, value, curve)
                        )
                    )
                    AnimatedPropertyType.MOTION_BLUR -> layer.copy(
                        effects = layer.effects.copy(
                            motionBlur = layer.effects.motionBlur.withKeyframe(timeMs, value, curve)
                        )
                    )
                    AnimatedPropertyType.LENS_DISTORTION -> layer.copy(
                        effects = layer.effects.copy(
                            lensDistortion = layer.effects.lensDistortion.withKeyframe(timeMs, value, curve)
                        )
                    )
                }
            } else layer
        }

        _uiState.value = _uiState.value.copy(
            composition = _uiState.value.composition.copy(layers = updated)
        )
    }

    fun toggleKeyframeOnCurrent() {
        val selectedId = _uiState.value.selectedLayerId ?: return
        val timeMs = _uiState.value.currentTimeMs
        val prop = _uiState.value.selectedProperty
        val curve = _uiState.value.currentCurve

        val updated = _uiState.value.composition.layers.map { layer ->
            if (layer.id == selectedId) {
                val anim = getAnimatable(layer, prop)
                val hasKey = anim.hasKeyframeAt(timeMs)
                val newAnim = if (hasKey) {
                    anim.withoutKeyframe(timeMs)
                } else {
                    val currentVal = anim.evaluate(timeMs)
                    anim.withKeyframe(timeMs, currentVal, curve)
                }
                setAnimatable(layer, prop, newAnim)
            } else layer
        }

        _uiState.value = _uiState.value.copy(
            composition = _uiState.value.composition.copy(layers = updated)
        )
        updateActiveCurveForCurrentState()
    }

    fun updateCurve(curve: CubicBezierCurve) {
        val selectedId = _uiState.value.selectedLayerId ?: return
        val timeMs = _uiState.value.currentTimeMs
        val prop = _uiState.value.selectedProperty

        val updated = _uiState.value.composition.layers.map { layer ->
            if (layer.id == selectedId) {
                val anim = getAnimatable(layer, prop)
                val updatedAnim = anim.updateEasingAt(timeMs, curve)
                setAnimatable(layer, prop, updatedAnim)
            } else layer
        }

        _uiState.value = _uiState.value.copy(
            composition = _uiState.value.composition.copy(layers = updated),
            currentCurve = curve
        )
    }

    fun applyPreset(presetName: String) {
        val selectedId = _uiState.value.selectedLayerId ?: return
        val updated = _uiState.value.composition.layers.map { layer ->
            if (layer.id == selectedId) {
                when (presetName) {
                    "PUNCH_IN" -> {
                        layer.copy(
                            scale = layer.scale
                                .withKeyframe(0L, 0.1f, CubicBezierCurve.Overshoot)
                                .withKeyframe(600L, 1.4f, CubicBezierCurve.SmoothPunch)
                                .withKeyframe(1200L, 1.0f, CubicBezierCurve.EaseOut),
                            effects = layer.effects.copy(
                                chromaticAberration = layer.effects.chromaticAberration
                                    .withKeyframe(0L, 20f, CubicBezierCurve.EaseOut)
                                    .withKeyframe(800L, 0f, CubicBezierCurve.EaseIn)
                            )
                        )
                    }
                    "WHIP_SPIN" -> {
                        layer.copy(
                            rotation = layer.rotation
                                .withKeyframe(0L, 0f, CubicBezierCurve.DramaticEase)
                                .withKeyframe(1000L, 360f, CubicBezierCurve.EaseOut),
                            effects = layer.effects.copy(
                                motionBlur = layer.effects.motionBlur
                                    .withKeyframe(0L, 12f, CubicBezierCurve.DramaticEase)
                                    .withKeyframe(1000L, 0f, CubicBezierCurve.EaseOut)
                            )
                        )
                    }
                    "GLITCH_HOP" -> {
                        layer.copy(
                            posX = layer.posX
                                .withKeyframe(0L, 0f, CubicBezierCurve.Linear)
                                .withKeyframe(300L, -40f, CubicBezierCurve.Overshoot)
                                .withKeyframe(600L, 35f, CubicBezierCurve.Overshoot)
                                .withKeyframe(900L, 0f, CubicBezierCurve.EaseOut),
                            effects = layer.effects.copy(
                                lensDistortion = layer.effects.lensDistortion
                                    .withKeyframe(0L, 0.5f, CubicBezierCurve.EaseInOut)
                                    .withKeyframe(600L, -0.4f, CubicBezierCurve.EaseInOut)
                                    .withKeyframe(1200L, 0f, CubicBezierCurve.EaseOut)
                            )
                        )
                    }
                    else -> layer
                }
            } else layer
        }
        _uiState.value = _uiState.value.copy(
            composition = _uiState.value.composition.copy(layers = updated)
        )
    }

    private fun updateActiveCurveForCurrentState() {
        val selectedId = _uiState.value.selectedLayerId ?: return
        val selected = _uiState.value.composition.layers.find { it.id == selectedId } ?: return
        val anim = getAnimatable(selected, _uiState.value.selectedProperty)
        val kf = anim.findKeyframeAt(_uiState.value.currentTimeMs)
        if (kf != null) {
            _uiState.value = _uiState.value.copy(currentCurve = kf.easing)
        }
    }

    private fun getAnimatable(layer: Layer, prop: AnimatedPropertyType) = when (prop) {
        AnimatedPropertyType.SCALE -> layer.scale
        AnimatedPropertyType.ROTATION -> layer.rotation
        AnimatedPropertyType.POS_X -> layer.posX
        AnimatedPropertyType.POS_Y -> layer.posY
        AnimatedPropertyType.OPACITY -> layer.opacity
        AnimatedPropertyType.CHROMATIC_ABERRATION -> layer.effects.chromaticAberration
        AnimatedPropertyType.MOTION_BLUR -> layer.effects.motionBlur
        AnimatedPropertyType.LENS_DISTORTION -> layer.effects.lensDistortion
    }

    private fun setAnimatable(layer: Layer, prop: AnimatedPropertyType, anim: com.example.engine.model.AnimatedFloat) = when (prop) {
        AnimatedPropertyType.SCALE -> layer.copy(scale = anim)
        AnimatedPropertyType.ROTATION -> layer.copy(rotation = anim)
        AnimatedPropertyType.POS_X -> layer.copy(posX = anim)
        AnimatedPropertyType.POS_Y -> layer.copy(posY = anim)
        AnimatedPropertyType.OPACITY -> layer.copy(opacity = anim)
        AnimatedPropertyType.CHROMATIC_ABERRATION -> layer.copy(effects = layer.effects.copy(chromaticAberration = anim))
        AnimatedPropertyType.MOTION_BLUR -> layer.copy(effects = layer.effects.copy(motionBlur = anim))
        AnimatedPropertyType.LENS_DISTORTION -> layer.copy(effects = layer.effects.copy(lensDistortion = anim))
    }
}
