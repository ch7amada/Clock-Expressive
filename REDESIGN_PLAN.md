# Clock Material 3 Expressive redesign

## User Requests Summary

Initial audit on 2026-11-19 found no root `user_requests/` directory. Recheck before dispatching each task. Record every new note and describe the placement, density, shape and interaction details in every applicable visual reference before assigning work.

## Visual Direction & Aesthetic System

- Use a neutral `surface` canvas, `surfaceContainerLow` for list sections and `surfaceContainerHigh` for grouped controls. Use `primaryContainer` for the main readout or action region and `tertiaryContainer` for secondary time or lap highlights. Pair containers with their corresponding content roles. Use wallpaper-derived schemes on API 31+ when appropriate, daylight and dark fallbacks on older devices, and preserve explicit Fossify color choices and widget colors.
- Put time values first in the hierarchy with tabular numerals and `displayLargeEmphasized` where space permits. Use `headlineSmallEmphasized` for page titles, `titleMedium` for list labels and `bodyMedium` for supporting text. Scale down or reflow time values on narrow screens and at large font scales. Preserve locale, 12/24-hour format, RTL mirroring and TalkBack labels.
- Start with 24 dp screen gutters and 16 dp section gaps, compacting gutters to 16 dp when needed. Use 28 dp focal cards and 20 dp supporting groups, 12 dp inner corners where surfaces nest, and pill-shaped time controls. Keep touch targets at least 48 dp.
- Use `MotionScheme.expressive()` for spatial and effect transitions, with fast spatial motion for direct manipulation and default spatial motion for larger layout changes. Respect system animation duration, keep timing accuracy independent of UI motion, and avoid animating every tick or announcing every digit change.
- Give each surface a clear focal point and keep contextual actions near their content. Preserve swipe and selection where currently available. In landscape, let readouts and their associated actions or lists sit alongside each other when width permits.

## Dependency Versions & Design Token Mappings

- AndroidX release notes dated November 18 list `androidx.compose.material3:material3:1.6.0-alpha01` as the newest alpha. Override the Material 3 version explicitly with the published Compose BOM `2026.11.01`, which documents Compose UI 1.13.0. Verify resolved Material 3 and runtime versions together with `dependencyInsight` before adoption. Google Maven currently returns HTTP 404 for both the announced BOM and Material 3 alpha. The visible Maven metadata ends at alpha BOM 2026.11.00 and Material 3 1.5.0-rc01. Keep the target pin pending publication verification. [Release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.6.0-alpha01) · [BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping)
- Use the Compose compiler plugin aligned with the catalog's Kotlin 2.4.21. Retain current application ID, signing, flavors and target/minimum SDKs. Identify any compile SDK requirement from AAR metadata once resolution works.
- Verified in the announced release's AndroidX API surface under `androidx.compose.material3`: `MaterialExpressiveTheme(colorScheme, motionScheme, shapes, typography, content)`, `MotionScheme.expressive()`, `MotionScheme.fastSpatialSpec<T>()`, `MotionScheme.defaultSpatialSpec<T>()`, `ShortNavigationBar(arrangement, content)`, `ShortNavigationBarItem(selected, onClick, icon, label, ...)`, `HorizontalFloatingToolbar(expanded, ..., content)`, `SplitButtonLayout(leadingButton, trailingButton, ...)`, `CircularWavyProgressIndicator(progress = { ... })` and `rememberTimePickerState(initialHour, initialMinute, is24Hour)` with `TimePicker(state)` and `TimeInput(state)`. Use indeterminate wavy progress only for genuinely unknown progress. Verify each assigned overload and experimental annotation from release source. [AndroidX API at the release commit](https://android.googlesource.com/platform/frameworks/support/+/7d68a49549322900fdd694b32f5eed2c74fc17fe/compose/material3/material3/api/current.txt)

## Migration boundaries and verification

The app starts with Activity and Fragment View bindings, ViewPager tab navigation, RecyclerView adapters and XML dialogs. The UI reads persisted alarms, Room timers, time zones and app settings. Existing activities, fragments and callbacks invoke controllers, services and persistence. Workers keep those owners and contracts while moving rendering to Compose. Preserve fragment/host disposal, lifecycle resubscription, stable list keys, preference IDs, intent extras and results, scheduling, notification actions, import/export formats and widget configuration IDs. The migration introduces no storage writes or schema upgrades. Revert feature commits in reverse order to restore prior rendering against the same stored data.

Stage host interoperability first using lifecycle-owned Compose views, then migrate individual dialogs and screens. Keep the previous route available until its replacement works through every entry point. Remove unused resources only after the final caller is replaced. The launcher-hosted analogue and digital widget views and system-owned notification, picker, permissions and splash surfaces use Android platform contracts. Modernize app-controlled UI around those boundaries and preserve the platform integration.

