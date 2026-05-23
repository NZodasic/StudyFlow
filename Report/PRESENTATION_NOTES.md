# StudyFlow Presentation Script & Slide Structure

This document provides a slide-by-slide structure and speaking notes for your presentation of **StudyFlow**. Place the specified screenshots and diagrams on each slide, and read the corresponding **Speaker Script** during your presentation.

---

## Slide 1: Title & Introduction
* **Slide Title**: StudyFlow: A Unified Student Productivity Ecosystem
* **Visuals to place**: 
  * StudyFlow Logo / Brand Header.
  * Your details: **Vũ Đăng Khương (Student ID: 25195662)**.
  * Course Name: **CMP6213: Mobile and Wearable Application Development**.
* **Speaker Script**:
  > *"Good morning, esteemed professors and fellow students. Today, I am proud to present my project: **StudyFlow**—a unified Android application designed to aggregate student productivity tracking and performance signals into a single, offline-first native ecosystem. In this presentation, I will cover our core features, architecture layout, and design choices."*

---

## Slide 2: Problem Statement & Motivation
* **Slide Title**: The Problem: Tool Fragmentation & Data Privacy
* **Visuals to place**: 
  * Left: Icons of fragmented apps (e.g. Forest + Notion + Habitica).
  * Right: Bullet points outlining:
    * Cognitive fatigue from switching apps.
    * Privacy risks with personal health/biological logs on cloud databases.
    * Weak offline availability on existing productivity systems.
* **Speaker Script**:
  > *"As students, we manage heavy workloads. However, the productivity tools we use are fragmented—forcing us to juggle task planners, habit trackers, and focus timers. This results in cognitive overload and scatters our data across multiple cloud servers, risking our privacy. StudyFlow resolves this by consolidating everything locally on-device, offering a private, offline-first workspace where productivity and wellness indicators finally converge."*

---

