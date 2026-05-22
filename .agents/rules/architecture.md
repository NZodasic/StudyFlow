---
trigger: always_on
---

# ARCHITECTURE.md — StudyFlow Project Architecture

## Overview

StudyFlow follows **Clean Architecture** with **MVVM** presentation layer,
organized as a single-module Android project.

```
app/
└── src/main/
    ├── java/com/studyflow/app/
    │   ├── StudyFlowApplication.kt       ← @HiltAndroidApp
    │   ├── MainActivity.kt               ← @AndroidEntryPoint, single activity
    │   │
    │   ├── data/
    │   │   ├── local/
    │   │   │   ├── AppDatabase.kt        ← Room DB, version, all entities
    │   │   │   ├── dao/
    │   │   │   │   ├── TaskDao.kt
    │   │   │   │   ├── HabitDao.kt
    │   │   │   │   ├── NoteDao.kt
    │   │   │   │   ├── ExpenseDao.kt
    │   │   │   │   ├── PomodoroDao.kt
    │   │   │   │   └── SettingsDao.kt
    │   │   │   └── entity/
    │   │   │       ├── TaskEntity.kt
    │   │   │       ├── HabitEntity.kt
    │   │   │       ├── HabitLogEntity.kt
    │   │   │       ├── NoteEntity.kt
    │   │   │       ├── ExpenseEntity.kt
    │   │   │       ├── PomodoroSessionEntity.kt
    │   │   │       └── UserSettingsEntity.kt
    │   │   │
    │   │   └── repository/
    │   │       ├── TaskRepository.kt
    │   │       ├── HabitRepository.kt
    │   │       ├── NoteRepository.kt
    │   │       ├── ExpenseRepository.kt
    │   │       ├── PomodoroRepository.kt
    │   │       └── SettingsRepository.kt
    │   │
    │   ├── di/
    │   │   ├── DatabaseModule.kt         ← provides AppDatabase, all DAOs
    │   │   └── RepositoryModule.kt       ← binds repositories
    │   │
    │   ├── navigation/
    │   │   ├── Screen.kt                 ← sealed class with all routes
    │   │   └── NavGraph.kt               ← NavHost + BottomNavigation
    │   │
    │   ├── presentation/
    │   │   ├── dashboard/
    │   │   │   ├── DashboardScreen.kt
    │   │   │   ├── DashboardViewModel.kt
    │   │   │   └── DashboardUiState.kt
    │   │   ├── tasks/
    │   │   │   ├── TaskListScreen.kt
    │   │   │   ├── TaskDetailScreen.kt
    │   │   │   ├── TaskViewModel.kt
    │   │   │   └── TaskUiState.kt
    │   │   ├── habits/
    │   │   │   ├── HabitScreen.kt
    │   │   │   ├── HabitViewModel.kt
    │   │   │   └── HabitUiState.kt
    │   │   ├── pomodoro/
    │   │   │   ├── PomodoroScreen.kt
    │   │   │   ├── PomodoroViewModel.kt
    │   │   │   └── PomodoroUiState.kt
    │   │   ├── notes/
    │   │   │   ├── NoteListScreen.kt
    │   │   │   ├── NoteDetailScreen.kt
    │   │   │   ├── NoteViewModel.kt
    │   │   │   └── NoteUiState.kt
    │   │   ├── expenses/
    │   │   │   ├── ExpenseScreen.kt
    │   │   │   ├── ExpenseViewModel.kt
    │   │   │   └── ExpenseUiState.kt
    │   │   ├── analytics/
    │   │   │   ├── AnalyticsScreen.kt
    │   │   │   ├── AnalyticsViewModel.kt
    │   │   │   └── AnalyticsUiState.kt
    │   │   └── settings/
    │   │       ├── SettingsScreen.kt
    │   │       ├── SettingsViewModel.kt
    │   │       └── SettingsUiState.kt
    │   │
    │   └── ui/
    │       ├── theme/
    │       │   ├── Color.kt
    │       │   ├── Theme.kt
    │       │   └── Type.kt
    │       └── components/              ← shared reusable composables
    │           ├── StudyFlowTopBar.kt
    │           ├── PriorityChip.kt
    │           ├── EmptyStateView.kt
    │           ├── LoadingIndicator.kt
    │           └── ConfirmDialog.kt
    │
    └── res/
        ├── values/strings.xml
        ├── values/colors.xml            ← only fallback, main colors in Color.kt
        └── drawable/                    ← launcher icon only
```

