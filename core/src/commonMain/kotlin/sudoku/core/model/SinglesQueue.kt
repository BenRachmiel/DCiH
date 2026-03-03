package sudoku.core.model

class SinglesQueue {
    private val indices = IntArray(Board.LENGTH * 3)
    private val values = IntArray(Board.LENGTH * 3)
    private var putIndex = 0
    private var getIndex = 0

    val isEmpty: Boolean get() = getIndex >= putIndex

    fun addSingle(index: Int, value: Int) {
        indices[putIndex] = index
        values[putIndex++] = value
    }

    /** Returns queue position or -1 if empty. Advances the get pointer. */
    fun getSingle(): Int {
        if (getIndex >= putIndex) return -1
        val ret = getIndex++
        if (getIndex >= putIndex) {
            getIndex = 0; putIndex = 0
        }
        return ret
    }

    fun getIndex(queueIndex: Int): Int = indices[queueIndex]
    fun getValue(queueIndex: Int): Int = values[queueIndex]

    fun deleteNakedSingle(cellIndex: Int) {
        for (i in getIndex until putIndex) {
            if (indices[i] == cellIndex) {
                for (j in i + 1 until putIndex) {
                    indices[j - 1] = indices[j]
                    values[j - 1] = values[j]
                }
                putIndex--
                break
            }
        }
    }

    fun deleteHiddenSingle(constraint: Int, value: Int) {
        for (i in getIndex until putIndex) {
            val idx = indices[i]
            if (values[i] == value &&
                (Board.CONSTRAINTS[idx][0] == constraint ||
                 Board.CONSTRAINTS[idx][1] == constraint ||
                 Board.CONSTRAINTS[idx][2] == constraint)
            ) {
                for (j in i + 1 until putIndex) {
                    indices[j - 1] = indices[j]
                    values[j - 1] = values[j]
                }
                putIndex--
                break
            }
        }
    }

    fun clear() {
        getIndex = 0; putIndex = 0
    }

    fun copyFrom(src: SinglesQueue) {
        src.indices.copyInto(indices)
        src.values.copyInto(values)
        getIndex = src.getIndex
        putIndex = src.putIndex
    }
}
