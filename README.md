# SmartFit

## Description

SmartFit is a native Android personal workout tracker built for the Mobile Systems coursework project. Users can log workouts, review their workout history, and visualize live motion data captured from the device's accelerometer and gyroscope sensors. The app is written entirely in Kotlin using traditional Android Views/XML (no Jetpack Compose) and stores all data locally on the device — there is no backend, database server, or external authentication layer.

The UI follows a modern dark theme (deep obsidian surfaces with a lime accent and clean typography), implemented natively with custom XML drawables, color state lists, and two custom `View` components — built entirely using AndroidX and Material Components.
## Features

- Dashboard with a live-computed weekly goal gauge, workout streak tracker, and week-over-week performance deltas — all derived from real saved workout data, nothing hardcoded
- Add a workout with a scrollable activity-type chip selector, a +/- duration stepper with quick presets, a 5-level RPE intensity scale, a completed toggle, and notes with a live character counter
- redesigned workout card list" to "custom workout card list
- Live accelerometer (X/Y/Z) and gyroscope (X/Y/Z) readings with per-axis intensity bars
- A live-computed sensor sample rate (Hz), movement magnitude (in g), and idle/peak tracking, with pause/resume streaming and a recalibrate action
- A custom-drawn, real-time movement graph rendered with Canvas/Paint/Path
- A custom radial gauge view (`GaugeArcView`) used for both the weekly goal ring and the live sensor magnitude dial
- A persistent bottom navigation bar shared across all four screens
- Local persistence via SharedPreferences + JSON — workouts survive an app restart

## Design

- **Palette & typography**: Dark surfaces (`colors.xml`) with high-contrast lime and cyan accents; typography using `Outfit` and `Space Grotesk` bundled under `res/font/`.
- **Components**: Reusable card/pill/badge drawables (`res/drawable/bg_card_container*.xml`, `bg_pill_*`, `bg_badge*`), Material `Chip`s styled via `Chip.Pill`, and `ColorStateList`s in `res/color/` for chips, switches, inputs, and nav items.
- **Custom Views**: `MovementGraphView` (live line/area chart) and `GaugeArcView` (a Canvas-drawn arc gauge with a gradient stroke and glowing indicator, used on the dashboard and sensor screen).
- Custom vector launcher icon and brand mark (`ic_logo_mark.xml`, `ic_launcher_foreground/background.xml`).
## Technologies

- Kotlin
- Android Views / XML layouts (no Jetpack Compose)
- AndroidX AppCompat & Material Components (Chip, BottomNavigationView, SwitchMaterial)
- View Binding
- RecyclerView
- Activity Result API / Intents
- SensorManager (TYPE_ACCELEROMETER, TYPE_GYROSCOPE)
- SharedPreferences + org.json for local persistence
- `java.time` for week/month/streak date math (native on API 26+, no desugaring needed)
- Canvas, Paint, Path, Shader (custom views)

## Activities

### MainActivity
The app's dashboard and entry point. It computes and displays: a live streak subtitle, a weekly-goal `GaugeArcView` with real progress/percentage/remaining-minutes text, a Monday–Sunday streak strip, week-over-week performance deltas, the most recent workout, and live sensor availability — all derived from `WorkoutRepository`. It launches `WorkoutActivity` via the Activity Result API, passing a default workout type through the `Intent`. When a workout is returned, it is saved through `WorkoutRepository` and the dashboard is refreshed. It also hosts the shared bottom navigation bar.

### WorkoutActivity
Used to create a new workout. It reads the default workout type sent by the launching Activity and pre-selects the matching `Chip` in a horizontally scrollable `ChipGroup`. The user sets duration via a +/- stepper (with quick presets and a gradient progress bar), a 5-level RPE intensity scale (a segmented bar, mapped to the 1–5 `rating` field), a completed `SwitchMaterial`, and optional notes with a live char counter. On save, the completed workout data (type, duration, rating, completed flag, notes, timestamp) is returned to the calling Activity as an activity result.

### HistoryActivity
Displays every saved workout in a `RecyclerView` backed by `WorkoutAdapter` and a custom `RecyclerView.ViewHolder`, plus a monthly summary card (logged time, completed sessions, goal pacing) and dynamic filter chips built from the workout types actually present in the data. Workouts are sorted newest-first. An empty-state view is shown instead of the list when there are no saved workouts, and the screen does not crash with zero items.

