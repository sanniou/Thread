package ai.saniou.thread.feature.cellularautomaton

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class CellularAutomatonUiState(
    val gridSize: Int = 64,
    val isRunning: Boolean = true,
    val delayMs: Long = 100L,
    val generation: Int = 0,
    val grid: IntArray = IntArray(0), // 1D array for better cache locality
    val version: Long = 0L // Force recomposition when grid updates
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CellularAutomatonUiState

        if (gridSize != other.gridSize) return false
        if (isRunning != other.isRunning) return false
        if (delayMs != other.delayMs) return false
        if (generation != other.generation) return false
        if (version != other.version) return false
        // Intentionally skip grid content check for performance, rely on version
        return true
    }

    override fun hashCode(): Int {
        var result = gridSize
        result = 31 * result + isRunning.hashCode()
        result = 31 * result + delayMs.hashCode()
        result = 31 * result + generation
        result = 31 * result + version.hashCode()
        return result
    }
}

class CellularAutomatonViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CellularAutomatonUiState())
    val uiState: StateFlow<CellularAutomatonUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null

    // Double buffering to avoid allocation
    private var currentBuffer: IntArray = IntArray(0)
    private var nextBuffer: IntArray = IntArray(0)

    init {
        initializeGrid(_uiState.value.gridSize)
        startSimulation()
    }

    private fun initializeGrid(size: Int, random: Boolean = true) {
        val totalSize = size * size
        currentBuffer = IntArray(totalSize)
        nextBuffer = IntArray(totalSize)

        if (random) {
            for (i in 0 until totalSize) {
                currentBuffer[i] = if (Random.nextBoolean()) 1 else 0
            }
        }

        _uiState.update {
            it.copy(
                gridSize = size,
                grid = currentBuffer,
                generation = 0,
                version = it.version + 1
            )
        }
    }

    fun togglePlayPause() {
        _uiState.update { it.copy(isRunning = !it.isRunning) }
        if (_uiState.value.isRunning) {
            startSimulation()
        } else {
            simulationJob?.cancel()
        }
    }

    fun reset(random: Boolean = true) {
        simulationJob?.cancel()
        initializeGrid(_uiState.value.gridSize, random)
        _uiState.update { it.copy(isRunning = true) }
        startSimulation()
    }

    fun changeGridSize(newSize: Int) {
        if (newSize != _uiState.value.gridSize) {
            simulationJob?.cancel()
            initializeGrid(newSize)
            _uiState.update { it.copy(isRunning = true) }
            startSimulation()
        }
    }

    fun changeDelay(newDelay: Long) {
        _uiState.update { it.copy(delayMs = newDelay) }
    }

    fun toggleCell(x: Int, y: Int) {
        val size = _uiState.value.gridSize
        if (x in 0 until size && y in 0 until size) {
            val index = y * size + x
            // Toggle: 1 -> 0, 0 -> 1
            currentBuffer[index] = 1 - currentBuffer[index]
            _uiState.update { it.copy(version = it.version + 1) }
        }
    }

    fun setCell(x: Int, y: Int, alive: Boolean) {
        val size = _uiState.value.gridSize
        if (x in 0 until size && y in 0 until size) {
            val index = y * size + x
            val newValue = if (alive) 1 else 0
            if (currentBuffer[index] != newValue) {
                currentBuffer[index] = newValue
                _uiState.update { it.copy(version = it.version + 1) }
            }
        }
    }

    fun loadPreset(pattern: List<Pair<Int, Int>>) {
        simulationJob?.cancel()
        val size = _uiState.value.gridSize
        // Clear grid
        currentBuffer.fill(0)

        val offsetX = (size - pattern.maxOf { it.first }) / 2
        val offsetY = (size - pattern.maxOf { it.second }) / 2

        pattern.forEach { (x, y) ->
            val finalX = x + offsetX
            val finalY = y + offsetY
            if (finalX in 0 until size && finalY in 0 until size) {
                currentBuffer[finalY * size + finalX] = 1
            }
        }

        _uiState.update {
            it.copy(
                generation = 0,
                version = it.version + 1,
                isRunning = false // Pause after loading preset to let user see it
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val state = _uiState.value
                if (state.isRunning) {
                    val startTime = Clock.System.now()

                    computeNextGeneration(state.gridSize)

                    // Swap buffers
                    val temp = currentBuffer
                    currentBuffer = nextBuffer
                    nextBuffer = temp

                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                grid = currentBuffer,
                                generation = it.generation + 1,
                                version = it.version + 1
                            )
                        }
                    }

                    val elapsedTime = Clock.System.now() - startTime
                    val delayTime = (state.delayMs - elapsedTime.inWholeMilliseconds).coerceAtLeast(0)
                    delay(delayTime)
                } else {
                    delay(100) // Idle wait
                }
            }
        }
    }

    // Optimized core logic: 1D array, no object allocation, minimized modulo
    private fun computeNextGeneration(size: Int) {
        val grid = currentBuffer
        val next = nextBuffer

        // Pre-calculate offsets to avoid repeated multiplication
        // Neighbors:
        // TL T TR
        // L  C  R
        // BL B BR

        for (y in 0 until size) {
            val yUp = if (y > 0) y - 1 else size - 1
            val yDown = if (y < size - 1) y + 1 else 0

            val rowOffset = y * size
            val upRowOffset = yUp * size
            val downRowOffset = yDown * size

            for (x in 0 until size) {
                val xLeft = if (x > 0) x - 1 else size - 1
                val xRight = if (x < size - 1) x + 1 else 0

                // Count neighbors
                var neighbors = 0

                // Top row
                neighbors += grid[upRowOffset + xLeft]
                neighbors += grid[upRowOffset + x]
                neighbors += grid[upRowOffset + xRight]

                // Middle row
                neighbors += grid[rowOffset + xLeft]
                neighbors += grid[rowOffset + xRight]

                // Bottom row
                neighbors += grid[downRowOffset + xLeft]
                neighbors += grid[downRowOffset + x]
                neighbors += grid[downRowOffset + xRight]

                val currentIndex = rowOffset + x
                val isAlive = grid[currentIndex] == 1

                // Rules:
                // 1. Any live cell with fewer than two live neighbours dies, as if by underpopulation.
                // 2. Any live cell with two or three live neighbours lives on to the next generation.
                // 3. Any live cell with more than three live neighbours dies, as if by overpopulation.
                // 4. Any dead cell with exactly three live neighbours becomes a live cell, as if by reproduction.

                next[currentIndex] = if (isAlive) {
                    if (neighbors == 2 || neighbors == 3) 1 else 0
                } else {
                    if (neighbors == 3) 1 else 0
                }
            }
        }
    }
}
