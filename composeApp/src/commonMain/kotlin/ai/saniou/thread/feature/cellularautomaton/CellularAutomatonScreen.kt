package ai.saniou.thread.feature.cellularautomaton

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellularAutomatonScreen() {
    val viewModel: CellularAutomatonViewModel = viewModel { CellularAutomatonViewModel() }
    val state by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("元胞自动机 (代: ${state.generation})") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            Row {
                FloatingActionButton(onClick = { viewModel.togglePlayPause() }) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "播放/暂停"
                    )
                }
                Spacer(Modifier.width(16.dp))
                FloatingActionButton(onClick = { viewModel.reset() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "重置")
                }
            }
        }
    ) { paddingValues ->
        val onBackgroundColor = MaterialTheme.colorScheme.onBackground
        
        // Zoom and Pan state
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(0.5f, 5f)
            offset += panChange
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformableState)
                .pointerInput(state.gridSize, scale, offset) {
                    detectTapGestures { tapOffset ->
                        // Calculate cell coordinates considering zoom and pan
                        // The tapOffset is relative to the transformed content
                        // We need to map it back to the grid coordinates
                        // Note: pointerInput inside graphicsLayer receives transformed coordinates? 
                        // Actually, pointerInput on the Box receives coordinates relative to the Box.
                        // But since we applied graphicsLayer to the Box, the visual representation is transformed.
                        // Wait, transformable handles the transformation of the visual layer.
                        // The pointer input coordinates are relative to the Box's bounds (untransformed).
                        // So we need to inverse transform the touch point.
                        
                        val localX = (tapOffset.x - size.width / 2) / scale + size.width / 2 - offset.x / scale
                        val localY = (tapOffset.y - size.height / 2) / scale + size.height / 2 - offset.y / scale
                        
                        // However, simpler approach:
                        // The Canvas size is fixed to the screen size.
                        // We draw the grid scaled and translated inside the Canvas?
                        // No, graphicsLayer transforms the whole composable.
                        // Let's stick to transforming the touch input.
                        
                        // Actually, let's simplify. If we use graphicsLayer, the pointerInput receives coordinates
                        // relative to the composable *before* transformation if placed *before* graphicsLayer?
                        // No, modifiers are applied sequentially.
                        
                        // Let's try a different approach for touch handling that is robust.
                        // We can calculate the cell size based on the screen size and grid size.
                        val cellSize = size.width / state.gridSize
                        
                        // Inverse transform:
                        // 1. Translate back: point - offset
                        // 2. Scale back: point / scale (relative to center?)
                        // The graphicsLayer pivot is center by default.
                        
                        // Let's assume standard pivot (center).
                        val pivot = Offset(size.width / 2f, size.height / 2f)
                        val unscaledX = (tapOffset.x - pivot.x) / scale + pivot.x - offset.x / scale
                        val unscaledY = (tapOffset.y - pivot.y) / scale + pivot.y - offset.y / scale
                        
                        val x = (unscaledX / cellSize).toInt()
                        val y = (unscaledY / cellSize).toInt()
                        
                        viewModel.toggleCell(x, y)
                    }
                }
                .pointerInput(state.gridSize, scale, offset) {
                    detectDragGestures { change, _ ->
                        val tapOffset = change.position
                        val cellSize = size.width / state.gridSize
                        val pivot = Offset(size.width / 2f, size.height / 2f)
                        val unscaledX = (tapOffset.x - pivot.x) / scale + pivot.x - offset.x / scale
                        val unscaledY = (tapOffset.y - pivot.y) / scale + pivot.y - offset.y / scale
                        
                        val x = (unscaledX / cellSize).toInt()
                        val y = (unscaledY / cellSize).toInt()
                        
                        viewModel.setCell(x, y, true)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSize = size.width / state.gridSize
                
                // Optimization: Use drawPoints instead of Path
                // We need to collect all points to draw
                // Since we are using a 1D array, we can iterate efficiently
                
                val points = ArrayList<Offset>()
                // Pre-allocate if possible, but ArrayList auto-grows. 
                // For 64x64 grid, max 4096 points.
                
                val grid = state.grid
                val gridSize = state.gridSize
                
                // Only iterate if grid is initialized
                if (grid.isNotEmpty()) {
                    for (i in 0 until gridSize * gridSize) {
                        if (grid[i] == 1) {
                            val x = i % gridSize
                            val y = i / gridSize
                            // Center of the cell for PointMode.Points with Cap.Square/Butt?
                            // Actually drawPoints with PointMode.Points draws pixels or squares depending on Cap.
                            // StrokeCap.Butt with strokeWidth = cellSize will draw a square centered at the point?
                            // No, drawPoints draws a square centered at the point if Cap is Square.
                            // If Cap is Butt, it might just be a dot.
                            // Let's use offsets to center the point.
                            
                            points.add(
                                Offset(
                                    x * cellSize + cellSize / 2,
                                    y * cellSize + cellSize / 2
                                )
                            )
                        }
                    }
                }
                
                drawPoints(
                    points = points,
                    pointMode = PointMode.Points,
                    color = onBackgroundColor,
                    strokeWidth = cellSize,
                    cap = StrokeCap.Square
                )
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            state = state,
            onDismiss = { showSettings = false },
            onDelayChange = { viewModel.changeDelay(it) },
            onGridSizeChange = { viewModel.changeGridSize(it) },
            onLoadPreset = { viewModel.loadPreset(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    state: CellularAutomatonUiState,
    onDismiss: () -> Unit,
    onDelayChange: (Long) -> Unit,
    onGridSizeChange: (Int) -> Unit,
    onLoadPreset: (List<Pair<Int, Int>>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var presetMenuExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("设置", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Text("速度 (延迟: ${state.delayMs}ms)")
            Slider(
                value = (500 - state.delayMs).toFloat(),
                onValueChange = { onDelayChange(500 - it.toLong()) },
                valueRange = 0f..490f
            )
            Spacer(Modifier.height(16.dp))

            Text("网格大小: ${state.gridSize}")
            Slider(
                value = state.gridSize.toFloat(),
                onValueChange = { onGridSizeChange(it.toInt()) },
                valueRange = 16f..128f,
                steps = 7
            )
            Spacer(Modifier.height(16.dp))

            Box {
                Button(onClick = { presetMenuExpanded = true }) {
                    Text("加载预设")
                }
                DropdownMenu(
                    expanded = presetMenuExpanded,
                    onDismissRequest = { presetMenuExpanded = false }
                ) {
                    Presets.presets.forEach { (name, pattern) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onLoadPreset(pattern)
                                presetMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismiss()
                    }
                }
            }) {
                Text("关闭")
            }
        }
    }
}