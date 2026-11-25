# 📸 PixelVault (Vault)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_Only-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin_2.2+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Backend-Go_Fiber_v3-00ADD8?style=for-the-badge&logo=go&logoColor=white" />
  <img src="https://img.shields.io/badge/Database-PostgreSQL_16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Storage-Local_%2F_MinIO_S3-FF6B6B?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Security-AES--256--GCM_%7C_StrongBox-00C853?style=for-the-badge" />
</p>

A production-grade, self-hosted, end-to-end encrypted photo and video cloud gallery built exclusively for **Android** and backed by a lightweight, lightning-fast **Go** server. All your media stays on your own server with zero third-party telemetry, commercial tracking, or cloud lock-in.

---

## ✨ Key Features

### 🎨 100% Liquid Glass UI/UX
* **Real-time Hardware Blur**: Hardware-accelerated `RenderEffect.createBlurEffect` on Android 12+ (API 31+) with a frosted semi-opaque layer fallback on Android 8–11 (API 26–30).
* **Floating Glass Bottom Nav**: Morphing pill selector with specular 1px light border, inner glow, and smooth spring animations (Photos · Albums · Search · Vault).
* **Dynamic Timeline Grid**: Chronologically grouped by *Today*, *Yesterday*, and *Month-Year*. Smooth pinch-to-zoom dynamically re-layouts between 2, 3, 4, and 5 columns.
* **Full-Screen Media Viewer**: Edge-to-edge swipeable pager, double-tap zoom, gesture dismiss, auto-hiding glass controls, and hardware-accelerated Media3 / ExoPlayer video playback.

### 🛡️ Industry-Leading Security & Zero-Knowledge Vault
* **App Lock Protection**: Dual PIN and 3×3 Pattern Lock (minimum 5 connected dots with animated bezier curve paths) plus strong `BiometricPrompt` (Class 3 hardware biometric).
* **Screen Capture & Recents Shield**: `WindowManager.LayoutParams.FLAG_SECURE` enforced dynamically on lock screens and inside the Secure Vault to prevent screenshots, screen recording, and task switcher thumbnail leaks.
* **Encrypted Secure Vault / Locked Folder**: User can move sensitive media into a private vault. Completely isolated from device MediaStore, timeline, albums, and system searches.
* **End-to-End Encryption (AES-256-GCM)**: Each vault item is encrypted with a unique 256-bit AES-GCM file key before storage. File keys are wrapped by an Android Keystore Master Key backed by dedicated hardware security chips (**StrongBox**) or hardware TEE.
* **Automatic EXIF Sanitization**: Strips GPS latitude/longitude, altitude, timestamps, device make/model, and user comments before storing items in the Secure Vault.
* **Brute-Force Guard**: Exponential lockout tiers (30s, 1m, 5m, 15m) with configurable auto-wipe of sensitive encryption keys after 10 consecutive failed attempts.
* **Root & Tamper Detection**: Proactively detects SU binaries, test-keys build tags, attached debuggers, and emulator environments.

### ⚡ Resumable Sync & Streaming Architecture
* **Resumable Chunked Uploads**: Tus-style 5 MB chunk streaming with byte-offset tracking, enabling seamless recovery from network drops during large 4K video uploads.
* **Background MediaStore Observer**: Automatically registers an Android `ContentObserver` for new camera photos and videos to trigger background sync.
* **Flexible Sync Policies**: Configurable options for unmetered Wi-Fi only and charging-only sync via Android `WorkManager`.
* **Instant Pure-Go Thumbnails**: Server generates lightweight 256×256 thumbnails on the fly with EXIF GPS and timestamp extraction.
* **Full-Text & Fuzzy Search**: PostgreSQL `tsvector` and `pg_trgm` fuzzy matching across dates, filenames, tags, and geolocation coordinates.

---

## 🏛️ Architecture & Tech Stack

```
Vault/
├── android/               # Native Android Application (Kotlin + Jetpack Compose)
│   ├── app/               # Main application module
│   │   ├── src/main/kotlin/com/pixelvault/app/
│   │   │   ├── data/      # MediaStore observer, chunk uploader, Retrofit API, DataStore
│   │   │   ├── domain/    # Media & album data models
│   │   │   ├── security/  # Keystore, AES-GCM vault, AppLock, Biometrics, Root detection
│   │   │   └── ui/        # Compose Liquid Glass components, Timeline, Viewer, Vault, Themes
│   │   └── build.gradle.kts
│   └── build.gradle.kts
└── backend/               # Go Fiber v3 Server
    ├── cmd/server/        # Application entrypoint
    ├── internal/          # Auth, Media, Albums, Search, Admin, Middleware, Storage
    ├── migrations/        # PostgreSQL schema migrations (000001 to 000004)
    ├── docs/              # OpenAPI 3.0 specification & VPS deployment manual
    ├── Dockerfile         # Multi-stage lightweight scratch/alpine container
    ├── docker-compose.yml # Server + PostgreSQL + MinIO + Caddy
    └── Caddyfile          # Automatic TLS reverse proxy
```

