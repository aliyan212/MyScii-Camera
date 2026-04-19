# Architecture

## Goals

- Stable real-time ASCII preview on mid-tier devices.
- Predictable frame-time behavior under load.
- Clean separation between processing and UI layers.

## Modules

- `app`: Android UI, CameraX integration, lifecycle, permissions.
- `ascii-engine`: pure Kotlin conversion and adaptive quality logic.

## Runtime pipeline

1. CameraX provides YUV frames.
2. Analyzer reads Y-plane luminance bytes.
3. `AsciiConverter` samples and transforms brightness into glyphs.
4. UI displays ASCII frame in a monospace overlay.
5. Save flow persists `.txt` and `.png`, then updates capture index.

## Performance strategy

- Use latest-frame backpressure for analysis.
- Keep allocations low in frame loop.
- Move heavy work off main thread.
- Adapt output density based on frame budget.

## Planned evolution

- PNG file sharing via `FileProvider`.
- Search and filtering for captures.
- Visual preset system and theme tokens.
