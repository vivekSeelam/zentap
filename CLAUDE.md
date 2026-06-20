# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project purpose

**Zentap** is a personal Android "guardian" app that intercepts doom-scrolling. When the user opens a guarded app (currently hardcoded to Instagram), a full-screen overlay fires that forces a moment of reflection before granting timed access. The app is sideloaded on a physical device — Play Store compliance is explicitly not a goal.

The build is being developed phase by phase:
- **Phase 0 (done):** AccessibilityService detects Instagram, SYSTEM_ALERT_WINDOW overlay fires with a Close button.
- **Phase 1 (done):** Overlay has "Grant 5 minutes" (starts a re-block timer) and "Not now" (sends user home via `GLOBAL_ACTION_HOME`). After 5 minutes, if user is still in Instagram, overlay fires again automatically.
- **Phase 2 (next):** Anthropic API integration — AI chat on the overlay asks why the user is opening the app, returns structured JSON (`{ decision, minutes, message }`), and either grants timed access or redirects to a breathing/reflection screen.

## Build commands

All commands run from the project root (`zentap/`).

```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Install to connected device (USB debugging must be on)
.\gradlew.bat installDebug

# Run unit tests (no device needed)
.\gradlew.bat test

# Run instrumented tests (device/emulator required)
.\gradlew.bat connectedAndroidTest

# Run a single unit test class
.\gradlew.bat test --tests "com.example.zentap.ExampleUnitTest"
```

After adding new source files, always **File → Sync Project with Gradle Files** in Android Studio before building — otherwise the IDE shows false "Unresolved reference" errors.

## Architecture

### The intercept loop

```
Instagram foreground
  → AccessibilityEvent TYPE_WINDOW_STATE_CHANGED
  → GuardAccessibilityService.onAccessibilityEvent()
      → updates lastKnownForegroundPackage
      → if Instagram + no active session → showOverlay()
  → OverlayManager.show() → WindowManager.addView(overlay_block.xml)
  → User taps "Grant 5 min"
      → hide overlay + sessionGrantedUntilMs = now+5min + postDelayed(reblockRunnable, 5min)
  → 5 minutes later: reblockRunnable fires
      → if lastKnownForegroundPackage == Instagram → showOverlay() again
  → User taps "Not now"
      → hide overlay + performGlobalAction(GLOBAL_ACTION_HOME)
```

### Key classes

**`GuardAccessibilityService`** — the heart of the app. Extends `AccessibilityService`. Receives ALL `TYPE_WINDOW_STATE_CHANGED` events (no `packageNames` filter — needed to track when the user leaves Instagram). Maintains `lastKnownForegroundPackage` (ignores `com.android.systemui`, `android`, and own package). Owns the `Handler`-based re-block timer. `OverlayManager` is initialized in `onServiceConnected()`, not at field declaration, because the service context isn't fully ready until that callback.

**`OverlayManager`** — stateless helper (holds one `View?`). Called from the service (main thread). Uses `WindowManager.addView()` with `TYPE_APPLICATION_OVERLAY` (API 26+) or `TYPE_PHONE` (API 24–25). Wraps the service context in `ContextThemeWrapper(context, R.style.Theme_Zentap)` before inflating — required so `Button` inflates correctly outside an Activity. `show()` takes `onGrant` and `onNotNow` callbacks; the service owns all timer and navigation logic.

**`MainActivity`** — purely a permission-check screen. Holds `canDrawOverlays` and `accessibilityEnabled` as `mutableStateOf` class-level properties (not inside a `@Composable`) so `onResume()` can update them and trigger recomposition automatically when the user returns from Settings.

### UI split: Compose vs XML

`MainActivity` uses **Jetpack Compose** (Material3). The overlay (`overlay_block.xml`) is **XML Views** — `ComposeView` requires a `LifecycleOwner` that isn't available in a `WindowManager`-attached view without extra wiring. Do not attempt to use Compose for the overlay without setting up `ViewTreeLifecycleOwner` and `ViewTreeSavedStateRegistryOwner`.

### Permissions required (both must be granted manually by the user)

| Permission | How granted | What it enables |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Settings → Apps → zentap → Display over other apps | `WindowManager.addView()` with overlay type |
| `BIND_ACCESSIBILITY_SERVICE` | Settings → Accessibility → Zentap Guard | Receiving `AccessibilityEvent`s |

`SYSTEM_ALERT_WINDOW` is not a runtime permission — `requestPermissions()` does nothing for it. Always check `Settings.canDrawOverlays(context)`.

### Accessibility service config

`res/xml/accessibility_service_config.xml` scopes events to `com.instagram.android` only (`android:packageNames`). When adding more guarded apps in Phase 1+, either add comma-separated packages there or remove the attribute to listen to all packages (battery impact is negligible for this use case).

### Adding a new guarded app

1. Add the package name to `android:packageNames` in `accessibility_service_config.xml`
2. Check `event.packageName` in `GuardAccessibilityService.onAccessibilityEvent()` against the new package
3. The session window (`sessionAllowedUntilMs`) is currently a single scalar — Phase 1 will need it to become a `Map<String, Long>` keyed by package name

## Gotchas

- **Manufacturer ROM battery optimization** (Xiaomi MIUI, Samsung One UI) aggressively kills accessibility services after reboot. Users must set the app to "No restrictions" / "Unrestricted" battery mode, or the service silently stops.
- **`TYPE_WINDOW_STATE_CHANGED` fires for every Instagram-internal navigation** (stories, DMs, reels). The `sessionAllowedUntilMs` guard is what prevents the overlay from flickering mid-session. Do not remove it without a replacement.
- **Back button does not dismiss the overlay** — it's a `WindowManager` view, not an Activity, so the system Back gesture never reaches it. Any Back-button handling must go through `AccessibilityEvent.TYPE_VIEW_CLICKED` on the Back key.
- **The overlay is drawn by the Service thread (main thread)**. `WindowManager.addView()` and `removeView()` must always be called on the main thread. Since `onAccessibilityEvent` is already on the main thread, this is satisfied automatically.