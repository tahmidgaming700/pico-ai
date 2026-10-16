# Pico AI — Privacy Policy & Guarantee

**Effective Date:** October 2026  
**Application Version:** 0.1.0  
**Package:** `ai.pico.assistant`

---

## 1. Core Principle: 100% On-Device AI

Pico AI is engineered from the ground up to respect absolute user privacy:
- **No Remote AI Inference:** Unlike cloud-based chatbots, Pico AI never sends your prompts, questions, documents, or answers to remote servers. All language models run entirely in memory on your local Android CPU/GPU.
- **No Account Required:** You do not need to register an account, log in, or provide an email address, telephone number, or API key to use Pico AI.
- **Zero Telemetry & Tracking:** Pico AI contains no third-party tracking SDKs, analytics suites, crash log uploaders, or advertising identifiers.

---

## 2. Conversation & Data Storage

- All conversations, messages, titles, timestamps, and model selections are stored exclusively in an app-private SQLite database managed via Android Jetpack Room.
- Data is stored in your device's internal storage (`/data/data/ai.pico.assistant/databases/`) and is protected by Android's sandbox isolation.
- Automatic cloud backup to Google Drive (`android:allowBackup="false"`) is deliberately disabled by default to prevent silent extraction or uploading of private chats.
- You can permanently wipe your conversation history at any time from **History > Clear All History** or **Settings > Clear All Chat History**.

---

## 3. Network & Internet Permission Usage

Pico AI requests the `android.permission.INTERNET` permission in its manifest for one explicit purpose:
- **Model Downloads on Demand:** When you explicitly browse the Model Manager catalogue and choose to download a model (such as SmolLM 135M or Gemma 3 1B), the app opens a direct HTTPS streaming connection to download the verified model weights directly to your device.
- **Zero Background Sync:** Pico AI will never ping servers in the background, check for telemetry, or upload logs.
- Once a model is downloaded or manually imported via the Storage Access Framework (SAF), the app operates 100% offline. You can test this by enabling Airplane Mode.

---

## 4. Hardware Diagnostics

Pico AI allows you to export a diagnostic report in **Settings > Export Diagnostic Report**.
- This report includes only device hardware metrics: device manufacturer, model, Android OS version, CPU ABI, RAM availability, and disk space.
- It **never** contains private conversation text, user messages, or sensitive system credentials.

---

## 5. Security & Model Integrity

- Pico AI inspects binary headers to reject corrupted, truncated, or tampered downloads.
- Models are placed in app-private storage (`/data/data/ai.pico.assistant/files/models/`), preventing other applications from altering model weights.
