---
id: transformer-architecture
title: Transformer Architecture
tagline: Drop the recurrence, keep only attention and feed-forward layers.
level: advanced
related: attention, backpropagation
---

## Note
- Replaces the recurrent connections in older sequence models entirely with **self-attention**, so every position can look at every other position directly, in parallel, instead of one step at a time.
- A block is two sub-layers — self-attention, then a position-wise feed-forward network — each wrapped in a residual connection and layer normalisation.
- Removing recurrence means positions carry no inherent order, so **positional encoding** is added to the input embeddings up front to put order back in.
- **Encoder-decoder** in the original paper — an encoder stack builds a representation of the input, a decoder stack attends to it while generating output — but many later models use just one half (BERT: encoder-only, GPT: decoder-only).
- **Residual connections** around both sub-layers let gradients skip past them entirely, the same fix deep CNNs use, essential once stacks reach dozens of blocks.
- Underlies essentially every large language model since 2018.

## Read More
The Illustrated Transformer | https://jalammar.github.io/illustrated-transformer/ | Jay Alammar

## Code: Kotlin
```kotlin
/** One transformer encoder block: self-attention then feed-forward, each with a residual connection. */
fun transformerBlock(
    input: List<DoubleArray>,
    selfAttention: (List<DoubleArray>) -> List<DoubleArray>,
    feedForward: (DoubleArray) -> DoubleArray,
    layerNorm: (DoubleArray) -> DoubleArray,
): List<DoubleArray> {
    val attended = selfAttention(input)
    val afterAttention = input.indices.map { i ->
        layerNorm(DoubleArray(input[i].size) { d -> input[i][d] + attended[i][d] }) // residual + norm
    }
    val fedForward = afterAttention.map { feedForward(it) }
    return afterAttention.indices.map { i ->
        layerNorm(DoubleArray(afterAttention[i].size) { d -> afterAttention[i][d] + fedForward[i][d] }) // residual + norm
    }
}
```

## Code: Go
```go
// TransformerBlock is self-attention then feed-forward, each with a residual connection.
func TransformerBlock(
	input [][]float64,
	selfAttention func([][]float64) [][]float64,
	feedForward func([]float64) []float64,
	layerNorm func([]float64) []float64,
) [][]float64 {
	attended := selfAttention(input)
	afterAttention := make([][]float64, len(input))
	for i := range input {
		sum := make([]float64, len(input[i]))
		for d := range sum {
			sum[d] = input[i][d] + attended[i][d] // residual
		}
		afterAttention[i] = layerNorm(sum)
	}

	output := make([][]float64, len(afterAttention))
	for i := range afterAttention {
		ff := feedForward(afterAttention[i])
		sum := make([]float64, len(afterAttention[i]))
		for d := range sum {
			sum[d] = afterAttention[i][d] + ff[d] // residual
		}
		output[i] = layerNorm(sum)
	}
	return output
}
```

## Questions
### Why does the transformer need positional encoding at all?
difficulty: easy
askedAt: Transformer-architecture fundamentals, common warm-up question
Self-attention computes the same output regardless of token order — it's permutation-invariant, since every position compares against every other position with no notion of "before" or "after". Positional encoding is added to the embeddings specifically to reintroduce order, since nothing else in the architecture carries it.

### What's the difference between an encoder-only, decoder-only, and encoder-decoder transformer?
difficulty: medium
askedAt: LLM-architecture interviews, common at model-focused companies
Encoder-only (BERT) sees the whole input at once via bidirectional self-attention and suits understanding tasks like classification. Decoder-only (GPT) uses masked (causal) self-attention so each position can only see earlier positions, suiting generation. Encoder-decoder (the original transformer, T5) keeps both, adding cross-attention so the decoder can attend to the encoder's output — suited to tasks like translation where input and output are distinct sequences.

## References
