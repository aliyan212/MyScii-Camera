# Release Guide

This guide is a practical checklist for shipping a release on GitHub and preparing for FOSS distribution.

## 1. Pre-release checks

Run from repository root:

```bash
./gradlew :ascii-engine:test :app:testDebugUnitTest :app:assembleDebug
```

Verify:

- `README.md` reflects current behavior.
- `CHANGELOG.md` has a section for the new version.
- `LICENSE`, `SECURITY.md`, and `CODE_OF_CONDUCT.md` are present and accurate.

## 2. Versioning

Pick a release version:

- Preview examples: `v0.1.0`, `v0.2.0`
- Patch examples: `v0.1.1`

Update any version references in docs/changelog before tagging.

## 3. Create Git tag

```bash
git checkout main
git pull --ff-only
git tag -a v0.1.0 -m "ASCII Vibe Camera v0.1.0"
git push origin v0.1.0
```

## 4. GitHub Release

On GitHub:

1. Open `Releases` -> `Draft a new release`.
2. Select the tag.
3. Title format: `ASCII Vibe Camera vX.Y.Z`.
4. Use release notes from `CHANGELOG.md`.
5. Attach artifacts (APK, checksums, optional source bundle).

Recommended attached assets:

- `app-debug.apk` or signed release APK/AAB
- `SHA256SUMS.txt`

## 5. FOSS distribution readiness

Before submission to FOSS catalogs/stores:

- Ensure app does not require proprietary services.
- Ensure all dependencies are license-compatible.
- Verify permissions are minimal and documented.
- Provide reproducible build instructions.
- Provide source tag that matches the binary build.

Useful metadata to publish:

- App name
- Short/long description
- License (MIT)
- Source repository URL
- Issue tracker URL
- Latest version and tag

## 6. Post-release

- Announce release with highlights and known limitations.
- Open milestone for next version.
- Triage incoming issues for regressions.
