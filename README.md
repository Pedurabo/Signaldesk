# SignalDesk

**Offline-capable incident management for Android, backed by Kotlin/Spring Boot and PostgreSQL.**

SignalDesk is a full-stack engineering project built around a native Android client that remains useful when connectivity is unreliable. It treats offline persistence, synchronization, API design, database migration, testing, release signing, containerization and deployment as parts of one system—not separate demos.

> **Status:** Core Android workflows, offline synchronization, backend APIs, automated testing and signed Android release builds are implemented. Cloud deployment validation is currently in progress.

## Highlights

- **Offline-first Android workflow** — incidents are cached locally with Room and remain readable without a network connection.
- **Reliable offline mutations** — writes can be queued in a local outbox and delivered later through WorkManager.
- **Observable synchronization** — retry attempts, timestamps and errors are tracked and surfaced rather than silently discarded.
- **Full incident lifecycle** — list/search/refresh, create, details, notes and status updates.
- **Kotlin backend** — Spring Boot REST API with JPA, PostgreSQL and Flyway migrations.
- **Release-oriented engineering** — HTTPS-only release API configuration, externalized signing secrets, signed APK/AAB builds and Docker packaging.
- **Layered verification** — backend, Android unit, Compose UI, WorkManager and Room migration tests plus physical-device validation.

## Screenshots

Android screenshots will be added after the final production-endpoint validation pass. The current repository intentionally avoids presenting mockups as finished product evidence.

## Architecture

```text
┌──────────────────────── Android ────────────────────────┐
│                                                        │
│  Jetpack Compose UI                                    │
│          │                                             │
│      ViewModels                                        │
│          │                                             │
│      Repository                                        │
│       /      \                                         │
│  Room cache   REST API                                 │
│      │           │                                     │
│  Offline outbox  │                                     │
│      │           │                                     │
│  WorkManager ────┘                                     │
└───────────────────────┬────────────────────────────────┘
                        │ HTTPS / JSON
                        ▼
┌──────────────────────── Backend ────────────────────────┐
│ Kotlin + Spring Boot                                   │
│ Spring Web MVC → Spring Data JPA → PostgreSQL          │
│                         │                              │
│                       Flyway                           │
└────────────────────────────────────────────────────────┘
```

The client does not assume continuous connectivity. Server data is cached locally for responsive reads, while mutations made offline can be persisted and retried in the background until client and server state converge.

## Android application

The Android client currently supports:

- cached incident list with loading, empty and error states
- search, refresh and pull-to-refresh
- incident creation with validation
- incident details
- notes and status changes
- Room-backed offline persistence
- offline mutation outbox
- WorkManager background synchronization
- retry metadata and user-visible sync feedback

### Android stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel / lifecycle-aware state |
| Persistence | Room + KSP |
| Background work | WorkManager |
| Testing | JUnit, coroutine test utilities, Compose UI Test, AndroidX Test, WorkManager Test |
| Release | Gradle signing config, HTTPS-only release endpoint |

## Backend

The backend is a Kotlin/Spring Boot service using:

- Spring Web MVC
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Flyway
- Java 17
- Gradle

Incident operations are exposed under `/api/incidents` and cover:

| Operation | Route shape |
| --- | --- |
| List incidents | `GET /api/incidents` |
| Retrieve incident | `GET /api/incidents/{id}` |
| Create incident | `POST /api/incidents` |
| Change status | `PATCH /api/incidents/{id}/status` |
| Add note | `POST /api/incidents/{id}/notes` |
| Timeline | `GET /api/incidents/{id}/timeline` |
| Delete incident | `DELETE /api/incidents/{id}` |

## Repository structure

```text
Signaldesk/
├── android/     # Native Android application
├── backend/     # Kotlin/Spring Boot REST API
└── frontend/    # Frontend workspace
```

The Android application and backend are the implemented core of the current project. The frontend directory is retained as a separate workspace and is not the primary client described by this README.

## Running the backend locally

### Prerequisites

- Java 17
- PostgreSQL
- the repository cloned locally

The backend reads database configuration from:

```text
SIGNALDESK_DB_URL
SIGNALDESK_DB_USERNAME
SIGNALDESK_DB_PASSWORD
```

The URL and username have local development defaults. The password must be supplied through the environment.

From `backend/` on Windows PowerShell:

```powershell
$env:SIGNALDESK_DB_PASSWORD = Read-Host "Database password"
.\gradlew.bat test
.\gradlew.bat bootRun
```

The API listens on port `8082` by default.

## Running Android against the local backend

The debug build targets:

```text
http://127.0.0.1:8082
```

For a physical Android device connected through ADB:

```powershell
adb reverse tcp:8082 tcp:8082
```

Then install the normal debug application and launch it on the device.

> Instrumentation tests and manual product verification are kept separate. Device test execution can replace/remove the normal installed app, so manual verification uses the standard debug install rather than an instrumentation-test run.

## Release builds

Release builds require an explicit HTTPS API endpoint. A release build will not silently fall back to the local development server.

From `android/`:

```powershell
.\gradlew.bat bundleRelease -PsignaldeskBaseUrl=https://your-api.example
```

Signing credentials are supplied through user-level Gradle properties and are intentionally excluded from Git.

The release pipeline has been exercised with signed APK and AAB artifacts. Distribution should wait until the real HTTPS backend and durable production database are finalized.

## Testing

SignalDesk is validated across several layers:

- backend automated tests
- Android unit tests
- Compose UI/instrumentation tests
- WorkManager synchronization tests
- Room migration coverage
- manual physical-device workflow verification

The project favors small, verifiable changes and treats failures in networking, synchronization, persistence and deployment as engineering states to be handled explicitly.

## Containerization and deployment

The backend uses a multi-stage Docker build and supports environment-driven database and server configuration.

Cloud deployment validation is **in progress**. The service has reached the cloud runtime and database integration stage, but this README will not describe the API as production-ready until the deployment, persistence and Android release endpoint have been verified end to end.

No production credentials are stored in this repository.

## Engineering goals

SignalDesk is designed to demonstrate:

- modern native Android development
- offline-first data handling
- reliable background work
- explicit retry and error state
- local/remote data synchronization
- database migration discipline
- client/server integration
- automated and physical-device testing
- secure release configuration
- reproducible backend packaging
- production-oriented deployment practices

## Roadmap

- [x] Core incident list/detail/create workflows
- [x] Room offline cache
- [x] Offline mutation outbox
- [x] WorkManager synchronization
- [x] Retry metadata and sync feedback
- [x] Backend incident API
- [x] PostgreSQL + Flyway persistence
- [x] Android automated testing
- [x] Physical-device validation
- [x] Signed APK/AAB pipeline
- [x] Backend Docker packaging
- [ ] Complete cloud deployment validation
- [ ] Verify public HTTPS API end to end
- [ ] Build Android release against the verified production endpoint
- [ ] Add final product screenshots
- [ ] Finalize durable production database and distribution path

## Author

**Joshua Tobey Wabulo**

- GitHub: https://github.com/Pedurabo
- LinkedIn: https://www.linkedin.com/in/joshua-wabulo-025894275/
