package dev.mks.algoatlas.content

import dev.mks.algoatlas.model.ComplexityRow
import dev.mks.algoatlas.model.Difficulty
import dev.mks.algoatlas.model.Lang
import dev.mks.algoatlas.model.Level
import dev.mks.algoatlas.model.Question
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.viz.coinChangeScene

val CoinChange = Topic(
    id = "coin-change",
    title = "Coin Change",
    tagline = "Where greed fails, and a table succeeds.",
    level = Level.ADVANCED,
    scene = { coinChangeScene() },

    intuition = listOf(
        "Making change with the fewest coins feels like it should be easy, because you already do it. Take the largest coin that fits, repeat. That is the greedy strategy, and with British or American currency it happens to be correct — which is exactly why the failure is so instructive when it comes.",
        "Take coins of **1, 3 and 4**, and try to make **6**. Greedy grabs the 4, then needs 2 more and can only manage 1 + 1: three coins. But **3 + 3** is two coins. Greedy did not make an arithmetic error; it made a *structural* one. Taking the biggest coin looked best locally and closed off the better answer, and no amount of care at each step recovers it. Whether greedy works on a coin system is a property of that system, not of the algorithm.",
        "So if you cannot decide one coin at a time, what can you do? Consider every first coin, and trust that the smaller problem left behind is already solved. The fewest coins for 6 is one more than the fewest coins for 6−1, 6−3 or 6−4, whichever is smallest. That recursion is correct immediately — the trouble is that it recomputes the same subproblems endlessly, and the work explodes exponentially.",
        "**Dynamic programming is that recursion, with the answers written down.** The subproblems overlap heavily — the fewest coins for 2 is needed by 3, 5 and 6 alike — so computing it once and storing it collapses exponential work into a table you fill in one pass. Two ways to arrange the same idea: **memoisation** keeps the recursion and caches results on the way down, and **tabulation** starts from the smallest case and builds up. The table is the honest picture, and it is what the visualisation shows.",
        "The condition that makes this legal is worth naming, because it is what interviewers are really testing. A problem yields to DP when it has **optimal substructure** — the best answer is built from best answers to smaller versions — and **overlapping subproblems**, so caching actually saves work. Merge sort has the first and not the second, which is why divide and conquer suits it and a table would be pointless.",
        "Reading the answer off the table is the last trick. `best[6] = 2` tells you *how many* coins, not which. If you need the coins themselves, either store the choice made at each cell or walk backwards afterwards, checking which predecessor cell is exactly one less. Interviews ask for the count; production usually wants the coins.",
    ),

    origin = "**Richard Bellman** developed dynamic programming at the **RAND Corporation in the 1950s**, and named it with a caution that had nothing to do with mathematics. In his autobiography *Eye of the Hurricane* (1984) he explains that the Secretary of Defense at the time was hostile to anything resembling research, so a word was needed that could not be objected to. *Programming* meant planning and scheduling, as it still does in *linear programming* — nothing to do with writing code. *Dynamic* was chosen partly because it described the multi-stage decision processes he was studying, and partly, in his own account, because it was impossible to use in a pejorative sense. The **Bellman equation** that came out of this work is still the foundation of reinforcement learning, so the name outlived the politics by a considerable margin.",

    keyPoints = listOf(
        "**Greedy is not universally correct.** With coins 1, 3, 4 and a target of 6 it returns three coins where two suffice.",
        "DP applies when there is **optimal substructure** *and* **overlapping subproblems**. Without the overlap, caching buys nothing.",
        "**Memoisation** is top-down: keep the recursion, cache each result. **Tabulation** is bottom-up: fill smallest to largest. Same answers, same complexity.",
        "Initialise the impossible cells to a sentinel — infinity, or `target + 1`. Using `-1` and then adding one to it is a common source of silent wrongness.",
        "`best[0] = 0` is the base case that makes the whole table work: zero coins make zero.",
        "The table gives **counts**, not the coins themselves. Recovering the actual coins needs a parent array or a backwards walk.",
        "Complexity is **O(target × coins)** time and O(target) space — pseudo-polynomial, because it scales with the *value* of the target, not the number of digits in it.",
    ),

    complexity = listOf(
        ComplexityRow("Tabulation", "O(target × coins)", "O(target)", "One pass per amount, considering each coin. The standard answer."),
        ComplexityRow("Memoisation", "O(target × coins)", "O(target)", "Same bound, plus recursion frames — which can overflow for a large target."),
        ComplexityRow("Naive recursion", "O(coins^target)", "O(target)", "Correct and unusable. Every subproblem is recomputed from scratch."),
        ComplexityRow("Greedy", "O(coins log coins)", "O(1)", "Fast, and **wrong** on coin systems where the largest coin is not always safe."),
        ComplexityRow("Recovering the coins", "O(target)", "O(target)", "Walk back through the table, or store the choice made at each cell."),
    ),

    pitfalls = listOf(
        "Assuming greedy works because it works with real money. It is a property of the coin system, not of the approach — and interviewers pick systems where it breaks.",
        "Using `-1` or `0` as the \"impossible\" marker and then adding one to it. Use `target + 1` as infinity: it is larger than any real answer and never overflows.",
        "Forgetting `best[0] = 0`. Every other cell is derived from it, so the whole table comes out wrong rather than slightly off.",
        "Swapping the loop order in **Coin Change II**. Coins outside, amounts inside counts *combinations*; the reverse counts *permutations*, and 1+3 becomes different from 3+1.",
        "Reaching for memoised recursion on a large target and overflowing the call stack. Tabulation has no stack to overflow.",
        "Reporting the table value as the answer when the question asked which coins. `best[n]` is a count; the coins need reconstructing.",
    ),

    code = mapOf(
        Lang.KOTLIN to """
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
            // One more coin than whatever was left after taking this one.
            best[amount] = minOf(best[amount], best[amount - coin] + 1)
        }
    }

    return if (best[target] == impossible) -1 else best[target]
}

/** Top-down: the same recursion, with results remembered. */
fun coinChangeMemo(coins: List<Int>, target: Int): Int {
    val cache = HashMap<Int, Int>()

    fun fewest(amount: Int): Int {
        if (amount == 0) return 0
        if (amount < 0) return -1
        cache[amount]?.let { return it }

        var best = -1
        for (coin in coins) {
            val rest = fewest(amount - coin)
            if (rest >= 0 && (best < 0 || rest + 1 < best)) best = rest + 1
        }

        cache[amount] = best
        return best
    }

    return fewest(target)
}

/** Which coins, not just how many — walk the table backwards. */
fun coinsUsed(coins: List<Int>, target: Int): List<Int> {
    val impossible = target + 1
    val best = IntArray(target + 1) { impossible }
    best[0] = 0
    for (amount in 1..target) {
        for (coin in coins) {
            if (coin <= amount) best[amount] = minOf(best[amount], best[amount - coin] + 1)
        }
    }
    if (best[target] == impossible) return emptyList()

    val used = mutableListOf<Int>()
    var amount = target
    while (amount > 0) {
        // The coin that took us here is the one whose predecessor is exactly
        // one cheaper.
        val coin = coins.first { it <= amount && best[amount - it] == best[amount] - 1 }
        used += coin
        amount -= coin
    }
    return used
}
        """.trim(),

        Lang.GO to """
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
			// One more coin than whatever was left after taking this one.
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

// CoinsUsed returns which coins, not just how many.
func CoinsUsed(coins []int, target int) []int {
	impossible := target + 1
	best := make([]int, target+1)
	for i := range best {
		best[i] = impossible
	}
	best[0] = 0

	for amount := 1; amount <= target; amount++ {
		for _, coin := range coins {
			if coin <= amount && best[amount-coin]+1 < best[amount] {
				best[amount] = best[amount-coin] + 1
			}
		}
	}
	if best[target] == impossible {
		return nil
	}

	used := []int{}
	for amount := target; amount > 0; {
		for _, coin := range coins {
			// The coin that took us here is the one whose predecessor is
			// exactly one cheaper.
			if coin <= amount && best[amount-coin] == best[amount]-1 {
				used = append(used, coin)
				amount -= coin
				break
			}
		}
	}
	return used
}
        """.trim(),

        Lang.JAVASCRIPT to """
/**
 * Bottom-up: fewest coins for every amount from 0 to target.
 * Returns -1 when the target cannot be made at all.
 */
function coinChange(coins, target) {
  // target + 1 is larger than any real answer, and unlike Infinity it stays
  // an integer when we add one to it.
  const impossible = target + 1;
  const best = new Array(target + 1).fill(impossible);
  best[0] = 0; // zero coins make zero — the base case everything rests on

  for (let amount = 1; amount <= target; amount++) {
    for (const coin of coins) {
      if (coin > amount) continue;
      // One more coin than whatever was left after taking this one.
      best[amount] = Math.min(best[amount], best[amount - coin] + 1);
    }
  }

  return best[target] === impossible ? -1 : best[target];
}

/** Top-down: the same recursion, with results remembered. */
function coinChangeMemo(coins, target, cache = new Map()) {
  if (target === 0) return 0;
  if (target < 0) return -1;
  if (cache.has(target)) return cache.get(target);

  let best = -1;
  for (const coin of coins) {
    const rest = coinChangeMemo(coins, target - coin, cache);
    if (rest >= 0 && (best < 0 || rest + 1 < best)) best = rest + 1;
  }

  cache.set(target, best);
  return best;
}

/** Which coins, not just how many — walk the table backwards. */
function coinsUsed(coins, target) {
  const impossible = target + 1;
  const best = new Array(target + 1).fill(impossible);
  best[0] = 0;
  for (let amount = 1; amount <= target; amount++) {
    for (const coin of coins) {
      if (coin <= amount) best[amount] = Math.min(best[amount], best[amount - coin] + 1);
    }
  }
  if (best[target] === impossible) return [];

  const used = [];
  let amount = target;
  while (amount > 0) {
    // The coin that took us here is the one whose predecessor is exactly
    // one cheaper.
    const coin = coins.find((c) => c <= amount && best[amount - c] === best[amount] - 1);
    used.push(coin);
    amount -= coin;
  }
  return used;
}
        """.trim(),
    ),

    steps = listOf(
        "**Make a table** with one cell per amount from 0 up to the target.",
        "**Set every cell to infinity** — here `target + 1`, which is larger than any real answer and safe to add one to.",
        "**Set `best[0] = 0`.** Zero coins make zero; every other cell is eventually derived from this.",
        "**For each amount, try every coin** that is not larger than it. The candidate answer is `best[amount − coin] + 1`.",
        "**Keep the smallest candidate.** That cell is now final and will never need revisiting — which is what makes one pass enough.",
        "**Read `best[target]`.** If it is still infinity, the target cannot be made from these coins at all.",
    ),

    questions = listOf(
        Question(
            id = 322,
            title = "Coin Change",
            difficulty = Difficulty.MEDIUM,
            idea = "The problem this topic is built on. The whole test is whether you notice greedy is wrong — say so explicitly with a counterexample like coins 1, 3, 4 and target 6, then build the table. Remember to return -1 rather than the sentinel when the target is unreachable.",
            askedAt = "Amazon, Google, Meta — the standard first DP question",
        ),
        Question(
            id = 70,
            title = "Climbing Stairs",
            difficulty = Difficulty.EASY,
            idea = "The gentlest possible DP, and worth doing first: ways to reach step n is ways to reach n−1 plus ways to reach n−2. That is Fibonacci wearing a hat. Since only the last two values matter, the array collapses to two variables and O(1) space — recognising that is the follow-up they want.",
            askedAt = "Very common as an opener",
        ),
        Question(
            id = 518,
            title = "Coin Change II",
            difficulty = Difficulty.MEDIUM,
            idea = "Counting combinations rather than minimising coins, and it hides the sharpest trap in beginner DP: **loop order decides the answer**. Coins in the outer loop counts combinations; amounts outer counts permutations, so 1+3 and 3+1 are double-counted. Being able to explain *why* is the real question.",
            askedAt = "Amazon, Google",
        ),
    ),

    related = listOf("dfs", "merge-sort"),
    references = Refs.basecs(),
)
