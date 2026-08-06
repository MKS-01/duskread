package dev.mks.stacks.content

import dev.mks.stacks.model.Scene
import dev.mks.stacks.viz.arrayScene
import dev.mks.stacks.viz.attentionScene
import dev.mks.stacks.viz.backpropagationScene
import dev.mks.stacks.viz.bfsScene
import dev.mks.stacks.viz.binarySearchScene
import dev.mks.stacks.viz.coinChangeScene
import dev.mks.stacks.viz.contextWindowsScene
import dev.mks.stacks.viz.dfsScene
import dev.mks.stacks.viz.evalsScene
import dev.mks.stacks.viz.gradientDescentScene
import dev.mks.stacks.viz.hashTableScene
import dev.mks.stacks.viz.linearRegressionScene
import dev.mks.stacks.viz.linkedListScene
import dev.mks.stacks.viz.mergeSortScene
import dev.mks.stacks.viz.planningLoopsScene
import dev.mks.stacks.viz.stackQueueScene
import dev.mks.stacks.viz.temperatureSamplingScene
import dev.mks.stacks.viz.tokenisationScene
import dev.mks.stacks.viz.toolUseScene
import dev.mks.stacks.viz.transformerArchitectureScene

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
    "gradientDescentScene" to { gradientDescentScene() },
    "linearRegressionScene" to { linearRegressionScene() },
    "backpropagationScene" to { backpropagationScene() },
    "attentionScene" to { attentionScene() },
    "transformerArchitectureScene" to { transformerArchitectureScene() },
    "tokenisationScene" to { tokenisationScene() },
    "contextWindowsScene" to { contextWindowsScene() },
    "temperatureSamplingScene" to { temperatureSamplingScene() },
    "toolUseScene" to { toolUseScene() },
    "planningLoopsScene" to { planningLoopsScene() },
    "evalsScene" to { evalsScene() },
)
