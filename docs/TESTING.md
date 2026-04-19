# Testing Guide

This repository uses a simple test pyramid:

1. JVM unit tests for deterministic logic.
2. Android unit tests for app-side utilities.
3. Android instrumentation tests for UI behavior.

## Current test suites

## `ascii-engine` JVM tests

Focus:

- ASCII conversion behavior.
- Adaptive quality scaling behavior.

Location:

- `ascii-engine/src/test/kotlin/...`

Command:

```bash
gradle :ascii-engine:test
```

## `app` local unit tests

Focus:

- Pure UI/domain utility logic that should not require a device.

Location:

- `app/src/test/java/...`

Command:

```bash
gradle :app:testDebugUnitTest
```

## `app` instrumentation tests

Focus:

- Compose screen behavior in an Android runtime.

Location:

- `app/src/androidTest/java/...`

Command:

```bash
gradle :app:connectedDebugAndroidTest
```

Requires:

- Running emulator or device.

## CI coverage

GitHub Actions currently runs:

- `:ascii-engine:test`
- `:app:testDebugUnitTest`

Workflow:

- `.github/workflows/tests.yml`

## Test quality rules

- Keep unit tests fast and deterministic.
- Prefer testing pure logic over framework plumbing in local unit tests.
- Use instrumentation tests for UI and permission flows.
- Add at least one regression test for bug fixes in core logic.

## Known local blocker

The current shell environment does not have Gradle installed. Once Gradle wrapper is committed, use wrapper commands:

```bash
./gradlew :ascii-engine:test :app:testDebugUnitTest
```
