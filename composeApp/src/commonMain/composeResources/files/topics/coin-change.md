---
id: coin-change
title: Coin Change
tagline: Where greed fails, and a table succeeds.
level: advanced
related: dfs, merge-sort
---

## Note
- **Greedy is not universally correct.** With coins 1, 3, 4 and a target of 6 it returns three coins where two suffice.
- DP applies when there is **optimal substructure** *and* **overlapping subproblems**. Without the overlap, caching buys nothing.
- **Memoisation** is top-down: keep the recursion, cache each result. **Tabulation** is bottom-up: fill smallest to largest. Same answers, same complexity.
- Initialise the impossible cells to a sentinel — infinity, or `target + 1`. Using `-1` and then adding one to it is a common source of silent wrongness.
- The table gives **counts**, not the coins themselves. Recovering the actual coins needs a parent array or a backwards walk.
- Complexity is **O(target × coins)** time and O(target) space — pseudo-polynomial, because it scales with the *value* of the target, not the number of digits in it.

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

## Code: Kotlin
```kotlin
/**
 * Bottom-up: fewest coins for every amount from 0 to target.
 * Returns -1 when the target cannot be made at all.
 */
fun coinChange(coins: List<Int>, target: Int): Int {
    // target + 1 is larger than any real answer, and unlike Int.MAX_VALUE
    // it cannot overflow when we add one to it.
    val impossible = target + 1
    val best = IntArray(target + 1) { impossible }
    best[0] = 0 // zero coins make zero — the base case everything rests on

    for (amount in 1..target) {
        for (coin in coins) {
            if (coin > amount) continue
            best[amount] = minOf(best[amount], best[amount - coin] + 1)
        }
    }
    return if (best[target] == impossible) -1 else best[target]
}
```

## Code: Go
```go
// CoinChange returns the fewest coins making target, or -1 if impossible.
func CoinChange(coins []int, target int) int {
	// target+1 is larger than any real answer, and unlike math.MaxInt it
	// cannot overflow when we add one to it.
	impossible := target + 1
	best := make([]int, target+1)
	for i := range best {
		best[i] = impossible
	}
	best[0] = 0 // zero coins make zero — the base case everything rests on

	for amount := 1; amount <= target; amount++ {
		for _, coin := range coins {
			if coin > amount {
				continue
			}
			if best[amount-coin]+1 < best[amount] {
				best[amount] = best[amount-coin] + 1
			}
		}
	}

	if best[target] == impossible {
		return -1
	}
	return best[target]
}
```

## Questions
### Coin Change
id: 322
difficulty: medium
askedAt: Amazon, Google, Meta — the standard first DP question
The problem this topic is built on. The whole test is whether you notice greedy is wrong — say so explicitly with a counterexample like coins 1, 3, 4 and target 6, then build the table. Remember to return -1 rather than the sentinel when the target is unreachable.

### Climbing Stairs
id: 70
difficulty: easy
askedAt: Very common as an opener
The gentlest possible DP, and worth doing first: ways to reach step n is ways to reach n−1 plus ways to reach n−2. That is Fibonacci wearing a hat. Since only the last two values matter, the array collapses to two variables and O(1) space — recognising that is the follow-up they want.

### Coin Change II
id: 518
difficulty: medium
askedAt: Amazon, Google
Counting combinations rather than minimising coins, and it hides the sharpest trap in beginner DP: **loop order decides the answer**. Coins in the outer loop counts combinations; amounts outer counts permutations, so 1+3 and 3+1 are double-counted. Being able to explain *why* is the real question.

## References
