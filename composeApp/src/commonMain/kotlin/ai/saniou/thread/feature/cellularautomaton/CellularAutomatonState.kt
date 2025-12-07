package ai.saniou.thread.feature.cellularautomaton

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import kotlin.random.Random

class CellularAutomatonState(
    private val coroutineScope: CoroutineScope
) {
    var gridSize by mutableStateOf(64)
    var isRunning by mutableStateOf(true)
    var delayMs by mutableStateOf(100L)
    var generation by mutableStateOf(0)
    var grid by mutableStateOf(initialGrid(gridSize))
        private set

    private var job: Job? = null

    init {
        start()
    }

    fun start() {
        job?.cancel()
        job = coroutineScope.launch {
            while (isActive) {
                val startTime = System.currentTimeMillis()
                if (isRunning) {
                    grid = nextGeneration(grid, gridSize)
                    generation++
                }
                val elapsedTime = System.currentTimeMillis() - startTime
                val delayTime = (delayMs - elapsedTime).coerceAtLeast(0)
                delay(delayTime)
            }
        }
    }

    fun togglePlayPause() {
        isRunning = !isRunning
    }

    fun reset(random: Boolean = true) {
        grid = if (random) initialGrid(gridSize) else Array(gridSize) { BooleanArray(gridSize) }
        generation = 0
        if (!isRunning) isRunning = true
    }

    fun changeGridSize(newSize: Int) {
        gridSize = newSize
        reset()
    }

    fun loadPreset(pattern: List<Pair<Int, Int>>) {
        grid = Presets.loadPreset(grid, gridSize, pattern)
        generation = 0
    }

    fun changeDelay(newDelay: Long) {
        delayMs = newDelay
    }

    fun toggleCell(x: Int, y: Int) {
        if (x in 0 until gridSize && y in 0 until gridSize) {
            val newGrid = grid.map { it.clone() }.toTypedArray()
            newGrid[x][y] = !newGrid[x][y]
            grid = newGrid
        }
    }

    private fun initialGrid(size: Int): Array<BooleanArray> {
        return Array(size) { BooleanArray(size) { Random.nextBoolean() } }
    }

    private fun nextGeneration(currentGrid: Array<BooleanArray>, size: Int): Array<BooleanArray> {
        val newGrid = Array(size) { BooleanArray(size) }
        for (i in 0 until size) {
            for (j in 0 until size) {
                val neighbors = countNeighbors(currentGrid, i, j, size)
                val isAlive = currentGrid[i][j]
                newGrid[i][j] = when {
                    isAlive && (neighbors < 2 || neighbors > 3) -> false
                    isAlive && (neighbors == 2 || neighbors == 3) -> true
                    !isAlive && neighbors == 3 -> true
                    else -> isAlive
                }
            }
        }
        return newGrid
    }

    private fun countNeighbors(currentGrid: Array<BooleanArray>, x: Int, y: Int, size: Int): Int {
        var count = 0
        for (i in -1..1) {
            for (j in -1..1) {
                if (i == 0 && j == 0) continue
                val newX = (x + i + size) % size
                val newY = (y + j + size) % size
                if (currentGrid[newX][newY]) {
                    count++
                }
            }
        }
        return count
    }
}