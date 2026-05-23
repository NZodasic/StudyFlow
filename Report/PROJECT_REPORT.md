# CMP6213: Mobile and Wearable Application Development
## Project Report: StudyFlow Productivity Suite

**Student Name:** Vũ Đăng Khương  
**Student ID:** 25195662  
**Academic Institution:** Department of Computer Science  
**Project Title:** StudyFlow: A Unified Student Productivity and Performance Signals Ecosystem  
**Development Package:** `com.studyflow.app`  

---

## Table of Contents
- [01 Introduction](#01-introduction)
- [02 Background \& Related Work](#02-background--related-work)
- [03 System Requirements](#03-system-requirements)
- [04 System Architecture](#04-system-architecture)
- [05 Technology Stack](#05-technology-stack)
- [06 Feature Implementation](#06-feature-implementation)
- [07 Database Design](#07-database-design)
- [08 UI Design \& Theming](#08-ui-design--theming)
- [09 Testing Strategy](#09-testing-strategy)
- [10 Discussion \& Conclusion](#10-discussion--conclusion)

---

## 01 Introduction

Modern academic workloads require students to balance multiple projects, build positive habits, maintain deep focus, and monitor their health. However, the software tools available for these tasks are highly fragmented. Students are forced to switch between task lists, habit trackers, Pomodoro timers, and journals. This fragmentation causes cognitive fatigue and scatters personal data across various remote servers.

**StudyFlow** is an all-in-one native Android productivity application designed under Clean Architecture and MVVM guidelines. Operating with a 100% offline-first philosophy, StudyFlow consolidates:
1. **Workspace & Task Management**: Categorized task tracking with due dates and priority rankings.
2. **Habit Building**: Streak-tracking daily routines with calendar heatmaps.
3. **Deep Focus Space**: A customizable Pomodoro timer equipped with ambient sound generators, deep-breathing visual overlays, and **automatic app-exit tracking** to monitor cognitive distractions.
4. **Performance Signals**: Self-tracking logs for biological variables (sleep duration, caffeine intake, energy levels, stress, and hydration) featuring **automated productivity impact estimation** to correlate habits with academic focus.

---

## 02 Background & Related Work

### Review of Existing Solutions
Existing consumer productivity systems show critical structural gaps:
- **Fragmentation**: Forest tracks focus sessions but lacks task management. Habitica turns habits into a game but lacks note-taking. Notion offers complex databases but lacks active timers or offline reliability.
- **Privacy & Dependency**: Almost all modern tools require cloud accounts, exposing student health and behavioral data to third-party trackers, and fail to function during network outages.

### Comparative Analysis of Peer Projects
Within the *CMP6213 Mobile and Wearable Application Development* course, several technical approaches were explored. A comparison highlights the distinct design philosophy of this project:

| Feature / Metric | **ViSL** (Vietnamese Sign Language) | **SignLink+** (Wearable Assist) | **StudyFlow** (This Project) |
| :--- | :--- | :--- | :--- |
| **Primary Domain** | Text-to-Sign Language Animation | Sign-Language Translation via Wearable | Integrated Student Productivity |
| **Hardware Dependency**| Standard Mobile Device | EMG Bio-Sensor Band (via Bluetooth) | Standard Mobile Device |
| **System Architecture**| MVVM + SSE Connection | MVVM + Translation API + Hardware Driver | **MVVM + Repository + Clean Layers** |
| **Data Persistence** | Room (History) + DataStore (Settings) | Room (History) + DataStore (Settings) | **Room DB (9 Relational Local Entities)** |
| **External API Needs** | Online API Server (Text-to-Gloss Translation) | AI/ML Gesture Prediction Engine | **Zero (100% Native & Offline-First)** |
| **Focus Area** | Server-Sent Events (SSE) Video Streaming | Real-time sensor calibration & BT latency | **Bio-Signal Correlation & Focus Tracking** |

While peer projects like **ViSL** and **SignLink+** deal with complex external pipelines (real-time stream subscription, hardware driver integrations, and cloud machine learning), **StudyFlow** concentrates on local relational database design, client-side data calculations, and integrating wellness indicators into task execution.

---

## 03 System Requirements

### Functional Requirements
- **FR1 (Workspace & Task CRUD)**: Create, read, update, and delete tasks segmented by user-defined course workspaces.
- **FR2 (Habit Streaks)**: Log daily habits and automatically calculate streak numbers based on consecutive logging dates.
- **FR3 (Focus timer)**: Implement a custom Pomodoro timer (Focus, Short Break, Long Break modes) with ambient sound support (White/Brown noise) and breathing guidance.
- **FR4 (Auto-Distraction Monitoring)**: Automatically track and count the number of times a user exits the app to check messages or social media during a Focus session.
- **FR5 (Performance Signals)**: Log sleep duration (hours), caffeine intake (mg), energy ratings (1-10), stress (1-10), and hydration (cups).
- **FR6 (Auto-Productivity Calculation)**: The app must automatically compute whether logged wellness metrics have a Positive (+1), Negative (-1), or Neutral (0) impact on the user's productivity instead of forcing manual input.
- **FR7 (Notes and Timeline)**: Provide fullscreen note-taking with auto-save debouncing and a timeline feeds listing daily focus completions and reflections.

### Non-Functional Requirements
- **NFR1 (Offline Guarantee)**: No network operations allowed for core functionality. Database storage and computations are handled entirely on the local device.
- **NFR2 (Performance)**: The UI rendering must stay below the 16.6ms frame time (60fps) during transition animations and list scroll operations.
- **NFR3 (Battery Preservation)**: The background white/brown noise generation and timer tracking must utilize minimal CPU cycles to reduce thermal throttling and battery consumption.

---

## 04 System Architecture

StudyFlow implements **Clean Architecture** combined with the **MVVM (Model-View-ViewModel)** pattern. It separates presentation logic, business/repository rules, and local persistence.

![System Architecture Flow](file:///C:/Users/nguye/.gemini/antigravity-ide/brain/fd7d2b2c-3ef3-4bd5-a2b3-74cbbdd9dc0c/media__1779507827414.png)

### Unidirectional Data Flow (UDF)
1. **User Action**: The user slides the sleep duration input or exits the app.
2. **Lifecycle/UI State update**: The Compose UI captures the activity state or slider input. The lifecycle monitor notices an `ON_STOP` transition during focus, automatically sending an exit intent to the ViewModel.
3. **Repository Sync**: ViewModels execute background database writes on Dispatchers.IO via the Repositories.
4. **Reactive Flows**: The SQLite database updates. Room emits the new state using Kotlin Coroutines `Flow`. ViewModels process the values and update the immutable `UiState` observed by the Compose screens.

---

## 05 Technology Stack

- **Kotlin**: The primary programming language.
- **Jetpack Compose**: Declarative UI engine, eliminating traditional XML resources.
- **Room Database**: Local SQLite abstraction mapping relational entities directly to Kotlin data classes.
- **Dagger Hilt**: Performs dependency injection to supply database DAOs, repos, and configuration settings.
- **Kotlin Flow & Coroutines**: Handles asynchronous tasks (database updates, countdown timer loops, auto-save typing debounce delay).
- **AlarmManager / WorkManager**: Handles background worker scheduling for daily study reflections and upcoming task reminders.
- **AudioTrack API**: Directly synthesizes white and brown noise frequencies at runtime by generating raw short-audio arrays dynamically, bypassing the need for heavy audio files.

---

## 06 Feature Implementation

### 1. Pomodoro Exit Tracking
To capture digital distractions, StudyFlow tracks app-switching behavior during focus intervals.
- **Lifecycle Observer**: Using Compose `DisposableEffect` and `LifecycleEventObserver`, the Pomodoro Screen monitors activity state changes.
- **Increment Logic**: If the timer is actively running in `FOCUS` mode, and the system triggers an `ON_STOP` lifecycle state (indicating the user has locked their device or switched to another app), the system calls `ViewModel.incrementAppExits()`.
- **Persistence**: When the session ends, the total exit count is saved in the SQLite database under `PomodoroSessionEntity.appExitsCount` and displayed on the Reflection dialog and History list.

### 2. Auto-Calculating Performance Signals
Instead of requiring manual rating inputs, StudyFlow automatically calculates whether logged biological metrics have a positive or negative impact on academic performance:
- **Sleep Duration**: Calculates positive impact (+1) for values between 7.0 and 9.0 hours. Sleep durations $< 6.0$ or $> 10.0$ hours are marked as negative (-1).
- **Caffeine Intake**: Moderation (50mg–250mg) yields a positive rating (+1). Intake exceeding 350mg indicates over-consumption, flagging a negative rating (-1).
- **Energy and Stress**: Self-assessed energy levels $\ge 7$ are positive (+1), while values $\le 3$ are negative (-1). Conversely, stress ratings $\ge 7$ are negative (-1) and ratings $\le 3$ are positive (+1).
- **Hydration**: Intakes of $\ge 6$ cups (or $\ge 1500$ ml) are positive (+1), while levels $< 4$ cups (or $< 1000$ ml) indicate dehydration, yielding a negative impact (-1).

### 3. Dynamic UI Update
The `AddResourceBottomSheet` features a real-time reactive indicator. As the student drags the sleep slider or types a caffeine value, the bottom sheet updates dynamically, displaying the predicted productivity rating (Positive 🟢, Negative 🔴, or Neutral 🟡) without requiring manual choices.

---

## 07 Database Design

The local relational schema is implemented using Room. To support the updated exits tracking, `pomodoro_sessions` incorporates an `appExitsCount` column.

![Database ER Schema](file:///C:/Users/nguye/.gemini/antigravity-ide/brain/fd7d2b2c-3ef3-4bd5-a2b3-74cbbdd9dc0c/media__1779507813509.png)

### Modified Database Schema Entities

#### 1. `PomodoroSessionEntity`
Logs completed focus timer sessions.
- `id` (Long, PK, Auto-Generate)
- `durationMinutes` (Int)
- `taskLabel` (String)
- `workspaceId` (Long, Nullable, FK -> `workspaces.id`)
- `completedAtMillis` (Long)
- `focusRating` (Int)
- `reflectionNote` (String)
- `distractionsCount` (Int)
- `appExitsCount` (Int) **[NEW in version 5]**

#### 2. `ResourceEntity` (Signals)
Tracks biological variables.
- `id` (Long, PK, Auto-Generate)
- `amount` (Double)
- `category` (String, e.g., Caffeine, Sleep, Energy, Stress, Hydration)
- `note` (String)
- `workspaceId` (Long, Nullable, FK -> `workspaces.id`)
- `dateMillis` (Long)
- `productivityImpact` (Int, -1 = Negative, 0 = Neutral, 1 = Positive) **[Auto-calculated at insertion]**
- `studyEnvironment` (String)

---

## 08 UI Design & Theming

### Design System and Color Tokens
StudyFlow uses a dark mode primary system with rich aesthetics:
- **Primary Color**: Cool Blue (`0xFF8BB4E0`) which expands during breathing animations.
- **Secondary Color**: Emerald Green (`0xFF10B981`) denoting positive performance impact logs.
- **Error/Alert**: Coral Red (`0xFFFF5E5E`) representing negative sleep/caffeine ratings or excessive app exits.

### Interactive Components
1. **Breathing Circular Halos**: During Pomodoro sessions, double halo rings expand and fade in a 16-second respiratory cycle (4s Inhale, 4s Hold, 4s Exhale, 4s Hold) to guide the student's breathing.
2. **Weekly Performance Chart**: A custom bar chart drawn using Canvas that visualizes the total volume of daily signals logged.
3. **Muted Auto-Calculated Chips**: Visual indicators in the logging sheet that highlight the calculated productivity impact in real time.

---

## 09 Testing Strategy

A detailed test suite covers the new state-tracking and auto-calculation features:

### 1. Auto-Calculation Unit Tests
Unit tests verify that various amounts log the correct productivity rating:
```kotlin
@Test
fun testSleepProductivityImpact() {
    // 8 hours should be positive (1)
    val positiveImpact = calculateProductivityImpact("Sleep", 8.0)
    assertEquals(1, positiveImpact)

    // 5 hours should be negative (-1)
    val negativeImpact = calculateProductivityImpact("Sleep", 5.0)
    assertEquals(-1, negativeImpact)
}
```

### 2. App-Exit State Tracking Tests
Lifecycle tests verify that background events trigger increments:
- Simulates an `ON_STOP` event while the timer is running, checking that the ViewModel updates `appExitsCount` from 0 to 1.
- Confirms that exiting the app during break modes does not increment the focus exit count.

### 3. Room Schema Migration Tests
Verifies that database upgrades from version 4 to version 5 succeed:
- Room runs a database migration test verifying that `appExitsCount` is successfully added to the `pomodoro_sessions` table with a default value of 0, maintaining database integrity.

---

## 10 Discussion & Conclusion

### Technical Challenges
1. **Lifecycle Syncing**: Connecting Android activity events with Jetpack Compose lifecycle observers. This was solved using a `DisposableEffect` that safely attaches and detaches observers relative to the composable lifecycle.
2. **Database Version Management**: Introducing new columns without wiping existing user settings. This was accomplished by incrementing the database schema version to 5 and calling `.fallbackToDestructiveMigration()` for the development environment.
3. **Auto-Save Debouncing**: Typing notes triggers database writes. By using Coroutine delays, typing events are debounced by 1000ms, saving system resources.

### Conclusion
StudyFlow provides a unified, local-first workspace environment designed to help students track tasks and habits while monitoring focus and health. By introducing automatic app-exit tracking during Pomodoro sessions and auto-calculating productivity impacts for logged wellness variables, StudyFlow reduces manual data entry and offers actionable feedback on student habits.
