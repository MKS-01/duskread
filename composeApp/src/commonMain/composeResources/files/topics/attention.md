---
id: attention
title: Attention
tagline: Let every token look directly at the tokens that matter to it.
level: intermediate
related: transformer-architecture, backpropagation
---

## Note
- Lets a model weigh every other position in a sequence when producing a representation for one position, instead of compressing the whole sequence through a single fixed-size bottleneck.
- Computed as `softmax(QKᵀ / √d_k) · V` — a query for the current position is compared against a key for every position, turned into weights, and used to blend their values.
- **Query, key, value** are the three learned projections of each token; the query/key comparison decides *how much* attention, the value is *what* gets passed through.
- The `√d_k` scaling factor exists because dot products grow with dimension, which pushes softmax into a region with vanishingly small gradients.
- **Self-attention** costs O(n²) in sequence length — every token compares against every other token — which is the main scaling limit for long contexts.
- **Multi-head** attention runs several attention computations in parallel with different learned projections, letting different heads specialise in different kinds of relationship.

## Read More
The Illustrated Transformer | https://jalammar.github.io/illustrated-transformer/ | Jay Alammar

## Code: Kotlin
```kotlin
/** Scaled dot-product attention for one query against a set of keys/values. */
fun attention(query: DoubleArray, keys: List<DoubleArray>, values: List<DoubleArray>): DoubleArray {
    val dk = query.size
    val scores = keys.map { key -> query.zip(key.toList()) { q, k -> q * k }.sum() / kotlin.math.sqrt(dk.toDouble()) }
    val maxScore = scores.max()
    val expScores = scores.map { kotlin.math.exp(it - maxScore) } // subtract max for numerical stability
    val weights = expScores.map { it / expScores.sum() }

    val output = DoubleArray(values[0].size)
    for (i in weights.indices) {
        for (d in output.indices) output[d] += weights[i] * values[i][d]
    }
    return output
}
```

## Code: Go
```go
// Attention computes scaled dot-product attention for one query against a set of keys/values.
func Attention(query []float64, keys, values [][]float64) []float64 {
	dk := float64(len(query))
	scores := make([]float64, len(keys))
	maxScore := math.Inf(-1)
	for i, key := range keys {
		dot := 0.0
		for d := range query {
			dot += query[d] * key[d]
		}
		scores[i] = dot / math.Sqrt(dk)
		if scores[i] > maxScore {
			maxScore = scores[i]
		}
	}

	weights := make([]float64, len(scores))
	sum := 0.0
	for i, s := range scores {
		weights[i] = math.Exp(s - maxScore) // subtract max for numerical stability
		sum += weights[i]
	}

	output := make([]float64, len(values[0]))
	for i, w := range weights {
		w /= sum
		for d := range output {
			output[d] += w * values[i][d]
		}
	}
	return output
}
```

## Questions
### Why divide by √d_k in scaled dot-product attention?
difficulty: medium
askedAt: NLP/deep-learning interviews, common at ML-focused companies
Dot products grow in magnitude as dimension increases, and large scores push softmax's inputs into a region where its gradient is near zero — training stalls. Dividing by `√d_k` keeps the scores' variance roughly constant regardless of dimension, so softmax stays in a range where gradients are useful.

### What's the difference between self-attention and cross-attention?
difficulty: easy
askedAt: Transformer-architecture fundamentals, frequently asked as a warm-up
Self-attention has queries, keys and values all drawn from the same sequence — a sentence attending to itself. Cross-attention draws queries from one sequence (the decoder) and keys/values from another (the encoder's output) — it's how a translation model lets the output attend back to the original input.

## References
