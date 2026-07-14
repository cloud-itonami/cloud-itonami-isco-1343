# Contributing

Contributions are welcome. Please start with a GitHub issue or discussion to
outline your proposal before writing code.

When contributing:

1. Write `.cljc` (portable Clojure) — no JVM-only constructs
2. Add tests for any new behavior
3. Ensure all tests pass: `clojure -M:test`
4. Do not commit changes that weaken the scope boundary (administrative support only)
5. Any change to the governor's hard or escalation rules requires issue discussion first

## Scope Boundary

Remember: **this actor is administrative support only**. Clinical decisions, medications,
care plans, and medical determinations must remain with licensed care staff. PRs
introducing clinical decision-making will be rejected.

## License

Contributions are licensed under AGPL-3.0-or-later.
