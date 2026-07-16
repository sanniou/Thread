package ai.saniou.thread.feature.cellularautomaton

import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.coreui.widgets.ContextHero
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
        Column(Modifier.padding(paddingValues).fillMaxSize()) {
            ContextHero(
                icon = Icons.Default.Science,
                title = "元胞实验室",
                subtitle = "缩放、绘制并观察 Conway 生命游戏",
                metric = "GEN ${state.generation}",
                modifier = Modifier.padding(16.dp),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
            )
        val onBackgroundColor = MaterialTheme.colorScheme.onBackground
        
        // Zoom and Pan state
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformableState = rememberTransformableState { _, zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(0.5f, 5f)
            offset += panChange
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformableState)
                .pointerInput(state.gridSize, scale, offset) {
                    detectTapGestures { tapOffset ->
                        val cellSize = size.width / state.gridSize
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
                
                val points = ArrayList<Offset>()
                val grid = state.grid
                val gridSize = state.gridSize
                if (grid.isNotEmpty()) {
                    for (i in 0 until gridSize * gridSize) {
                        if (grid[i] == 1) {
                            val x = i % gridSize
                            val y = i / gridSize
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
    var presetMenuExpanded by remember { mutableStateOf(false) }

    AdaptiveModal(onDismissRequest = onDismiss) {
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

            Button(onClick = onDismiss) {
                Text("关闭")
            }
        }
    }
}
