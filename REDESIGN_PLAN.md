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
- AndroidX Fragment Compose `1.9.1` hosts the existing fragment controllers inside Foundation HorizontalPager with selected-page lifecycle and saved-state support.
- `sh.calvin.reorderable:reorderable:3.1.0` provides lazy-list drag handles and edge autoscroll for existing custom alarm ordering, replacing app-owned drag bookkeeping as screens migrate.
- Current build uses stable Compose BOM `2026.09.00` (UI and runtime `1.12.1`) with Material 3 explicitly pinned to the approved `1.5.0-beta02`. Its AAR requires compile SDK 37.0. Minimum SDK 26 and target SDK 36 retain their existing values.

- AndroidX release notes dated November 18 list `androidx.compose.material3:material3:1.6.0-alpha01` as the newest alpha. Override the Material 3 version explicitly alongside the documented Compose BOM `2026.11.01`, which documents Compose UI 1.13.0. Verify resolved Material 3 and runtime versions together with `dependencyInsight` before adoption. Google Maven currently returns HTTP 404 for both the announced BOM and Material 3 alpha. The visible Maven metadata ends at alpha BOM 2026.11.00 and Material 3 1.5.0-rc01. Keep the target pin pending publication verification. [Release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.6.0-alpha01) · [BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping)
- Use the Compose compiler plugin aligned with the catalog's Kotlin 2.4.21. Retain current application ID, signing, flavors and target/minimum SDKs. Identify any compile SDK requirement from AAR metadata once resolution works.
- Verified in the announced release's AndroidX API surface under `androidx.compose.material3`: `MaterialExpressiveTheme(colorScheme, motionScheme, shapes, typography, content)`, `MotionScheme.expressive()`, `MotionScheme.fastSpatialSpec<T>()`, `MotionScheme.defaultSpatialSpec<T>()`, `ShortNavigationBar(arrangement, content)`, `ShortNavigationBarItem(selected, onClick, icon, label, ...)`, `HorizontalFloatingToolbar(expanded, ..., content)`, `SplitButtonLayout(leadingButton, trailingButton, ...)`, `CircularWavyProgressIndicator(progress = { ... })` and `rememberTimePickerState(initialHour, initialMinute, is24Hour)` with `TimePicker(state)` and `TimeInput(state)`. Use indeterminate wavy progress only for genuinely unknown progress. Verify each assigned overload and experimental annotation from release source. [AndroidX API at the release commit](https://android.googlesource.com/platform/frameworks/support/+/7d68a49549322900fdd694b32f5eed2c74fc17fe/compose/material3/material3/api/current.txt)

## Migration boundaries and verification

The app starts with Activity and Fragment View bindings, ViewPager tab navigation, RecyclerView adapters and XML dialogs. The UI reads persisted alarms, Room timers, time zones and app settings. Existing activities, fragments and callbacks invoke controllers, services and persistence. Workers keep those owners and contracts while moving rendering to Compose. Preserve fragment/host disposal, lifecycle resubscription, stable list keys, preference IDs, intent extras and results, scheduling, notification actions, import/export formats and widget configuration IDs. The migration introduces no storage writes or schema upgrades. Revert feature commits in reverse order to restore prior rendering against the same stored data.

Stage host interoperability first using lifecycle-owned Compose views, then migrate individual dialogs and screens. Keep the previous route available until its replacement works through every entry point. Remove unused resources only after the final caller is replaced. The launcher-hosted analogue and digital widget views and system-owned notification, picker, permissions and splash surfaces use Android platform contracts. Modernize app-controlled UI around those boundaries and preserve the platform integration.

Before each dispatch, check the requests directory and clean worktree. Mark one task in progress and give a fresh Luna worker exact files, verified API signatures, design specs, callbacks and commands. Review its diff, run `./gradlew :app:assembleFossDebug :app:testFossDebugUnitTest` with the local SDK and JDK, and check the changed path on device when visual or lifecycle behavior changes. Use a fresh worker for each correction. Commit the verified feature, then immediately commit the plan update with its hash. At the end build both flavors, run unit tests and lint, and exercise cold starts, rotation, process restoration, dark mode, large fonts, RTL, accessibility and legacy stored data.

