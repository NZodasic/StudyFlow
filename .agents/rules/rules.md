---
trigger: always_on
---

# RULES.md — Coding Standards & Conventions for StudyFlow

## Hard Rules (Agent Must Never Break)

| # | Rule |
|---|------|
| R1 | No XML layout files. UI is 100% Jetpack Compose. |
| R2 | No Java source files. Kotlin only. |
| R3 | No network calls for data persistence. Room is the only data store. |
| R4 | No `runBlocking` on the main thread. All suspend calls go in `viewModelScope` or `lifecycleScope`. |
| R5 | No business logic inside `@Composable` functions. Composables only render state. |
| R6 | No direct DAO access from ViewModels. Always go through Repository. |
| R7 | Never expose `MutableStateFlow` or `MutableLiveData` from a ViewModel's public API. |
| R8 | Every Room `@Entity` change = increment `AppDatabase.version` + add migration or `fallbackToDestructiveMigration()`. |
| R9 | No hardcoded strings in Composables. Use `stringResource()` or constants in a `Constants.kt` file. |
| R10 | No hardcoded colors in Composables. Use `MaterialTheme.colorScheme.*` tokens only. |

---

## Naming Conventions

### Files & Classes

| Type | Pattern | Example |
|------|---------|---------|
| Room Entity | `<Name>Entity` | `TaskEntity` |
| DAO interface | `<Name>Dao` | `TaskDao` |
| Repository | `<Name>Repository` | `TaskRepository` |
| ViewModel | `<Name>ViewModel` | `TaskViewModel` |
| UI State | `<Name>UiState` | `TaskUiState` |
| UI Event | `<Name>UiEvent` | `TaskUiEvent` |
| Screen composable | `<Name>Screen` | `TaskListScreen` |
| Content composable | `<Name>Content` | `TaskListContent` |
| Hilt module | `<Scope>Module` | `DatabaseModule`, `RepositoryModule` |

### Functions

```kotlin
// Composables → PascalCase
@Composable fun TaskCard(task: TaskEntity, onClick: () -> Unit) { ... }

// ViewModel methods → camelCase, verb first
fun addTask(title: String, priority: Int) { ... }
fun deleteTask(task: TaskEntity) { ... }
fun updateSearchQuery(query: String) { ... }
fun toggleTaskCompletion(taskId: Long) { ... }

// Repository methods → camelCase, verb first
suspend fun insertTask(task: TaskEntity)
suspend fun deleteTask(task: TaskEntity)
fun getAllTasks(): Flow<List<TaskEntity>>
fun getTaskById(id: Long): Flow<TaskEntity?>
```

### Constants

```kotlin
// Constants.kt
object DbConstants {
    const val DB_NAME = "studyflow.db"
    const val DB_VERSION = 1
}

object PriorityLevel {
    const val LOW = 0
    const val MEDIUM = 1
    const val HIGH = 2
}

object ExpenseCategory {
    const val FOOD = "Food"
    const val BOOKS = "Books"
    const val TRANSPORT = "Transport"
    const val ENTERTAINMENT = "Entertainment"
    const val OTHER = "Other"
    val ALL = listOf(FOOD, BOOKS, TRANSPORT, ENTERTAINMENT, OTHER)
}
```

---

## Compose UI Rules

### Composable Function Structure

```kotlin
// ✅ CORRECT — screen composable collects state, delegates to content
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel = hiltViewModel(),
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddTask: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TaskListContent(
        uiState = uiState,
        onTaskClick = onNavigateToDetail,
        onAddClick = onNavigateToAddTask,
        onToggleComplete = viewModel::toggleTaskCompletion,
        onDelete = viewModel::deleteTask,
        onSearchChange = viewModel::updateSearchQuery
    )
}

// ✅ CORRECT — content composable is pure, preview-friendly
@Composable
private fun TaskListContent(
    uiState: TaskUiState,
    onTaskClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onToggleComplete: (Long) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onSearchChange: (String) -> Unit
) {
    // Render uiState here — no ViewModel, no side effects
}
```

```kotlin
// ❌ WRONG — logic inside composable
@Composable
fun TaskListScreen(repo: TaskRepository) {
    val tasks = remember { repo.getAllTasks() }   // ❌ wrong
    if (tasks.isEmpty()) repo.seed()              // ❌ wrong
}
```

### Preview Rules

Every non-trivial `Content` composable must have a `@Preview`:

```kotlin
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Preview(showBackground = true)
@Composable
private fun TaskListContentPreview() {
    StudyFlowTheme {
        TaskListContent(
            uiState = TaskUiState(
                tasks = listOf(
                    TaskEntity(id = 1, title = "Study Compose", priority = 2),
                    TaskEntity(id = 2, title = "Read ROOM docs", priority = 1, isCompleted = true)
                )
            ),
            onTaskClick = {},
            onAddClick = {},
            onToggleComplete = {},
            onDelete = {},
            onSearchChange = {}
        )
    }
}
```