Before each dispatch, check the requests directory and clean worktree. Mark one task in progress and give a fresh Luna worker exact files, verified API signatures, design specs, callbacks and commands. Review its diff, run `./gradlew :app:assembleFossDebug :app:testFossDebugUnitTest` with the local SDK and JDK, and check the changed path on device when visual or lifecycle behavior changes. Use a fresh worker for each correction. Commit the verified feature, then immediately commit the plan update with its hash. At the end build both flavors, run unit tests and lint, and exercise cold starts, rotation, process restoration, dark mode, large fonts, RTL, accessibility and legacy stored data.

## Checklist of Atomic Tasks

- [ ] Compose foundation — catalog, root/module Gradle, and new `ui/theme/ClockTheme.kt`. Resolve latest prerelease and verify Compose and Views build together.
- [ ] Main shell — `MainActivity.kt`, `activity_main.xml`, and `ViewPagerAdapter.kt`. Keep page swipe, starting tab, extras, shortcuts, reselection, tab-specific sort menu and sound results. Use compact `ShortNavigationBar` with an inset background and selection indicator, preserving tab labels. Keep Info and Settings reachable.
- [ ] Alarm time picker — `MyTimePickerDialogDialog.kt`. Use the verified analog and keyboard pickers with existing initialized time, clock format, validation and cancel contract.
- [ ] Alarm editor — `EditAlarmDialog.kt`. Lead with selected time and group weekday toggles, label, sound, vibration and reminder options; retain add/edit/delete and scheduling callbacks.
- [ ] Alarm sort — `ChangeAlarmSortDialog.kt`. Use tonal radio rows for existing sort choices and direction without changing persisted sort values.
- [ ] Alarm list — `AlarmFragment.kt` and `AlarmsAdapter.kt`. Group alarm time and recurrence in tonal rows with accessible switches, keep selection, add, edit, bulk actions and permission flows.
- [ ] Zone picker — `AddTimeZonesDialog.kt` and `SelectTimeZonesAdapter.kt`. Preserve search, selection and confirmation with a scrollable Compose list and stable keys.
- [ ] Zone editor — `EditTimeZoneDialog.kt`. Preserve label edits, cancellation and deletion behavior.
- [ ] World clock — `ClockFragment.kt`, `TimeZonesAdapter.kt`, and clock readout views. Feature the local time and next alarm, with date, UTC offsets and zone times in supporting tonal rows. Preserve tick cadence, 12/24-hour format and zone ordering.
- [ ] Stopwatch — `StopwatchFragment.kt` and `StopwatchAdapter.kt`. Pair a high-contrast time readout with expressive Start/Pause and Lap/Reset controls. Preserve timing, service binding, lap deltas, ordering and landscape access.
- [ ] Timer editor — `EditTimerDialog.kt`. Group duration picker, label and sound in a scrollable form with existing input ranges and save semantics.
- [ ] Timer sort — `ChangeTimerSortDialog.kt`. Retain sort keys and commit timing.
- [ ] Timers — `TimerFragment.kt` and `TimerAdapter.kt`. Use a determinate wavy indicator for the active timer, with non-motion status and accessible controls. Preserve event updates, start/pause/reset, sorting, edit/delete and permission handling.
- [ ] Ringing alarm — `AlarmActivity.kt` and `activity_alarm.xml`. Feature the alarm label and time, with spring-return drag feedback and TalkBack-accessible snooze/dismiss actions. Preserve lock-screen flags, thresholds, haptics and exactly-once dispatch.
- [ ] Snooze reminder — `SnoozeReminderActivity.kt` and its duration dialog call sites. Use grouped duration choices with the original range and timing.
- [ ] Alarm intent selector — `SelectAlarmDialog.kt` and `IntentHandlerActivity.kt` UI call site. Preserve dismiss alarm matching, selection and finish behavior.
- [ ] Export form — `ExportDataDialog.kt`. Preserve default path, filename validation, scopes and completion contract.
- [ ] Settings — `SettingsActivity.kt` and `activity_settings.xml`. Use visually grouped appearance, clock, reminders, widget and data rows with existing resources, preference values and conditional availability.
- [ ] Digital widget configuration — `WidgetDigitalConfigureActivity.kt` and its app-owned configuration layout. Pair a live preview with color and opacity controls, preserving widget result protocol and formatting.
- [ ] Analogue widget configuration — `WidgetAnalogueConfigureActivity.kt` and its app-owned configuration layout. Match the digital config hierarchy and preserve its analogue face and preference semantics.
- [ ] Shared Fossify UI audit and follow-through — identify dependency-owned About, color customization, sound selection, confirmation and permission routes. Split into one worker per remaining app-controlled flow, preserve upstream contracts and verify intent results.
- [ ] Final legacy cleanup and parity — remove orphaned app-owned layouts, adapters, binding and UI dependencies after caller inventory. Smoke test launcher, shortcuts, intents, notifications, widget host, all pages and settings with existing persisted data.

## Changelog

- Baseline `4f2b697d` — initial branch audit and plan. Compose foundation awaits publication verification.