## Checklist of Atomic Tasks

- [x] Compose foundation — catalog, root/module Gradle, and new `ui/theme/ClockTheme.kt`. Pin approved Material 3 1.5.0-beta02 and verify Compose and Views build together.
- [ ] Latest-alpha upgrade — adopt Material 3 1.6.0-alpha01 and Compose BOM 2026.11.01 when Google Maven serves both, then verify resolved dependencies and component behavior.
- [x] Main shell — `MainActivity.kt`, `activity_main.xml`, and `ViewPagerAdapter.kt`. Keep page swipe, starting tab, extras, shortcuts, reselection, tab-specific sort menu and sound results. Use compact `ShortNavigationBar` with an inset background and selection indicator, preserving tab labels. Keep Info and Settings reachable.
- [x] Alarm time picker — `EditAlarmDialog.kt` and a new Compose time picker host. Replace both existing theme-dependent picker paths, preserving initialized hour/minute, clock format, validation and cancel contract.
- [x] Alarm editor — `EditAlarmDialog.kt`. Lead with selected time and group weekday toggles, label, sound, vibration and reminder options; retain add/edit/delete and scheduling callbacks.
- [x] Alarm and timer sort dialogs — migrate both consumers of the same tonal radio-list dialog, keeping each preference key, default, option order and confirm-only write.
- [x] Alarm list — `AlarmFragment.kt` and `AlarmsAdapter.kt`. Group alarm time and recurrence in tonal rows with accessible switches, keep selection, add, edit, bulk actions and permission flows.
- [x] Zone picker — `AddTimeZonesDialog.kt` and `SelectTimeZonesAdapter.kt`. Preserve ordering, checkbox selection and confirmation with a scrollable Compose list and stable keys.
- [x] Zone editor — `EditTimeZoneDialog.kt`. Preserve title edits, dismissal, and clearing a title to restore its default.
- [x] World clock — `ClockFragment.kt`, `TimeZonesAdapter.kt`, and clock readout views. Feature the local time and next alarm, with date, UTC offsets and zone times in supporting tonal rows. Preserve tick cadence, 12/24-hour format and zone ordering.
- [x] Stopwatch — `StopwatchFragment.kt` and `StopwatchAdapter.kt`. Pair a high-contrast time readout with expressive Start/Pause and Lap/Reset controls. Preserve timing, service binding, lap deltas, ordering and landscape access.
- [x] Timer duration picker — `MyTimePickerDialogDialog.kt` and its layout. Preserve hours 0–23 and minutes/seconds 0–59, initial selection and confirm-only callback. Support localized numeric input and touch adjustment.
- [x] Timer editor — `EditTimerDialog.kt`. Group duration picker, label and sound in a scrollable form with existing input ranges and save semantics.
- [x] Timers — `TimerFragment.kt` and `TimerAdapter.kt`. Use a determinate wavy indicator for the active timer, with non-motion status and accessible controls. Preserve event updates, start/pause/reset, sorting, edit/delete and permission handling.
- [x] Compose pager host — replace the temporary RtlViewPager and shell XML with Foundation HorizontalPager and lifecycle-aware AndroidFragment hosts for the now-Compose tab content. Preserve RTL swipes, fragment controllers, selected-page persistence, deep links, shortcuts and cross-tab callbacks.
- [x] Ringing alarm — `AlarmActivity.kt` and `activity_alarm.xml`. Feature the alarm label and time, with spring-return drag feedback and TalkBack-accessible snooze/dismiss actions. Preserve lock-screen flags, thresholds, haptics and exactly-once dispatch.
- [x] Snooze reminder — `SnoozeReminderActivity.kt` and shared duration dialog call sites in AlarmActivity and SettingsActivity. Use grouped duration choices with the original range and timing.
- [x] Alarm intent selector — `SelectAlarmDialog.kt` and `IntentHandlerActivity.kt` UI call site. Preserve dismiss alarm matching, selection and finish behavior.
- [x] Export form — `ExportDataDialog.kt`. Preserve default path, filename validation, scopes and completion contract.
- [x] Typography preference parity — feed the existing Fossify system, monospace, and imported-font choices into both standard and emphasized Compose typography without changing font storage or Commons customization.
- [x] Settings — `SettingsActivity.kt` and `activity_settings.xml`. Use visually grouped appearance, clock, reminders, widget and data rows with existing resources, preference values and conditional availability.
- [x] Digital widget configuration — `WidgetDigitalConfigureActivity.kt` and its app-owned configuration layout. Pair a live preview with color and opacity controls, preserving widget result protocol and formatting.
- [x] Analogue widget configuration — `WidgetAnalogueConfigureActivity.kt` and its app-owned configuration layout. Match the digital config hierarchy and preserve its analogue face and preference semantics.
- [x] Alarm and timer sound chooser — migrate the app call sites to a Compose chooser while reusing Fossify sound lookup and preserving preview, custom-file permission/result, recent sounds and deletion callbacks.
- [x] Permission and first-alarm prompts — use Expressive app-owned message surfaces for permission explanations and the one-time alarm confirmation, preserving Android permission and Settings actions.
- [x] Alarm switch preference — apply the existing Fossify checked-thumb icon preference to the Compose Alarm list as well as Settings.
- [x] Shared Fossify UI audit and follow-through — audit dependency-owned About, appearance customization, and color picker boundaries. Preserve suite-wide functionality and document any retained upstream View content.
- [x] Timer reset interaction guard — keep controls from submitting Pause against a stale Running row while a user-initiated Reset is awaiting its state refresh.
- [x] Final legacy cleanup and parity — remove orphaned app-owned layouts, adapters, binding and UI dependencies after caller inventory. Smoke test launcher, shortcuts, intents, notifications, widget host, all pages and settings with existing persisted data.
- [x] Final Compose lint corrections — guard Android 13 language settings, observe configuration locale in the timer duration picker, and simplify alarm time picker constraint measurement.

