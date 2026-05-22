---
trigger: always_on
---

# PROJECT_SPEC.md — StudyFlow Feature Specification & Build Checklist

## App Identity

| Field | Value |
|-------|-------|
| App Name | StudyFlow |
| Package | `com.studyflow.app` |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Database | Room (SQLite) |
| Architecture | MVVM + Repository + Hilt |

---

## Screen Inventory (10 Screens Total)

| # | Screen | Route | Nav Location |
|---|--------|-------|-------------|
| 1 | Dashboard | `dashboard` | Bottom nav tab 1 |
| 2 | Task List | `tasks` | Bottom nav tab 2 |
| 3 | Task Detail / Add | `task_detail/{taskId}` | Push from Task List |
| 4 | Habits | `habits` | Bottom nav tab 3 |
| 5 | Pomodoro Timer | `pomodoro` | Bottom nav tab 4 |
| 6 | Expenses | `expenses` | Bottom nav tab 5 |
| 7 | Notes List | `notes` | Dashboard quick-access |
| 8 | Note Detail / Edit | `note_detail/{noteId}` | Push from Notes |
| 9 | Analytics | `analytics` | Dashboard quick-access / top bar |
| 10 | Settings | `settings` | Top bar icon |

---

## Feature Specifications

---

### 1. Dashboard Screen

**Purpose:** Bird's-eye view of the student's day.

**UI Elements:**
- Greeting header with current date (e.g., "Good morning, Student!")
- **Stats row:** Tasks due today (count) | Pomodoro sessions today | Expenses this week
- **Today's Tasks** — horizontal scrollable card list (max 5, "See All" button)
- **Active Habits** — row of habit emoji circles, filled = done today
- **Quick Action buttons** — "+ Task", "Start Focus", "+ Expense", "+ Note"
- **Upcoming Deadlines** section — next 3 tasks with due dates

**Room queries needed:**
```
TaskDao.getTasksDueToday(todayEnd: Long): Flow<List<TaskEntity>>
HabitDao.getHabitsWithTodayLog(): Flow<List<HabitWithLog>>
ExpenseDao.getWeeklyTotal(weekStart: Long): Flow<Double>
PomodoroDao.getSessionsToday(dayStart: Long): Flow<Int>
```

---

### 2. Task List Screen

**Purpose:** Full task management with filtering and search.

**UI Elements:**
- `SearchBar` at top
- Filter chips row: **All | Today | Pending | Completed | High Priority**
- Lazy column of `TaskCard` items
  - Title, priority badge (color-coded chip), due date, category tag
  - Swipe-to-delete with `SwipeToDismissBox`
  - Checkbox to toggle completion
- `FloatingActionButton` → navigate to Task Detail (new task)
- Empty state view when no tasks match filter

**Room queries needed:**
```
TaskDao.getAllTasks(): Flow<List<TaskEntity>>
TaskDao.getTasksByFilter(completed: Boolean): Flow<List<TaskEntity>>
TaskDao.searchTasks(query: String): Flow<List<TaskEntity>>
```

---

### 3. Task Detail / Add Screen

**Purpose:** Create or edit a single task.

**UI Elements:**
- `OutlinedTextField` — Title (required)
- `OutlinedTextField` — Description (optional, multiline)
- **Priority selector** — segmented button row (Low / Medium / High)
- **Due date picker** — `DatePickerDialog` triggered by a button
- **Category dropdown** — `ExposedDropdownMenuBox`
- Save button (validates and inserts/updates)
- Delete button (only shown for existing tasks)

**Behavior:**
- If `taskId == -1` → new task mode
- If `taskId > 0` → load from Room, pre-fill fields, save = update

---

### 4. Habits Screen

**Purpose:** Track daily habits with streaks.

**UI Elements:**
- Header: "Today's Habits" + current date
- Habit grid or list — each item shows:
  - Emoji icon + habit name
  - Streak count badge (🔥 5-day streak)
  - Toggle button: "Done Today" / "Mark Done"
  - Grayed out if already logged for today
- "+ Add Habit" button → bottom sheet dialog
- Swipe-to-delete habit (with confirmation dialog)
- Weekly overview mini-calendar (7-day row with filled/empty circles)

