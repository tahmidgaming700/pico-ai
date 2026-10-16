# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - Initial Public Release

### Added
- **Core On-Device AI Engine**:
  - Offline local inference using Google MediaPipe Tasks GenAI / LiteRT-LM runtime.
  - Zero paid API key requirements; no external endpoints needed.
  - Multi-turn conversation prompt construction with `<start_of_turn>` Gemma formatting, ChatML, and sliding window context bounds.
  - Real-time generation elapsed timer with graceful cancellation support.
- **Model Management Suite**:
  - Curated catalogue of verified mobile models: Gemma 3 1B IT, SmolLM 135M IT, Gemma 2B IT, and Phi-2.
  - Storage space pre-flight verification before downloads begin.
  - Binary header magic signature verification (MediaPipe `.task`, LiteRT FlatBuffer `TFL3`, and GGUF `GGUF`) to reject corrupted files.
  - SHA-256 integrity verification.
  - Manual local file import via Android Storage Access Framework (SAF).
  - Dynamic model loading and unloading in runtime memory.
- **Modern Jetpack Compose UI**:
  - Distinctive deep navy background (`#070D19`) with luminous cyan (`#38BDF8`) and electric violet accents.
  - Custom adaptive launcher icon with vector foreground, 66dp safe zone, and all mipmap raster densities.
  - Rounded message bubbles with monospace markdown code blocks, language badge, and 1-tap copy action.
  - Empty state with quick starter prompt suggestions.
  - Responsive bottom navigation bar with active indicators.
- **Local Persistence & Privacy**:
  - Local SQLite conversation storage backed by Android Jetpack Room with foreign key cascade deletions.
  - Search conversation history by title and content.
  - Clear history and delete individual conversation controls.
  - Conversation export to text via Android system sharesheet.
  - Device diagnostic report generator.
- **Testing & Continuous Integration**:
  - Robolectric JVM tests for Room database queries, cascading deletion, and context string retrieval.
  - Unit tests for prompt building, Unicode handling, and byte header validation.
  - GitHub Actions CI workflow (`android.yml`) and reproducible release workflow (`release.yml`) with SHA-256 generation.
