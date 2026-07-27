package dev.mks.algoatlas.viz

import dev.mks.algoatlas.model.AuxValue
import dev.mks.algoatlas.model.MatrixFrame
import dev.mks.algoatlas.model.Scene
import dev.mks.algoatlas.model.Tone

/**
 * The coin-change table, filled left to right.
 *
 * A table is the honest picture of dynamic programming: every cell is a smaller
 * version of the same question, and each new answer is built from cells already
 * on screen. Watching the dependencies light up is the whole lesson.
 */
fun coinChangeScene(
    coins: List<Int> = listOf(1, 3, 4),
    target: Int = 6,
): Scene {
    val frames = mutableListOf<MatrixFrame>()
    val infinity = target + 1
    val best = IntArray(target + 1) { infinity }
    best[0] = 0

    val colLabels = (0..target).map { "$it" }

    fun cells(): List<String?> = best.map { if (it >= infinity) "∞" else "$it" }

    fun emit(caption: String, marks: Map<String, Tone>, aux: List<AuxValue> = emptyList()) {
        frames += MatrixFrame(
            caption = caption,
            grid = listOf(cells()),
            marks = marks,
            rowLabels = listOf("min"),
            colLabels = colLabels,
            aux = aux,
        )
    }

    emit(
        "Question: the fewest coins from ${coins.joinToString(", ")} that make $target. Each column is the answer for a smaller amount.",
        emptyMap(),
        listOf(AuxValue("coins", coins.joinToString(",")), AuxValue("target", "$target")),
    )

    emit(
        "The base case is the only free answer: making 0 needs 0 coins. Everything else starts unknown.",
        mapOf("0,0" to Tone.GOOD),
    )

    for (amount in 1..target) {
        emit(
            "Now solve amount $amount. Try each coin and ask what is left over — that leftover is a column we already answered.",
            mapOf("0,$amount" to Tone.ACTIVE),
            listOf(AuxValue("amount", "$amount")),
        )

        for (coin in coins) {
            if (coin > amount) {
                emit(
                    "Coin $coin is larger than $amount, so it cannot be used here.",
                    mapOf("0,$amount" to Tone.ACTIVE),
                    listOf(AuxValue("coin", "$coin"), AuxValue("skipped", "too large")),
                )
                continue
            }

            val remainder = amount - coin
            val candidate = if (best[remainder] >= infinity) infinity else best[remainder] + 1

            emit(
                "Use coin $coin: that leaves $remainder, which needs ${if (best[remainder] >= infinity) "∞" else "${best[remainder]}"} coins. " +
                    "So this route costs ${if (candidate >= infinity) "∞" else "$candidate"}.",
                mapOf("0,$amount" to Tone.ACTIVE, "0,$remainder" to Tone.INFO),
                listOf(
                    AuxValue("coin", "$coin"),
                    AuxValue("subproblem", "amount $remainder"),
                    AuxValue("candidate", if (candidate >= infinity) "∞" else "$candidate"),
                ),
            )

            if (candidate < best[amount]) {
                best[amount] = candidate
                emit(
                    "That beats what we had, so amount $amount now costs ${best[amount]} coin(s).",
                    mapOf("0,$amount" to Tone.GOOD, "0,$remainder" to Tone.INFO),
                    listOf(AuxValue("best", "${best[amount]}")),
                )
            }
        }

        emit(
            "Amount $amount is settled at ${if (best[amount] >= infinity) "∞ (impossible)" else "${best[amount]} coin(s)"}. It never needs recomputing.",
            (0..amount).associate { "0,$it" to Tone.GOOD },
        )
    }

    emit(
        "Done. Making $target takes ${best[target]} coins. Every column was computed exactly once — that is the whole saving over brute force.",
        (0..target).associate { "0,$it" to Tone.GOOD } + ("0,$target" to Tone.WARN),
        listOf(AuxValue("answer", "${best[target]}"), AuxValue("cells filled", "${target + 1}")),
    )

    return Scene.Matrix(frames)
}
