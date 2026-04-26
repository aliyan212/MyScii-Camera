package com.fossift.asciicam.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.fossift.asciicam.camera.AsciiFrameAnalyzer
import com.fossift.asciicam.camera.CameraAsciiController
import com.fossift.asciicam.engine.AsciiCharset
import com.fossift.asciicam.engine.AsciiConfig
import com.fossift.asciicam.engine.AsciiConverter
import com.fossift.asciicam.engine.AsciiFrame
import com.fossift.asciicam.storage.AsciiCaptureStore
import com.fossift.asciicam.storage.CaptureRecord
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class RenderPreset {
    FACE,
    SCENE,
}

private enum class AppScreen {
    CAMERA,
    GALLERY,
}

private enum class ColorMode {
    MONO,
    WARM,
    COOL,
    NEON,
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AsciiCameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val captureStore = remember { AsciiCaptureStore(context) }
    val scope = rememberCoroutineScope()

    var contrast by remember { mutableFloatStateOf(1.2f) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var showCameraBackground by remember { mutableStateOf(false) }
    var renderPreset by remember { mutableStateOf(RenderPreset.FACE) }
    var colorMode by remember { mutableStateOf(ColorMode.COOL) }
    var frameMs by remember { mutableStateOf(0.0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var screenMode by remember { mutableStateOf(AppScreen.CAMERA) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var overlayView by remember { mutableStateOf<AsciiOverlayView?>(null) }
    var recentCaptures by remember { mutableStateOf<List<CaptureRecord>>(emptyList()) }
    var showOptionsSheet by remember { mutableStateOf(false) }

    val fixedGridWidth = AsciiUiDefaults.baseWidth
    val fixedGridHeight = AsciiUiDefaults.baseHeight

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

    val analyzer = remember(converter, useFrontCamera, contrast, renderPreset) {
        AsciiFrameAnalyzer(
            converter = converter,
            configProvider = {
                AsciiConfig(
                    width = fixedGridWidth,
                    height = fixedGridHeight,
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
            maxAnalysisFps = 20,
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
                        if (now - last >= 250L) {
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

    LaunchedEffect(renderPreset) {
        if (renderPreset == RenderPreset.FACE) {
            useFrontCamera = true
        }
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

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF05070A), Color(0xFF090D12), Color(0xFF0C1118)),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
    ) {
        if (screenMode == AppScreen.CAMERA) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 10.dp, start = 12.dp, end = 12.dp)
                    .background(Color(0xCC090C12), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0x55324A62), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "MySCII Camera",
                    color = Color(0xFFEFF4FB),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${"%.1f".format(frameMs)} ms",
                    color = Color(0xFF9FB0C4),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            CameraTab(
                modifier = Modifier.fillMaxSize(),
                hasCameraPermission = hasCameraPermission,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                asciiColor = asciiToneColor(colorMode),
                colorMode = colorMode,
                showCameraBackground = showCameraBackground,
                onPreviewReady = { previewView = it },
                onOverlayReady = { view ->
                    overlayView = view
                },
            )

            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .background(Color(0xAA0A0D11), RoundedCornerShape(22.dp))
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            ) {
                val actionWidth = maxWidth / 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            recentCaptures = captureStore.listRecent(limit = 60)
                            screenMode = AppScreen.GALLERY
                        },
                        modifier = Modifier
                            .width(actionWidth)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF11161D)),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("Gallery")
                    }

                    Box(
                        modifier = Modifier
                            .width(actionWidth)
                            .height(84.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(
                            onClick = {
                                val snapshot = latestFrame.get()?.asText()
                                if (snapshot == null) {
                                    statusMessage = "No frame yet. Try again in a moment."
                                    return@Button
                                }
                                // Read from PreviewView on UI thread, then save on IO.
                                val originalPreviewBitmap = previewView?.bitmap?.let { Bitmap.createBitmap(it) }
                                statusMessage = "Saving capture..."
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
                            modifier = Modifier.size(78.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE7E9EF)),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .border(2.dp, Color(0xFF0A0D11), androidx.compose.foundation.shape.CircleShape)
                                    .background(Color.Transparent, androidx.compose.foundation.shape.CircleShape),
                            )
                        }
                    }

                    Button(
                        onClick = { showOptionsSheet = true },
                        modifier = Modifier
                            .width(actionWidth)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF11161D)),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("Menu")
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
            )
        }

