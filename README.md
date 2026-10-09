# WiFi & Internet Speed Tester (Internet Speed Meter)

[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](https://github.com/skblinkhelp-cloud/Internet-Speed-Meter)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-blue.svg)](https://android.com)
[![Developer](https://img.shields.io/badge/Developer-SK%20Software-orange.svg)](mailto:sk.blink.help@gmail.com)
[![Privacy Policy](https://img.shields.io/badge/Privacy-Policy-green.svg)](PRIVACY_POLICY.md)

A modern, fast, simple, and intelligent Android application that provides **real-time internet speed monitoring in status bar**, **daily & monthly data usage tracking**, and **on-demand network speed tests** with smart AI interpretations.

---

## 🌟 Key Features

### 1. Live Internet Speed Meter
- Real-time download & upload speed sampling using `android.net.TrafficStats`.
- Displays real-time speed in **Mbps** and **MB/s** / **KB/s**.
- Live real-time traffic activity graph.

### 2. Status Bar Speed Notification
- Persistent status bar notification showing live download & upload throughput (`Speed: 1.4 MB/s   Signal 100% | Mobile: 10 MB   WiFi: 3.1 MB`).
- Automatically starts on device boot (configurable in Settings).

### 3. Daily & Monthly Data Usage Log
- Separate tracking for **Mobile Data** vs **Wi-Fi** data consumption.
- Comprehensive table view showing **Today**, **Past 30 Days**, and **This Month** summary totals.

### 4. On-Demand Internet Speed Test
- Real HTTP streaming speed measurement (Download, Upload, Ping, Jitter, Packet Loss, and Stability %).
- Dynamic Anycast server selection with lowest latency.
- **Smart AI Interpretation**: Plain-English network quality assessment and suitability ratings for 4K Streaming, Video Calls, Gaming, and Browsing.
- **Share Results**: Share test results via native Android share intent.

### 5. Privacy & Settings
- Wi-Fi SSID, Network Type (5G/4G/Wi-Fi), Carrier/ISP, and City/Country detection.
- Complete Privacy Policy and Settings options.

---

## 🔒 Privacy Policy

Read our full Google Play compliant Privacy Policy in [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

---

## 🛠️ Tech Stack & Architecture

- **Language:** 100% Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel) + Coroutines & Flow
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 37 (Android 15+)

---

## 🚀 How to Build

1. Clone this repository:
   ```bash
   git clone https://github.com/skblinkhelp-cloud/Internet-Speed-Meter.git
   ```
2. Open the project in **Android Studio**.
3. Build the debug APK:
   ```bash
   ./gradlew app:assembleDebug
   ```

---

## 📬 Contact & Support

- **Developer / Console:** SK Software
- **Support Email:** [sk.blink.help@gmail.com](mailto:sk.blink.help@gmail.com)
- **Repository:** [https://github.com/skblinkhelp-cloud/Internet-Speed-Meter](https://github.com/skblinkhelp-cloud/Internet-Speed-Meter)
