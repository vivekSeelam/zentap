# Zentap

A personal Android guardian app that intercepts doom-scrolling. When you open Instagram, a full-screen overlay fires and makes you justify why you're opening it. An AI (Guardian Mom) decides whether to let you in, redirect you to something better, or send you home.

---

## How it works

- You open Instagram → Guardian Mom appears and asks for a reason
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

Open Zentap. You will see three setup steps:

#### Draw over other apps
1. Tap **Grant** — this opens Settings
2. Find **Zentap** in the list and turn it on
3. Go back to the Zentap app

#### Accessibility service
1. Tap **Grant** — this opens Accessibility Settings
2. Tap **Zentap Guard** → turn it on
3. Tap **Allow** on the confirmation prompt
4. Go back to the Zentap app

#### Anthropic API key
Zentap uses an AI model to evaluate your reasons. You need your own free API key — it takes about 2 minutes to get one.

1. Tap **Get your API key →** — this opens the Anthropic Console in your browser
2. Create a free account at [console.anthropic.com](https://console.anthropic.com)
3. Go to **Settings → API Keys → Create Key**
4. Copy the key (it starts with `sk-ant-api03-...`)
5. Come back to Zentap, paste the key into the field, tap **Save key**

> Your key is stored only on your device. It is never shared with anyone.

---

### Step 6 — Test it

Open Instagram. The Guardian Mom overlay should appear immediately asking for a reason.

---

## Uninstalling

Android won't let you uninstall an app with an active accessibility service. Disable it first:

1. **Settings → Accessibility → Zentap Guard → turn off**
2. **Settings → Apps → Zentap → Uninstall**

---

## About the API key and cost

Each conversation with the AI costs a fraction of a cent. At roughly 20 Instagram open attempts per day, expect around **$0.03/day (~$1/month)** on your Anthropic account. Anthropic offers free credits when you sign up.

---

## Privacy

- The only data sent externally is the reason you type, which goes to Anthropic's API to generate a response
- No usage data, analytics, or telemetry is collected
- Your API key never leaves your device
