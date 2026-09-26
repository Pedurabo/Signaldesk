# SignalDesk

SignalDesk is a full-stack incident-management project centered on a native Android client that remains useful when connectivity is unreliable. It combines an offline-capable Android workflow with a Kotlin/Spring Boot API and PostgreSQL persistence.

The project is being built as a production-oriented engineering case study: application architecture, local persistence, background synchronization, API design, database migrations, automated testing, release configuration, signing, containerization, and cloud deployment are treated as parts of one system rather than isolated demos.

## What it does

The Android application supports an incident workflow with:

- incident list, search, refresh, loading, empty, and error states
- incident creation with validation
- incident detail views
- notes and status updates
- local Room caching for offline access
- an outbox for mutations made while offline
- WorkManager-based background synchronization
- retry metadata and user-visible sync feedback

## Architecture

```text
Android app
  Jetpack Compose UI
        |
    ViewModels
        |
   Repository layer
     /        \
 Room cache   HTTP API
     \        /
  Offline outbox
        |
   WorkManager sync
        |
Kotlin / Spring Boot API
        |
 Spring Data JPA
        |
   PostgreSQL
```

The Android client is designed around local persistence and synchronization rather than assuming continuous network availability. Mutations can be queued locally and retried in the background, while server data is cached for responsive reads.

## Technology

### Android

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- ViewModel / lifecycle-aware state
- Room + KSP
- WorkManager
- JUnit, coroutine test utilities, Compose UI testing, AndroidX Test and WorkManager testing
- separate debug/release network configuration
- release signing configuration with secrets kept outside the repository

### Backend

- Kotlin
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Flyway
- Java 17
- Gradle

### Delivery

- multi-stage Docker build
- environment-driven database configuration
- environment-driven server port
- HTTPS-only API configuration for Android release builds
- signed APK/AAB release pipeline

## Repository structure

```text
Signaldesk/
├── android/    # Native Android application
├── backend/    # Kotlin/Spring Boot REST API
└── frontend/   # Frontend workspace
```

## API

The backend exposes incident operations under `/api/incidents`, including:

- list and retrieve incidents
- create incidents
- update incident status
- add notes
- retrieve an incident timeline
- delete incidents

## Local backend setup

The backend expects PostgreSQL and reads its connection settings from environment variables.

```text
SIGNALDESK_DB_URL
SIGNALDESK_DB_USERNAME
SIGNALDESK_DB_PASSWORD
```

Local defaults are provided for the database URL and username; the password must be supplied through the environment.

From `backend/`:

```powershell
$env:SIGNALDESK_DB_PASSWORD = Read-Host "Database password"
.\gradlew.bat test
.\gradlew.bat bootRun
```

The local API listens on port `8082` by default.

## Android development

The debug build targets a locally running backend at `http://127.0.0.1:8082`.

For a physical Android device, ADB reverse port forwarding can expose the development machine's backend to the device:

```powershell
adb reverse tcp:8082 tcp:8082
```

Release builds deliberately require an explicit HTTPS endpoint:

```powershell
.\gradlew.bat bundleRelease -PsignaldeskBaseUrl=https://your-api.example
```

Release signing credentials are supplied through user-level Gradle properties and are not stored in Git.

## Testing and validation

The project includes tests across multiple layers:

- backend tests
- Android unit tests
- Compose UI/instrumentation tests
- WorkManager synchronization tests
- Room migration coverage
- physical-device validation of the Android workflow

Manual product verification and instrumentation test execution are intentionally kept separate so automated device tests do not interfere with the installed app used for manual testing.

## Deployment status

The backend has been containerized and the cloud deployment path is currently being validated. Production database credentials and release endpoints are environment-specific and are not committed to the repository.

The Android release pipeline has been exercised with signed APK/AAB artifacts, but a genuine production Android build should only be distributed after the production HTTPS API endpoint and durable database configuration are finalized.

## Engineering focus

SignalDesk is intended to demonstrate more than screen implementation. The project emphasizes:

- offline-first data handling
- reliable background work
- explicit retry/error state
- persistence and schema migration
- client/server integration
- testable architecture
- secure release configuration
- reproducible backend packaging
- production deployment discipline

## Current status

Core Android incident workflows, offline persistence/synchronization, backend APIs, testing, and Android release preparation are implemented. Cloud deployment validation and final production endpoint configuration are in progress.

## Author

**Joshua Tobey Wabulo**

- GitHub: https://github.com/Pedurabo
- LinkedIn: https://www.linkedin.com/in/joshua-wabulo-025894275/
