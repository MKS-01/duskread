package dev.mks.algoatlas.content

import dev.mks.algoatlas.model.Reference

/**
 * Sources worth reading alongside these notes.
 *
 * Vaidehi Joshi's **basecs** series is the model for how these topics are
 * written — open on something relatable, explain why the thing was invented
 * before what it does, and always say where the name came from. The notes here
 * are original, but the approach is hers and is credited on every topic that
 * her series also covers.
 */
object Refs {
    val BasecsHome = Reference(
        label = "basecs — computer science fundamentals, explained properly",
        url = "https://medium.com/basecs",
        source = "Vaidehi Joshi · Medium",
    )

    val BasecsIndex = Reference(
        label = "basecs-series — the full index of articles and resources",
        url = "https://github.com/vaidehijoshi/basecs-series",
        source = "vaidehijoshi · GitHub",
    )

    /** Every topic gets the two series links, plus any topic-specific pieces. */
    fun basecs(vararg specific: Reference): List<Reference> = specific.toList() + listOf(BasecsHome, BasecsIndex)
}
