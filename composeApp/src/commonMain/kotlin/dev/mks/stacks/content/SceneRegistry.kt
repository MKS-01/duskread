package dev.mks.stacks.content

import dev.mks.stacks.model.Scene
import dev.mks.stacks.viz.arrayScene
import dev.mks.stacks.viz.bfsScene
import dev.mks.stacks.viz.binarySearchScene
import dev.mks.stacks.viz.coinChangeScene
import dev.mks.stacks.viz.dfsScene
import dev.mks.stacks.viz.hashTableScene
import dev.mks.stacks.viz.linkedListScene
import dev.mks.stacks.viz.mergeSortScene
import dev.mks.stacks.viz.stackQueueScene

/**
 * Front matter can't hold a Kotlin function reference, so a topic's `scene:`
 * key is a string that resolves through this map instead. A topic with no
 * scene, or an unmatched key, simply gets no visualisation.
 */
val SceneRegistry: Map<String, () -> Scene> = mapOf(
    "arrayScene" to { arrayScene() },
    "linkedListScene" to { linkedListScene() },
    "stackQueueScene" to { stackQueueScene() },
    "hashTableScene" to { hashTableScene() },
    "binarySearchScene" to { binarySearchScene() },
    "mergeSortScene" to { mergeSortScene() },
    "bfsScene" to { bfsScene() },
    "dfsScene" to { dfsScene() },
    "coinChangeScene" to { coinChangeScene() },
)
