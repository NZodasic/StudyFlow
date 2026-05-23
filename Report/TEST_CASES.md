# StudyFlow Test Suite: Features & Test Cases

This document lists all features implemented in **StudyFlow** and provides a comprehensive test suite with step-by-step test cases for each feature to ensure stability, UX responsiveness, and correct database operations.

---

## 1. Workspace Management
### Feature Description
Allows students to create color-coded compartments (e.g., "Math 101", "Project A") to segment tasks, habits, notes, and wellness signals.

| Test Case ID | Test Objective | Preconditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-WS-01** | Create a Workspace | App is launched; user is on the Dashboard. | 1. Tap the Workspace Selector.<br>2. Select "Add Workspace".<br>3. Enter name: `"Computer Science"`, select `📁` emoji, choose Violet theme.<br>4. Tap "Save". | Workspace is created, stored in Room DB, and appears in the selector dropdown. |
| **TC-WS-02** | Filter Dashboard by Workspace | Multiple workspaces exist; some tasks/notes are mapped to CS, others to General. | 1. Tap the Workspace Selector on the Dashboard.<br>2. Select `"Computer Science"`. | Dashboard cards, today's tasks, and notes instantly filter to display only items mapped to `"Computer Science"`. |

---

## 2. Task Management
### Feature Description
A complete task planner supporting prioritization, categories, timelines, search, and swipe-to-delete with undo capabilities.

| Test Case ID | Test Objective | Preconditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-TK-01** | Add a New Task | User is on the Tasks screen. | 1. Tap the Floating Action Button (`+`).<br>2. Enter Title: `"Submit Thesis Outline"`.<br>3. Select Priority: `High`, Category: `"Research"`, set Due Date via Calendar picker.<br>4. Tap "Save". | Task is persisted in Room `tasks` table and displays at the top of the Task list with a Red High Priority badge. |
| **TC-TK-02** | Search and Filter Tasks | Multiple tasks with varying priorities and names exist. | 1. Type `"Thesis"` in the Search Bar.<br>2. Tap the "High Priority" filter chip. | Only tasks containing `"Thesis"` in the title and having a High Priority rating are shown. |
| **TC-TK-03** | Complete a Task | Task is listed on screen. | 1. Check the checkbox next to `"Submit Thesis Outline"`. | The task text is struck-through, checked, and automatically moved or updated in state. |
| **TC-TK-04** | Swipe to Delete and Undo | Task list contains at least one task. | 1. Swipe a task card to the left.<br>2. When the Red trash background appears, complete the swipe.<br>3. Tap "Undo" on the popup Snackbar. | Tapping "Undo" re-inserts the task into the database, and it reappears in the list. |

---

## 3. Habit Tracker
### Feature Description
Daily routine builder that monitors habits, maintains streaks, and manages completion history on a weekly mini-calendar.

| Test Case ID | Test Objective | Preconditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-HB-01** | Create a Habit | User is on the Habits screen. | 1. Tap "+ Add Habit".<br>2. Enter name: `"Read Research Paper"`, select `📚` emoji.<br>3. Click "Create". | Habit is created and appears in the daily checklist. The "Create Habit" button is disabled if the text field is empty. |
| **TC-HB-02** | Mark Habit Complete & Streak Counter | Habit is not logged today. Streak is currently 0. | 1. Tap the circular habit icon to check it off today. | The circle fills, yesterday and today log calculations run, and the streak increments to `🔥1`. |
| **TC-HB-03** | Dashboard Streak Badge | Habit has an active streak of 3. | 1. Navigate to the Dashboard. | The habit circular icon on the Dashboard display shows a `🔥3` badge on its top-right corner. |

---

## 4. Pomodoro Focus Space
### Feature Description
A timed focus area combining circular count-down sweeping arcs, audio white/brown noise generation, breathing overlays, and automated app-exit tracking.

| Test Case ID | Test Objective | Preconditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-PD-01** | Start/Pause Countdown | User is on the Focus Space. | 1. Tap "START".<br>2. Wait 3 seconds, then tap "PAUSE". | The circular progress starts sweeping and the timer digits countdown. Tapping "PAUSE" stops the countdown instantly. |
| **TC-PD-02** | Toggle Ambient Audio | Audio systems are functional. | 1. Tap the "Brown Noise" filter chip.<br>2. Tap "Silent". | Dynamic audio synthesis engine begins streaming brown noise (ocean wave sounds). Tapping "Silent" stops the audio track. |
| **TC-PD-03** | Auto-Track App Exits | Focus timer is running (`mode = FOCUS`). | 1. Press the home button to background the app.<br>2. Open another app.<br>3. Re-enter StudyFlow. | The background observer detects the `ON_STOP` lifecycle state transition and increments `appExitsCount` in UI state. |
| **TC-PD-04** | Save Focus Reflection | Focus timer countdown reaches `00:00`. | 1. When the reflection dialog appears, rate focus `4 Stars`.<br>2. Input accomplishment: `"Wrote introduction"`.<br>3. Verify that app-exits are displayed correctly.<br>4. Tap "Save Reflection". | Session metadata (duration, rating, distraction count, and auto-tracked app exits) is stored in Room and displayed in History Logs. |

