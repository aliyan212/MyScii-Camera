package com.fossift.asciicam.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.fossift.asciicam.R
import com.fossift.asciicam.camera.AsciiFrameAnalyzer
import com.fossift.asciicam.camera.CameraAsciiController
import com.fossift.asciicam.engine.AsciiCharset
import com.fossift.asciicam.engine.AsciiConfig
import com.fossift.asciicam.engine.AsciiConverter
import com.fossift.asciicam.engine.AsciiFrame
import com.fossift.asciicam.storage.AsciiCaptureStore
import com.fossift.asciicam.storage.CaptureRecord
import com.fossift.asciicam.ui.theme.AsciiCool
import com.fossift.asciicam.ui.theme.AsciiMono
import com.fossift.asciicam.ui.theme.AsciiNeon
import com.fossift.asciicam.ui.theme.AsciiWarm
import com.fossift.asciicam.ui.theme.CyberCyan
import com.fossift.asciicam.ui.theme.DarkBackground
import com.fossift.asciicam.ui.theme.DarkOutline
import com.fossift.asciicam.ui.theme.DarkSurface
import com.fossift.asciicam.ui.theme.DarkSurfaceElevated
import com.fossift.asciicam.ui.theme.NeonMint
import com.fossift.asciicam.ui.theme.TextMuted
import com.fossift.asciicam.ui.theme.TextPrimary
import com.fossift.asciicam.ui.theme.TextSecondary
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RenderPreset {
    FACE,
    SCENE,
}

enum class AppScreen {
    CAMERA,
    GALLERY,
}

enum class ColorMode {
    MONO,
    WARM,
    COOL,
    NEON,
}

enum class GridDensity(val label: String, val width: Int, val height: Int) {
    COMPACT("Compact (96x54)", 96, 54),
    STANDARD("Standard (128x72)", 128, 72),
    FINE("Fine (160x90)", 160, 90),
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AsciiCameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val captureStore = remember { AsciiCaptureStore(context) }
    val scope = rememberCoroutineScope()

