package sudoku.core.model

/**
 * 81-bit bitset representing a set of cell indices (0-80).
 * Port of HoDoKu's SudokuSetBase/SudokuSet.
 * Uses two Longs: mask1 for cells 0-63, mask2 for cells 64-80.
 */
class CellSet(
    var mask1: Long = 0L,
    var mask2: Long = 0L
) {
    val isEmpty: Boolean get() = mask1 == 0L && mask2 == 0L

    fun add(value: Int) {
        if (value >= 64) mask2 = mask2 or (1L shl (value - 64))
        else mask1 = mask1 or (1L shl value)
    }

    fun remove(value: Int) {
        if (value >= 64) mask2 = mask2 and (1L shl (value - 64)).inv()
        else mask1 = mask1 and (1L shl value).inv()
    }

    operator fun contains(value: Int): Boolean =
        if (value >= 64) (mask2 and (1L shl (value - 64))) != 0L
        else (mask1 and (1L shl value)) != 0L

    fun clear() { mask1 = 0L; mask2 = 0L }

    fun setAll() { mask1 = MAX_MASK1; mask2 = MAX_MASK2 }

    fun or(other: CellSet) { mask1 = mask1 or other.mask1; mask2 = mask2 or other.mask2 }
    fun and(other: CellSet) { mask1 = mask1 and other.mask1; mask2 = mask2 and other.mask2 }
    fun andNot(other: CellSet) { mask1 = mask1 and other.mask1.inv(); mask2 = mask2 and other.mask2.inv() }

    fun intersects(other: CellSet): Boolean =
        (mask1 and other.mask1) != 0L || (mask2 and other.mask2) != 0L

    fun containsAll(other: CellSet): Boolean =
        (other.mask1 and mask1.inv()) == 0L && (other.mask2 and mask2.inv()) == 0L

    /** Returns an IntArray of all set cell indices. */
    fun toArray(): IntArray {
        val result = IntArray(size())
        var idx = 0
        var m = mask1
        var bit = 0
        while (m != 0L) {
            if (m and 1L != 0L) result[idx++] = bit
            m = m ushr 1
            bit++
        }
        m = mask2
        bit = 64
        while (m != 0L) {
            if (m and 1L != 0L) result[idx++] = bit
            m = m ushr 1
            bit++
        }
        return result
    }

    fun size(): Int = mask1.countOneBits() + mask2.countOneBits()

    fun copy(): CellSet = CellSet(mask1, mask2)

    override fun equals(other: Any?): Boolean =
        other is CellSet && mask1 == other.mask1 && mask2 == other.mask2

    override fun hashCode(): Int = (mask1 xor (mask1 ushr 32)).toInt() * 31 +
            (mask2 xor (mask2 ushr 32)).toInt()

    companion object {
        const val MAX_MASK1 = -1L // 0xFFFFFFFFFFFFFFFF
        const val MAX_MASK2 = 0x1FFFFL // 17 bits for cells 64-80
    }
}

private fun Long.countOneBits(): Int {
    // Kotlin/JVM has countOneBits but for KMP we use this
    var v = this
    v = v - ((v ushr 1) and 0x5555555555555555L)
    v = (v and 0x3333333333333333L) + ((v ushr 2) and 0x3333333333333333L)
    v = (v + (v ushr 4)) and 0x0f0f0f0f0f0f0f0fL
    return ((v * 0x0101010101010101L) ushr 56).toInt()
}
