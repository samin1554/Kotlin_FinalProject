 Student Pomodoro App

A native Android Pomodoro timer with task management, streak tracking, and customizable work/break durations — built in Kotlin as a final mobile app development project.


📋 Table of Contents

Overview
Features
Architecture
Project Structure
Tech Stack
Setup & Installation
How It Works
Screens
Data Persistence
Known Limitations


Overview
The Student Pomodoro App implements the Pomodoro Technique: 25-minute focused work intervals separated by 5-minute breaks, cycling through 4 sessions before a long break. It helps students stay productive by combining a timer, a task list, and motivational streak tracking.
Built with Kotlin, Jetpack MVVM, Room, and DataStore, it follows modern Android architecture best practices.

Features
FeatureDescription⏱️ Pomodoro TimerCountdown timer with circular progress indicator▶️ Start / Pause / ResetFull timer control with state management🔁 Session CyclingAutomatically alternates between Focus Time and Break Time across 4 sessions🔥 Streak TrackingTracks current and longest streak of completed Pomodoro sessions✅ Task ManagerAdd, view, delete tasks; each task tracks its Pomodoro count💾 Persistent TasksTasks saved to Room (SQLite) — survive app restarts🎛️ SettingsAdjustable work duration (5–60 min) and break duration (1–30 min) via SeekBar🎨 Material Design 3Cards, circular progress, themed buttons, light/dark mode support

Architecture
This app follows the MVVM (Model-View-ViewModel) pattern — Google's recommended Android architecture.
┌────────────────────────────────────────────────────┐
│                     VIEW LAYER                     │
│  TimeFragment  │  TaskFragment  │ SettingsFragment  │
│                  (XML layouts)                     │
└──────────────────────┬─────────────────────────────┘
                       │  observes LiveData
                       │  calls methods
┌──────────────────────▼─────────────────────────────┐
│                  VIEWMODEL LAYER                   │
│     TimeViewModel          TaskViewModel           │
│  (timer logic, streak)   (task CRUD bridge)        │
└───────────────┬──────────────────┬─────────────────┘
                │                  │
┌───────────────▼──────┐  ┌────────▼────────────────┐
│     STREAK (Model)   │  │     TASK (Model)         │
│     StreakManager    │  │  TaskRepository          │
│     (DataStore)      │  │  TaskDao + AppDatabase   │
│                      │  │  (Room / SQLite)         │
└──────────────────────┘  └─────────────────────────┘
Key Principles

Fragment → ViewModel: Fragments only call ViewModel methods, never touch data directly
ViewModel → Repository: ViewModels delegate to Repository; never hold database references
LiveData: All UI state flows reactively through LiveData observers
Coroutines: All database and DataStore operations run off the main thread via viewModelScope


Project Structure
app/src/main/
├── AndroidManifest.xml
└── java/com/example/student_pomodoro/
    ├── MainActivity.kt              # Host activity + navigation setup
    ├── data.kt                      # Task entity, DAO, AppDatabase
    ├── TaskRepository.kt            # Repository abstraction layer
    ├── StreakManager.kt             # DataStore streak read/write
    └── ui/
        ├── timer/
        │   ├── TimeFragment.kt      # Timer UI (View)
        │   ├── TimeViewModel.kt     # Timer logic + streak integration
        │   └── TimeState.kt        # Enum: IDLE | RUNNING | PAUSED
        ├── tasks/
        │   ├── TaskFragment.kt      # Task list UI
        │   ├── TaskViewModel.kt     # Task state bridge
        │   └── TaskAdapter.kt      # RecyclerView ListAdapter + DiffUtil
        └── settings/
            └── SettingsFragment.kt  # Work/break duration controls

Tech Stack
LayerLibrary / ToolVersionLanguageKotlinLatest stableArchitectureMVVM + Jetpack-NavigationJetpack Navigation Component-DatabaseRoom (SQLite ORM)2.6.1PreferencesDataStore Preferences1.1.1Reactive DataLiveData + Kotlin Flow2.8.7AsyncKotlin Coroutines-UIAndroid Views (XML) + Material Design 3-BuildGradle Kotlin DSL (.kts)-Min SDKAPI 24 (Android 7.0+)-Target SDK36-

