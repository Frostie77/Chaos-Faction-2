# Chaos Faction 2 release preparation

The first public release is [v0.1.0](https://github.com/Frostie77/Chaos-Faction-2/releases/tag/v0.1.0), Android version code **2**, package `io.github.frostie77.chaosfaction2`, built from `8294635bb705b5be2337ebf140e6985bdcf4d2d0`. Its stable feed is active, and the owner confirmed the signed APK works on the Odin.

The permanent release certificate SHA-256 is `807a5ceb18503c6da705f5105f93ae97cee22cf3e9233c038706f0236f09f280`. The APK is **30,487,132 bytes**, SHA-256 `8b5ab276f9ec77da8fc9dae8ec042d16d44f3d34ab3364591b56a9afe54d8b35`. The public tag and frozen `release/v0.1.0` branch both point to the build source. `RELEASE_DRAFT.md` contains clean release-note text, including the SWF link and credits.

## Prepare on Windows

For the next release, increase the version name and Android version code, update the release notes and run from a clean checkout:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build-public-release.ps1
```

The helper synchronizes FrostFrontend to its pinned revision, verifies Ruffle 0.7.1, builds Release and creates the port's permanent RSA signing key on the owner's PC if it does not already exist. The default key is `%USERPROFILE%\.android\Chaos-Faction-2-Release.keystore`, alias `chaos-faction-2-release`. Keep the password and back up the key and its `.certificate.sha256` companion privately. Reuse that same key for all future public updates; never upload the key to GitHub. A missing established key is not silently replaced.

The helper checks v2/v3 signatures, alignment, package/version/minimum Android API and the non-debuggable state. It verifies the packaged HTML and every pinned Ruffle runtime file, compares decoded launcher-art pixels to the approved source image, and rejects any APK containing a SWF. It stages these files in `release/Chaos-Faction-2-v<version>-<source commit prefix>/`:

- `Chaos-Faction-2-v<version>-Android.apk`
- `Chaos-Faction-2-v<version>-Android.apk.sha256.txt`
- `android-stable.json`, generated from that exact signed APK and signing certificate
- `release-notes.md`

The source commit is recorded in the staged feed and must match the draft release target. A clean-source check also runs before the verified files are staged. Preparation does not install or uninstall an app, publish a release, or activate the update feed.

Development APKs have the PC's Debug signing identity. Android cannot update such an installation directly to the separately signed public APK. Preserve the development installation's imported game, saves and settings before switching; leave it installed during preparation. Public-to-public updates use the permanent release key and a higher version code.

## Draft and publication order

1. Prepare the signed candidate and back up its permanent key. Validate that exact signed APK on the Odin before publishing.
2. Use tag `v<version>`, title **Chaos Faction 2 Android v<version>**, and the staged release notes. Target the source commit in the staged feed; a `release/v<version>` branch may hold that exact commit while the draft is assembled.
3. Attach the signed APK and checksum file to the draft. Keep the staged `android-stable.json` for the repository feed. Review the complete draft before publication.
4. Once publication is authorized, publish and verify the public APK name, size and SHA-256 against the staged feed.
5. Only then replace `updates/android-stable.json` with that staged feed. Keep `source_commit` pointing to the build source, rather than the later feed-activation commit.
6. Check Updates from the signed APK. Future releases need the same signing key and a higher code; test that an older signed version offers the matching public APK before distributing it.

## Automated checks

Android CI runs the shared/frontend touch-layout tests, CF2 input/profile tests and 20 production update-verifier scenarios, then builds both APK variants and checks their payload and identity. Windows CI parses both helpers and exercises the release helper using an isolated first-release fixture and a disposable key. It verifies the staged feed matches the actual signed APK, rejects a changed certificate pin without overwriting the valid candidate and confirms the live feed remains unchanged. CI keys and signed test candidates are not published as release assets.
