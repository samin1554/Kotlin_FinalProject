# Student Pomodoro

A beautiful, feature-rich Pomodoro timer app built with Kotlin and Material 3. Designed for students who want to stay focused, track productivity, and build consistent study habits.

## Features

### Core Timer
- **Pomodoro Cycles** — Customizable work, short break, and long break durations
- **Session Tracking** — Automatically cycles through focus and break sessions
- **Infinite Focus Mode** — Long-press the timer card to start an open-ended flow-state session for deep work
- **Background Reliability** — Timer persists across process death via DataStore and uses AlarmManager for exact completion alarms
- **Keep Screen On** — Display stays awake during active sessions

### Task Management
- Create, edit, and delete tasks
- Tap a task to set it as the active timer task
- Track completed pomodoros per task
- Checkbox completion with confirmation dialogs

### Statistics & Charts
- **Bar Chart** — Last 7 days of focus sessions with goal-highlighted colors
- **Pie Chart** — Session type distribution (Focus / Break / Long Break)
- **Hourly Productivity** — Heatmap-style horizontal bar chart showing your most productive hours
- **Lifetime Stats** — Total pomodoros, focus hours, and best day ever
- **Recent Sessions** — Scrollable log of completed sessions

### Motivation & Gamification
- **Daily Streaks** — Current and longest streak tracking
- **Daily Goal** — Set a target (1–20 pomodoros) and watch your progress
- **Confetti Celebration** — Animated particle burst when you hit your daily goal
- **Stats Card Animations** — Staggered entrance animations for a premium feel

### Customization
- Fully adjustable timer durations (1–60 min work, 1–30 min break, 5–45 min long break)
- Sessions before long break (2–8)
- Sound and vibration toggles
- Auto-start next session option
- Light & dark theme support (Material 3 dynamic theming)

### Widgets & Quick Actions
- **Home Screen Widget** — See live timer countdown and control start/pause/skip without opening the app
- **Quick Settings Tile** — Start the timer directly from your notification shade
- **App Icon Badge** — Today's pomodoro count shown on your launcher icon

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0+ |
| UI | XML Layouts + ViewBinding |
| Architecture | MVVM with LiveData |
| Navigation | Jetpack Navigation Component + BottomNavigationView |
| Database | Room 2.8.4 |
| Preferences | DataStore 1.1.1 |
| Charts | MPAndroidChart 3.1.0 |
| Design | Material 3 (Material You) |
| Minimum SDK | 24 (Android 7.0) |
| Target SDK | 36 (Android 16) |

## Architecture

```
app/
├── data/                 # Room entities, DAOs, TypeConverters
├── repository/           # SessionRepository, TaskRepository
├── ui/
│   ├── timer/           # TimeFragment, TimeViewModel, TimeState
│   ├── tasks/           # TaskFragment, TaskViewModel, TaskAdapter
│   ├── stats/           # StatsFragment, StatsViewModel, SessionAdapter
│   └── settings/        # SettingsFragment, SettingsDataStore
├── notifications/       # NotificationHelper, TimerNotificationHelper
├── widget/              # TimerWidgetProvider
└── MainActivity.kt
```

## Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17

### Build & Run
```bash
git clone https://github.com/samin1554/Kotlin_FinalProject.git
cd Kotlin_FinalProject
./gradlew :app:assembleDebug
```

Or open in Android Studio and click **Run**.

## Permissions
- `POST_NOTIFICATIONS` — Timer completion and badge notifications
- `SCHEDULE_EXACT_ALARM` — Reliable background timer completion
- `VIBRATE` — Haptic feedback on timer complete and button presses
- `WAKE_LOCK` — Keep device awake during sessions

## Key Implementation Details

### Timer Accuracy
Uses `CountDownTimer` for in-app ticking and `AlarmManager` with `RTC_WAKEUP` for background completion. Timer state is serialized to DataStore on every tick so it survives process death.

### Notifications
- **Ongoing Notification** — Shows live countdown with progress while timer runs
- **Completion Notification** — Sound, vibration, and content intent to reopen app
- **Badge Notification** — Low-priority status notification for launcher icon badge count

### Widget Updates
The widget reads from `TimerPreferences` DataStore and updates via broadcast from `TimeViewModel` every 5 seconds while running, ensuring the home screen always shows approximate remaining time.

## License

This project is for educational purposes.

---

Built with ☕ and 🍅 for focused students everywhere.
