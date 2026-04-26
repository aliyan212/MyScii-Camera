# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project follows Semantic Versioning (pre-1.0).

## [Unreleased]

### Added
- Full-screen gallery experience with swipe navigation between captures.
- Android back-button behavior in gallery: open-photo -> gallery grid -> camera.
- Optional original-frame storage at capture time for A/B comparison in the viewer.
- Full-screen compare toggle: `Show Original` / `Show ASCII`.
- Zoom controls in the full-screen viewer (`Zoom In`, `Zoom Out`, `Reset`).

### Changed
- Gallery header now respects system status bar insets to avoid punch-hole/cutout overlap.
- Capture save flow now snapshots preview bitmap on UI thread before background file writes for stability.

## [0.1.0] - 2026-04-19

### Added
- Fast ASCII rendering pipeline with custom overlay view for smoother camera updates.
- Camera-first interaction model with centered circular capture control.
- In-app gallery with indexed capture preview and export/share support.
- Governance docs and contribution templates for open-source collaboration.
- CI workflow for JVM and Android unit tests.

### Changed
- Reworked converter/frame representation to reduce per-frame allocation pressure.
- Dark visual treatment across camera surface and settings menu.
- Larger default ASCII grid for better viewport coverage.

### Removed
- Saturation boost processing path from the analyzer hot loop to improve performance consistency.
- Legacy adaptive quality and density controls from runtime camera workflow.
