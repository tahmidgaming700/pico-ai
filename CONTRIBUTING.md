# Contributing to Pico AI

Thank you for your interest in contributing to **Pico AI**! We welcome bug reports, feature suggestions, and code contributions aimed at improving private, free on-device artificial intelligence on Android.

---

## 1. Code of Conduct

Please maintain a welcoming, respectful, and constructive tone in all issues, discussions, and pull requests.

---

## 2. Development Setup

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- JDK 17
- Android SDK Platform 36 (Minimum SDK: 24, Target SDK: 36)
- Git

### Build Instructions
Clone the repository and build using the provided Gradle wrapper:

```bash
git clone https://github.com/tahmidgaming700/pico-ai.git
cd pico-ai
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 3. Architecture & Guidelines

- **Zero Cloud Dependence:** Never introduce mandatory cloud AI endpoints, paid subscriptions, or remote telemetry SDKs. The core functionality must continue to run 100% offline.
- **Language & Frameworks:** Kotlin exclusively with Jetpack Compose and Material 3 design tokens.
- **Persistence:** Local persistence uses Android Room. Do not store plain credentials.
- **Model Formats:** Inference engine uses MediaPipe GenAI / LiteRT (.task, .bin). Ensure any new model recommendations verify memory suitability for 2GB - 4GB RAM devices.

---

## 4. Submitting a Pull Request

1. Fork the repo and create your feature branch: `git checkout -b feature/awesome-feature`
2. Ensure all unit and Robolectric tests pass: `./gradlew testDebugUnitTest`
3. Commit your changes with clear commit messages.
4. Push to your branch and open a Pull Request against `main`.
