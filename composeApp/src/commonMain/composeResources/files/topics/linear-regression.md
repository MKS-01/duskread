---
id: linear-regression
title: Linear Regression
tagline: Draw the line that makes the total error smallest.
level: intermediate
related: gradient-descent
---

## Note
- Fits a straight line to data by minimising the **sum of squared errors** between the line's predictions and the actual values.
- **Squared** error, not absolute error, because it's differentiable everywhere and punishes large misses disproportionately.
- Has a **closed-form solution** — the normal equations — so it doesn't strictly need gradient descent, but the iterative version generalises to every model that comes after it.
- Assumes the relationship is genuinely **linear** and that errors have roughly constant spread — violate that and "optimal" stops meaning "good."
- The name is a historical accident: "regression" comes from Francis Galton's 1886 study of height, nothing to do with lines or errors.

## Read More
Linear regression | https://developers.google.com/machine-learning/crash-course/linear-regression | Google Machine Learning Crash Course

## Code: Kotlin
```kotlin
/** Fits y = w*x by gradient descent on mean squared error. */
fun fitLinearRegression(xs: List<Double>, ys: List<Double>, learningRate: Double = 0.013, steps: Int = 200): Double {
    var w = 0.0
    repeat(steps) {
        var gradSum = 0.0
        for (i in xs.indices) {
            val error = w * xs[i] - ys[i]
            gradSum += xs[i] * error
        }
        w -= learningRate * (2.0 * gradSum / xs.size)
    }
    return w
}
```

## Code: Go
```go
// FitLinearRegression fits y = w*x by gradient descent on mean squared error.
func FitLinearRegression(xs, ys []float64, learningRate float64, steps int) float64 {
	w := 0.0
	for s := 0; s < steps; s++ {
		gradSum := 0.0
		for i := range xs {
			error := w*xs[i] - ys[i]
			gradSum += xs[i] * error
		}
		w -= learningRate * (2.0 * gradSum / float64(len(xs)))
	}
	return w
}
```

## Questions
### Why square the error instead of using the absolute value?
difficulty: easy
askedAt: ML fundamentals — a very common first conceptual question
Squared error is smooth and differentiable everywhere (absolute error has a sharp corner at zero, which gradient-based methods handle badly), and it penalises large errors superlinearly. The trade-off is sensitivity to outliers — absolute error or Huber loss is the standard fix when outliers are a real concern.

### When would you use gradient descent instead of the normal equations for plain linear regression?
difficulty: medium
askedAt: ML fundamentals, systems-flavoured interviews
When the closed form gets expensive: the normal equations cost roughly O(d³) to invert a d×d matrix, fine for hundreds of features and painful for millions. Gradient descent's per-step cost is only O(n·d), so past a certain feature count, iterative fitting wins even though the loss has an exact answer.

## References
