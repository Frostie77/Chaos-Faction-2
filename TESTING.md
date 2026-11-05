# Initial Chaos Faction 2 test build

The supplied game is Flash 8 / AVM1, native 800×600 at 38 fps. Its SHA-256 is `b964fbe7a3b60f0bf00ff78d1a2fe16c2ea47ecc61bd0ea07b46c5236294e6a3` (12,336,544 bytes). The original file is unchanged and excluded from source and APKs.

Input uses the creator's defaults: arrows for movement/jump/shield, Z/X attacks, Shift as the held weapon-melee modifier, D to drop and Space to pause. Frontend remapping changes Android button assignments, not the game's keyboard settings.

The input bridge merges ownership of shared Up/Jump and Down/Shield keys. Touch, stick, hat and physical-button sources can overlap without releasing each other's held inputs. It emits the Shift modifier on melee/attack events and clears held inputs when settings open or the app backgrounds. Start opens FrostFrontend; R3 defaults to game pause, keeping it on an assignable button.

The source initially uses the pinned FrostFrontend revision `2147f7ab4e183ea6b19b3d8bedf6bbe04b05c56c` and Ruffle 0.7.1. The CF2-specific frontend profile, controls, storage, package and update feed are independent of other ports.

## Odin acceptance

Gameplay, performance, double jump, shield/stomp, both attacks, held weapon melee, weapon drop, touch layout, remapping, sound/speaker test, saves across restarts and background/resume need device confirmation before release. Full-game compatibility and multiplayer controller assignment are not yet established.
