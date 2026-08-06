---
id: temperature-sampling
title: Temperature & Sampling
tagline: Decide how much the model is allowed to gamble on its next word.
level: intermediate
related: context-windows
---

## Note
- A model doesn't output "the next word" — it outputs a raw score, a **logit**, for every token in its vocabulary; **softmax** turns those into a probability distribution.
- **Temperature** rescales the logits before softmax: low temperature sharpens the distribution toward the top candidate, high temperature flattens it toward more randomness.
- T = 1 leaves the raw distribution unchanged; T approaching 0 collapses sampling to greedy decoding — always the single highest-probability token.
- **Sampling** draws a token from that distribution instead of always taking the top one — always taking the top one (**greedy decoding**) is deterministic but repetitive.
- **Top-k** and **top-p (nucleus)** sampling filter *which* tokens are eligible before drawing — usually combined with temperature so an implausible token can't be picked at all.

## Read More
How to generate text: using different decoding methods for language generation | https://huggingface.co/blog/how-to-generate | Hugging Face

## Code: Kotlin
```kotlin
import kotlin.math.exp
import kotlin.random.Random

/** Scales logits by temperature, then samples one index via softmax and a weighted draw. */
fun sampleWithTemperature(logits: List<Double>, temperature: Double, random: Random = Random.Default): Int {
    val scaled = logits.map { it / temperature }
    val maxScaled = scaled.max()
    val exps = scaled.map { exp(it - maxScaled) } // subtract max for numerical stability
    val sum = exps.sum()
    val probs = exps.map { it / sum }

    var r = random.nextDouble()
    for (i in probs.indices) {
        r -= probs[i]
        if (r <= 0.0) return i
    }
    return probs.lastIndex
}
```

## Code: Go
```go
// SampleWithTemperature scales logits by temperature, then samples one index
// via softmax and a weighted draw.
func SampleWithTemperature(logits []float64, temperature float64, rnd *rand.Rand) int {
	scaled := make([]float64, len(logits))
	maxScaled := math.Inf(-1)
	for i, l := range logits {
		scaled[i] = l / temperature
		if scaled[i] > maxScaled {
			maxScaled = scaled[i]
		}
	}
	sum := 0.0
	probs := make([]float64, len(scaled))
	for i, s := range scaled {
		probs[i] = math.Exp(s - maxScaled) // subtract max for numerical stability
		sum += probs[i]
	}
	for i := range probs {
		probs[i] /= sum
	}

	r := rnd.Float64()
	for i, p := range probs {
		r -= p
		if r <= 0 {
			return i
		}
	}
	return len(probs) - 1
}
```

## Questions
### What's the practical difference between temperature 0 and temperature 1?
difficulty: easy
askedAt: LLM fundamentals — common in applied-AI interviews
Temperature 0 collapses decoding to greedy: always the single highest-probability token, deterministic across repeated calls. Temperature 1 samples from the model's raw, unscaled probability distribution. Values in between sharpen it toward greedy; values above 1 flatten it toward more randomness.

### Why combine temperature with top-p (nucleus) sampling instead of using temperature alone?
difficulty: medium
askedAt: LLM application-design interviews
Temperature only reshapes an existing distribution — it never removes a token from consideration, so a high enough temperature can still leave non-trivial probability on a nonsense token. Top-p caps the candidate pool to the smallest set covering probability mass p *before* sampling, so the draw only ever happens among plausible continuations regardless of temperature.

## References
