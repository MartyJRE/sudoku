package io.github.martyjre.sudoku.engine

const val SIZE = 9
const val CELLS = 81
const val ALL_DIGITS = 0x1FF

fun rowOf(cell: Int) = cell / SIZE
fun colOf(cell: Int) = cell % SIZE
fun boxOf(cell: Int) = rowOf(cell) / 3 * 3 + colOf(cell) / 3

fun bit(digit: Int) = 1 shl (digit - 1)

fun Int.digits(): List<Int> = (1..SIZE).filter { this and bit(it) != 0 }

object Units {
    val rows: List<IntArray> = List(SIZE) { r -> IntArray(SIZE) { c -> r * SIZE + c } }
    val cols: List<IntArray> = List(SIZE) { c -> IntArray(SIZE) { r -> r * SIZE + c } }
    val boxes: List<IntArray> = List(SIZE) { b ->
        IntArray(SIZE) { i -> (b / 3 * 3 + i / 3) * SIZE + b % 3 * 3 + i % 3 }
    }
    val all: List<IntArray> = rows + cols + boxes

    val peers: List<IntArray> = List(CELLS) { cell ->
        (0 until CELLS).filter { it != cell && sees(it, cell) }.toIntArray()
    }

    fun sees(a: Int, b: Int) =
        rowOf(a) == rowOf(b) || colOf(a) == colOf(b) || boxOf(a) == boxOf(b)
}

fun parseGrid(text: String): IntArray {
    val chars = text.filter { it.isDigit() || it == '.' }
    require(chars.length == CELLS) { "Expected $CELLS cells, got ${chars.length}" }
    return IntArray(CELLS) { chars[it].digitToIntOrNull() ?: 0 }
}

fun IntArray.toGridString() = joinToString("") { if (it == 0) "." else it.toString() }
