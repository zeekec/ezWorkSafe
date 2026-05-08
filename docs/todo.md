# Todo

## Remaining gaps from PLAN.md edge cases

- [x] **Observe runtime permission changes** — `refreshTrigger` + `flatMapLatest` restarts sensor flows on `ON_RESUME`, re-checking permissions.

## Beyond the plan

- [x] **App icon** — Shield + eye on green adaptive icon with fallback PNGs.
- [x] **Dark mode** — Material You dynamic colors on API 31+, green-seeded fallback below.
- [x] **Release signing** — Conditional keystore config in `build.gradle.kts` reading from `keystore.properties` (see `keystore.properties.template`).
- [x] **CI** — GitHub Actions workflow (`.github/workflows/android.yml`) builds, lints, and runs unit tests on push/PR to `main`. Decodes keystore from secrets when available.
