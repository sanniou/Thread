# Project Tasks & Roadmap

This document outlines the development roadmap for the "Thread" application, broken down into actionable tasks. It is intended to be used by developers (or AI assistants) to guide future work, ensuring consistency with the established Clean Architecture.

---

## Phase 1: Stabilize & Build Core Feature (The Aggregated Feed)

**Objective:** Make the project runnable again after the major refactoring and display the first version of the aggregated feed.

- [ ] **Task 1: Fix Compilation Errors**
  - **Priority:** Highest. The project is currently unbuildable.
  - **Action:** Resolve all build errors in the `composeApp` and `feature-nmb` modules. This primarily involves:
    - Updating `import` statements to point to the new locations of models and UseCases in `core-domain` and `core-data`.
    - Refactoring `ViewModel` classes to inject and use `UseCase`s from the `core-domain` layer instead of old `Repository` implementations.
    - Adjusting DI (Kodein) configurations in `feature-nmb` to correctly obtain dependencies from the new core modules.

- [ ] **Task 2: Create `feature-feed` Module**
  - **Action:** Create a new feature module named `feature-feed`.
    - Create `feature-feed/build.gradle.kts` (can be based on `feature-nmb`'s file).
    - Add `implementation(project(":core-domain"))` and `implementation(project(":core-ui"))` as dependencies.
    - Add `include(":feature-feed")` to `settings.gradle.kts`.

- [ ] **Task 3: Implement `FeedViewModel`**
  - **Location:** `feature-feed/src/commonMain/kotlin/ai/saniou/feature/feed/`
  - **Action:** Create a `FeedViewModel` that:
    - Injects `GetAggregatedFeedUseCase` from the `core-domain` module.
    - Exposes a `StateFlow` of the UI state (e.g., `Loading`, `Success(List<Post>)`, `Error`).
    - Contains a function to trigger the `GetAggregatedFeedUseCase` to fetch data.

- [ ] **Task 4: Implement `FeedScreen` UI**
  - **Location:** `feature-feed/src/commonMain/kotlin/ai/saniou/feature/feed/`
  - **Action:** Create a Composable `FeedScreen` that:
    - Observes the state from `FeedViewModel`.
    - Displays a loading indicator, an error message, or a list of posts.
    - Uses a generic `PostCard` composable (can be placed in `core-ui`) to display each item from the `List<Post>`. The card should show basic info like title, author, and `sourceName`.

- [ ] **Task 5: Integrate `feature-feed` into `composeApp`**
  - **Action:**
    - Add `implementation(project(":feature-feed"))` to `composeApp/build.gradle.kts`.
    - In `composeApp`'s main navigation graph (likely using Voyager), set `FeedScreen` as the default/home screen.

---

## Phase 2: Enhance Data Layer (Caching)

**Objective:** Implement a database cache to provide offline capabilities and improve performance.

- [ ] **Task 6: Integrate SQLDelight into `core-data`**
  - **Action:** Configure the SQLDelight Gradle plugin in `core-data/build.gradle.kts` and add necessary dependencies. Create the `sqldelight` source folder.

- [ ] **Task 7: Define Database Schema**
  - **Location:** `core-data/src/commonMain/sqldelight/`
  - **Action:** Create `.sq` files to define the schema for tables like `Post`, `Forum`, and `RemoteKeys` (for pagination). The schema should align with the models in `core-domain`.

- [ ] **Task 8: Implement Caching in `FeedRepositoryImpl`**
  - **Action:** Refactor `FeedRepositoryImpl` to implement a "cache-then-network" strategy.
    - When data is requested, first query the SQLDelight database.
    - If data is present and not expired, return it directly.
    - If data is missing or expired, fetch from the appropriate remote `Source`, save the new data to the database, and then return it.

---

## Phase 3: Expand Source-Specific Features

**Objective:** Build out the UI and logic for viewing content from specific sources.

- [ ] **Task 9: Re-implement `feature-nmb` Details Screen**
  - **Action:**
    - In `core-domain`, define a specific `NmbThreadDetails` model and a `GetNmbThreadDetailsUseCase`.
    - In `core-data`, update `NmbSource` and `FeedRepositoryImpl` to implement the new `UseCase`.
    - In `feature-nmb`, create a new `NmbThreadScreen` and `ViewModel` that uses the `GetNmbThreadDetailsUseCase` to display a post with NMB-specific UI elements (e.g., formatted user hash, references).

- [ ] **Task 10: Implement `NgaSource`**
  - **Action:** Replace the skeleton `NgaSource` in `core-data` with a real implementation. This involves:
    - Researching or reverse-engineering the NGA API.
    - Creating `NgaApi.kt`, `NgaDto.kt`, and `NgaMapper.kt`.
    - Filling in the logic in `NgaSource.kt`.

---

## Phase 4: Implement User-Facing Utilities

**Objective:** Add settings and data synchronization features.

- [ ] **Task 11: Implement Settings Screen**
  - **Action:** Create a new `feature-settings` module.
    - Build a UI that allows users to manage which information sources are active.
    - Provide configuration options for data sync (e.g., entering WebDAV credentials).

- [ ] **Task 12: Implement Data Sync**
  - **Action:**
    - Implement the real logic for `WebDavSyncProvider` and `LocalSyncProvider` in `core-data`.
    - Create `ExportUserDataUseCase` and `ImportUserDataUseCase` in `core-domain`.
    - Connect the `SettingsScreen` UI to these `UseCase`s.