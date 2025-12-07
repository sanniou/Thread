package ai.saniou.thread.feature.cellularautomaton

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellularAutomatonScreen() {
    val coroutineScope = rememberCoroutineScope()
    val state = remember { CellularAutomatonState(coroutineScope) }
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
                FloatingActionButton(onClick = { state.togglePlayPause() }) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "播放/暂停"
                    )
                }
                Spacer(Modifier.width(16.dp))
                FloatingActionButton(onClick = { state.reset() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "重置")
                }
            }
        }
    ) { paddingValues ->
        val onBackgroundColor = MaterialTheme.colorScheme.onBackground
        Canvas(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .pointerInput(state.gridSize) {
                    detectTapGestures { offset ->
                        val cellSize = size.width / state.gridSize
                        val x = (offset.x / cellSize).toInt()
                        val y = (offset.y / cellSize).toInt()
                        state.toggleCell(x, y)
                    }
                }
        ) {
            val cellSize = size.width / state.gridSize
            val path = Path()
            for (i in 0 until state.gridSize) {
                for (j in 0 until state.gridSize) {
                    if (state.grid[i][j]) {
                        path.addRect(Rect(offset = Offset(i * cellSize, j * cellSize), size = Size(cellSize, cellSize)))
                    }
                }
            }
            drawPath(path = path, color = onBackgroundColor)
        }
    }

    if (showSettings) {
        SettingsSheet(
            state = state,
            onDismiss = { showSettings = false },
            onDelayChange = { state.changeDelay(it) },
            onGridSizeChange = { state.changeGridSize(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    state: CellularAutomatonState,
    onDismiss: () -> Unit,
    onDelayChange: (Long) -> Unit,
    onGridSizeChange: (Int) -> Unit
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
                                state.loadPreset(pattern)
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
