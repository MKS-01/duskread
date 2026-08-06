---
id: planning-loops
title: Planning Loops
tagline: Plan, act, observe, and let the result decide the next step.
level: intermediate
related: tool-use, evals
---

## Note
- Wraps a model in a loop — plan a next step, act on it (often via a [tool](tool-use)), observe the result, and use that observation to re-plan — instead of asking for one giant answer up front.
- The **observe** step is what makes it a loop and not just a longer prompt: the plan is revised on real feedback, not just the model's own guess of what would happen.
- Left unchecked, a planning loop can run forever, so a real harness caps it with a step limit, a cost budget, or a model call that decides "I'm done."
- Each cycle costs another model call and often a growing transcript, so the stopping condition matters as much as the planning logic.
- Interleaving reasoning with acting, rather than planning the whole sequence upfront, tends to beat either extreme because the plan can react to what actually happened.
- The pattern behind almost everything called an "agent" in current practitioner usage, from a coding assistant to a research assistant.

## Code: Kotlin
```kotlin
sealed interface Action {
    data class Act(val description: String) : Action
    data class Done(val answer: String) : Action
}

// Plans the next action from what's been observed, acts, and feeds the
// observation back in, until the plan reports Done or maxSteps is hit.
fun runPlanningLoop(maxSteps: Int = 10, plan: (observations: List<String>) -> Action, act: (String) -> String): String {
    val observations = mutableListOf<String>()
    repeat(maxSteps) {
        when (val next = plan(observations)) {
            is Action.Done -> return next.answer
            is Action.Act -> observations += act(next.description)
        }
    }
    return "stopped after $maxSteps steps without finishing"
}
```

## Code: Go
```go
// Action is either another step to act on, or a final answer that ends the
// loop.
type Action struct {
	Description string
	Done        bool
	Answer      string
}

// RunPlanningLoop repeatedly plans the next action from everything observed
// so far, acts on it, and feeds the observation back in, until the plan
// reports Done or maxSteps is hit.
func RunPlanningLoop(maxSteps int, plan func(observations []string) Action, act func(string) string) string {
	var observations []string
	for i := 0; i < maxSteps; i++ {
		next := plan(observations)
		if next.Done {
			return next.Answer
		}
		observations = append(observations, act(next.Description))
	}
	return fmt.Sprintf("stopped after %d steps without finishing", maxSteps)
}
```

## Questions
### What stops a planning loop from running forever?
difficulty: easy
askedAt: Agent-harness design interviews
Nothing intrinsic to the loop — the harness has to enforce it, usually with a maximum step count, a cost or token budget, or a model call whose only job is to decide whether the goal is actually met. Relying on the model to "just know" when to stop is the common bug.

### Why does interleaving reasoning and acting outperform planning the whole sequence upfront?
difficulty: medium
askedAt: Agentic-systems interviews
Planning fully upfront commits to a sequence before any of it has been tested against reality, so an early wrong assumption propagates through every later step unnoticed. Interleaving lets each action's actual result correct the plan before the next one is chosen, catching bad assumptions one step in instead of at the end.

## References