**Room queries needed:**
```
HabitDao.getAllHabits(): Flow<List<HabitEntity>>
HabitDao.getLogsForHabit(habitId, weekStart): Flow<List<HabitLogEntity>>
HabitDao.isLoggedToday(habitId, todayStart, todayEnd): Flow<Boolean>
```

**Streak logic (in Repository):**
- When marking done: insert `HabitLogEntity` + update `currentStreak` and `bestStreak`
- Streak = consecutive days with a log entry
- Reset streak if yesterday has no log entry

---

### 5. Pomodoro Timer Screen

**Purpose:** Focus session timer with session history.

**UI Elements:**
- Large circular progress ring (custom `Canvas` drawing or `CircularProgressIndicator`)
- Timer display: `MM:SS` in large typography
- State label: "Focus" / "Short Break" / "Long Break"
- Control buttons: **Start | Pause | Reset | Skip**
- Session counter: "Session 2 of 4"
- `OutlinedTextField` — optional task label for this session
- **Session History** — lazy column below timer
  - Date, duration, task label
- Settings summary: "25 min focus · 5 min break" (links to Settings)

**Logic:**
- Timer runs with `CountDownTimer` or `viewModelScope.launch` loop
- On session complete → insert `PomodoroSessionEntity` to Room
- After 4 pomodoros → trigger long break

---

### 6. Expense Tracker Screen

**Purpose:** Log and review student spending.

**UI Elements:**
- **Month selector** — left/right arrow + "May 2025" label
- **Total spent card** — large number, category breakdown bar
- **Category filter chips** — All | Food | Books | Transport | Entertainment | Other
- Lazy column of `ExpenseItem` rows
  - Category icon emoji, note, formatted amount, date
  - Swipe-to-delete
- `FloatingActionButton` → Add Expense bottom sheet
  - Amount field (numeric keyboard)
  - Category dropdown
  - Note field
  - Date (defaults to today, tappable to change)
- **Weekly bar chart** (using Vico or Canvas) — spending per day

**Room queries needed:**
```
ExpenseDao.getExpensesForMonth(start: Long, end: Long): Flow<List<ExpenseEntity>>
ExpenseDao.getTotalForMonth(start: Long, end: Long): Flow<Double>
ExpenseDao.getTotalByCategory(start: Long, end: Long): Flow<List<CategoryTotal>>
```

---

### 7. Notes List Screen

**Purpose:** Quick subject-organized notes.

**UI Elements:**
- Search bar
- **Pinned Notes** section (horizontal scroll of cards) — shown only if pins exist
- **All Notes** lazy column
  - Title, subject tag, first line preview, last updated time
  - Long press → context menu (Pin / Delete)
- `FloatingActionButton` → navigate to new Note Detail
- Empty state with illustration

**Room queries needed:**
```
NoteDao.getAllNotes(): Flow<List<NoteEntity>>        // pinned first, then by updatedAt
NoteDao.searchNotes(query: String): Flow<List<NoteEntity>>
```

---

### 8. Note Detail / Edit Screen

**Purpose:** Full-screen note writing.

**UI Elements:**
- Large `BasicTextField` for title (no border, large typography)
- `OutlinedTextField` for subject tag
- Fullscreen `BasicTextField` for body content
- Pin toggle button in top bar
- Auto-save on navigate back (`LaunchedEffect` / `DisposableEffect`)
- Character count in bottom bar

**Behavior:**
- Auto-save: update Room every time user pauses typing (debounce 1 second with `delay` in coroutine)
- New note: insert on first keystroke

---

### 9. Analytics Screen

**Purpose:** Visualize productivity trends.

**UI Elements:**
- Tab row: **Tasks | Habits | Pomodoro | Expenses**

**Tasks tab:**
- Completed vs Total tasks this week (line or bar chart)
- Completion rate % card
- Category breakdown pie/donut chart

**Habits tab:**
- 30-day heatmap grid (colored squares per day)
- Best streak per habit list

**Pomodoro tab:**
- Total focus hours this week
- Sessions per day bar chart
- Daily average focus time

**Expenses tab:**
- Monthly total trend (last 6 months line chart)
- Category donut chart
- Biggest expense category badge