## Platform and Fossify Commons surfaces

- The launcher hosts the analogue and digital clock widgets through Android `RemoteViews`. Their platform clock views supply live time in the launcher. Both app-controlled configuration activities use Compose. Android also owns runtime permission prompts, the document picker, and the launch splash.
- Commons 7.1.3 already uses Compose in its About, FAQ, and license activities. Clock preserves those suite destinations and their navigation contracts. [About](https://github.com/FossifyOrg/Commons/blob/7.1.3/commons/src/main/kotlin/org/fossify/commons/activities/AboutActivity.kt) · [FAQ](https://github.com/FossifyOrg/Commons/blob/7.1.3/commons/src/main/kotlin/org/fossify/commons/activities/FAQActivity.kt) · [Licenses](https://github.com/FossifyOrg/Commons/blob/7.1.3/commons/src/main/kotlin/org/fossify/commons/activities/LicenseActivity.kt)
- Commons still owns the View-based Customize appearance activity and its cross-app theme, font, and icon synchronization. The widget color-picker component uses a Compose dialog around Commons' internal View-based HSV editor. These upstream surfaces retain their existing implementations so Clock continues to use the suite's shared preferences and behavior. [Customize appearance](https://github.com/FossifyOrg/Commons/blob/7.1.3/commons/src/main/kotlin/org/fossify/commons/activities/CustomizationActivity.kt) · [Widget color picker](https://github.com/FossifyOrg/Commons/blob/7.1.3/commons/src/main/kotlin/org/fossify/commons/dialogs/ColorPickerDialog.kt)

## Changelog

- Plan commit `e61cbd17` — design system, migration sequence and acceptance criteria.
- Baseline `4f2b697d` — initial branch audit and plan. Compose foundation awaits publication verification.
- Baseline verification — `:app:assembleFossDebug` passed with the Android Studio JDK and SDK Platform 36. `:app:testFossDebugUnitTest` reported `NO-SOURCE`. Luna independently reproduced the latest BOM and Material 3 HTTP 404 responses. Foundation remains pending. No device visual verification has been performed.
- Foundation `f5d4e368` — added Compose alongside View Binding and implemented the shared Expressive theme. Foss debug assembled, AAR metadata validation passed and unit tests reported `NO-SOURCE`. Dependency insight confirmed the Material 3 beta and Compose runtime pins.
- Main shell `9c9a2214` — Compose header and four-tab navigation. Foss debug assembled and emulator checks passed for tab selection, swipes, timer intent and sort visibility. Unit test task reported `NO-SOURCE`.
- Alarm time picker `46095fc4` — verified dial, keypad, confirmation, cancellation, narrow portrait and landscape IME on emulator. Build passed. Unit tests reported `NO-SOURCE`.
- Alarm editor `e747be3e` — verified edit/new, time picker, weekday and vibration, label and IME, ordinary sound selection, cancellation, and save through first-run permission on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Sort dialogs `afb09bcb` — verified options, persistence on confirm, all dismissal paths, and compact large-font scrolling on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Alarm list `2d87c0c4` — verified add, edit, recurring/dayless toggles, scheduling, contextual selection/delete and persisted drag ordering on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`. Existing Clock next-alarm text can remain stale after bulk delete until its next refresh.
- Zone picker `12444db0` — verified catalog order, multi-select, long-list scrolling, confirmation persistence and all cancellation paths on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Zone editor `7075c769` — verified initial focus, keyboard, trimmed names, default reset and cancellation on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- World Clock `3d9e360c` — verified ticking, 12/24 format, next alarm, zone offsets and dates, add/rename/multi-delete, cancellation, and 320 dp large-font reflow on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Stopwatch `bb16bf33` — verified elapsed display, lap recording and sorts, scroll reset, start/pause/resume/reset, orientation, and shortcut routes on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Timer duration picker `3134ca55` — verified wheel and keypad, 23:59:59 and zero boundaries, existing ten-second fallback, all dismissal paths, and compact IME actions. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Timer editor `6bd02782` — verified edit/new, duration confirmation and cancel, labels and keyboard, sound and vibration, saved defaults and ACTION_SET_TIMER through the running state. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Timers `7d909121` — verified countdown states, permissions, multi-select/delete, custom drag order, sorting and warm/cold timer jumps on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`. Rapid Reset then Pause exposed a stale-control interaction, addressed by the later reset guard.
- Compose pager `0539dec0` — verified tab swipes in LTR/RTL, rotation, process restoration with stable controller identities, cold/warm shortcuts and jumps, sort/menu routes and audio result forwarding on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Ringing alarm `8131debc` — verified actual alarm wakeup, partial swipe spring return, guide fade, swipe and accessible actions, rotation, notification dismissal and explicit snooze intents on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Snooze and reminders `45b6df98` — verified preset/current selection, custom units and bounds, cancellation and exactly one snooze through both reminder entry points on emulator. Custom Cancel returns to presets. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Alarm intent selector `7114cb21` — verified multiple matches through ACTION_DISMISS_ALARM, distinguishable blank/duplicate labels, each no-selection dismissal, and only the selected alarm dismissed. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Export form `4ba14147` — verified name generation and validation, duplicate detection, default and configured path handling, cancellation, and real SAF export with the existing backup schema. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Typography `2d1e3861` — verified system, monospace, imported font via Files, live resume refresh and missing-font fallback on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Settings `277c3c7e` — verified toggles, immediate selectors, reminder units, locale and customization routes, file pickers, 200% font scale, and Google Play Thank You visibility/route. Both flavors assembled. Unit tests reported `NO-SOURCE`.
- Digital widget `3dffa371` — verified Settings and launcher entry, real widget ID and placement, live minute/date, 12/24 format, opacity/colors, confirm and cancel. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Analogue widget `e6d53a69` — verified a live Canvas minute change, launcher placement with valid widget ID, color and opacity persistence, invalid ID handling, and cancellation. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Sound chooser `e92490e8` — verified system/custom alarm-stream previews, release on exit, actual file picker results, persisted grants, cancellation, and custom sound deletion and fallback on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Permission and alarm prompts `333d720e` — verified first-use warning confirmation/cancel through list and editor, and denied notification permission actions through the timer intent on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Switch appearance `3d35190d` — verified Alarm and Settings checked-thumb preference refresh in live activities, accessibility nodes and unchanged toggle writes. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Timer reset guard `6edb309f` — verified immediate Reset/Pause/duplicate Reset, recovery to Idle, one-shot removal and valid Start/Pause/Resume actions on emulator. Foss debug assembled. Unit tests reported `NO-SOURCE`.
- Legacy cleanup `2c87c0f2` — removed unused app-owned views and layout dependencies. Both debug APKs assembled, unit-test tasks were NO-SOURCE, and final device smoke preserved the existing database fixture. Final lint found three presentation diagnostics requiring correction.
- Final lint corrections `df1ed62a` — explicitly gated the app-language route, keyed duration formatting to configuration locale, and used measured alarm-picker width. Both debug builds and both lints passed against the existing lint baseline. Both unit-test tasks reported `NO-SOURCE`. API 37 smoke checks covered language and duration routes and portrait/compact/wide landscape picker layouts. The final fixture database checksums matched.

## Expressive visual pass

The user found the Compose migration visually improved but too close to standard Material 3. The first pass reused similarly rounded cards and controls across tabs. Give each tab a recognizable layout and make motion respond to meaningful state changes. Preserve current callbacks, timing, navigation, preferences and accessibility. Verify before/after imagery and interaction on the emulator.

- [x] Stopwatch — an asymmetric, oversized timing face with a connected control cluster. Running changes the silhouette and tonal role, and the lap table reads as a distinct timing history.
- [x] World Clock — a large local time composition with asymmetric nested shapes, distinct time/date hierarchy, and supporting next-alarm and zone regions.
- [x] Alarm — emphasize the next occurrence and make enabled, disabled and selected alarms visually distinct. Preserve reorder, switch and contextual actions.
- [x] Timer — distinguish running countdowns from idle presets through placement, shape and determinate progress. Keep timer ordering and controls intact.
- [x] Navigation shell — emphasize the current tab and put its utility actions in an Expressive floating toolbar.
- [x] Ringing alarm — carry the alarm ticket and connected shapes into the full-screen interruption.
- [x] Editors — carry the time and duration visual hierarchy and connected actions into the alarm and timer forms.
- [x] Settings — build stronger section hierarchy and use distinct nested shapes across preference groups.

- Stopwatch visual pass `1e99f7a8` — verified stopped, running and paused states, connected actions, lap/sort/reset and compact landscape on API 37. Foss debug and lint passed.
- World Clock visual pass `f33afe20` — verified light/dark hierarchy, RTL, compact large type, time semantics and zone operations on API 37. Foss debug and lint passed.
- Alarm visual pass `d228cd55` — verified next-occurrence hierarchy, switch and add/edit actions, drag persistence, and compact 200% font layout on API 37. Foss debug and lint passed.
- Timer visual pass `4c4d99cf` — verified running/paused/idle, persistent reorder, compact large type, and offscreen timer link on API 37. Foss debug and lint passed.
- Navigation visual pass `987a800b` — verified contextual floating toolbar, sort and menu, all tabs and swipes, Arabic RTL and 200% compact layout. Foss debug and lint passed.
- Ringing visual pass `7a453b66` — verified MaterialShapes Cookie9Sided-to-circle motion, connected actions, spring return and compact type on API 37. Foss debug and lint passed.
- Editor visual pass `9faf2d5f` — verified both initialized picker routes, cancellation, and pinned connected actions with large type and the landscape keyboard. Foss debug and lint passed.
- Settings visual pass `310ecabb` — verified both customization routes, preferences and choices, and 320 dp at 200% type in light and dark on API 37. Foss debug and lint passed.

## Alarm density, appearance and launcher widgets

The latest screenshot shows a large upcoming-alarm banner above a much smaller second card. Match card density and switch alignment across alarms. Give every enabled alarm the existing upcoming color treatment, with a restrained next-occurrence marker. Increase floating action buttons where space allows.

- [x] Replace the Fossify appearance routes with Clock-owned System, Dark and Light modes and a System palette or curated colors. Remove the Settings widget customization route. Investigate the reported crash and verify changes on device.
- [x] Balance Alarm cards and increase floating action button sizes while preserving compact layouts and callbacks.
- [x] Redesign the hosted digital and analogue widgets with original, expressive shapes inspired by Google Clock. Preserve launcher time and widget compatibility.

The user supplied two additional visual references. The first pairs a large violet dial with sparse, aligned world-clock rows and a coral add action. The second uses equal-size alarm tiles, tall numerals and clear coral enabled versus muted violet disabled states. Use these as direction for strong time typography, saturated active colors and quiet inactive states. Keep Clock's own layout and assets. Include a plum and coral appearance option so the reference palette is available without overriding wallpaper colors for everyone.

- Appearance `c3299ca0` — verified three modes, five palettes including Plum, persistence, system bar contrast, and Settings-only widget shortcut removal. Existing legacy crash was not reproduced on API 37.
- Alarm density and floating actions `f3f92336` — verified equal card sizes, matching enabled emphasis, next-marker changes, 72 dp add actions and compact large-type layouts. Foss debug and lint passed.

Launcher widget implementation. Use an original asymmetric digital capsule with live platform TextClock digits and a separate date/next-alarm region. Give the analogue widget an original scalloped face and prominent hands, informed by the reference dial. Keep RemoteViews, provider identities, resize behavior, live time and app-open intent. Use Clock's palette resolver for defaults, preserve saved launcher color choices, and keep widget customization out of Settings. Use Android widget previews and confirm placement and resizing in a launcher. Implementation dispatched to GPT-6-Luna in a dedicated Codex worker thread.

- Launcher widgets `5b021e6b` — implemented and reviewed using dedicated GPT-6-Luna workers. Both debug builds and lint passed. API 37 verified digital placement and saving without overriding automatic colors. Analogue placement and resizing were not exercised in this final smoke. Older launchers retain their existing static preview images.

## System-only launcher widgets

- [x] Remove launcher widget configuration and ignore app appearance and saved widget color overrides. Use Android system light/dark and wallpaper colors, preserving live time, orientation and resize behavior, provider IDs, tap-to-open, date and next alarm.
- Use Google Clock as the reference for layout, typography and how the widgets sit on the launcher. Use original artwork and a distinct composition. Check digital and analogue placement from the real launcher, including that Add opens no configuration screen.

- System-only widgets `59ca2382` — verified on the API 37 launcher that both variants add directly, show live time and respond to system light/dark, and that tapping the analogue widget opens the app. The analogue hands stay inside the tick ring. Both debug builds and lint passed against the existing baseline. Wallpaper changes, resizing and the older-platform fallback were not exercised end to end.

### Launcher system palette follow-up

- [x] Correct the reported system-color mismatch. Verify both existing widgets respond to a distinct Android palette change with Clock closed, preserving system light/dark and the existing widget layout and behavior.

- Widget palette `10adb101` — both widgets switched between blue and magenta wallpaper palettes with Clock closed on API 37 and followed system light/dark. Both debug builds and lints passed. Older Android color fallbacks were not exercised on a device.
