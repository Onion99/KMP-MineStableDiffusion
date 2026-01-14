---
trigger: always_on
---

# Kotlin Multiplatform Project Coding Rules

Current State: 2026-01-14

## 1. Tech Stack Overview
- **Language**: Kotlin (Multiplatform)
- **UI Framework**: Compose Multiplatform (Android, iOS, Desktop/JVM)
- **Dependency Injection**: Koin (v4.1.1)
- **Navigation**: Jetpack Navigation Compose (v2.9.0-beta04)
- **Networking**: Ktor (v3.2.3) + Sandwich (v2.1.2)
- **Asynchronous**: Kotlin Coroutines (v1.10.2)
- **Image Loading**: Coil3 (v3.3.0)
- **File I/O**: FileKit (v0.10.0-beta03)
- **Animations**: Compottie (Lottie)

## 2. Project Structure & Modules
- **`composeApp`**: Main application module. Contains features, UI screens, ViewModels, and App entry points.
- **`ui-theme`**: Design system module. Contains Colors, Typography, Shapes (`Theme.kt`, `style/`).
- **`data-network`**: Network layer. Contains Ktor client setup (`NetworkModule.kt`), API services.
- **`data-model`**: Domain objects and Data classes.
- **`shared`**: Shared core logic (if used).
- **`cpp`**: Native C++ implementations (Stable Diffusion).

## 3. Architecture & Patterns
### Implementation Architecture - MVVM
- **View**: Composable functions in `ui/screen/`.
- **ViewModel**: `androidx.lifecycle.ViewModel` in `viewmodel/`.
- **DI**: Koin modules defined in `di/` (e.g., `KoinDi.kt`, `ViewModelModule.kt`).

### Navigation
- Use **Jetpack Navigation Compose**.
- Routes defined in `ui/navigation/`.
- Type-safe navigation arguments recommended.

### Data Layer
- **Network**: Use `HttpClient` provided by `NetworkModule.kt`.
- **JSON Serialization**: `kotlinx.serialization` (configured with `ignoreUnknownKeys = true`, `isLenient = true`).

## 4. Coding Conventions
### UI Development
- **Styling**: ALWAYS use values from `ui-theme` (e.g., `MaterialTheme.colorScheme`, `MaterialTheme.typography`).
- **Layout**: Use `AdaptiveLayout` (found in `ui-theme`) for cross-platform responsiveness.
- **Adaptive Strategy**: Use `AppTheme.contentType` to adapt complex layouts.
    - `ContentType.Single` (Mobile): Use vertical layouts (Column) to prevent overcrowding.
    - `ContentType.Dual` (Desktop): Use horizontal layouts (Row) to maximize screen usage.
- **Resources**: Use `compose.components.resources` for images/strings.

### Concurrency
- Use `viewModelScope` for ViewModel-bound coroutines.
- Use `Dispatchers.Default` for CPU-intensive tasks (like image generation logic).

### Native Integration
- **JNI**: Interact with native code via `DiffusionLoader` (found in `native/`).
- **Resources**: DLLs/SOs are managed via Gradle tasks (`BuildNativeLibTask`) and copied to JVM resources.

## 5. Development Workflow
1.  **New Feature**:
    - Define data models in `data-model`.
    - Create/Update API in `data-network` if needed.
    - Create ViewModel in `composeApp/.../viewmodel` and register in `ViewModelModule.kt`.
    - Create Screen in `ui/screen` and add route in `ui/navigation`.
2.  **Dependency Updates**: Check `gradle/libs.versions.toml`.

## 6. Specific implementation details
- **Image Generation**: Logic resides in `ChatViewModel.kt`, invoking `DiffusionLoader.txt2Img`.
- **Context Management**: `ChatViewModel` manages chat state (`_currentChatMessages`).

---
> [!NOTE]
> This document should be updated as new patterns are introduced.