        statusMessage?.let {
            Text(
                text = it,
                color = Color(0xFFE7EEF7),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 54.dp, start = 12.dp, end = 12.dp)
                    .fillMaxWidth()
                    .background(Color(0xAA0C1118), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }

    if (showOptionsSheet && screenMode == AppScreen.CAMERA) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            containerColor = Color(0xFF05070A),
            contentColor = Color(0xFFF0F3F8),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "MySCII Settings",
                    color = Color(0xFFF0F3F8),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Frame ${"%.1f".format(frameMs)} ms",
                    color = Color(0xFFB8C2CF),
                    style = MaterialTheme.typography.bodySmall,
                )
                CameraControlsPanel(
                    useFrontCamera = useFrontCamera,
                    onSwitchCamera = { useFrontCamera = !useFrontCamera },
                    showCameraBackground = showCameraBackground,
                    onToggleAsciiOnly = { showCameraBackground = !showCameraBackground },
                    renderPreset = renderPreset,
                    onFacePreset = { renderPreset = RenderPreset.FACE },
                    onScenePreset = { renderPreset = RenderPreset.SCENE },
                    colorMode = colorMode,
                    onColorModeChange = { colorMode = it },
                    contrast = contrast,
                    onContrastChange = { contrast = it },
                    gridWidth = fixedGridWidth,
                    gridHeight = fixedGridHeight,
                )
                Spacer(modifier = Modifier.height(16.dp))
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
            .background(Color(0xFF040608)),
    ) {
        if (!hasCameraPermission) {
            PermissionGate(onGrant = onRequestPermission)
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    PreviewView(it).also { view ->
                        view.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        view.scaleType = PreviewView.ScaleType.FILL_CENTER
                        view.alpha = 0f
                        onPreviewReady(view)
                    }
                },
            )
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (showCameraBackground) Color(0xA00B0F16) else Color(0xFF040608))
                    .padding(8.dp),
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
private fun CameraControlsPanel(
    useFrontCamera: Boolean,
    onSwitchCamera: () -> Unit,
    showCameraBackground: Boolean,
    onToggleAsciiOnly: () -> Unit,
    renderPreset: RenderPreset,
    onFacePreset: () -> Unit,
    onScenePreset: () -> Unit,
    colorMode: ColorMode,
    onColorModeChange: (ColorMode) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    gridWidth: Int,
    gridHeight: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onSwitchCamera,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF11161D)),
            ) {
                Text(if (useFrontCamera) "Front cam" else "Back cam")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onFacePreset,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (renderPreset == RenderPreset.FACE) Color(0xFF213A34) else Color(0xFF11161D)),
            ) {
                Text("Face")
            }
            Button(
                onClick = onScenePreset,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (renderPreset == RenderPreset.SCENE) Color(0xFF2E3B3F) else Color(0xFF11161D)),
            ) {
                Text("Scene")
            }
            Button(
                onClick = onToggleAsciiOnly,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1512)),
            ) {
                Text(if (showCameraBackground) "Blend" else "ASCII only")
            }
        }

        Text("Color mode", color = Color(0xFFD4D9E2), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onColorModeChange(ColorMode.MONO) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (colorMode == ColorMode.MONO) Color(0xFF1D2730) else Color(0xFF11161D)),
            ) { Text("Mono") }
            Button(
                onClick = { onColorModeChange(ColorMode.WARM) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (colorMode == ColorMode.WARM) Color(0xFF382818) else Color(0xFF11161D)),
            ) { Text("Warm") }
            Button(
                onClick = { onColorModeChange(ColorMode.COOL) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (colorMode == ColorMode.COOL) Color(0xFF18393D) else Color(0xFF11161D)),
            ) { Text("Cool") }
            Button(
                onClick = { onColorModeChange(ColorMode.NEON) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (colorMode == ColorMode.NEON) Color(0xFF13211A) else Color(0xFF11161D)),
            ) { Text("Neon") }
        }

        Text("Contrast", color = Color(0xFFD4D9E2), style = MaterialTheme.typography.labelLarge)
        Slider(value = contrast, onValueChange = onContrastChange, valueRange = 0.7f..2.0f)

        Text(
            text = "Grid ${gridWidth}x${gridHeight}",
            color = Color(0xFF8794A5),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PhotoGalleryTab(
    modifier: Modifier = Modifier,
    captures: List<CaptureRecord>,
    onRefresh: () -> Unit,
    onExportCapture: (CaptureRecord) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x5520120B)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MySCII Gallery",
                        color = Color(0xFFFFF4E5),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${captures.size} photos saved",
                        color = Color(0xFFDEC9B8),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }

            if (captures.isEmpty()) {
                EmptyGalleryState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(captures) { record ->
                        PhotoCard(record = record, onExport = { onExportCapture(record) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGalleryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x332A1E16), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0x554A382D), RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "No photos yet",
            color = Color(0xFFF4E6D8),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Capture a frame in MySCII Camera and it will appear here.",
            color = Color(0xFFD3C0AF),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhotoCard(record: CaptureRecord, onExport: () -> Unit) {
    val context = LocalContext.current
    val captureFile = remember(record.pngFileName) { File(File(context.filesDir, "captures"), record.pngFileName) }
    val previewBitmap = remember(captureFile.absolutePath) {
        if (captureFile.exists()) {
            runCatching { BitmapFactory.decodeFile(captureFile.absolutePath) }.getOrNull()
        } else {
            null
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x662A1A11)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExport),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .background(Color(0xFF120E0A), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x55496D7E), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Saved ASCII photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                    )
                } else {
                    Text(
                        text = "Photo preview unavailable",
                        color = Color(0xFFBDAFA2),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
            Text(
                text = prettyTimestamp(record.timestampUtc),
                color = Color(0xFFDABDA6),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A5E3A)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Export photo")
            }
        }
    }
}

@Composable
private fun PermissionGate(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Camera permission required", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Allow camera access to start the live MySCII renderer.",
            color = Color(0xFFB8C9D8),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(onClick = onGrant) {
            Text("Grant permission")
        }
    }
}

private fun prettyTimestamp(raw: String): String {
    return raw.replace('T', ' ').removeSuffix("Z").take(19)
}

private fun asciiToneColor(colorMode: ColorMode): Color {
    return when (colorMode) {
        ColorMode.MONO -> Color(0xFFDCE5EF)
        ColorMode.WARM -> Color(0xFFFFD5A8)
        ColorMode.COOL -> Color(0xFFCBF7FF)
        ColorMode.NEON -> Color(0xFFEAFBFF)
    }
}

private fun sharePngFile(context: android.content.Context, pngFile: File, onStatus: (String) -> Unit) {
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
        putExtra(Intent.EXTRA_TEXT, "ASCII render PNG")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    onStatus("Ready to export: ${pngFile.name}")
    context.startActivity(Intent.createChooser(shareIntent, "Export photo"))
}