### SensorActivity
Registers listeners for `Sensor.TYPE_ACCELEROMETER` and `Sensor.TYPE_GYROSCOPE` using `SensorManager`, following the `onResume()`/`onPause()` lifecycle so listeners are always cleanly registered and unregistered (plus a manual Pause/Resume Stream toggle on top of that). It displays a live-computed sample rate (Hz), a movement-magnitude `GaugeArcView` (in g, with idle/peak tracking and a Recalibrate action), live X/Y/Z values with per-axis bars for both sensors, and feeds the accelerometer magnitude into `MovementGraphView`. If a sensor is unavailable on the device, a fallback message is shown instead of crashing.

### MovementGraphView (Custom View — required)
A hand-rolled `View` subclass that overrides `onDraw()` and draws a live line/area chart using `Canvas`, `Paint`, and `Path` — no charting library is used. It keeps a bounded buffer of the most recent readings (older values are dropped once the limit is reached) and calls `invalidate()` whenever a new value arrives so the graph redraws in real time.

### GaugeArcView (Custom View — additional)
A second `View` subclass used for the dashboard's weekly-goal ring and the sensor screen's magnitude dial. Draws a background track and a gradient progress arc with `Canvas`/`Paint`/`LinearGradient`, plus a glowing indicator dot via `Paint.setShadowLayer`.

## University Requirements Implemented

| Requirement | Implementation |
|---|---|
| Native Android app in Kotlin | Entire app written in Kotlin, no Compose, traditional Views/XML |
| API 34+ support | `compileSdk`/`targetSdk` = 34, `minSdk` = 26 |
| At least 3 Activities | 4 Activities: `MainActivity`, `WorkoutActivity`, `HistoryActivity`, `SensorActivity` |
| Transfer data between Activities | Default workout type sent to `WorkoutActivity` via `Intent` extras |
| Return data to the previous Activity | `WorkoutActivity` returns the completed workout via the Activity Result API |
| RecyclerView | `HistoryActivity` uses a `RecyclerView` + `WorkoutAdapter` + `WorkoutViewHolder` |
| CustomView | `MovementGraphView` (required) and `GaugeArcView` (additional), both extend `View` and override `onDraw()` |
| Accelerometer | `SensorActivity` registers and reads `Sensor.TYPE_ACCELEROMETER` |
| Gyroscope | `SensorActivity` registers and reads `Sensor.TYPE_GYROSCOPE` |
| Local persistence | `WorkoutRepository` stores workouts as JSON in `SharedPreferences`; data survives app restarts |

## How to Run

1. Open Android Studio and choose **Open**, then select the `SmartFit` project folder.
2. Let Gradle sync complete (the project uses its own Gradle wrapper, so no manual Gradle install is required).
3. Select an Android emulator or physical device running Android 8.0 (API 26) or later.
4. Click **Run ▶** to build and launch the app.

Alternatively, from the command line:

```bash
./gradlew clean assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Project Structure

```text
app/src/main/
├── java/com/example/smartfit/
│   ├── MainActivity.kt        # Dashboard
│   ├── WorkoutActivity.kt     # Add workout + return result
│   ├── HistoryActivity.kt     # RecyclerView workout history
│   ├── SensorActivity.kt      # Accelerometer + gyroscope + custom views
│   ├── NavUtils.kt            # Shared bottom-navigation wiring
│   ├── model/Workout.kt, WorkoutTypeIcons.kt
│   ├── adapter/WorkoutAdapter.kt
│   ├── storage/WorkoutRepository.kt   # SharedPreferences + JSON persistence
│   │   └── WorkoutStats.kt            # Streak/week/month date-math helpers
│   └── view/MovementGraphView.kt, GaugeArcView.kt
└── res/
    ├── layout/     # activity_main, activity_workout, activity_history, activity_sensor, item_workout, ...
    ├── values/     # colors.xml, strings.xml, themes.xml, dimens.xml
    ├── color/      # chip/switch/input/nav ColorStateLists
    ├── font/       # Outfit + Space Grotesk static instances
    ├── drawable/   # card/pill/badge shapes, launcher icon, Material Symbols icons
    └── menu/       # bottom_nav_menu.xml
```

