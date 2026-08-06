---
id: gradient-descent
title: Gradient Descent
tagline: Follow the slope down, one careful step at a time.
level: intermediate
related: linear-regression, backpropagation
---

## Note
- Repeatedly steps parameters against the **gradient** — the direction of steepest loss increase — until the slope flattens out.
- The **learning rate** sets the step size: too small and training crawls, too large and it overshoots or diverges.
- Guaranteed to reach the *global* minimum only when the loss is convex; on a neural network's tangled loss surface it finds *a* minimum, not necessarily *the* minimum.
- **Batch** descent uses the whole dataset per step (accurate, expensive); **stochastic** uses one example (cheap, noisy); **mini-batch** is the practical default.
- The training loop underneath almost all of modern ML, from a two-parameter regression to a trillion-parameter language model.

## Read More
Linear regression: Gradient descent | https://developers.google.com/machine-learning/crash-course/linear-regression/gradient-descent | Google Machine Learning Crash Course

## Code: Kotlin
```kotlin
/** Steps against the gradient until it settles near the minimum. */
fun gradientDescent(start: Double, learningRate: Double, steps: Int, gradient: (Double) -> Double): Double {
    var w = start
    repeat(steps) { w -= learningRate * gradient(w) }
    return w
}
```

## Code: Go
```go
// GradientDescent steps against the gradient until it settles near the minimum.
func GradientDescent(start, learningRate float64, steps int, gradient func(float64) float64) float64 {
	w := start
	for i := 0; i < steps; i++ {
		w -= learningRate * gradient(w)
	}
	return w
}
```

## Questions
### What happens if the learning rate is too high or too low?
difficulty: easy
askedAt: ML fundamentals — asked in nearly every applied-ML interview
Too high overshoots the minimum and the loss oscillates or diverges; too low converges correctly but wastes compute, sometimes drastically. The practical fix in both directions is usually a **learning-rate schedule** — start larger, decay over training — rather than one fixed value for the whole run.

### Batch, stochastic, or mini-batch — what's the trade-off?
difficulty: medium
askedAt: ML fundamentals, training-infrastructure interviews
It's gradient accuracy versus per-step cost. Batch is exact but touches every example per step; stochastic is cheap but noisy. Mini-batch wins in practice not just as a compromise — it vectorises well on a GPU, and the noise it keeps can help the optimiser escape shallow local minima.

## References
