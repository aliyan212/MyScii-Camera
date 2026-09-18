# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and this project follows Semantic Versioning (pre-1.0).

## [1.2.0] - 2026-09-18

### Added
- Minimalist Typographic ASCII Camera app icon and Material 3 adaptive icon system (`[(@)]` lens iris with `mipmap-anydpi-v26` foreground, background, and Android 13+ monochrome theming).
- Full multi-density raster icon assets (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).
- In-app brand vector logo displayed in top camera HUD.
- Material Design 3 theme system (`MySciiTheme`) with dark palette, typography, and shapes.
- Resolution density selector in camera settings: Compact (96x54), Standard (128x72), Fine (160x90).
- Live HUD displaying frame processing latency (ms) and real-time FPS counter.
- Direct front/back camera switch shortcut in the camera HUD.
- "Copy ASCII" action in the gallery and viewer to copy raw ASCII art directly to system clipboard.
- Delete confirmation dialog in the gallery to prevent accidental capture deletions.
- Full edge-to-edge support with safe display cutout and navigation bar system insets.

### Changed
- Refactored `AsciiOverlayView` to dynamically calculate font scale and center the ASCII grid across any screen aspect ratio.
- Switched `PreviewView` implementation mode to `COMPATIBLE` (TextureView) for reliable preview snapshot capturing.
- Fixed camera "Blend" mode by properly coordinating preview alpha and translucent overlay background.
- Upgraded tactile shutter button with mechanical styling, press animation, and haptic feedback.
- Configured JDK 21 in build properties and aligned `ascii-engine` toolchain with Java 17 bytecode target.

### Removed
- Obsolete duplicate gallery components (`PhotoGalleryTab`, `PhotoCard`) from `AsciiCameraScreen`.

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
