<div align="center">

```
     .---.
   _/__o__\_
  |  [(@)]  |    M y S C I I   C A M E R A
  '---------'    Real-Time Camera-to-ASCII for Android
```

# MySCII Camera

**Turn your reality into living, breathing ASCII art in real-time.**

[![Release](https://img.shields.io/github/v/release/aliyan212/MyScii-Camera?style=flat-square&color=00E5FF)](https://github.com/aliyan212/MyScii-Camera/releases)
[![Build Status](https://img.shields.io/github/actions/workflow/status/aliyan212/MyScii-Camera/tests.yml?branch=main&style=flat-square)](https://github.com/aliyan212/MyScii-Camera/actions)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Android Target](https://img.shields.io/badge/Android-SDK%2035%20(15)-green?style=flat-square&logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-blue?style=flat-square)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/github/license/aliyan212/MyScii-Camera?style=flat-square)](LICENSE)

</div>

---

## Overview

**MySCII Camera** is an open-source Android camera app that converts live camera feeds into animated ASCII character matrices with ultra-low latency. Built with Jetpack Compose, CameraX, and a pure-Kotlin rendering engine, it delivers a tactile, camera-centric shooting experience with modern Material Design 3 styling.

---

## ✨ Features

- **⚡ Real-Time ASCII Engine**: High-performance luminance quantization, gamma correction, temporal smoothing to prevent flicker, and configurable glyph lookups.
- **🎨 Dynamic Palette Filters**:
  - **Mono**: High-contrast terminal ivory.
  - **Warm**: Vintage sepia amber glow.
  - **Cool**: Cyberpunk electric cyan.
  - **Neon**: Dynamic rainbow gradient shader.
- **🎛️ Resolution Density Presets**:
  - **Compact** (`96x54`): Chunky retro-terminal aesthetic, maximum FPS.
  - **Standard** (`128x72`): Balanced high-speed real-time capture.
  - **Fine** (`160x90`): High-definition character density.
- **🌓 Dual Display Modes**:
  - **ASCII Only**: Pure character matrix on deep dark background.
  - **Camera Blend**: Live camera video feed subtly blended beneath the character overlay.
- **🖼️ Built-in Gallery & Capture Comparison**:
  - Full-screen swipeable viewer with pinch-to-zoom (up to 4x).
  - One-tap **A/B Comparison**: Toggle between ASCII render and the original camera photo.
  - **Copy ASCII Art**: Directly copy the raw text representation to your clipboard.
  - **Share & Export**: Export high-resolution PNG renders directly to social apps or storage.
- **🎯 Material 3 Design**:
  - Clean typographic ASCII camera app icon (`[(@)]` iris) with Android 13+ dynamic Material You monochrome support.
  - Edge-to-edge layout with full support for display cutouts, notches, and navigation bars.
  - Tactile mechanical shutter button with micro-animations and haptic feedback.
  - Floating HUD with live latency readout (ms) and real-time FPS telemetry.

---

## 🏗️ Architecture

The project is architected as a modular, separation-of-concerns Kotlin repository:

```
MyScii-Camera/
├── ascii-engine/           # Pure Kotlin module (Multiplatform-ready)
│   ├── src/main/kotlin/    # AsciiConverter, AdaptiveQualityController, AsciiCharset, AsciiConfig
│   └── src/test/kotlin/    # Unit tests for quantization, smoothing, and LUT generation
│
└── app/                    # Android application module
    ├── src/main/java/      # Jetpack Compose UI, CameraX controller, Frame analyzer, Capture storage
    │   ├── camera/         # CameraAsciiController & AsciiFrameAnalyzer
    │   ├── storage/        # AsciiCaptureStore (TSV indexing, file persistence)
    │   ├── ui/             # AsciiCameraScreen, AsciiGalleryScreen, AsciiOverlayView
    │   └── ui/theme/       # Material 3 design system (Color, Shape, Type, Theme)
    └── src/main/res/       # Vector drawables, Material 3 adaptive icons, string definitions
```

---

## 🚀 Getting Started

### Prerequisites

- **JDK**: Java 17 or Java 21
- **Android SDK**: `compileSdk = 35`, `minSdk = 26`
- **Android Studio**: Ladybug (2024.2+) or Meerkat, or command-line Gradle

### Clone & Build

```bash
git clone https://github.com/aliyan212/MyScii-Camera.git
cd MyScii-Camera
```

### Run Unit Tests

```bash
# Run pure Kotlin engine tests
./gradlew :ascii-engine:test

# Run Android app unit tests
./gradlew :app:testDebugUnitTest
```

### Assemble Debug APK

```bash
./gradlew :app:assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📖 Governance & Documentation

- [Contributing Guidelines](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)
- [Architecture Details](docs/ARCHITECTURE.md)
- [Roadmap & Milestones](docs/ROADMAP.md)
- [Testing Guide](docs/TESTING.md)
- [Release Guide](docs/RELEASE.md)
- [Security Policy](SECURITY.md)

---

## 📄 License

This project is licensed under the terms of the [MIT License](LICENSE).
