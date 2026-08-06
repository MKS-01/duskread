---
id: tool-use
title: Tool Use
tagline: Give the model hands, not just a mouth.
level: intermediate
related: planning-loops, evals
---

## Note
- A tool call is a structured request the model makes instead of replying in prose — a name and arguments, executed outside the model, whose result is fed back in as text.
- The model never runs anything itself; the **harness** runs the tool and hands the result back, so the model only ever knows what comes back in that text.
- A tool is described to the model as a name, a description, and a typed argument schema — the model never sees the implementation, only the contract.
- The model decides *whether* to call a tool at all; a well-designed toolset always leaves it the option to answer without calling anything.
- Turns a model with a fixed training cutoff and no side effects into one that can read a file, hit an API, or run code — bounded by whatever tools it has been given.
- More tools is not free: a large toolset makes *selecting* the right tool part of the model's job, and past a few dozen choices that selection degrades.

## Code: Kotlin
```kotlin
data class ToolCall(val name: String, val args: Map<String, String>)

fun interface Tool {
    fun run(args: Map<String, String>): String
}

// Loops until the model answers directly instead of calling a tool, or maxSteps is hit.
fun runToolLoop(
    tools: Map<String, Tool>,
    maxSteps: Int = 5,
    nextModelStep: (transcript: List<String>) -> Any, // ToolCall or a final String answer
): String {
    val transcript = mutableListOf<String>()
    repeat(maxSteps) {
        when (val step = nextModelStep(transcript)) {
            is ToolCall -> {
                val tool = tools[step.name] ?: error("Unknown tool: ${step.name}")
                val result = tool.run(step.args)
                transcript += "tool ${step.name}(${step.args}) -> $result"
            }
            is String -> return step
            else -> error("Unexpected step type")
        }
    }
    return "gave up after $maxSteps steps"
}
```

## Code: Go
```go
// ToolCall is what the model emits instead of a final answer: a tool name
// and its arguments.
type ToolCall struct {
	Name string
	Args map[string]string
}

type Tool func(args map[string]string) string

// RunToolLoop runs until the model answers directly instead of calling a
// tool, or maxSteps is exhausted.
func RunToolLoop(tools map[string]Tool, maxSteps int, nextModelStep func(transcript []string) (ToolCall, string, bool)) string {
	var transcript []string
	for i := 0; i < maxSteps; i++ {
		call, answer, isCall := nextModelStep(transcript)
		if !isCall {
			return answer
		}
		tool, ok := tools[call.Name]
		if !ok {
			return fmt.Sprintf("unknown tool: %s", call.Name)
		}
		result := tool(call.Args)
		transcript = append(transcript, fmt.Sprintf("tool %s(%v) -> %s", call.Name, call.Args, result))
	}
	return fmt.Sprintf("gave up after %d steps", maxSteps)
}
```

## Questions
### What's the difference between a tool call and an ordinary function call in a codebase?
difficulty: easy
askedAt: LLM systems fundamentals — common first conceptual question
An ordinary function call is resolved at compile or interpret time by the program itself. A tool call is *chosen* by the model at inference time from a described menu — the harness still has to execute it like any function, but which one runs, and with what arguments, is a model decision, not a fixed line of code.

### How do you stop a model from picking the wrong tool when two tools look similar?
difficulty: medium
askedAt: Agent-harness design interviews
Mostly by making the schemas do the disambiguating work: distinct names, descriptions that state when *not* to use each one, and non-overlapping argument shapes. When two tools are genuinely close in purpose, the more reliable fix is usually to merge them into one tool with a parameter, rather than trust the model to pick correctly every time.

## References
