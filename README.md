# ScoreCore

A small cricket scorer for international matches (T20I and ODI), built with Kotlin and Jetpack Compose.

- Score ball by ball: runs, 4s and 6s, wides, no-balls, byes, leg-byes, wickets, free hits
- Undo the last ball at any time
- Live scoreboard with run rate, target and required rate
- Ball-by-ball view grouped by over, plus an innings summary with extras and fall of wickets
- Match history stored on the device (Room)
- iOS-style design: true-black AMOLED background, solid blues, Inter typeface

Every match is stored as a list of ball events; score, overs, rates and results are all derived from that list.

## Builds

GitHub Actions builds the APK on every push to `main` (unit tests first). Grab it from the
**latest** release, or from the `ScoreCore-apk` artifact of any run. The APK is signed with the
debug key so it installs directly.

Local build: `./gradlew assembleRelease`

## Licenses

Inter font: SIL Open Font License 1.1, see `licenses/Inter-OFL.txt`.