---

## 5. Performance Signals (Resources)
### Feature Description
Self-tracking tool logging Sleep, Caffeine, Energy, Stress, and Hydration, featuring automated productivity impact calculations.

| Test Case ID | Test Objective | Preconditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-RS-01** | Log Sleep with Slider | User opens "Log Signal" sheet. | 1. Select category: `"Sleep"`.<br>2. Verify that the input text box changes to a slider.<br>3. Drag the slider to `8.0` hours.<br>4. Tap "Save". | Sleep duration is saved as 8.0 hours. The slider increment behaves in 0.5-hour steps. |
| **TC-RS-02** | Auto-Calculate Caffeine Impact | User opens "Log Signal" sheet. | 1. Select category: `"Caffeine"`.<br>2. Enter `"150"` mg.<br>3. Observe the "Productivity Impact" row. | The UI dynamically highlights "Positive" (🟢) because 150mg falls in the optimal 50mg-250mg window. Buttons are non-interactive. |
| **TC-RS-03** | Auto-Calculate Sleep Deprivation | User opens "Log Signal" sheet. | 1. Select category: `"Sleep"`.<br>2. Drag the slider to `5.0` hours. | The UI dynamically shifts the highlight to "Negative" (🔴) because 5 hours is below the 6-hour sleep threshold. |

---

## 6. Academic Note-Taking
### Feature Description
A distraction-free writing editor supporting tags, workspaces, and coroutine-debounced auto-save.

| Test Case ID | Test Objective | Preconditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-NT-01** | Create and Search Notes | User is on the Notes screen. | 1. Tap `+ FAB` to create a note.<br>2. Type title `"Biology Lab 3"` and subject `"Bio"`. | Note is listed. Typing `"Lab"` in the notes search bar filters list to display this note. |
| **TC-NT-02** | Verify Auto-Save Debounce | Note is open in the editor. | 1. Type `"The cellular respiration test..."` into the body.<br>2. Pause typing and wait 2 seconds. | After 1 second of pause, the coroutine debounce fires, updating the database record silently in the background. |

---

## 7. Timeline & Analytics
### Feature Description
Visual feed of past items and analytics showing task completion charts and daily productivity patterns.

| Test Case ID | Test Objective | Preconditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-AN-01** | Display Timeline | Actions (focus sessions, habits checked, signals logged) exist today. | 1. Navigate to the Timeline screen. | A clean chronological list shows all completed actions for today, ordered by time. |
| **TC-AN-02** | Render Analytics Charts | Productivity data is stored. | 1. Navigate to Analytics. | Line charts and bar graphs (e.g., focus hours this week, signal counts) render correctly without crashes. |

---

## 8. Settings & Security
### Feature Description
Preferences manager enabling theme selections, Pomodoro customizations, and safe database wipes.

| Test Case ID | Test Objective | Preconditions | Test Steps | Expected Result |
| :--- | :--- | :--- | :--- | :--- |
| **TC-ST-01** | Persist Dark Theme Toggle | User is in Settings. | 1. Toggle "Dark Mode" switch to active. | App theme changes instantly to dark Navy colors, and this preference persists after app restarts. |
| **TC-ST-02** | Custom Pomodoro Durations | User is in Settings. | 1. Adjust Focus slider to `30` min, Break to `8` min.<br>2. Return to the Pomodoro screen. | The Focus countdown duration updates to `30:00` and the Break duration updates to `08:00`. |
| **TC-ST-03** | Danger Zone Confirmation | User is in Settings. | 1. Tap "Clear all tasks".<br>2. Type `"WRONG"` in the safety input field, tap confirm.<br>3. Tap "Clear all tasks" again.<br>4. Type `"DELETE"` and tap confirm. | - Typing `"WRONG"` blocks deletion.<br>- Typing `"DELETE"` successfully clears all tasks from Room and closes the dialog. |