    var contrast by remember { mutableFloatStateOf(1.2f) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var showCameraBackground by remember { mutableStateOf(false) }
    var renderPreset by remember { mutableStateOf(RenderPreset.FACE) }
    var colorMode by remember { mutableStateOf(ColorMode.COOL) }
    var gridDensity by remember { mutableStateOf(GridDensity.STANDARD) }
    var frameMs by remember { mutableStateOf(0.0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var screenMode by remember { mutableStateOf(AppScreen.CAMERA) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var overlayView by remember { mutableStateOf<AsciiOverlayView?>(null) }
    var recentCaptures by remember { mutableStateOf<List<CaptureRecord>>(emptyList()) }
    var showOptionsSheet by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    val converter = remember { AsciiConverter() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val uiDispatchScheduled = remember { AtomicBoolean(false) }
    val pendingFrame = remember { AtomicReference<AsciiFrame?>(null) }
    val latestFrame = remember { AtomicReference<AsciiFrame?>(null) }
    val pendingFrameMs = remember { AtomicReference(0.0) }
    val lastFrameMetricUpdateMs = remember { AtomicReference(0L) }

    val analyzer = remember(converter, useFrontCamera, contrast, renderPreset, gridDensity) {
        AsciiFrameAnalyzer(
            converter = converter,
            configProvider = {
                AsciiConfig(
                    width = gridDensity.width,
                    height = gridDensity.height,
                    contrast = if (renderPreset == RenderPreset.FACE) contrast else (contrast * 0.95f).coerceIn(0.7f, 2.0f),
                    gamma = if (renderPreset == RenderPreset.FACE) 0.92f else 1.0f,
                    charset = if (renderPreset == RenderPreset.FACE) AsciiCharset.PORTRAIT else AsciiCharset.CLASSIC,
                    temporalSmoothing = if (renderPreset == RenderPreset.FACE) 0.14f else 0.16f,
                    edgeEnhancement = if (renderPreset == RenderPreset.FACE) 0.24f else 0.18f,
                    toneClipLowPercent = 0.0f,
                    toneClipHighPercent = 0.0f,
                    centerFocusBoost = if (renderPreset == RenderPreset.FACE) 0.22f else 0f,
                    centerFocusRadius = if (renderPreset == RenderPreset.FACE) 0.64f else 0.9f,
                )
            },
            mirrorHorizontallyProvider = { useFrontCamera },
            maxAnalysisFps = 25,
            onAsciiFrame = { nextFrame, processingMs ->
                pendingFrame.set(nextFrame)
                pendingFrameMs.set(processingMs)

                if (uiDispatchScheduled.compareAndSet(false, true)) {
                    mainHandler.post {
                        pendingFrame.get()?.let {
                            latestFrame.set(it)
                            overlayView?.setAsciiFrame(it)
                        }
                        val now = SystemClock.uptimeMillis()
                        val last = lastFrameMetricUpdateMs.get()
                        if (now - last >= 200L) {
                            frameMs = pendingFrameMs.get()
                            lastFrameMetricUpdateMs.set(now)
                        }
                        uiDispatchScheduled.set(false)
                    }
                }
            },
        )
    }

    DisposableEffect(previewView, lifecycleOwner, useFrontCamera, hasCameraPermission, screenMode) {
        val view = previewView
        if (screenMode != AppScreen.CAMERA || view == null || !hasCameraPermission) {
            onDispose { }
        } else {
            val selector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            val controller = CameraAsciiController(context, lifecycleOwner, view, selector)
            controller.bind(analyzer) {
                statusMessage = "Camera error: ${it.message ?: "unknown"}"
            }
            onDispose {
                controller.close()
            }
        }
    }

    LaunchedEffect(Unit) {
        recentCaptures = captureStore.listRecent(limit = 60)
    }

    LaunchedEffect(statusMessage) {
        val message = statusMessage ?: return@LaunchedEffect
        val timeoutMs = if (message.contains("Saving", ignoreCase = true)) 3000L else 2200L
        delay(timeoutMs)
        if (statusMessage == message) {
            statusMessage = null
        }
    }

    BackHandler(enabled = screenMode == AppScreen.GALLERY) {
        screenMode = AppScreen.CAMERA
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        if (screenMode == AppScreen.CAMERA) {
            CameraTab(
                modifier = Modifier.fillMaxSize(),
                hasCameraPermission = hasCameraPermission,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                asciiColor = asciiToneColor(colorMode),
                colorMode = colorMode,
                showCameraBackground = showCameraBackground,
                onPreviewReady = { previewView = it },
                onOverlayReady = { view -> overlayView = view },
            )

            // Top Glassmorphic HUD with Material 3 App Brand Logo
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Brand pill
                Row(
                    modifier = Modifier
                        .background(Color(0xEE0D121B), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF28384D), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_myscii_logo),
                        contentDescription = "MySCII Logo",
                        modifier = Modifier.size(24.dp),
                    )
                    Column {
                        Text(
                            text = "MySCII",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        val fps = if (frameMs > 0.0) (1000.0 / frameMs).coerceAtMost(60.0) else 0.0
                        Text(
                            text = "${"%.1f".format(frameMs)} ms • ${"%.0f".format(fps)} fps",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                        )
                    }
                }

                // Quick Action: Switch Camera button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        useFrontCamera = !useFrontCamera
                    },
                    modifier = Modifier
                        .background(Color(0xCC0D121B), RoundedCornerShape(14.dp))
                        .border(1.dp, DarkOutline, RoundedCornerShape(14.dp))
                        .size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = TextPrimary,
                    )
                }
            }

            // Bottom Navigation & Shutter Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .background(Color(0xCC0D121B), RoundedCornerShape(26.dp))
                    .border(1.dp, Color(0x3328384D), RoundedCornerShape(26.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Gallery Button
                    IconButton(
                        onClick = {
                            recentCaptures = captureStore.listRecent(limit = 60)
                            screenMode = AppScreen.GALLERY
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(DarkSurfaceElevated, RoundedCornerShape(16.dp))
                            .border(1.dp, DarkOutline, RoundedCornerShape(16.dp)),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    // Tactile Material 3 Shutter Button
                    val shutterInteraction = remember { MutableInteractionSource() }
                    val isShutterPressed by shutterInteraction.collectIsPressedAsState()
                    val shutterScale by animateFloatAsState(
                        targetValue = if (isShutterPressed) 0.92f else 1f,
                        animationSpec = tween(100),
                        label = "Shutter scale",
                    )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(shutterScale)
                            .clip(CircleShape)
                            .background(Color(0xFF161E2C))
                            .border(3.dp, Color(0xFFF1F5F9), CircleShape)
                            .clickable(
                                interactionSource = shutterInteraction,
                                indication = null,
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val snapshot = latestFrame.get()?.asText()
                                if (snapshot == null) {
                                    statusMessage = "No frame yet. Try again in a moment."
                                    return@clickable
                                }
                                val originalPreviewBitmap = previewView?.bitmap?.let { Bitmap.createBitmap(it) }
                                statusMessage = "Saving capture…"
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching {
                                            captureStore.save(snapshot, originalBitmap = originalPreviewBitmap)
                                        }
                                    }
                                    result.onSuccess { saved ->
                                        statusMessage = "Saved: ${saved.pngPath.substringAfterLast('/')}"
                                        recentCaptures = captureStore.listRecent(limit = 60)
                                    }.onFailure { err ->
                                        statusMessage = "Save failed: ${err.message ?: "unknown"}"
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .border(2.dp, Color(0xFF0F141F), CircleShape),
                        )
                    }

                    // Settings / Menu Button
                    IconButton(
                        onClick = { showOptionsSheet = true },
                        modifier = Modifier
                            .size(54.dp)
                            .background(DarkSurfaceElevated, RoundedCornerShape(16.dp))
                            .border(1.dp, DarkOutline, RoundedCornerShape(16.dp)),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        } else {
            AsciiGalleryScreen(
                modifier = Modifier.fillMaxSize(),
                captures = recentCaptures,
                onBackToCamera = { screenMode = AppScreen.CAMERA },
                onRefresh = {
                    recentCaptures = captureStore.listRecent(limit = 60)
                    statusMessage = "Gallery refreshed"
                },
                onDeleteCapture = { record ->
                    scope.launch {
                        val deleted = withContext(Dispatchers.IO) {
                            captureStore.deleteCapture(record)
                        }
                        if (deleted) {
                            recentCaptures = captureStore.listRecent(limit = 60)
                            statusMessage = "Capture deleted"
                        } else {
                            statusMessage = "Delete failed: file missing"
                        }
                    }
                },
                onExportCapture = { capture ->
                    val pngFile = captureStore.resolveCapturePngFile(capture)
                    if (pngFile == null) {
                        statusMessage = "Export failed: file missing"
                    } else {
                        sharePngFile(context, pngFile) { status ->
                            statusMessage = status
                        }
                    }
                },
                onCopyAscii = { record ->
                    val text = captureStore.readCaptureText(record)
                    if (text != null) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("MySCII ASCII", text)
                        clipboard.setPrimaryClip(clip)
                        statusMessage = "ASCII text copied to clipboard"
                    } else {
                        statusMessage = "Text file not found"
                    }
                },
            )
        }

        // Animated Status Toast Banner
        AnimatedVisibility(
            visible = statusMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp, start = 16.dp, end = 16.dp),
        ) {
            statusMessage?.let { msg ->
                Text(
                    text = msg,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .background(Color(0xF0101824), RoundedCornerShape(12.dp))
                        .border(1.dp, CyberCyan, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }

    // Material 3 Settings Bottom Sheet
    if (showOptionsSheet && screenMode == AppScreen.CAMERA) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            containerColor = DarkSurface,
            contentColor = TextPrimary,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${gridDensity.width}x${gridDensity.height}",
                        color = CyberCyan,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                CameraControlsPanel(
                    useFrontCamera = useFrontCamera,
                    onSwitchCamera = { useFrontCamera = !useFrontCamera },
                    showCameraBackground = showCameraBackground,
                    onToggleAsciiOnly = { showCameraBackground = !showCameraBackground },
                    renderPreset = renderPreset,
                    onPresetChange = { renderPreset = it },
                    gridDensity = gridDensity,
                    onDensityChange = { gridDensity = it },
                    colorMode = colorMode,
                    onColorModeChange = { colorMode = it },
                    contrast = contrast,
                    onContrastChange = { contrast = it },
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun CameraTab(
    modifier: Modifier = Modifier,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    asciiColor: Color,
    colorMode: ColorMode,
    showCameraBackground: Boolean,
    onPreviewReady: (PreviewView) -> Unit,
    onOverlayReady: (AsciiOverlayView) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        if (!hasCameraPermission) {
            PermissionGate(onGrant = onRequestPermission)
        } else {
            // Live CameraX Preview - Set to COMPATIBLE (TextureView) for snapshot capture and dynamic alpha for blending
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PreviewView(context).also { view ->
                        view.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        view.scaleType = PreviewView.ScaleType.FILL_CENTER
                        view.alpha = if (showCameraBackground) 1f else 0f
                        onPreviewReady(view)
                    }
                },
                update = { view ->
                    view.alpha = if (showCameraBackground) 1f else 0f
                },
            )

            // Dynamic ASCII Overlay View
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (showCameraBackground) Color(0x66070A0F) else DarkBackground),
                factory = { context ->
                    AsciiOverlayView(context).also { asciiView ->
                        asciiView.setAsciiColorInt(asciiColor.toArgb())
                        asciiView.setNeonMode(colorMode == ColorMode.NEON)
                        onOverlayReady(asciiView)
                    }
                },
                update = { asciiView ->
                    asciiView.setAsciiColorInt(asciiColor.toArgb())
                    asciiView.setNeonMode(colorMode == ColorMode.NEON)
                },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CameraControlsPanel(
    useFrontCamera: Boolean,
    onSwitchCamera: () -> Unit,
    showCameraBackground: Boolean,
    onToggleAsciiOnly: () -> Unit,
    renderPreset: RenderPreset,
    onPresetChange: (RenderPreset) -> Unit,
    gridDensity: GridDensity,
    onDensityChange: (GridDensity) -> Unit,
    colorMode: ColorMode,
    onColorModeChange: (ColorMode) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Preset and Background Mode Row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = renderPreset == RenderPreset.FACE,
                onClick = { onPresetChange(RenderPreset.FACE) },
                label = { Text("Portrait") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF004F58),
                    selectedLabelColor = CyberCyan,
                ),
            )
            FilterChip(
                selected = renderPreset == RenderPreset.SCENE,
                onClick = { onPresetChange(RenderPreset.SCENE) },
                label = { Text("Scene") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF004F58),
                    selectedLabelColor = CyberCyan,
                ),
            )
            FilterChip(
                selected = showCameraBackground,
                onClick = onToggleAsciiOnly,
                label = { Text(if (showCameraBackground) "Blend" else "ASCII Only") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF005237),
                    selectedLabelColor = NeonMint,
                ),
            )
        }

        // Density / Resolution Selector
        Text("Resolution Density", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            GridDensity.entries.forEach { density ->
                FilterChip(
                    selected = gridDensity == density,
                    onClick = { onDensityChange(density) },
                    label = { Text(density.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1E2B3C),
                        selectedLabelColor = CyberCyan,
                    ),
                )
            }
        }

        // Color Mode Row
        Text("Color Palette", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ColorMode.entries.forEach { mode ->
                FilterChip(
                    selected = colorMode == mode,
                    onClick = { onColorModeChange(mode) },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (mode) {
                            ColorMode.MONO -> Color(0xFF243040)
                            ColorMode.WARM -> Color(0xFF422C14)
                            ColorMode.COOL -> Color(0xFF0C3844)
                            ColorMode.NEON -> Color(0xFF143026)
                        },
                        selectedLabelColor = when (mode) {
                            ColorMode.MONO -> AsciiMono
                            ColorMode.WARM -> AsciiWarm
                            ColorMode.COOL -> AsciiCool
                            ColorMode.NEON -> NeonMint
                        },
                    ),
                )
            }
        }

        // Contrast Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Contrast", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text("${"%.2f".format(contrast)}x", color = CyberCyan, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = contrast,
            onValueChange = onContrastChange,
            valueRange = 0.7f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = CyberCyan,
                activeTrackColor = CyberCyan,
                inactiveTrackColor = Color(0xFF1A2636),
            ),
        )
    }
}

@Composable
private fun PermissionGate(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_myscii_logo),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(id = R.string.camera_permission_required),
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(id = R.string.camera_permission_rationale),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkBackground),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(id = R.string.grant_permission), fontWeight = FontWeight.Bold)
        }
    }
}

private fun asciiToneColor(colorMode: ColorMode): Color {
    return when (colorMode) {
        ColorMode.MONO -> AsciiMono
        ColorMode.WARM -> AsciiWarm
        ColorMode.COOL -> AsciiCool
        ColorMode.NEON -> AsciiNeon
    }
}

private fun sharePngFile(context: Context, pngFile: File, onStatus: (String) -> Unit) {
    if (!pngFile.exists()) {
        onStatus("PNG export failed: file missing")
        return
    }

    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pngFile)
    }.getOrElse {
        onStatus("PNG export failed: ${it.message ?: "provider error"}")
        return
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "ASCII render PNG from MySCII Camera")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    onStatus("Ready to export: ${pngFile.name}")
    context.startActivity(Intent.createChooser(shareIntent, "Export photo"))
}
