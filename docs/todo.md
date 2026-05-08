# Todo

## Remaining gaps from PLAN.md edge cases

- [x] **Observe runtime permission changes** — `refreshTrigger` + `flatMapLatest` restarts sensor flows on `ON_RESUME`, re-checking permissions.

## Beyond the plan

- [x] **App icon** — Shield + eye on green adaptive icon with fallback PNGs.
- [x] **Dark mode** — Material You dynamic colors on API 31+, green-seeded fallback below.
- [ ] **Release signing** — No signing config in `build.gradle.kts`. Add keystore config for release builds.
- [ ] **CI** — No CI configuration (GitHub Actions, etc.).
