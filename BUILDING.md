# Building Chaos Faction 2 on Windows

```powershell
git clone --recurse-submodules https://github.com/Frostie77/Chaos-Faction-2.git
```

From the repository, run `powershell -NoProfile -ExecutionPolicy Bypass -File .\build-test.ps1`. Requires JDK 17 or newer and Android SDK 35. The helper locates Android Studio's SDK, retrieves verified Gradle 8.11.1 and Ruffle 0.7.1 when missing, builds Debug and installs it when an authorized device is connected.

Do not copy a SWF under source assets. Import the game through the app. The package is `io.github.frostie77.chaosfaction2`, version `0.1.0` / code `1` initially. Reuse the PC's Debug key and increase the code for development updates that retain saves. Public releases need this port's own permanent signing key; do not reuse another port's stable feed or release key.

## Checks

- `python tests/test_control_profile.py`: loads the Activity's real default bindings through the pinned production mapper.
- `node tests/test_input.cjs`: exercises the real input bridge, shared jump/shield ownership, melee modifier, held attacks, drop, pause and lifecycle.
- `python tests/test_updates.py`: tests the production verifier against controlled HTTPS release metadata.
- Android builds and package/icon/game-free payload checks run in GitHub Actions.

The stable feed starts with `latest: null`. Publish a tested signed APK before activating matching update metadata. Publishing is a separate step from development builds.
