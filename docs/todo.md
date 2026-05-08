# Todo

## Remaining gaps from PLAN.md edge cases

- [ ] **Observe runtime permission changes** — Camera/mic flows check permissions at flow creation, but if the user revokes from Settings while the app is running, status stays stale until the flow restarts. Need to observe permission changes at runtime (e.g., `OnSharedPreferenceChangeListener` or re-check on app resume).

## Beyond the plan

- [x] **App icon** — Shield + eye on green adaptive icon with fallback PNGs.
- [x] **Dark mode** — Material You dynamic colors on API 31+, green-seeded fallback below.
- [ ] **Release signing** — No signing config in `build.gradle.kts`. Add keystore config for release builds.
- [ ] **CI** — No CI configuration (GitHub Actions, etc.).
