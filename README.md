# Impostor — Android Party Deduction Game

Standalone offline Android app built with Kotlin & Jetpack Compose.  
**Zero Firebase, zero `google-services.json`, zero `.env` secrets, and zero account login required.**

## Game Highlights
- **Pass & Play Party Mode**: Supports 2 to unlimited players on one phone with Privacy Shield reveal cards and private secret voting.
- **5 Distinct Roles with Custom Glowing Emblems & Colors**:
  - **Agent** (Emerald Crest)
  - **Impostor** (Crimson Phantom Visor)
  - **Agent Killer** (Scarlet Assassin Crosshair — voids 1 player's vote)
  - **Savior Agent** (Cyan Aegis Shield — protects 1 player from Agent Killer for +1 bonus)
  - **Agent Director** (Gold Sovereign Crown — 2x vote weight, no wrong-vote deduction)
- **Circular Countdown Timer + Expiration Broadcast**: Animated 60fps progress dial that broadcasts turn-expired alerts (`TurnTimerEventBus` + local Android broadcast).
- **Custom Words & Category Naming**: Add custom secret words, genres, and 1-word hints, rename your custom category, or mix All + Custom words.
- **Built-in Sound Cues**: Synthesized audio chimes for role reveals, timer expiration, and round verdicts (with mute toggle).

---

## Option 1: Install Directly on Your Phone via GitHub Releases (No PC Needed)
1. Push this repository to GitHub (`.github/workflows/build-apk.yml` automatically builds on every push to `main`/`master`).
2. Open your repo on GitHub from your phone -> go to **Releases** -> **Impostor Latest Build**.
3. Download **`Impostor-debug.apk`** and tap it to install (allow *Install unknown apps* if prompted).

---

## Option 2: Clone with Git & Build/Install Locally

### Prerequisites
- **JDK 17**
- **Android SDK** (with `ANDROID_HOME` or `ANDROID_SDK_ROOT` set)

### Linux / macOS / Termux
```bash
git clone <your-repo-url>
cd Impostor
./install-apk.sh
```

### Windows (CMD / PowerShell)
```bat
git clone <your-repo-url>
cd Impostor
install-apk.bat
```

### Manual Gradle Commands
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
The compiled APK is saved at:
`app/build/outputs/apk/debug/app-debug.apk`
