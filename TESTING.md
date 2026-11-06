# Initial Chaos Faction 2 test build

The supplied game is Flash 8 / AVM1, native 800×600 at 38 fps. Its SHA-256 is `b964fbe7a3b60f0bf00ff78d1a2fe16c2ea47ecc61bd0ea07b46c5236294e6a3` (12,336,544 bytes). The original file is unchanged and excluded from source and APKs.

Input uses the creator's defaults: arrows for movement/jump/shield, Z/X attacks, Shift as the held weapon-melee modifier, D to drop and Space to pause. Frontend remapping changes Android button assignments, not the game's keyboard settings.

The input bridge merges ownership of shared Up/Jump and Down/Shield keys. Touch, stick, hat and physical-button sources can overlap without releasing each other's held inputs. It emits the Shift modifier on melee/attack events and clears held inputs when settings open or the app backgrounds. Start opens FrostFrontend; R3 defaults to game pause, keeping it on an assignable button.

The source initially uses the pinned FrostFrontend revision `2147f7ab4e183ea6b19b3d8bedf6bbe04b05c56c` and Ruffle 0.7.1. The CF2-specific frontend profile, controls, storage, package and update feed are independent of other ports.

## Completed checks

[GitHub Actions run 42064012459](https://github.com/Frostie77/Chaos-Faction-2/actions/runs/42064012459), source `bc1f604ed4665b59480cb8bfa32774b1fecac75d`:

- Debug and unsigned Release APKs compile successfully against Android 35.
- Both APKs pass package/version, Android 8 minimum, launcher resource, pinned Ruffle file hashes and no-bundled-SWF checks.
- The real controller profile initializes with fresh and stale preferences. Input ownership/modifier/lifecycle checks and all 20 updater scenarios pass.
- The Windows helper parses under Windows PowerShell 5.1.

A desktop Chromium smoke test loaded the exact supplied SWF using the pinned runtime and production HTML at the Android virtual asset origin, with external requests blocked. The unmodified preloader completed, and the main menu, character selection and Town Tussle campaign scene rendered. The production input bridge also opened the in-game Space pause menu. Ruffle created the game's `CF2` save object. At 1280×720, the original display fills a centered 960×720 viewport. This checks browser compatibility and wrapper sizing; Odin performance, audio, native input routing and save restoration still require the device checks below.

## Odin acceptance

The initial Odin build was reported working by the user. The next build removes the redundant touch Pause button and adds Touch Settings → Edit Touch Layout. Confirm dragging all four buttons and the movement pad, saving and restarting, Cancel, Reset followed by Save/Cancel, both pad types, swapped sides, size changes and background/resume while editing. The game must remain paused until FrostFrontend closes, and editor gestures must not trigger attacks or movement.

`TouchLayoutTest` exercises the real Android touch view with Robolectric: saved positions and relocated hit areas, cancellation and reset, proportional resizing and mirrored layouts, screen boundaries, pointer-ID changes, releasing held keys, removal of the old Pause hit area, invalid stored positions and Activity editor/pause lifecycle. CI runs it before assembling the APKs.

Gameplay, performance, double jump, shield/stomp, both attacks, held weapon melee, weapon drop, touch layout, remapping, sound/speaker test, saves across restarts and background/resume need device confirmation before release. Full-game compatibility and multiplayer controller assignment are not yet established.
