package ai.saniou.thread.feature.cellularautomaton

object Presets {

    val glider = listOf(
        0 to 1, 1 to 2, 2 to 0, 2 to 1, 2 to 2
    )

    val lwss = listOf(
        1 to 0, 4 to 0,
        0 to 1,
        0 to 2, 4 to 2,
        0 to 3, 1 to 3, 2 to 3, 3 to 3
    )

    val pulsar = listOf(
        2 to 0, 3 to 0, 4 to 0, 8 to 0, 9 to 0, 10 to 0,
        0 to 2, 5 to 2, 7 to 2, 12 to 2,
        0 to 3, 5 to 3, 7 to 3, 12 to 3,
        0 to 4, 5 to 4, 7 to 4, 12 to 4,
        2 to 5, 3 to 5, 4 to 5, 8 to 5, 9 to 5, 10 to 5,

        2 to 7, 3 to 7, 4 to 7, 8 to 7, 9 to 7, 10 to 7,
        0 to 8, 5 to 8, 7 to 8, 12 to 8,
        0 to 9, 5 to 9, 7 to 9, 12 to 9,
        0 to 10, 5 to 10, 7 to 10, 12 to 10,
        2 to 12, 3 to 12, 4 to 12, 8 to 12, 9 to 12, 10 to 12
    )

    val pentadecathlon = listOf(
        0 to 1, 1 to 1,
        2 to 0, 2 to 2,
        3 to 1, 4 to 1, 5 to 1, 6 to 1,
        7 to 0, 7 to 2,
        8 to 1, 9 to 1
    )

    val gosperGliderGun = listOf(
        0 to 24,
        1 to 22, 1 to 24,
        2 to 12, 2 to 13, 2 to 20, 2 to 21, 2 to 34, 2 to 35,
        3 to 11, 3 to 15, 3 to 20, 3 to 21, 3 to 34, 3 to 35,
        4 to 0, 4 to 1, 4 to 10, 4 to 16, 4 to 20, 4 to 21,
        5 to 0, 5 to 1, 5 to 10, 5 to 14, 5 to 16, 5 to 17, 5 to 22, 5 to 24,
        6 to 10, 6 to 16, 6 to 24,
        7 to 11, 7 to 15,
        8 to 12, 8 to 13
    )

    val presets = mapOf(
        "滑翔机" to glider,
        "轻型飞船" to lwss,
        "脉冲星" to pulsar,
        "十项全能" to pentadecathlon,
        "高斯帕滑翔机枪" to gosperGliderGun
    )

    fun loadPreset(grid: Array<BooleanArray>, gridSize: Int, pattern: List<Pair<Int, Int>>): Array<BooleanArray> {
        val newGrid = Array(gridSize) { BooleanArray(gridSize) }
        val offsetX = (gridSize - pattern.maxOf { it.first }) / 2
        val offsetY = (gridSize - pattern.maxOf { it.second }) / 2
        pattern.forEach { (x, y) ->
            val finalX = x + offsetX
            val finalY = y + offsetY
            if (finalX in 0 until gridSize && finalY in 0 until gridSize) {
                newGrid[finalX][finalY] = true
            }
        }
        return newGrid
    }
}