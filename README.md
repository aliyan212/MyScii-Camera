# MySCII Camera

Real-time camera-to-ASCII app with a minimal camera-centric UI.

## Open-source status

- License: MIT
- Current stage: 1.0 public preview
- Contributions: welcome

Project governance and contributor docs:

- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Security Policy](SECURITY.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
- [Testing Guide](docs/TESTING.md)
- [Release Guide](docs/RELEASE.md)

## Implemented so far

- Multi-module Kotlin project scaffold
- `ascii-engine` module with fast luminance-to-ASCII conversion, temporal smoothing, and configurable render presets
- Android `app` module with CameraX preview/analyzer integration and a custom high-performance ASCII overlay renderer
- Camera-first UX with centered circular capture control, gallery access, and settings menu
- Capture save flow exporting `.png` into app-private storage
- Indexed capture metadata with in-app gallery preview and share/export
- Runtime camera permission gate for first launch usability
- Continuous test workflow on GitHub Actions

## Current architecture

- `ascii-engine`: pure processing and tests
- `app`: camera acquisition and UI

## Repository quality checklist

- [x] License and governance files
- [x] Issue and PR templates
- [x] Architecture and roadmap docs
- [x] First capture persistence path (TXT + PNG)
- [x] Gradle wrapper committed
- [x] CI pipeline for tests and build
- [x] Initial release notes/changelog scaffold


