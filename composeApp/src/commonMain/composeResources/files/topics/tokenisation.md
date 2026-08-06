---
id: tokenisation
title: Tokenisation
tagline: Turn raw text into the integers a model actually reads.
level: basic
related: context-windows
---

## Note
- Splits text into subword pieces called **tokens**, each mapped to an integer id from a fixed vocabulary — the only form a model actually reads.
- Modern tokenisers learn subword merges (**byte-pair encoding**) so any string, including typos and rare words, can be represented without an "unknown word" fallback.
- Common words become a single token; rare or novel ones fragment into several — vocabulary size trades off directly against sequence length.
- Tokenisation is a fixed preprocessing step decided before training, not something the model itself learns or can change at inference time.
- The vocabulary is fixed at training time; a tokeniser trained mostly on English badly fragments other scripts and languages.
- Token count, not character or word count, is what context windows and API usage are measured in.

## Read More
Summary of the tokens / transformers pipeline | https://huggingface.co/docs/transformers/en/tokenizer_summary | Hugging Face

## Code: Kotlin
```kotlin
/** Repeatedly merges the highest-priority adjacent pair until none apply. */
fun bpeEncode(word: String, mergeRanks: Map<Pair<String, String>, Int>): List<String> {
    var symbols = word.map { it.toString() }
    while (symbols.size > 1) {
        var bestPair: Pair<String, String>? = null
        var bestRank = Int.MAX_VALUE
        for (i in 0 until symbols.size - 1) {
            val pair = symbols[i] to symbols[i + 1]
            val rank = mergeRanks[pair] ?: continue
            if (rank < bestRank) {
                bestRank = rank
                bestPair = pair
            }
        }
        val (left, right) = bestPair ?: break
        val merged = mutableListOf<String>()
        var i = 0
        while (i < symbols.size) {
            if (i < symbols.size - 1 && symbols[i] == left && symbols[i + 1] == right) {
                merged += left + right
                i += 2
            } else {
                merged += symbols[i]
                i += 1
            }
        }
        symbols = merged
    }
    return symbols
}
```

## Code: Go
```go
// BPEEncode repeatedly merges the highest-priority adjacent pair until none apply.
func BPEEncode(word string, mergeRanks map[[2]string]int) []string {
	symbols := strings.Split(word, "")
	for len(symbols) > 1 {
		bestRank := math.MaxInt32
		bestIdx := -1
		for i := 0; i < len(symbols)-1; i++ {
			pair := [2]string{symbols[i], symbols[i+1]}
			if rank, ok := mergeRanks[pair]; ok && rank < bestRank {
				bestRank = rank
				bestIdx = i
			}
		}
		if bestIdx == -1 {
			break
		}
		merged := append([]string{}, symbols[:bestIdx]...)
		merged = append(merged, symbols[bestIdx]+symbols[bestIdx+1])
		merged = append(merged, symbols[bestIdx+2:]...)
		symbols = merged
	}
	return symbols
}
```

## Questions
### Why do LLM providers measure usage in tokens rather than words or characters?
difficulty: easy
askedAt: LLM fundamentals — common in applied-AI interviews
Tokens are the actual unit the model computes over — every token costs one step of attention and one row of the output distribution. Characters are too fine-grained (a word explodes into many), whole words are too coarse (the vocabulary would need to cover every inflection and typo); subword tokens are the granularity that keeps sequences short while still handling any input.

### Why can the same word tokenise to a different number of tokens depending on where it appears in a sentence?
difficulty: medium
askedAt: LLM fundamentals, tokenisation-specific interview questions
Byte-level BPE tokenisers usually treat the leading space as part of the token, so `"cat"` and `" cat"` are distinct vocabulary entries with potentially different merge histories. A word at the very start of a prompt (no leading space) can therefore tokenise differently — sometimes into more pieces — than the same word appearing mid-sentence.

## References