## Slide 3: StudyFlow Feature Suite
* **Slide Title**: Core Features: A Complete Student Toolkit
* **Visuals to place**: 
  * Screenshot of the **Dashboard Screen** (showing stats row, today's tasks, active habits with fire badges).
  * Checkmarks pointing to:
    * Workspace Compartments (Math, Computer Science, etc.).
    * Active Habits Checklist.
    * Task Planner with Swipe Deletion.
    * Full Screen Notes with debounced Auto-saving.
* **Speaker Script**:
  > *"StudyFlow offers a complete, integrated toolkit. The entry point is a personalized Dashboard that filters all tasks, habits, and notes according to the selected course Workspace. Users can track tasks by priority, build habits with streak triggers directly from the main feed, view upcoming due dates, and open subject-specific notebooks that save automatically in the background as they write."*

---

## Slide 4: Deep Focus Space & Distraction Tracker
* **Slide Title**: Deep Focus: Breathing Halos & Exit Tracking
* **Visuals to place**: 
  * Left: Screenshot of the **Active Pomodoro Timer** showing the circular sweep progress and the breathing pulse circles.
  * Right: Screenshot of the **Reflection Dialog** displaying the **App Exits (Auto-tracked)** count.
* **Speaker Script**:
  > *"One of our core features is the Deep Focus Space. It integrates a custom Pomodoro countdown timer with breathing visual halos that expand and contract on a 16-second cycle to guide user focus. We also synthesize White and Brown noise natively at runtime using the AudioTrack API. Most importantly, to capture digital distractions, StudyFlow automatically tracks app-exits—incrementing a counter whenever the user backgrounds the app during a focus interval."*

---

## Slide 5: Performance Signals & Auto-Calculated Impact
* **Slide Title**: Performance Signals: Biological Correlation
* **Visuals to place**: 
  * Left: Screenshot of the **Add Signal Sheet** with the Sleep slider and the highlighted Positive/Negative impact chips.
  * Right: Screenshot of the **Weekly Activity Bar Chart** drawn using Canvas.
* **Speaker Script**:
  > *"StudyFlow correlates wellness with productivity. In the Performance Signals screen, students can log sleep duration, caffeine intake, energy, stress, and hydration. Instead of manual input, our system automatically calculates the productivity impact of each log in real-time. For instance, sleep between 7 to 9 hours is automatically flagged as positive, while caffeine over-consumption is flagged as negative, updating color-coded indicator chips dynamically."*

---

## Slide 6: System Architecture (UDF Loop)
* **Slide Title**: System Architecture: Unidirectional Data Flow (MVI)
* **Visuals to place**: 
  * **[System Architecture Diagram](file:///C:/Users/nguye/.gemini/antigravity-ide/brain/fd7d2b2c-3ef3-4bd5-a2b3-74cbbdd9dc0c/media__1779507827414.png)** (UDF Loop showing UI $\leftrightarrow$ VM $\leftrightarrow$ Repo $\leftrightarrow$ SQLite).
* **Speaker Script**:
  > *"StudyFlow implements Clean Architecture with Model-View-Intent, or MVI. As seen in the diagram, data flows in a strict loop: user actions emit Intents to the ViewModel, which updates the local Room database via Repositories. Once Room persists the data, it triggers a reactive Flow emission back to the ViewModel, which updates the immutable UI State and recomposes the screen safely."*

---

## Slide 7: Database Design
* **Slide Title**: Relational Database Schema (Room)
* **Visuals to place**: 
  * **[Database ER Schema Diagram](file:///C:/Users/nguye/.gemini/antigravity-ide/brain/fd7d2b2c-3ef3-4bd5-a2b3-74cbbdd9dc0c/media__1779507813509.png)** (Hierarchical tables layout).
* **Speaker Script**:
  > *"Our local relational schema runs entirely on Room SQLite, using 9 entities. Workspaces act as the primary structural root, grouping tasks, notes, habits, resources, and focus sessions using one-to-many foreign keys. Standalone daily reflections and user settings exist outside this flow, ensuring settings persist globally while reflections track daily mood changes independent of course divisions."*

---

## Slide 8: Development Technology Stack
* **Slide Title**: Core Android Technologies
* **Visuals to place**: 
  * Icons or list of tech stack components:
    * **Kotlin & Coroutines**: Handles async tasks & typing debounces.
    * **Jetpack Compose**: 100% declarative UI (zero XML layouts).
    * **Dagger Hilt**: Dynamic dependency injection.
    * **AlarmManager / WorkManager**: Local background task reminder notifications.
* **Speaker Script**:
  > *"The technology stack is 100% Kotlin and uses Jetpack Compose for declarative layout design. There are no XML layouts in the project. We manage background threads using Coroutine dispatchers, inject repositories via Dagger Hilt, and schedule local reminders using Android's AlarmManager, ensuring notifications fire even if the application is closed."*

---

## Slide 9: Testing & Safety Strategies
* **Slide Title**: Testing and Data Security
* **Visuals to place**: 
  * Left: Bullet points of test tiers (In-memory Room tests, state flow tests, alarm checks).
  * Right: Screenshot of the **Danger Zone confirmation dialog** (requiring `"DELETE"` input).
* **Speaker Script**:
  > *"We enforce strict testing and data security. Unit tests run on in-memory Room SQLite instances to verify relational CRUD operations, while ViewModel tests check state transitions. For user safety, we implemented a validation gate in Settings: to clear databases or delete tasks, the user must explicitly type the confirmation phrase 'DELETE' inside an alert dialog, blocking accidental taps."*

---

## Slide 10: Future Enhancements & Conclusion
* **Slide Title**: Future Work & Final Takeaways
* **Visuals to place**: 
  * Icons or points outlining future features:
    * Biometric API syncing (Health Connect).
    * PDF/Markdown exports.
    * Real-time split-screen Markdown previews.
* **Speaker Script**:
  > *"In the future, we plan to sync biological data directly from wearable devices via Android Health Connect, enable Split-screen Markdown previews, and support exporting notes into PDF documents. In conclusion, StudyFlow provides a robust, responsive, and private workspace built on standard Clean Architecture principles. Thank you, and I am open to any questions."*
