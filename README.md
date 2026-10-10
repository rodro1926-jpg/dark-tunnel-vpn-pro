# 🛡️ Dark Tunnel VPN Pro

[![Android Build & APK Release](https://github.com/USERNAME/REPOSITORY/actions/workflows/android-build-release.yml/badge.svg)](https://github.com/USERNAME/REPOSITORY/actions/workflows/android-build-release.yml)
[![Latest Release](https://img.shields.io/github/v/release/USERNAME/REPOSITORY?color=00FF87&label=Release)](https://github.com/USERNAME/REPOSITORY/releases/latest)
[![API](https://img.shields.io/badge/API-24%2B%20%28Android%207.0%2B%29-00D2FF)](https://developer.android.com/about/dashboards)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Dark Tunnel VPN Pro** is a modern, high-performance Android VPN client built with **Jetpack Compose**, **Material Design 3**, and Android's native **`VpnService`** API. It is engineered for secure SSH and WebSocket tunneling, custom HTTP injection payload generation, and TLS/SNI domain fronting with real-time network telemetry.

---

## 📥 Download APK

| File | Target Android | Architecture | Download Link |
| :--- | :--- | :--- | :--- |
| **DarkTunnelVPN-Pro-v1.0.0.apk** | Android 7.0+ (API 24–36) | Universal (`arm64-v8a`, `armeabi-v7a`, `x86_64`) | [**⬇️ Download Latest Release**](https://github.com/USERNAME/REPOSITORY/releases/latest) |

> 💡 *Replace `USERNAME` and `REPOSITORY` with your actual GitHub username and repo slug.*

### Verification (SHA-256)
To verify the integrity of your downloaded APK, compare the SHA-256 hash against `checksums.txt` attached to the release:
```bash
sha256sum DarkTunnelVPN-Pro-v1.0.0.apk
```

---

## ⚡ Key Features

* **🎨 Cyberpunk Dark UI:** Deep midnight navy palette (`#090D16`, `#0F172A`) with radiant neon green (`#00FF87`) and electric blue (`#00D2FF`) glowing accents.
* **🔘 Glowing Master Connect Button:** Large pulsing action button with real-time state transitions (`DISCONNECTED`, `CONNECTING`, `CONNECTED`, `ERROR`).
* **📊 Live Telemetry:**
  * Real-time download & upload speedometers (KB/s, MB/s).
  * Data usage counters (KB, MB, GB).
  * Session duration timer (`HH:MM:SS`).
  * Live network status detection (`Wi-Fi`, `Cellular`, `Offline`).
  * Protected exit IP detection.
* **🌐 Multi-Location Server Manager:**
  * 🇧🇩 **Bangladesh** (Dhaka 01 — SSH & WebSocket)
  * 🇸🇬 **Singapore** (Cloud Core 01 — Cloudflare CDN WS + SSH)
  * 🇮🇳 **India** (Mumbai Ultra — Low-Latency WebSocket)
  * 🇺🇸 **United States** (New York Pro & Los Angeles Fast — Direct SSH & TLS)
  * **Live Ping Probe:** Socket-based network latency engine testing actual round-trip time in milliseconds.
* **🛠️ Built-in Payload Generator & Custom HTTP Injection:**
  * Visual Payload Generator supporting `GET`, `POST`, `HEAD`, `CONNECT`, and `OPTIONS`.
  * Injection methods: `Normal`, `Front Inject`, and `Back Inject`.
  * Header macro support: `[crlf]`, `[host]`, `[port]`, `[host_port]`, `[protocol]`, `[ua]`.
  * Presets for Cloudflare CDN WebSocket, FastSSH, and Direct Connect.
* **📁 Configuration File Manager:**
  * Save, edit, and rename multiple tunnel profiles.
  * Star any profile as the default launch configuration.
  * Import & export configurations in JSON / `.dtvpn` format via clipboard or file sharing.
  * Passphrase encryption option for sensitive profiles.
* **🔒 Real Native `VpnService` Tunnel:**
  * Routes packets over a virtual `TUN` interface (`10.8.0.2/24`).
  * Outbound sockets protected with `protect(socket)` to avoid routing loops.
  * Non-simulated: real connection verification; unreachable endpoints report genuine connection errors.
  * Persistent foreground notification with disconnect toggle.
* **⚙️ Advanced Settings:**
  * Auto-Connect on App Launch.
  * Reconnect on Network Switch.
  * Kill Switch (block leaks if tunnel drops).
  * Custom DNS provider selection (Cloudflare `1.1.1.1`, Google `8.8.8.8`, AdGuard, Quad9).

---

## 📂 Project Structure

```
├── .github/
│   └── workflows/
│       └── android-build-release.yml  # Automated CI/CD & GitHub Release workflow
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml    # Permissions & DarkTunnelVpnService declaration
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt    # Root Compose Activity with bottom navigation
│   │   │   │   ├── model/             # VpnState, VpnServer, VpnConfig, UserSettings
│   │   │   │   ├── data/              # Repositories for servers, configs & history
│   │   │   │   ├── vpn/               # Android VpnService & VpnManager controller
│   │   │   │   └── ui/                # Compose screens, components, theme tokens
│   │   │   └── res/                   # Drawables, mipmap adaptive icons, strings
│   │   └── test/java/com/example/     # Unit & Robolectric test suite
│   └── build.gradle.kts
├── metadata.json
├── settings.gradle.kts
├── LICENSE
└── README.md
```

---

## 🚀 Setting Up Real Authorized Servers

Dark Tunnel VPN Pro connects to genuine SSH and WebSocket endpoints. To run your own server:

### 1. Ubuntu / Debian VPS
```bash
# 1. Update and install OpenSSH
sudo apt update && sudo apt install openssh-server -y

# 2. Configure OpenSSH for port 443
sudo nano /etc/ssh/sshd_config
# Add or modify:
# Port 443
# PermitTunnel yes

sudo systemctl restart ssh

# 3. (Optional) Run BadVPN for UDP/Gaming forwarding
wget https://raw.githubusercontent.com/ambrop72/badvpn/master/badvpn.tar.gz
# Run on UDP gateway port 7300:
badvpn-udpgw --listen-addr 127.0.0.1:7300 --max-clients 500 &
```

### 2. Configure in Dark Tunnel VPN Pro
1. Navigate to the **Configs** tab and tap **`+`**.
2. Enter your server IP/domain in **Host** and the port (e.g. `443` or `22`).
3. Enter your SSH **Username** and **Password**.
4. Use the **Payload Generator** to generate a Cloudflare WebSocket injection or enter a custom payload.
5. Tap **Save Configuration** and return to **Home** to tap **Connect**.

---

## 💻 Local Building & Development

### Prerequisites
* **Android Studio Ladybug (2024.2+)** or newer.
* **JDK 17** or **JDK 21**.
* Android SDK Platform 36 (Minimum SDK: 24, Target SDK: 36).

### Build Commands
```bash
# Clone the repository
git clone https://github.com/USERNAME/REPOSITORY.git
cd REPOSITORY

# Run unit and Robolectric tests
./gradlew :app:testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🤖 GitHub Actions CI/CD & Automated Releases

This repository includes an automated GitHub Actions workflow (`.github/workflows/android-build-release.yml`):

1. **Continuous Integration (Push / PR):**
   * Automatically compiles and tests the code on pushes to `main` or `master`.
   * Uploads the compiled APK as a workflow artifact named `DarkTunnelVPN-Debug-APK`.

2. **Automated GitHub Releases (Git Tag):**
   * Creating and pushing any version tag (e.g. `v1.0.0`) automatically compiles the release-ready APK.
   * Generates SHA-256 checksums.
   * Creates a public GitHub Release with the APK (`DarkTunnelVPN-Pro-v1.0.0.apk`) and checksum file attached!

### Step-by-Step GitHub Publish Guide

```bash
# 1. Initialize git and commit files
git init
git add .
git commit -m "Initial commit: Dark Tunnel VPN Pro v1.0.0"

# 2. Add your GitHub remote repository
git remote add origin https://github.com/USERNAME/REPOSITORY.git
git branch -M main
git push -u origin main

# 3. Create and push a version tag to trigger automated APK release
git tag -a v1.0.0 -m "Release v1.0.0: Dark Tunnel VPN Pro"
git push origin v1.0.0
```
> Within 2–3 minutes, GitHub Actions will build the project and publish the release with downloadable APKs at `https://github.com/USERNAME/REPOSITORY/releases/tag/v1.0.0`.

---

## 🛡️ Security & Zero-Logs Privacy

* **Zero Logging:** Dark Tunnel VPN Pro does not store, log, or inspect user DNS queries or traffic destinations.
* **Credential Hygiene:** Passwords, tokens, and private keys reside exclusively in on-device private storage (`SharedPreferences`) and are never sent to third-party analytic servers.
* **Security Verification:** Outgoing TLS connections perform certificate verification against standard root certificate authorities.

---

## 📜 License

Distributed under the **Apache License 2.0**. See [`LICENSE`](LICENSE) for more information.
