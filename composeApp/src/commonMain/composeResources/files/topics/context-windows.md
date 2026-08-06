---
id: context-windows
title: Context Windows
tagline: Everything the model can see before it starts forgetting.
level: intermediate
related: tokenisation, temperature-sampling
---

## Note
- The fixed maximum number of tokens a model can attend to at once, spanning the prompt, conversation history and the reply being generated — all combined.
- Self-attention cost grows **quadratically** with sequence length, which is why the window has a hard limit rather than growing for free with more memory.
- Once a conversation exceeds the window, the oldest tokens are dropped, truncated or summarised — content outside it is absent, not degraded, as if it had never been sent.
- A larger window helps, but doesn't guarantee even use of it: models often recall information placed near the start or end far better than information buried in the middle (the "lost in the middle" effect).

## Read More
What is a context window? | https://cloud.google.com/discover/what-is-a-context-window | Google Cloud

## Code: Kotlin
```kotlin
/** Keeps only the most recent turns that fit within maxTokens. */
fun fitToContextWindow(turns: List<String>, tokensOf: (String) -> Int, maxTokens: Int): List<String> {
    val kept = mutableListOf<String>()
    var used = 0
    for (turn in turns.asReversed()) {
        val cost = tokensOf(turn)
        if (used + cost > maxTokens) break
        kept.add(0, turn)
        used += cost
    }
    return kept
}
```

## Code: Go
```go
// FitToContextWindow keeps only the most recent turns that fit within maxTokens.
func FitToContextWindow(turns []string, tokensOf func(string) int, maxTokens int) []string {
	var kept []string
	used := 0
	for i := len(turns) - 1; i >= 0; i-- {
		cost := tokensOf(turns[i])
		if used+cost > maxTokens {
			break
		}
		kept = append([]string{turns[i]}, kept...)
		used += cost
	}
	return kept
}
```

## Questions
### Why can't context windows just be made arbitrarily large?
difficulty: easy
askedAt: LLM fundamentals — common in applied-AI and systems-flavoured interviews
Self-attention cost scales roughly with the square of the sequence length, so doubling the context window roughly quadruples the compute and memory needed for attention alone. That quadratic curve, not an arbitrary product limit, is what caps window sizes — though sparse and linear-attention variants exist specifically to chip away at it.

### If a model has a very large context window, does giving it more information always help?
difficulty: medium
askedAt: LLM application-design interviews
Not reliably. Models show a "lost in the middle" effect where recall degrades for information buried in the centre of a long context, even when the total input is well within the window limit. Relevant, concise context reliably beats maximal context — which is why retrieval systems trim to what's relevant rather than stuffing the whole window.

## References
