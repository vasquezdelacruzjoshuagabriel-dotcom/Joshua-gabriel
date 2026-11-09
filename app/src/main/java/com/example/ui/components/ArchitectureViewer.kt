package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visor Técnico y de Arquitectura del Motor (Alight Motion Core).
 * Permite a los arquitectos e ingenieros inspeccionar el diagrama de capas,
 * la separación UI vs RenderThread, los shaders GLSL/MSL y las optimizaciones de GPU.
 */
@Composable
fun ArchitectureViewer(
    fps: Float,
    frameTimeMs: Float,
    activeLayerCount: Int,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pipeline & Arquitectura", "Shaders GLSL/GPU", "Zero-Copy MediaCodec", "Optimización Móvil")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F16))
            .padding(16.dp)
            .testTag("architecture_viewer_screen")
    ) {
        // Cabecera Técnica
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Arquitectura del Motor Móvil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Core Engine Spec • 60 FPS GPU Composition",
                    fontSize = 12.sp,
                    color = Color(0xFF00F2FE)
                )
            }

            // Badge de Telemetría en tiempo real
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF161A26))
                    .border(1.dp, Color(0x3300F2FE), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FF66))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${"%.1f".format(fps)} FPS (${"%.2f".format(frameTimeMs)} ms)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF00FF66)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF141724),
            contentColor = Color(0xFF00F2FE),
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedTab) {
                0 -> PipelineArchitectureTab(activeLayerCount)
                1 -> ShadersGpuTab()
                2 -> MediaCodecZeroCopyTab()
                3 -> MobileOptimizationTab()
            }
        }
    }
}

