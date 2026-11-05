# Chaos Faction 2 — Android

An unofficial Android adaptation of Chaos Faction 2, powered by Ruffle and FrostFrontend. Requires Android 8.0 or newer.

This is an initial development build for Odin testing. Import your own `chaos-faction-2-5645.swf` after installation; the game is not bundled with the source or APK.

## Build and install

Clone with submodules, then run this in the `Chaos-Faction-2` folder with your Odin connected and USB debugging authorized:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build-test.ps1
```

The helper uses your PC's existing Android Debug signing identity and installs with `adb install -r`. Open the app and select your SWF. Menus use the touchscreen. The imported game, saves and settings are stored privately by this app.

## Default controller controls

| Control | Action |
| --- | --- |
| Left stick / D-pad | Move; Up to jump/double jump; Down to shield/stomp |
| A | Jump |
| X | Attack 1 (Z) |
| Y | Attack 2 (X) |
| B | Shield / stomp |
| L1 | Hold with an attack to melee while carrying a weapon (Shift) |
| R1 | Drop weapon (D) |
| R3 | Game pause (Space) |
| Start / menu icon | FrostFrontend |

Remap actions in FrostFrontend. Keep the game's keyboard bindings at their defaults. Touch controls provide a joystick or D-pad and Attack 1, Attack 2, Melee, Drop and Pause. Up jumps; Down shields/stomps. Hold Melee with an attack when carrying a weapon.

FrostFrontend includes original 800×600 proportions or stretch display, sound settings, a speaker test, controller mapping and touch layout settings. The initial CF2 launcher icon is temporary.

## Updates

The stable checker is isolated to `Frostie77/Chaos-Faction-2` and package `io.github.frostie77.chaosfaction2`. No stable release is published yet. Development APKs are excluded from stable updates. Public release signing and feed activation follow device testing.

## Credits

- **Original game:** [Chaos Faction 2](https://www.newgrounds.com/portal/view/535370), by [EON / Dissolute Productions](https://eon.newgrounds.com/).
- **Original art:** [mungo-raginbrain](https://mungo-raginbrain.newgrounds.com/).
- **Original publication:** [Armor Games](https://armorgames.com/play/5645/chaos-faction-2).
- **Android adaptation:** [Frostie77](https://github.com/Frostie77).
- **Flash runtime:** [Ruffle](https://ruffle.rs/), pinned to 0.7.1.
- **Settings interface:** [FrostFrontend](https://github.com/Frostie77/FrostFrontend), pinned by `FROSTFRONTEND_REVISION`.

Original artwork, audio, characters and other game content belong to their respective creators.
