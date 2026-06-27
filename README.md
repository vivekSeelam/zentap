# Zentap

A personal Android guardian app that intercepts doom-scrolling. When you open a guarded app, a full-screen overlay fires and makes you justify why you're opening it. An AI (Guardian Mom) decides whether to let you in, redirect you to something better, or send you home.

Supports Instagram, Facebook, Reddit, YouTube, TikTok, X (Twitter), LinkedIn, Snapchat, Pinterest, and Threads — you pick which ones to guard.

---

## How it works

- You open a guarded app → Guardian Mom appears and asks for a reason
- You negotiate. Vague answers get roasted and redirected to a TED talk, a reflection question, or a physical nudge
- Specific, honest reasons get you in for a set amount of time
- When the time is up, the overlay fires again

---

## Installing on your Android phone

### Step 1 — Download the APK

Download `app-debug.apk` from the [Releases](../../releases) page.

---

### Step 2 — Turn off Play Protect

Play Protect will block the APK since it isn't from the Play Store.

1. Open the **Play Store**
2. Tap your profile picture (top right) → **Play Protect**
3. Tap the **⚙️ gear icon** → turn off **Scan apps with Play Protect**
4. Tap **Turn off** on the warning

---

### Step 3 — Install the APK

1. Open the APK file you downloaded (check your Downloads folder or the app you received it on)
2. If Android says "not allowed to install unknown apps" → tap **Settings** → enable **Allow from this source** → go back → tap **Install**

---

### Step 4 — Unlock restricted settings for Zentap

Android 13 and above blocks sideloaded apps from accessing sensitive permissions by default. You need to unlock this once before granting anything.

1. Go to **Settings → Apps → Zentap**
2. Tap the **three dots (⋮)** in the top right corner
3. Tap **Allow restricted settings**
4. Confirm

---

### Step 5 — Grant permissions inside the Zentap app

Open Zentap. You will see two setup steps:

#### Draw over other apps
1. Tap **Grant** — this opens Settings
2. Find **Zentap** in the list and turn it on
3. Go back to the Zentap app

#### Accessibility service
1. Tap **Grant** — this opens Accessibility Settings
2. Tap **Zentap Guard** → turn it on
3. Tap **Allow** on the confirmation prompt
4. Go back to the Zentap app

Once both show a green ✓, you're ready.

---

### Step 6 — Choose which apps to guard

Tap **Choose apps to guard →** at the bottom of the setup screen. Toggle on any apps you want Zentap to intercept. Instagram is on by default.

---

### Step 7 — Test it

Open one of your guarded apps. The Guardian Mom overlay should appear immediately asking for a reason.

---

## Uninstalling

Android won't let you uninstall an app with an active accessibility service. Disable it first:

1. **Settings → Accessibility → Zentap Guard → turn off**
2. **Settings → Apps → Zentap → Uninstall**

---

## Privacy

- The only data sent externally is the reason you type, which goes to Anthropic's API to generate a response
- No usage data, analytics, or telemetry is collected