@Composable
private fun PipelineArchitectureTab(activeLayers: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ArchitectureCard(
            title = "1. Desacoplamiento UI Thread vs Render Thread",
            icon = Icons.Default.Speed,
            accentColor = Color(0xFF00F2FE)
        ) {
            Text(
                text = "Para garantizar 60 FPS constantes al hacer scrubbing del timeline en teléfonos móviles, la arquitectura separa estrictamente el hilo de UI del hilo de renderizado:",
                fontSize = 12.sp,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            CodeSnippet(
                """
UI Layer (Jetpack Compose / SwiftUI)
    │  • Captura gestos táctiles (Drag, Pinch) a 120Hz
    │  • Emite cambios de tiempo 't' via StateFlow sin recomponer vistas
    ▼
Timeline State Dispatcher (Lock-Free Command Queue)
    │  • Estructura inmutable: ProjectState(t)
    │  • Búsqueda binaria O(log N) de Keyframes
    │  • Evaluación Bézier Newton-Raphson (< 1.2 microsegundos)
    ▼
Engine Render Loop (RenderThread dedicado con OpenGL ES 3.0 / Vulkan / Metal)
    │  • SurfaceView / CAMetalLayer independiente del hilo principal
    │  • Render Pass 1..N a Framebuffer Objects (FBOs) con Shaders
    │  • SwapBuffers sincronizado con VSYNC (Choreographer)
                """.trimIndent()
            )
        }

        ArchitectureCard(
            title = "2. Árbol de Escena y Grafo de Composición (DAG)",
            icon = Icons.Default.Memory,
            accentColor = Color(0xFF9D4EDD)
        ) {
            Text(
                text = "Cada elemento en Alight Motion es un nodo en un Grafo Acíclico Dirigido (DAG). Capas activas en la escena actual: $activeLayers",
                fontSize = 12.sp,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "• Nodos de Transformada: Matriz 3x3 local acumulada por jerarquía de emparentado (Parenting Matrix).\n" +
                        "• Nodos de Efectos: Generan pases de posprocesamiento encadenados (FBO Ping-Pong).\n" +
                        "• Nodos de Contenido: Vector Paths cacheados en GPU VBOs o texturas externas OES de video.",
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ShadersGpuTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ArchitectureCard(
            title = "Pipeline de Fragment Shaders (GLSL / AGSL)",
            icon = Icons.Default.Code,
            accentColor = Color(0xFFFF0055)
        ) {
            Text(
                text = "1. Chromatic Aberration Shader (Separación espectral RGB):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF00F2FE)
            )
            CodeSnippet(
                """
// GLSL ES 3.0 Fragment Shader
precision mediump float;
uniform sampler2D uTexture;
uniform vec2 uOffset; // Vector de desplazamiento cromático
in vec2 vTexCoord;
out vec4 fragColor;

void main() {
    float r = texture(uTexture, vTexCoord - uOffset).r;
    float g = texture(uTexture, vTexCoord).g;
    float b = texture(uTexture, vTexCoord + uOffset).b;
    float a = texture(uTexture, vTexCoord).a;
    fragColor = vec4(r, g, b, a);
}
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "2. Directional Motion Blur (Muestreo ponderado Gaussiano):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFFFFB703)
            )
            CodeSnippet(
                """
// Motion Blur con Shutter Angle y Velocidad de Keyframe
uniform vec2 uVelocity; // Velocidad calculada dTransform/dt
const int SAMPLES = 7;

void main() {
    vec4 color = vec4(0.0);
    float totalWeight = 0.0;
    vec2 step = uVelocity / float(SAMPLES);
    for(int i = -3; i <= 3; i++) {
        float weight = exp(-float(i * i) / 4.5);
        color += texture(uTexture, vTexCoord + step * float(i)) * weight;
        totalWeight += weight;
    }
    fragColor = color / totalWeight;
}
                """.trimIndent()
            )
        }
    }
}

@Composable
private fun MediaCodecZeroCopyTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ArchitectureCard(
            title = "Decodificación Zero-Copy con MediaCodec / SurfaceTexture",
            icon = Icons.Default.VideoLibrary,
            accentColor = Color(0xFF4FACFE)
        ) {
            Text(
                text = "El mayor error de rendimiento en apps móviles es descargar los frames de video a la memoria RAM (CPU YUV->RGB). En su lugar se utiliza una arquitectura Zero-Copy:",
                fontSize = 12.sp,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "1. MediaCodec decodifica el stream H.264/H.265 directamente a una 'Surface' vinculada a un SurfaceTexture en GPU.\n" +
                        "2. En el RenderThread, se enlaza como textura 'GL_TEXTURE_EXTERNAL_OES'.\n" +
                        "3. Zero-Copy: El hardware de la GPU lee el frame directamente de los buffers de decodificación sin pasar nunca por la memoria de la CPU ni gastar ancho de banda de bus.\n" +
                        "4. Soporta hasta 3-4 capas 1080p simultáneas en procesadores móviles modernos (Snapdragon 7/8 series, Apple A-series).",
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun MobileOptimizationTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ArchitectureCard(
            title = "Estrategias Antitérmicas y Ahorro de Batería en Móviles",
            icon = Icons.Default.Speed,
            accentColor = Color(0xFF00FF66)
        ) {
            Text(
                text = "Para evitar el Thermal Throttling y caídas a 30 FPS tras 2 minutos de edición continua:",
                fontSize = 12.sp,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "• Dynamic Resolution Scaling: Renderizar la previsualización al 50% de resolución (540p / 720p) durante el scrubbing rápido del timeline y subir a 1080p full-res al detener el cursor.\n" +
                        "• Shader LOD: Desactivar Motion Blur multipass durante el drag interactivo del cursor del timeline y activarlo en la reproducción continua.\n" +
                        "• Frame Invalidation Inteligente: Solo redibujar cuando 't' cambie o cuando las transformaciones hayan sido modificadas (Dirty Flag checking).\n" +
                        "• Object Pooling: Reutilizar estructuras Keyframe, Matrix y FloatArrays para no disparar el Garbage Collector de Android en el bucle de renderizado.",
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ArchitectureCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0x25FFFFFF)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun CodeSnippet(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF08090E))
            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp,
            color = Color(0xFF00F2FE),
            lineHeight = 15.sp
        )
    }
}