### Android Client
* **Language & SDK**: Kotlin 2.2+, `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`
* **UI**: 100% Jetpack Compose with Material 3 base and custom Liquid Glass styling
* **Architecture**: Clean Architecture + MVVM with Kotlin Coroutines and StateFlow
* **Networking**: Retrofit 2 + OkHttp (TLS 1.3, certificate pinning support)
* **Image & Video**: Coil 3 (shimmer skeleton loaders, blur-in crossfade) + Media3 / ExoPlayer
* **Background Processing**: AndroidX WorkManager + Foreground Service
* **Local Security**: Android Keystore, EncryptedSharedPreferences, StrongBox

### Go Backend
* **Framework**: Go 1.22+ with Fiber v3
* **Database**: PostgreSQL 16/18 with `pgxpool` driver
* **Storage Options**: Local filesystem storage or MinIO S3 object storage
* **Authentication**: Short-lived JWT (15 min) + rotating refresh tokens (30 days) + device fingerprint binding
* **Security Middleware**: In-memory token bucket rate limiter (10 req/s auth, 100 req/s global), audit logger, HMAC-SHA256 expiring signed media URLs

---

## 🚀 Quick Start Guide

### Option 1: Docker Compose (Recommended for VPS / Production)

1. Clone the repository on your server:
   ```bash
   git clone https://github.com/Samanta2087/Vault.git
   cd Vault/backend
   ```

2. Copy the environment template:
   ```bash
   cp .env.example .env
   ```
   *Edit `.env` to configure your database password, domain name, and JWT secret keys.*

3. Start all services (Go Server, PostgreSQL, MinIO, and Caddy with automatic SSL):
   ```bash
   docker compose up -d --build
   ```

4. Check server status:
   ```bash
   curl https://yourdomain.com/health
   ```

---

### Option 2: Native Host Deployment (Without Docker)

1. Ensure **PostgreSQL 16+** is running locally and create the database:
   ```sql
   CREATE DATABASE pixelvault;
   ```

2. Apply migrations located in `backend/migrations/`:
   ```bash
   psql -U postgres -d pixelvault -f migrations/000001_init.up.sql
   psql -U postgres -d pixelvault -f migrations/000002_media.up.sql
   psql -U postgres -d pixelvault -f migrations/000003_search.up.sql
   psql -U postgres -d pixelvault -f migrations/000004_vault.up.sql
   ```

3. Build and launch the native Go server:
   ```bash
   cd backend
   go build -o server ./cmd/server
   ./server
   ```

---

### 📱 Android Client Build Instructions

1. Open the `android` folder in **Android Studio Ladybug (2024.2+)** or later.
2. Build the debug APK via Gradle:
   ```bash
   cd android
   ./gradlew :app:assembleDebug
   ```
3. Install onto your connected Android device or emulator:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. Launch PixelVault and enter your server URL in Settings (e.g. `http://10.0.2.2:8080` for emulator or `https://yourdomain.com` for VPS).

---

## 🔒 Security Specifications

| Layer | Standard / Implementation |
|---|---|
| **App Authentication** | Argon2id / SHA-256 salted PIN, 3×3 Pattern (min 5 dots), BiometricPrompt (Class 3) |
| **Vault Encryption** | 256-bit AES-GCM per-file keys wrapped by Android Keystore Master Key |
| **Hardware Key Protection** | StrongBox Keymaster hardware chip fallback to Trusted Execution Environment (TEE) |
| **Screen Protection** | `FLAG_SECURE` enforced on Vault and Lock screens (prevents screenshots & recents leaks) |
| **Transport Layer** | TLS 1.3 enforced, Certificate Pinning support in `network_security_config.xml` |
| **API Auth** | Short-lived JWTs (15 min) + rotating refresh tokens with strict device binding |
| **Brute-Force Shield** | Exponential lockout tiers (30s, 1m, 5m, 15m), optional wipe after 10 failures |
| **Tamper Detection** | Proactive audit for SU binaries, root management apps, debuggers, and test keys |

---

## 📖 API Documentation

Complete OpenAPI 3.0 specification is available at [`backend/docs/openapi.yaml`](backend/docs/openapi.yaml).

Detailed VPS setup, Caddy reverse proxy, and systemd service guides are documented at [`backend/docs/VPS_SETUP.md`](backend/docs/VPS_SETUP.md).

---

## 📄 License

This project is licensed under the Apache 2.0 License. All media and data belong strictly to the owner of the self-hosted instance.
