# TRIM ✂️
**A Premium Subscription & Trial Manager for Android**

TRIM is a sleek, Netflix-themed Android application designed to help users track their recurring subscriptions, calculate total monthly spend, and prevent surprise credit card charges. It operates entirely locally using Room Database and utilizes Android's WorkManager to send proactive push notifications 48 hours before a trial or subscription renews.

---

## ✨ Features

* **Cinematic Dark UI:** A premium, fully custom dark theme built with Jetpack Compose, featuring pure black backgrounds, floating dark cards, and glowing neon red accents.
* **Proactive Alerts:** Never miss a cancellation window. TRIM automatically schedules local push notifications exactly 48 hours before a billing date.
* **Dynamic Spend Tracking:** Instantly calculates and displays your total monthly subscription spend using reactive Kotlin Flows.
* **Full CRUD Functionality:** Easily add, edit, view, and delete subscriptions with a streamlined input form and DatePicker integration.
* **100% Offline (Currently):** All data is stored securely on the device using a local SQLite database, ensuring privacy and speed.

---

## 🛠️ Tech Stack & Architecture

TRIM is built using modern Android development standards and architecture:

* **Language:** Kotlin (2.1.0)
* **UI Toolkit:** Jetpack Compose (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Local Database:** Room (via KSP)
* **Background Tasks:** WorkManager (for precise, reliable notification scheduling)
* **Navigation:** Navigation Compose
* **Build System:** Gradle Kotlin DSL (AGP 9.1.1+)

---

## 📸 Screenshots
*(Note: Replace these placeholder paths with actual screenshots of your app once uploaded to your repo's `images` folder)*

| Dashboard | Add Subscription | Notification Alert |
| :---: | :---: | :---: |
| <img src="images/dashboard.png" width="250"/> | <img src="images/add_screen.png" width="250"/> | <img src="images/notification.png" width="250"/> |

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio:** Ladybug (or newer recommended for AGP 9.1.1 support)
* **Minimum SDK:** 24
* **Compile SDK:** 36

### Installation
1. Clone this repository:
   ```bash
   git clone [https://github.com/yourusername/TRIM.git](https://github.com/yourusername/TRIM.git)
