# SecondServing - Android App

## Project Overview

**SecondServing** is an Android application built with Kotlin and Jetpack Compose that helps users manage their food pantry/despensa. The app provides features like manual inventory management, receipt scanning with OCR, expiration date tracking, shelf-life prediction, and push notifications for expiring items.

### Team Members
- Gabriela Carvajal (202111058)
- Juan Nieves (202116708)
- Jeronimo A. Pineda Cano (202212778)

**Group:** Grupo32

### Package
`com.app.secondserving`

---

## Architecture

The app follows a **Clean Architecture / MVVM** pattern:

| Layer | Components |
|-------|------------|
| **UI** | Jetpack Compose screens, ViewModels (`InventoryViewModel`, `WeatherViewModel`, `LoginViewModel`) |
| **Repository** | `InventoryRepository` - decides between network (Retrofit) or local cache (Room) |
| **Data Sources** | Remote API (`ApiService` via Retrofit) + Local DB (`AppDatabase` via Room) |
| **Application DI** | `SecondServingApp` class holds singleton instances |

### Package Structure

```
com.app.secondserving/
├── MainActivity.kt              # Main entry point
├── SecondServingApp.kt          # Application class (DI)
├── SecondServingMessagingService.kt  # FCM service
├── data/
│   ├── InventoryRepository.kt   # Repository pattern
│   ├── SessionManager.kt        # JWT session management
│   ├── local/                   # Room database (DAO, Entity, Database)
│   ├── model/                   # Domain models
│   ├── network/                 # Retrofit (API service, interceptors, DTOs)
│   └── ShelfLifePredictor.kt    # Expiry prediction logic
├── ui/
│   ├── inventory/               # Inventory screens + ViewModel
│   ├── login/                   # Login/Register (ViewBinding)
│   └── theme/                   # Compose theming
└── utils/                       # Utility classes
```

---

## Key Features

1. **Authentication** - Login/Register via REST API with JWT token session management (55-min TTL)
2. **Inventory Management** - Full CRUD for pantry items with search, category filtering, and urgency color-coding (green/yellow/red based on days to expiry)
3. **Receipt Scanner (OCR)** - CameraX + ML Kit Text Recognition to scan receipts and auto-populate items
4. **Shelf Life Prediction** - Predicts expiry dates based on food category and storage type (refrigerated, frozen, ambient)
5. **Expiration Notifications** - Background coroutine observes expiring items (<3 days) and sends local Android notifications
6. **Firebase Cloud Messaging** - FCM for push notifications from the backend
7. **Weather Integration** - Weather data displayed in item detail for storage advice
8. **Navigation** - Bottom nav with 4 destinations: Inicio, Despensa (Pantry), Recetas (Recipes), Perfil (Profile)

---

## Tech Stack

- **Language:** Kotlin 2.2.10
- **UI:** Jetpack Compose + Material3 (+ legacy ViewBinding for login)
- **Min SDK:** 24 (Android 7.0) | **Target SDK:** 35
- **Local DB:** Room 2.7.2
- **Networking:** Retrofit 2.11.0 + OkHttp 4.12.0
- **Async:** Kotlin Coroutines + Flow
- **OCR:** ML Kit Text Recognition 16.0.1
- **Camera:** CameraX 1.3.1
- **Push Notifications:** Firebase (BOM 33.7.0)
- **Build:** Gradle (AGP 9.1.0), Version Catalog (`libs.versions.toml`)

---

## Building and Running

### Prerequisites
- Android Studio (recommended IDE)
- JDK 11+
- Android SDK with API level 35

### Commands

```bash
# Build the project (Windows - use gradlew.bat)
gradlew.bat build

# Clean build
gradlew.bat clean build

# Run tests
gradlew.bat test

# Install on connected device
gradlew.bat installDebug

# Assemble release APK
gradlew.bat assembleRelease
```

> **Note:** For backend API connectivity during development, run `adb reverse tcp:<port> tcp:<port>` to forward localhost. The backend API base URL is configured at `http://10.0.2.2:8000/api/v1/`.

---

## Development Conventions

- **Kotlin code style:** `official` (as per gradle.properties)
- **AndroidX:** Enabled with non-transitive R class
- **Compose:** Enabled as primary UI framework
- **ViewBinding:** Enabled for legacy XML-based login screen
- **Commit style:** Follow [gitmoji](https://gitmoji.dev/) and [conventional commits](https://www.conventionalcommits.org/)

---

## Permissions

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Backend API calls |
| `POST_NOTIFICATIONS` | Local expiration alerts (Android 13+) |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Weather service |
| `CAMERA` | Receipt scanning |

---

## Key Files

| File | Description |
|------|-------------|
| `app/build.gradle.kts` | App-level build configuration |
| `gradle/libs.versions.toml` | Version catalog for dependencies |
| `app/src/main/AndroidManifest.xml` | Permissions, activities, services |
| `app/src/main/java/.../MainActivity.kt` | Main activity with navigation |
| `app/src/main/java/.../SecondServingApp.kt` | Application class for DI |
| `app/src/main/java/.../data/InventoryRepository.kt` | Core repository for inventory data |
| `app/src/main/java/.../data/local/AppDatabase.kt` | Room database definition |
| `app/src/main/java/.../data/network/ApiService.kt` | Retrofit API interface |
