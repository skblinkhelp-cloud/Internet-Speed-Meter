# Privacy Policy for WiFi & Internet Speed Tester

**Last Updated:** October 24, 2026

**Developer:** SK Software  
**Contact Email:** [sk.blink.help@gmail.com](mailto:sk.blink.help@gmail.com)  
**GitHub Repository:** [https://github.com/skblinkhelp-cloud/Internet-Speed-Meter](https://github.com/skblinkhelp-cloud/Internet-Speed-Meter)

---

## 1. Overview

**SK Software** respects your privacy and is committed to protecting it. This Privacy Policy explains how our application, **WiFi & Internet Speed Tester (Internet Speed Meter)**, handles your data, permissions, and network information.

---

## 2. Information Collection and Usage

Our application is designed with a **privacy-first approach**:

### A. Real-Time Network Speed & Data Usage
- **Real-Time Speed Measurement:** The app samples system network statistics (`android.net.TrafficStats`) to display your live download speed, upload speed, latency (ping), jitter, and packet loss.
- **Local Data Usage Tracking:** The app records daily Mobile Data and Wi-Fi data usage locally on your device to show daily/monthly usage logs.
- **No Remote Data Storage:** All data usage logs and speed test history remain **stored locally on your Android device**. We do NOT collect, transmit, or sell your personal network activity or browsing history.

### B. Location Data
- **Purpose:** Android requires Location permission (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) to display the connected Wi-Fi Network Name (SSID) and identify the city/country for speed test result summary screens.
- **Privacy Protection:** Precise GPS coordinates are **never stored or transmitted**. Only the broad city/country name is displayed on the results summary screen.
- **Optional Permission:** If you deny location permission, the app will continue to function fully, displaying "Location unavailable" or "Wi-Fi Network" without crashing.

---

## 3. App Permissions and Why They Are Required

| Permission | Purpose |
| :--- | :--- |
| `INTERNET` | Required to perform real-time speed measurements against Anycast speed test servers. |
| `ACCESS_NETWORK_STATE` & `ACCESS_WIFI_STATE` | Detects whether you are connected to Wi-Fi, Mobile Data, or Ethernet. |
| `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION` | Required by Android OS to retrieve the connected Wi-Fi SSID and city name. |
| `FOREGROUND_SERVICE` | Enables the status bar speed meter notification to show live network speed in your notification shade. |
| `POST_NOTIFICATIONS` | Allows displaying live speed updates in the notification bar on Android 13+. |
| `RECEIVE_BOOT_COMPLETED` | Allows the Live Speed Meter service to start automatically when your device reboots (if enabled in Settings). |

---

## 4. Third-Party Services and Analytics

- **No Ads or Profiling:** This application does **NOT** contain third-party advertising networks, user tracking SDKs, or data broker integrations.
- **Speed Test Servers:** Speed test throughput measurements connect to standard public Anycast speed endpoints (such as Cloudflare Speed CDN) solely to download/upload synthetic data buffers for speed calculation. No personal identifiers or user account data are transmitted.

---

## 5. Security and Data Retention

- All user settings, test history, and daily data usage logs are stored in secure local `SharedPreferences` on your device.
- Uninstalling the application completely removes all locally stored app data.

---

## 6. Children's Privacy

Our application does not knowingly collect or solicit any personal information from children under the age of 13.

---

## 7. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. Any changes will be posted on this page and pushed to our official GitHub repository.

---

## 8. Contact Us

If you have any questions, suggestions, or privacy concerns regarding **WiFi & Internet Speed Tester**, please contact us at:

- **Developer:** SK Software
- **Email:** [sk.blink.help@gmail.com](mailto:sk.blink.help@gmail.com)
- **GitHub:** [https://github.com/skblinkhelp-cloud/Internet-Speed-Meter](https://github.com/skblinkhelp-cloud/Internet-Speed-Meter)
