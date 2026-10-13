<p align="center">
  <img src="assets/logo.png" alt="RuneRelay logo" width="128" height="128">
</p>

<h1 align="center">RuneRelay</h1>

<p align="center"><b>Good RuneLite settings for the computer you're on, in one place.</b></p>

RuneRelay is a small piece of **middleware** for [RuneLite](https://runelite.net). It doesn't add game features
of its own. It helps players, especially new ones, set up the features that RuneLite and the community already
built: it suggests sensible settings for your computer, explains what plugins and settings do in plain language,
and lets you switch, tune, install and remove those plugins from a single side panel.

All the real work in the game is done by the plugins listed under [Credits](#credits). Their authors
deserve the credit for what they do; RuneRelay only makes them easier to find and set up.

## What it does

- **Reads your hardware**: CPU threads, memory, screen resolution and refresh rate. From that it picks a
  performance tier (Low, Balanced, High) that you can change at any time, including Ultra.
- **Suggests settings** for that tier, such as GPU draw distance and anti-aliasing, a lower frame rate while
  RuneLite is in the background, or Stretched Mode on very large screens.
  - *Apply all* uses every suggestion; *Review* lets you tick which ones to use.
- **One list of plugins**, built-in and Plugin Hub, grouped by what they help with (graphics, tiles and
  markers, keybinds and camera, bank, quality of life). Each one has an on/off switch, and plugins you don't
  have yet show an *Install* button.
- **A page per plugin** explaining what it does and how to use it, with its settings and RuneRelay's
  suggestion next to each one. Changes save instantly, like RuneLite's own settings.
- **Undo** for every change RuneRelay makes, including *Apply all*.
- **Calm defaults for added plugins.** For example, it sets Quick Tiles to only show "Mark" while Shift is held
  and switches Line of Sight to outline-only, so new players aren't greeted by a noisy screen.
- **Avoids clashing plugins.** Plugins that fight each other (two key remappers, GPU and 117 HD, two tile
  indicator plugins) are never left on together; RuneRelay asks before switching the other one off.
- **Makes edits take effect.** Changing a key binding switches on the remapping it needs, and changing a
  setting of a switched-off plugin switches that plugin on.

## What it doesn't do

- It never changes anything until you press a button. The one exception: when you install a plugin through
  RuneRelay, its suggested settings for that plugin are applied straight away, and *Undo* still works.
- It doesn't send input to the game, read your account details, or talk to any server of its own. Installing
  and removing plugins goes through RuneLite's own Plugin Hub, exactly like RuneLite's Plugin Hub panel.
- It doesn't add markers or helpers for bosses, raids or wave minigames. Jagex's third-party client rules only
  allow those to be marked by hand, so RuneRelay points players to Ground Markers, Radius Markers and Tile
  Packs, where they can mark things themselves.

## Using it

1. Open the RuneRelay panel (the orange **R** in RuneLite's sidebar).
2. Check that **Your PC** matches your computer. Graphics cards can't be detected, so pick a lower tier
   if the game feels slow.
3. Press **Apply all**, or **Review** to choose suggestions one by one.
4. Click any plugin to read what it does and change its settings. **Undo** at the bottom puts things back.

## Credits

RuneRelay suggests settings for, switches, and can install the plugins below. It doesn't include or modify
their code; it only changes their settings through RuneLite's normal configuration system.

### Built into RuneLite

By the [RuneLite](https://github.com/runelite/runelite) contributors: GPU, FPS Control, Low Detail,
Stretched Mode, Tile Indicators, Ground Markers, NPC Indicators, Key Remapping, Camera, Bank,
Menu Entry Swapper, Idle Notifier, Status Bars, Interact Highlight, Anti Drag and Virtual Levels.

### From the Plugin Hub

| Plugin | Author | Source |
|---|---|---|
| Quest Helper | Zoinkwiz | [Zoinkwiz/quest-helper](https://github.com/Zoinkwiz/quest-helper) |
| Inventory Setups | dillydill123 | [dillydill123/inventory-setups](https://github.com/dillydill123/inventory-setups) |
| Dude, Where's My Stuff? | Thource | [Thource/dude-wheres-my-stuff](https://github.com/Thource/dude-wheres-my-stuff) |
| Shortest Path | Skretzo, FIrgolitsch, mpickering | [Skretzo/shortest-path](https://github.com/Skretzo/shortest-path) |
| Item Charges Improved | TicTac7x | [TicTac7x/runelite-plugins](https://github.com/TicTac7x/runelite-plugins) |
| GE Filters | Salverrs, Nick2bad4u | [nick2bad4u/GE-Filters](https://github.com/nick2bad4u/GE-Filters) |
| Tile Packs | TrevorMDev | [TrevorMDev/tile-packs](https://github.com/TrevorMDev/tile-packs) |
| Radius Markers | Skretzo | [Skretzo/runelite-plugins](https://github.com/Skretzo/runelite-plugins) |
| Ground Marker Profiles | Dang It | [DangItOSRS/ground-marker-roles](https://github.com/DangItOSRS/ground-marker-roles) |
| Improved Tile Indicators | LeikvollE | [LeikvollE/tileindicators](https://github.com/LeikvollE/tileindicators) |
| Quick Tiles | AoceanP | [AoceanP/quick-tiles](https://github.com/AoceanP/quick-tiles) |
| Path Marker | GeChallengeM | [GeChallengeM/path-marker](https://github.com/GeChallengeM/path-marker) |
| Line of Sight | krazune | [Krazune/LineOfSight](https://github.com/Krazune/LineOfSight) |
| Key Remapping Plus | Macica2 | [macica2/key-remapping-plus](https://github.com/macica2/key-remapping-plus) |
| Smart Key Remapping | Taz441 | [Taz441/Smart-KeyRemapping](https://github.com/Taz441/Smart-KeyRemapping) |
| Plugin Hotkeys | pvff13 | [Pvff13/plugin-hotkeys](https://github.com/Pvff13/plugin-hotkeys) |
| Keybinds Overlay | Kuldar Vakker | [kuldarvakker/keybinds-overlay](https://github.com/kuldarvakker/keybinds-overlay) |
| 117 HD | 117 | [117HD/RLHD](https://github.com/117HD/RLHD) |

### Design

RuneRelay's panel follows the layout of RuneLite's own plugin list and settings pages so it feels familiar.
Ideas for managing many plugin settings at once came from [Plugin Presets](https://github.com/antero111/plugin-presets)
by antero111.

## Building

RuneRelay is built from RuneLite's [example plugin](https://github.com/runelite/example-plugin) template and
targets Java 11.

```sh
./gradlew build   # compile and test
./gradlew run     # start a RuneLite development client with RuneRelay loaded
```

To log in to the development client with a Jagex account, follow
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## License

[BSD 2-Clause](LICENSE).