### Material 3 Component Usage

| Use | Don't Use |
|-----|-----------|
| `Scaffold` | Custom root layouts |
| `TopAppBar` / `CenterAlignedTopAppBar` | Custom toolbar composables |
| `NavigationBar` + `NavigationBarItem` | Custom bottom bar |
| `FloatingActionButton` | Custom FAB |
| `Card` | `Box` with manual border |
| `OutlinedTextField` | Custom input fields |
| `AlertDialog` | Custom dialog composable from scratch |
| `DropdownMenu` | Custom spinner |
| `MaterialTheme.colorScheme.*` | Hardcoded `Color(0xFF...)` |
| `MaterialTheme.typography.*` | Hardcoded `TextStyle` |

---

## Room / Database Rules

### DAO Pattern

```kotlin
@Dao
interface TaskDao {

    // Observable queries → return Flow
    @Query("SELECT * FROM tasks ORDER BY priority DESC, dueDateMillis ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDateMillis <= :todayEnd")
    fun getTasksDueToday(todayEnd: Long): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    fun getCompletedTaskCount(): Flow<Int>

    // Writes → suspend functions, no return value needed unless you need rowId
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, completed: Boolean)
}
```

### Repository Pattern

```kotlin
class TaskRepository @Inject constructor(private val dao: TaskDao) {

    fun getAllTasks(): Flow<List<TaskEntity>> = dao.getAllTasks()

    fun getTaskById(id: Long): Flow<TaskEntity?> = dao.getTaskById(id)

    suspend fun addTask(task: TaskEntity) = dao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = dao.deleteTask(task)

    suspend fun toggleCompletion(taskId: Long, completed: Boolean) =
        dao.setTaskCompleted(taskId, completed)
}
```

---

## ViewModel Rules

```kotlin
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    // Side effects channel
    private val _events = Channel<TaskUiEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            taskRepository.getAllTasks()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { tasks ->
                    _uiState.update { it.copy(tasks = tasks, isLoading = false) }
                }
        }
    }

    fun addTask(title: String, description: String, priority: Int, dueDateMillis: Long?) {
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
            return
        }
        viewModelScope.launch {
            taskRepository.addTask(
                TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    priority = priority,
                    dueDateMillis = dueDateMillis
                )
            )
            _events.send(TaskUiEvent.TaskAdded)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            _events.send(TaskUiEvent.ShowSnackbar("Task deleted"))
        }
    }

    fun toggleTaskCompletion(taskId: Long) {
        val currentTask = _uiState.value.tasks.find { it.id == taskId } ?: return
        viewModelScope.launch {
            taskRepository.toggleCompletion(taskId, !currentTask.isCompleted)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

// UiEvent sealed class
sealed class TaskUiEvent {
    object TaskAdded : TaskUiEvent()
    data class ShowSnackbar(val message: String) : TaskUiEvent()
    data class NavigateTo(val route: String) : TaskUiEvent()
}
```

---

## Coroutine & Threading Rules

```kotlin
// ✅ CORRECT — IO dispatcher for Room (Room handles this internally for Flow,
//    but use Dispatchers.IO for one-shot suspend calls if wrapping callbacks)
viewModelScope.launch(Dispatchers.IO) {
    repository.insertTask(task)
}

// ✅ CORRECT — collect on default dispatcher (Compose handles UI thread)
viewModelScope.launch {
    repository.getAllTasks().collect { ... }
}

// ❌ WRONG
runBlocking { repository.getAllTasks().first() }   // blocks main thread

// ❌ WRONG
GlobalScope.launch { ... }   // not tied to ViewModel lifecycle
```

---

## Theming Rules

```kotlin
// Color.kt — define your brand palette
val PrimaryBlue = Color(0xFF4A6FA5)
val SecondaryTeal = Color(0xFF2EC4B6)
val AccentOrange = Color(0xFFFF9F1C)
val ErrorRed = Color(0xFFE63946)

// Theme.kt — wire into Material3 color scheme
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryTeal,
    tertiary = AccentOrange,
    error = ErrorRed,
    // ... fill other tokens
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8BB4E0),
    secondary = SecondaryTeal,
    // ...
)

@Composable
fun StudyFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

---

## Error Handling Checklist

- [ ] Empty list state shows an `EmptyStateView` composable (illustration + message)
- [ ] Loading state shows a `CircularProgressIndicator`
- [ ] Error state shows a `Snackbar` or inline error text, never a crash
- [ ] Form validation happens in ViewModel before calling repository
- [ ] Room errors are caught with `.catch {}` in ViewModel flow collection