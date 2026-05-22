---
trigger: always_on
---

# idea.md — AI Agent Instructions for StudyFlow Android Project

## What You Are Building

**StudyFlow** — A Student Productivity Android App
- Language: **Kotlin only**
- UI framework: **Jetpack Compose only** (NO XML, NO View system, NO fragments)
- Local database: **Room (SQLite)**
- Architecture: **MVVM + Repository Pattern**
- Minimum SDK: 26 | Target SDK: 35

---

## Prime Directives (NEVER Violate These)

1. **NO XML layouts** — Every UI must be a `@Composable` function. Never touch `res/layout/`.
2. **NO external database** — All data lives in Room. No Firebase, no Supabase, no REST persistence.
3. **NO crypto features** — The topic is prohibited.
4. **NO AI/ML features** — They are ignored during marking. Spend zero time on them.
5. **Room is MANDATORY** — Every feature must read/write from the local Room database.
6. **Kotlin only** — No Java files, ever.

---

## How to Approach Each Task

### Before writing any code:
1. Read `ARCHITECTURE.md` to understand the project structure.
2. Read `RULES.md` for naming, styling, and pattern conventions.
3. Read `PROJECT_SPEC.md` for the full feature list and database schema.
4. Check if the feature's DAO, Entity, and ViewModel already exist before creating new ones.

### When adding a new screen:
1. Create the Entity in `data/local/entity/`
2. Create the DAO in `data/local/dao/`
3. Register the Entity in `AppDatabase.kt`
4. Create the Repository in `data/repository/`
5. Create the ViewModel in `presentation/<feature>/`
6. Create the Composable screen in `presentation/<feature>/`
7. Register the route in `navigation/NavGraph.kt`

### When editing existing code:
- Prefer editing over rewriting. Touch only what is necessary.
- Never remove a Room Entity or DAO without explicit instruction.
- Keep all `@Database` entity registrations in sync.

---

## Dependency Injection

Use **Hilt** for all dependency injection.

- All ViewModels: annotated with `@HiltViewModel`
- All Repositories: provided via `@Singleton` in a Hilt module
- Entry point: `@AndroidEntryPoint` on `MainActivity`
- Application class: `@HiltAndroidApp` on `StudyFlowApplication`

Never use manual `ViewModel` factories or pass repositories through constructors manually.

---

## Navigation

Use **Navigation Compose** (`androidx.navigation:navigation-compose`).

- All routes defined as `sealed class` or `object` in `navigation/Screen.kt`
- Single `NavHost` in `navigation/NavGraph.kt`
- Use `BottomNavigation` composable for the main tab bar
- Pass `navController` only to top-level screens; child composables receive lambdas

---

## State Management

- All UI state is a **data class** named `<Feature>UiState`
- State is exposed from ViewModel as `StateFlow<UiState>` collected with `collectAsStateWithLifecycle()`
- **Never** use `MutableState` directly inside a ViewModel
- Side effects (navigation, snackbars) use `Channel<UiEvent>` collected with `LaunchedEffect`

---

## Common Commands

```bash
# Build the project
./gradlew assembleDebug

# Run all unit tests
./gradlew test

# Run lint checks
./gradlew lint

# Generate Room schema export
# (schema export dir set in build.gradle)
./gradlew kspDebugKotlin
```

---

## What "Done" Means for Any Feature

A feature is complete when:
- [ ] Room Entity exists and is registered in `AppDatabase`
- [ ] DAO has all required queries (insert, update, delete, select flows)
- [ ] Repository wraps DAO calls and exposes `Flow` / `suspend fun`
- [ ] ViewModel exposes `UiState` as `StateFlow`
- [ ] Composable screen renders from `UiState` with no logic inside
- [ ] Route registered in `NavGraph.kt`
- [ ] Bottom nav badge/icon updated if it's a primary screen
- [ ] Basic error state handled (empty list message, loading indicator)

---

## File Generation Rules

- Do NOT generate placeholder/stub files with `TODO()` and leave them — implement them fully.
- Every new file must compile. Do not leave import errors.
- After adding a new Entity, always update `AppDatabase.kt` entities list and increment `version`.
- After a DB version bump, add a `Migration` object or use `fallbackToDestructiveMigration()` during development only.