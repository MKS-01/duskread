package dev.mks.stacks.viz

import dev.mks.stacks.model.AuxValue
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.SeqFrame

/**
 * A single static wireframe of one transformer block's pipeline — not a
 * pass of real data through it. The chain itself is the whole lesson: every
 * stage here is one of the paper's building blocks, in the order data meets
 * them.
 */
fun transformerArchitectureScene(): Scene {
    val frame = SeqFrame(
        values = listOf(
            "Input Embedding",
            "+ Positional Encoding",
            "Self-Attention",
            "Feed-Forward",
            "Output",
        ),
        caption =
        "A transformer block strung out as a pipeline. Attention mixes " +
            "information across positions; the feed-forward layer then " +
            "processes each position independently. Positional encoding is " +
            "added once, up front, because attention itself has no notion " +
            "of order.",
        aux = listOf(
            AuxValue("stacked", "N identical blocks, output of one feeding the next"),
        ),
    )

    return Scene.Chain(listOf(frame))
}