---

## Layer Responsibilities

### Data Layer (`data/`)

**Entities** — Plain Kotlin data classes annotated with `@Entity`.
- No business logic
- Only Room annotations + basic field defaults
- Use `Long` for timestamps (store as epoch milliseconds)

**DAOs** — Interfaces annotated with `@Dao`.
- Return `Flow<List<Entity>>` for observable queries
- Use `suspend fun` for insert / update / delete
- Queries that aggregate (SUM, COUNT) return `Flow<SomeType>`

**AppDatabase** — Single `@Database` class.
- Lists ALL entities in the `entities` array
- Provides all DAOs as abstract functions
- Singleton, provided by Hilt

**Repositories** — Plain Kotlin classes (not interfaces for this project scope).
- Inject the DAO via constructor
- Map between Entity ↔ Domain model if needed (keep it simple: Entity IS the domain model here)
- Expose `Flow` for reads, `suspend fun` for writes

### DI Layer (`di/`)

```kotlin
@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "studyflow.db")
            .fallbackToDestructiveMigration()   // dev only; add Migrations before release
            .build()

    @Provides fun provideTaskDao(db: AppDatabase) = db.taskDao()
    // ... repeat for all DAOs
}
```

### Presentation Layer (`presentation/`)

**UiState** — Immutable data class.
```kotlin
data class TaskUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedFilter: TaskFilter = TaskFilter.ALL
)
```

**ViewModel** — `@HiltViewModel`, `@Inject constructor(repo: TaskRepository)`.
```kotlin
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repo: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init { observeTasks() }

    private fun observeTasks() {
        viewModelScope.launch {
            repo.getAllTasks().collect { tasks ->
                _uiState.update { it.copy(tasks = tasks, isLoading = false) }
            }
        }
    }
}
```

**Screen Composable** — Stateless; receives state and lambdas.
```kotlin
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel = hiltViewModel(),
    onNavigateToDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TaskListContent(
        uiState = uiState,
        onAddTask = viewModel::addTask,
        onTaskClick = onNavigateToDetail,
        onDeleteTask = viewModel::deleteTask
    )
}

@Composable
private fun TaskListContent(   // testable, preview-friendly
    uiState: TaskUiState,
    onAddTask: (String) -> Unit,
    onTaskClick: (Long) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit
) { ... }
```

---

## Navigation Architecture

```kotlin
// Screen.kt
sealed class Screen(val route: String) {
    object Dashboard  : Screen("dashboard")
    object Tasks      : Screen("tasks")
    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: Long) = "task_detail/$taskId"
    }
    object Habits     : Screen("habits")
    object Pomodoro   : Screen("pomodoro")
    object Notes      : Screen("notes")
    object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: Long) = "note_detail/$noteId"
    }
    object Expenses   : Screen("expenses")
    object Analytics  : Screen("analytics")
    object Settings   : Screen("settings")
}
```

Bottom nav tabs: **Dashboard | Tasks | Habits | Pomodoro | Expenses**
Secondary screens (Notes, Analytics, Settings) accessed from Dashboard or top-bar icons.

---

## Database Schema

```kotlin
// AppDatabase.kt
@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        NoteEntity::class,
        ExpenseEntity::class,
        PomodoroSessionEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun noteDao(): NoteDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun pomodoroDao(): PomodoroDao
    abstract fun settingsDao(): SettingsDao
}
```

### Entity Definitions

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDateMillis: Long? = null,
    val priority: Int = 1,           // 0=Low, 1=Medium, 2=High
    val category: String = "General",
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconEmoji: String = "⭐",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "habit_logs")
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,               // FK to habits.id
    val dateMillis: Long             // start of day epoch
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String = "",
    val subject: String = "",
    val isPinned: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String,            // Food, Books, Transport, etc.
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val durationMinutes: Int,
    val taskLabel: String = "",
    val completedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,     // single-row settings table
    val isDarkTheme: Boolean = false,
    val pomodoroDurationMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15
)
```

---

## Key Gradle Dependencies

```kotlin
// build.gradle.kts (app)
dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")

    // DataStore (for theme preference)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Charts
    implementation("com.patrykandpatrick.vico:compose-m3:1.15.0")

    // WorkManager (notifications)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
}
```