Setup & Installation
Prerequisites

Android Studio Hedgehog or newer
JDK 11+
Android device or emulator with API 24+

Steps

Clone the repository

bash   git clone https://github.com/samin1554/Kotlin_FinalProject.git
   cd Kotlin_FinalProject

Open in Android Studio

File → Open → select the cloned folder


Sync Gradle

Android Studio will prompt to sync; click "Sync Now"


Run the app

Select a device/emulator
Press ▶️ Run (or Shift + F10)



No API keys or external services required — the app is fully self-contained.

How It Works
Timer Logic (TimeViewModel.kt)
The countdown uses a Handler + self-scheduling Runnable on the main thread:
kotlinrunnable = object : Runnable {
    override fun run() {
        val current = _timeLeft.value ?: 0
        if (current > 0) {
            _timeLeft.postValue(current - 1)
            handler.postDelayed(this, 1000)
        } else {
            onTimerFinished()
        }
    }
}
handler.postDelayed(runnable!!, 1000)
Each tick decrements the time by 1 second and reschedules itself. When the timer hits zero, onTimerFinished() handles the work→break cycle transition.
Session Cycling
Work (25 min) → Break (5 min) → Work → Break → Work → Break → Work → Break
     Session 1          →      Session 2     →     Session 3    →    Session 4
                    (restarts from Session 1 after 4)

Completing a work session increments the streak
Resetting mid-session resets the current streak to 0

Streak Tracking (StreakManager.kt)
Streaks are stored in DataStore Preferences (key-value, persisted to disk):
kotlinsuspend fun incrementStreak() {
    context.dataStore.edit { preferences ->
        val current = (preferences[CURRENT_STREAK] ?: 0) + 1
        preferences[CURRENT_STREAK] = current
        if (current > (preferences[LONGEST_STREAK] ?: 0)) {
            preferences[LONGEST_STREAK] = current
        }
    }
}
Task Persistence (data.kt + TaskRepository.kt)
Tasks are stored in a Room database (task_database). The DAO returns LiveData<List<Task>> so the UI automatically updates whenever tasks change:
kotlin@Query("SELECT * FROM tasks ORDER BY id DESC")
fun getAllTasks(): LiveData<List<Task>>

Screens
🕐 Timer Screen

Circular progress indicator (counts down from 100% → 0%)
Large bold countdown (MM:SS)
Session label: "Session X of 4" and "Focus Time" / "Break Time"
Longest streak display
Start/Pause and Reset buttons

✅ Tasks Screen

Input field + Add button to create new tasks
RecyclerView list of task cards, each showing:

Task title
Number of Pomodoros completed
Checkbox (UI only — DB persistence not wired yet)
Delete button



⚙️ Settings Screen

Work Duration SeekBar (5–60 minutes)
Break Duration SeekBar (1–30 minutes)
Live label updates as the user drags


Data Persistence
DataStorageWhyTask listRoom / SQLiteStructured relational data; reactive LiveData queriesStreak countersDataStore PreferencesSimple key-value integers; lighter than Room
Both storage layers use Kotlin Coroutines for all write operations, keeping the main thread free.

Known Limitations

Settings ↔ Timer not connected: SeekBar values in Settings are stored locally in the Fragment and do not update the timer duration. TimeViewModel.updateDuration() exists and is ready — the inter-Fragment communication just needs to be wired (e.g., via a shared ViewModel or DataStore).
Task completion not persisted: The isCompleted checkbox state is rendered but not written back to the Room database when toggled.
Settings not in nav graph: SettingsFragment exists but is not registered as a navigation destination, so it cannot be reached via the bottom nav.
No background notifications: The timer does not post a notification when it finishes if the app is in the background.


License
This project was created for educational purposes as a final course project.
