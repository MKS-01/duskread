---
id: evals
title: Evals
tagline: Measure whether a change made the model better, not just different.
level: intermediate
related: tool-use, planning-loops
---

## Note
- A test suite for model behaviour: a fixed set of inputs with a way to score each output, run automatically whenever the model, prompt, or pipeline changes.
- Two broad scoring styles — **programmatic** checks (exact match, regex, code executes) and **model-graded** checks, an **LLM-as-judge**, for anything too fuzzy to string-match.
- Without evals, "I improved the prompt" is a guess; with them it's a **pass rate** you can compare before and after a change.
- A pass rate summarises the *suite*, not the model — it only measures what the cases happen to cover.
- The harder problem is usually not running the eval but writing cases that actually catch the failure mode you care about — a suite of easy cases can sit at 100% while missing the real regression.
- An **LLM-as-judge** needs its own validation — spot-check its grades against human judgement, or it just moves the uncertainty up one level instead of removing it.

## Code: Kotlin
```kotlin
data class EvalCase(val input: String, val expected: String)

// Runs every case through `run` and scores it with `grade` — grade might be
// exact match or a call out to a judge model, the loop doesn't care which.
fun runEvalSuite(cases: List<EvalCase>, run: (String) -> String, grade: (actual: String, expected: String) -> Boolean): Double {
    val passed = cases.count { case -> grade(run(case.input), case.expected) }
    return passed.toDouble() / cases.size
}
```

## Code: Go
```go
// EvalCase is one fixed input/expected pair in a suite.
type EvalCase struct {
	Input    string
	Expected string
}

// RunEvalSuite runs every case through run and scores it with grade,
// returning the fraction that passed.
func RunEvalSuite(cases []EvalCase, run func(string) string, grade func(actual, expected string) bool) float64 {
	passed := 0
	for _, c := range cases {
		if grade(run(c.Input), c.Expected) {
			passed++
		}
	}
	return float64(passed) / float64(len(cases))
}
```

## Questions
### How do you evaluate something with no single correct answer, like a summary?
difficulty: medium
askedAt: LLM evaluation interviews
Exact match doesn't apply, so scoring shifts to an LLM-as-judge against an explicit rubric (covers the key points, right length, right tone), or to a narrower programmatic proxy (does it mention the required facts) when one exists. The rubric itself has to be validated against human judgement occasionally, or the eval just measures agreement with the judge model's taste.

### What's the risk of using an LLM as the judge in your own eval suite?
difficulty: medium
askedAt: LLM evaluation, agentic-systems interviews
The judge inherits whatever biases and blind spots the underlying model has — favouring longer or more confidently worded answers, or sharing the same failure mode as the model being graded if they're closely related. Treating judge scores as ground truth without periodic human spot-checks lets a suite look healthy while actually just agreeing with itself.

## References
