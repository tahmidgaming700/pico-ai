# Pico AI ⚡ — Native On-Device Android AI Assistant

![Pico AI Logo](app/src/main/res/drawable/pico_ai_logo_1792141051988.jpg)

**Pico AI** is a completely free, native Android AI assistant that runs Large Language Models **100% locally on your device**. It requires no paid API keys (no OpenAI, no Gemini API, no subscriptions), transmits no conversation data to remote servers, and continues to operate seamlessly offline once a compatible model is installed.

---

## 🌟 Key Features

- **Genuine On-Device Local Inference:** Runs real instruction-tuned language models directly on your Android phone's CPU and GPU via the Google MediaPipe Tasks GenAI / LiteRT-LM runtime.
- **Zero API Key Requirement:** Completely free and open. No accounts, logins, credit cards, or developer keys required.
- **100% Offline Chat:** Full multi-turn conversational AI without an active internet connection.
- **Integrated Model Manager:**
  - Curated catalogue of verified mobile models: **SmolLM 135M**, **Gemma 3 1B**, **Gemma 2B**, and **Phi-2**.
  - Storage space safety checks before downloads begin.
  - Binary magic byte validation (`PK\x03\x04`, `TFL3`, `GGUF`) and SHA-256 integrity checks.
  - Manual local file import via Android's Storage Access Framework (SAF).
  - Dynamic loading and unloading in runtime memory without restarting the application.
- **Local SQLite Conversation History:**
  - Persisted privately via Android Jetpack Room.
  - Real-time search across conversations.
  - Rename, delete, and 1-tap export of conversation transcripts via Android's native share sheet.
- **Polished Material Design 3 UI:**
  - Signature dark theme with deep navy (`#070D19`), luminous cyan (`#38BDF8`), and electric violet accents.
  - Rounded message bubbles with monospace markdown code blocks, syntax tags, and 1-tap copy buttons.
  - Quick action toolbar: copy, share, and regenerate responses.
  - Custom adaptive launcher icon with safe-zone compliance across all display densities.
- **Hardware Diagnostics & Privacy:**
  - Real-time RAM and storage consumption meters.
  - Exportable privacy-safe diagnostic report.

---

## 📱 Hardware & Compatibility Considerations

### Supported Android Versions
- **Minimum Android Version:** Android 7.0 (API Level 24)
- **Target Android Version:** Android 16 (API Level 36)
- **Architectures:** `arm64-v8a`, `armeabi-v7a`, `x86_64`

### Test Target: Huawei Y9 2019 (JKM-LX2)
- **Processor:** HiSilicon Kirin 710 (4x Cortex-A73 @ 2.2GHz + 4x Cortex-A53 @ 1.7GHz)
- **RAM Variants:** 3 GB / 4 GB
- **Recommendations for 3GB Devices:**
  - **SmolLM 135M Instruct:** Ultra-compact (~98 MB), minimal RAM footprint (<1 GB), near-instant responses. Recommended for rapid testing and low-memory conditions.
  - **Gemma 3 1B Instruct:** Compact (~840 MB), excellent reasoning and multilingual knowledge, runs comfortably within 2 GB available RAM.
  - **Large Models (2B+):** May trigger memory warnings or thermal throttling on 3GB devices. Use the built-in memory warning dialogue before loading.

---

## 🧠 Model Catalogue & Licensing