---

### 10. Settings Screen

**Purpose:** App-wide preferences stored in Room.

**UI Elements:**
- **Appearance section**
  - Dark mode toggle `Switch`
- **Pomodoro section**
  - Focus duration slider (15–60 min)
  - Short break slider (3–10 min)
  - Long break slider (10–30 min)
- **Data section**
  - "Clear all tasks" button (with `AlertDialog` confirmation)
  - "Clear all expenses" button (with confirmation)
- **About section**
  - App version
  - Developer name

**Persistence:** Read/write `UserSettingsEntity` (single-row Room table, id=1).

---

## Shared UI Components (Build These First)

| Component | File | Description |
|-----------|------|-------------|
| `StudyFlowTopBar` | `ui/components/StudyFlowTopBar.kt` | `CenterAlignedTopAppBar` wrapper |
| `PriorityChip` | `ui/components/PriorityChip.kt` | Color-coded chip (Low/Med/High) |
| `EmptyStateView` | `ui/components/EmptyStateView.kt` | Illustration + message + optional action button |
| `LoadingIndicator` | `ui/components/LoadingIndicator.kt` | Centered `CircularProgressIndicator` |
| `ConfirmDialog` | `ui/components/ConfirmDialog.kt` | Reusable `AlertDialog` with title/body/confirm/cancel |
| `CategoryChip` | `ui/components/CategoryChip.kt` | Tappable filter chip |
| `SectionHeader` | `ui/components/SectionHeader.kt` | Bold label + optional "See All" text button |

---

## Build Order (Recommended)

Follow this order to avoid dependency issues:

```
Phase 1 — Foundation
  1. Set up Hilt, Room, Navigation dependencies in build.gradle
  2. Create all Entity classes
  3. Create AppDatabase with all entities
  4. Create all DAO interfaces (stubs are OK here)
  5. Create DatabaseModule (Hilt)
  6. Create all Repository classes
  7. Create RepositoryModule (Hilt)
  8. Set up Theme (Color, Typography, Theme.kt)
  9. Set up Navigation (Screen.kt, NavGraph.kt skeleton)

Phase 2 — Shared Components
  10. Build all shared UI components listed above

Phase 3 — Features (one at a time, fully working)
  11. Settings screen + SettingsViewModel (simplest, establishes the pattern)
  12. Task List + Task Detail screens
  13. Dashboard screen (reads from Task + Habit + Pomodoro + Expense repos)
  14. Habits screen
  15. Pomodoro Timer screen
  16. Expense Tracker screen
  17. Notes screens
  18. Analytics screen (last — reads from all other repos)

Phase 4 — Polish
  19. Add animations (AnimatedVisibility, animateItemPlacement on LazyColumn)
  20. Add WorkManager notifications for due tasks
  21. Refine dark/light theme
  22. Add Previews for all Content composables
```

---

## Assignment Compliance Checklist

### Mandatory (Must Pass)
- [ ] Kotlin-only — zero Java files
- [ ] Zero XML layout files in `res/layout/`
- [ ] Room database with ≥ 4 entities in use
- [ ] All CRUD operations present for at least Tasks and Expenses
- [ ] ≥ 8 distinct screens
- [ ] App runs on an emulator or physical device without crashing

### Score-Boosting (Do These)
- [ ] ≥ 10 screens with meaningful content on each
- [ ] Search + filter functionality (Task List, Notes)
- [ ] Charts/data visualization (Analytics, Expense)
- [ ] Notifications via WorkManager (task deadlines)
- [ ] Dark/light theme toggle persisted in Room
- [ ] Swipe-to-delete with undo Snackbar
- [ ] Date picker for tasks and expenses
- [ ] Streak tracking with visual indicators
- [ ] Empty state views (not just blank screens)
- [ ] Loading states (not just instant renders)
- [ ] Bottom sheet dialogs (Add Expense, Add Habit)
- [ ] Animation on list items

### Prohibited (Instant Penalty)
- [ ] ❌ No XML layout files used
- [ ] ❌ No crypto features
- [ ] ❌ No external database replacing Room
- [ ] ❌ No AI/ML features (ignored in marking)