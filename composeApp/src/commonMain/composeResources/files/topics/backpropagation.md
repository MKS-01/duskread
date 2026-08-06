---
id: backpropagation
title: Backpropagation
tagline: One chain-rule pass tells every weight in the network how much it cost.
level: intermediate
related: gradient-descent, attention, transformer-architecture
---

## Note
- Computes the gradient of the loss with respect to **every** weight in a network in one backward pass, by applying the chain rule from the output back to the input.
- Without it, gradient descent has no direction to step in for a multi-layer network — backprop is what makes deep learning's "deep" trainable at all.
- Cost is **O(one forward pass)** for the whole network, regardless of how many weights there are — the saving comes entirely from reusing shared partial derivatives.
- Backprop computes a **gradient**; it does not itself decide how far to step — that's gradient descent's job, applied afterward using the values backprop produces.
- Needs every operation in the forward pass to be **differentiable** (or piecewise, like ReLU); modern frameworks (PyTorch, TensorFlow) implement this generically as **autodiff**.

## Read More
Yes you should understand backprop | https://karpathy.medium.com/yes-you-should-understand-backprop-e2f06eab496b | Andrej Karpathy

## Code: Kotlin
```kotlin
/** Backprop through y = w2 * relu(w1 * x) against squared error, chain rule one step at a time. */
fun backpropTiny(x: Double, target: Double, w1: Double, w2: Double): Pair<Double, Double> {
    val z1 = w1 * x
    val h = maxOf(0.0, z1) // ReLU
    val yHat = w2 * h

    val dLoss_dYHat = 2 * (yHat - target)
    val dLoss_dW2 = dLoss_dYHat * h

    val dH_dZ1 = if (z1 > 0) 1.0 else 0.0 // ReLU's derivative
    val dLoss_dW1 = dLoss_dYHat * w2 * dH_dZ1 * x

    return dLoss_dW1 to dLoss_dW2
}
```

## Code: Go
```go
// BackpropTiny backprops through y = w2 * relu(w1 * x) against squared
// error, chain rule one step at a time.
func BackpropTiny(x, target, w1, w2 float64) (dLossDW1, dLossDW2 float64) {
	z1 := w1 * x
	h := math.Max(0.0, z1) // ReLU
	yHat := w2 * h

	dLossDYHat := 2 * (yHat - target)
	dLossDW2 = dLossDYHat * h

	dHDZ1 := 0.0
	if z1 > 0 {
		dHDZ1 = 1.0 // ReLU's derivative
	}
	dLossDW1 = dLossDYHat * w2 * dHDZ1 * x

	return dLossDW1, dLossDW2
}
```

## Questions
### Why does backpropagation cost roughly the same as a forward pass, rather than one pass per weight?
difficulty: medium
askedAt: ML fundamentals — common conceptual check in ML-systems interviews
Because the chain rule lets partial derivatives be reused: the gradient arriving at a layer is exactly what the layer behind it needs to keep going, computed once and passed along. Naively perturbing each weight and re-running the forward pass would cost O(number of weights) forward passes instead of one.

### What causes vanishing gradients, and what are the standard fixes?
difficulty: medium
askedAt: Deep learning interviews, common at ML-infra-adjacent companies
Chaining many derivatives smaller than 1 through a deep network shrinks the gradient exponentially by the time it reaches early layers, so those layers barely update. Standard fixes include ReLU-family activations (derivative 1, not shrinking, where active), residual/skip connections that give gradients a shorter path back, and normalisation layers that keep activations in a well-scaled range.

## References