| Model | Publisher | Parameters | Approx Size | Recommended RAM | License | Format | Access Status |
|---|---|---|---|---|---|---|---|
| **SmolLM 135M Instruct** | Hugging Face TB | 135M | ~166 MB | 1024 MB | [Apache-2.0](https://huggingface.co/HuggingFaceTB/SmolLM-135M-Instruct) | MediaPipe `.task` | **100% Public (No token needed)** |
| **Qwen 2.5 1.5B Instruct** | Alibaba / Qwen | 1.5B | ~1.86 GB | 2048 MB | [Apache-2.0](https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct) | MediaPipe `.task` | **100% Public (No token needed)** |
| **Gemma 3 1B Instruct** | Google DeepMind | 1B | ~840 MB | 2048 MB | [Gemma Terms](https://ai.google.dev/gemma/terms) | MediaPipe `.bin` | Gated (Requires HF Token) |
| **Gemma 2B Instruct** | Google DeepMind | 2B | ~1.35 GB | 3072 MB | [Gemma Terms](https://ai.google.dev/gemma/terms) | MediaPipe `.bin` | Gated (Requires HF Token) |

*Note: The Apache-2.0 license for the Pico AI application source code covers the software itself; individual model weights are distributed under their respective creator licenses.*

### 🛡️ Resolving HTTP 403 / 401 Download Errors
If a model download fails with **HTTP 403 Forbidden** or **HTTP 401 Unauthorized**:
1. **Un-gated Models (SmolLM & Qwen):** Download immediately with zero credentials required. The built-in downloader now passes standard mobile browser headers to avoid CloudFront CDN blocks.
2. **Gated Models (Google Gemma):** Google requires users to accept the Gemma license on Hugging Face before downloading weights:
   - Accept the license at [huggingface.co/google/gemma-3-1b-it-litert](https://huggingface.co/google/gemma-3-1b-it-litert).
   - Generate a free Read access token at [huggingface.co/settings/tokens](https://huggingface.co/settings/tokens).
   - In Pico AI, go to **Settings > Hugging Face Access Token** and paste your token (`hf_...`).
   - Re-try the download; authentication will succeed automatically!

---

## 🔄 Weekly Automated App Updates

Pico AI includes automated weekly CI/CD publishing via GitHub Actions:
- **Schedule:** Every Sunday at 00:00 UTC (`.github/workflows/weekly-release.yml`).
- **In-App Checker:** Tap **Settings > Check Updates** inside the app to query for new weekly releases and download latest APKs directly.
- **Manual Trigger:** Run the workflow at any time from GitHub Actions via `workflow_dispatch`.

---

## 🛠️ Build & Installation Instructions

### Building from Source

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/tahmidgaming700/pico-ai.git
   cd pico-ai
   ```

2. **Run Unit & Robolectric Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

3. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The generated APK will be at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on Device via ADB:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📥 How to Install Models

### Option A: In-App Download (Direct)
1. Open **Pico AI** and navigate to the **Models** tab at the bottom.
2. Switch to the **Model Catalogue** tab.
3. Tap **Download** next to **SmolLM 135M** (recommended first model) or **Gemma 3 1B**.
4. The app checks free storage, streams the download with a progress meter, validates the binary header signature, and installs it atomically.
5. Tap **Load into Memory** to activate the model.

### Option B: Manual Local Import
1. Download a compatible MediaPipe Task (`.task`) or LiteRT (`.bin`) model via your browser or transfer it from a PC.
2. In Pico AI, navigate to **Models** and tap the **Import** button in the top right.
3. Select your `.bin` or `.task` file using the Android file picker.
4. Pico AI validates the binary header and imports it to secure app-private storage.

---

## 🔒 Privacy & Security Architecture

1. **Zero Cloud AI Endpoints:** Prompts are processed strictly in local device memory.
2. **App-Private Storage:** Chat databases and model files reside in `/data/data/ai.pico.assistant/`, inaccessible to other apps.
3. **Backup Disabled:** `android:allowBackup="false"` is enforced in `AndroidManifest.xml` to prevent unintentional extraction to Google Drive backups.
4. **Internet Permission Disclosure:** The `INTERNET` permission is used **exclusively** when you trigger a model download from the catalogue. No network requests occur during chat generation.

---

## 🔐 Release signing setup

The release workflow requires these repository Actions secrets before a signed release can be built:

- `KEYSTORE_BASE64`: the Base64-encoded contents of the release keystore file.
- `STORE_PASSWORD`: the keystore password.
- `KEY_PASSWORD`: the password for the `upload` key alias.

The Gradle release configuration currently uses the key alias `upload`. Keep the same keystore and signing key for future releases so users can install updates over earlier versions. Never commit the keystore or passwords to the repository.

To encode a keystore locally, use `base64 -w 0 my-upload-key.jks` on Linux, then save the output as the `KEYSTORE_BASE64` secret. On systems where `-w 0` is unsupported, use the equivalent command for a single-line Base64 value.

## 🚀 GitHub Actions CI / CD & Publishing Releases

The repository includes pre-configured continuous integration workflows in `.github/workflows/`:
- **`android.yml`**: Runs on pushes and pull requests to run unit and Robolectric tests, assemble the debug APK, and publish the APK artifact.
- **`release.yml`**: Triggers on version tags (`v*.*.*`) or manual dispatch. Configures the release keystore, signs the release APK, computes its SHA-256 checksum, and creates a GitHub Release with `pico-ai-release.apk`.
- **`weekly-release.yml`**: Runs automatically every Sunday at 00:00 UTC (and on manual dispatch) to build, test, sign, and publish pre-release weekly updates with date-based tagging (`v0.1.0-weekly-YYYYMMDD`).

### How to Publish a Release

#### Method 1: Push a Version Tag (CLI)
```bash
git tag -a v0.1.0 -m "Pico AI v0.1.0"
git push origin v0.1.0
```

#### Method 2: GitHub Web Actions (No terminal needed)
1. Navigate to your repository on GitHub.
2. Go to the **Actions** tab.
3. Select **Release Build** from the left workflow menu.
4. Click **Run workflow**, enter the version tag (e.g. `v0.1.0`), and click the green **Run workflow** button.
5. Once complete, your signed APK and checksum will be automatically attached to a new GitHub Release.

---

## 📄 License

Original Pico AI application source code is licensed under the [Apache License, Version 2.0](LICENSE).
