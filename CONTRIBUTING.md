# Contributing

Thanks for helping improve ASCII Vibe Camera.

## Development workflow

1. Fork the repository and create a branch from `main`.
2. Keep pull requests focused and small.
3. Add tests for behavior changes where possible.
4. Update docs when behavior or interfaces change.

## Branch naming

Use one of these prefixes:

- `feat/<short-description>`
- `fix/<short-description>`
- `docs/<short-description>`
- `refactor/<short-description>`
- `test/<short-description>`

## Commit message guideline

Use Conventional Commits where possible:

- `feat: add adaptive density control`
- `fix: avoid frame buffer overrun`
- `docs: update setup instructions`

## Code style

- Prefer simple, readable functions.
- Keep allocations low in frame processing paths.
- Avoid blocking UI thread.
- Add concise comments only for non-obvious logic.

## Pull request checklist

- [ ] Build/test passes locally.
- [ ] New or changed behavior includes tests when practical.
- [ ] No unrelated formatting or refactors.
- [ ] Documentation updated if needed.
- [ ] Screenshots or short clip attached for UI changes.

## Local setup

Use the committed Gradle wrapper:

```bash
./gradlew :ascii-engine:test :app:assembleDebug
```

Recommended before opening a PR:

```bash
./gradlew :ascii-engine:test :app:testDebugUnitTest :app:assembleDebug
```

## Release-aware contributions

- If a PR changes user-visible behavior, add a short changelog entry proposal in the PR description.
- If a PR changes licensing, dependencies, or permissions, call that out explicitly.
- For release preparation work, follow [docs/RELEASE.md](docs/RELEASE.md).
