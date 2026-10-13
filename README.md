<p align="center">
  <img src="assets/logo.png" alt="RuneRelay logo" width="128" height="128">
</p>

<h1 align="center">RuneRelay</h1>

<p align="center"><b>Good RuneLite settings for the computer you're on, in one place.</b></p>

<p align="center"><a href="https://runerelay.com">runerelay.com</a></p>

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
  RuneLite is in the background, or Stretched Mode on very large screens. It also suggests a few comfort
  settings for everyone, like the Logout Timer at 30 minutes and typing your bank PIN with the keyboard.
  - *Apply all* uses every suggestion; *Review* lets you tick which ones to use.
- **One list of plugins**, built-in and Plugin Hub, grouped by what they help with (graphics, tiles and
  markers, keybinds and camera, bank, quality of life). Each one has an on/off switch, and plugins you don't
  have yet show an *Install* button. From the Plugin Hub it only offers a handful of essentials, each with
  well over 100,000 installs, rather than every alternative.
- **A page per plugin** explaining what it does and how to use it, with its settings and RuneRelay's
  suggestion next to each one. Changes save instantly, like RuneLite's own settings.
- **Undo** for every change RuneRelay makes, including *Apply all*.
- **Calm defaults for added plugins.** For example, it sets Quick Tiles to only show "Mark" while Shift is held
  and switches Line of Sight to outline-only, so new players aren't greeted by a noisy screen.
- **Avoids clashing plugins.** Plugins that fight each other (two key remappers, GPU and 117 HD, two tile
  indicator plugins) are never left on together; RuneRelay asks before switching the other one off.
- **Makes edits take effect.** Changing a key binding switches on the remapping it needs, and changing a
  setting of a switched-off plugin switches that plugin on.
- **Shows your real F-key layout.** Key remapping rows say which side-panel tab each key opens in your game
  (for example "F6 key → Prayer"), read from your in-game keybinding settings.
- **Boss and raid markers in one click.** See [Boss and raid markers](#boss-and-raid-markers).

## What it doesn't do

- It never changes anything until you press a button. The one exception: when you install a plugin through
  RuneRelay, its suggested settings for that plugin are applied straight away, and *Undo* still works.
- It doesn't send input to the game, read your account details, or talk to any server of its own. Installing
  and removing plugins goes through RuneLite's own Plugin Hub, exactly like RuneLite's Plugin Hub panel.
- It doesn't work out boss mechanics or where to stand. Boss and raid markers only come from packs the
  player chooses to switch on, made by players and shared through Tile Packs or Radius Markers.

## Using it

1. Open the RuneRelay panel (the orange **R** in RuneLite's sidebar).
2. Check that **Your PC** matches your computer. Graphics cards can't be detected, so pick a lower tier
   if the game feels slow.
3. Press **Apply all**, or **Review** to choose suggestions one by one.
4. Click any plugin to read what it does and change its settings. **Undo** at the bottom puts things back.

## Boss and raid markers

RuneRelay doesn't create markers itself; it switches on markers other players already made:

- **Tile Packs.** On the Tile Packs page, *Set up boss & raid tiles* lists the community packs that ship with
  [Tile Packs](https://github.com/TrevorMDev/tile-packs): raids (CoX, ToB, ToA), bosses and the Inferno and
  Colosseum. Tick the fights you do; where a boss has several kill methods, pick yours. RuneRelay switches those
  packs on in Tile Packs, and *Undo* switches them back.
- **Radius Markers.** When [Radius Markers](https://github.com/Skretzo/runelite-plugins/tree/radius-markers)
  is installed through RuneRelay, it comes with RuneRelay's raid and boss markers (Theatre of Blood Nylocas and
  Verzik in normal and hard mode, Tombs of Amascut Akkha and Apmeken baboons, Phantom Muspah and Nex) and boss
  markers published on the OSRS Wiki: Doom of Mokhaiotl's halberd range (it turns green in the shield phase) and
  burrowed phase, Vardorvis, and the Leviathan's Abyssal pathfinder buff radius. A larger pack of 164 clue step markers can be added with one click, and *Add markers
  from clipboard* adds markers someone exported with Radius Markers' copy button. Markers already present
  are skipped.

The raid and boss markers were made by RuneRelay's author. The other bundled Radius Markers data comes from
the OSRS Wiki and stays under the wiki's
[CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/) license; see
[its license file](src/main/resources/com/runerelay/radius-packs/LICENSE.md) for the exact pages and authors.

### Contributing a Radius Markers pack

Radius Markers doesn't detect ranges; they have to be measured in game (see its
[guide to range types](https://github.com/Skretzo/runelite-plugins/wiki/Range-types)). To bundle a pack:

1. Add the markers in Radius Markers' panel at each boss, and check them in game.
2. Press Radius Markers' copy button and save the text as
   `src/main/resources/com/runerelay/radius-packs/<name>.json`.
3. Add it to `src/main/resources/com/runerelay/radius-packs/index.json`:
   `{"name": "Raid bosses", "file": "<name>.json", "credit": "who measured it", "description": "what it marks", "installWithPlugin": false}`

## Credits

RuneRelay suggests settings for, switches, and can install the plugins below. It doesn't include or modify
their code; it only changes their settings through RuneLite's normal configuration system.

### Built into RuneLite

By the [RuneLite](https://github.com/runelite/runelite) contributors: GPU, FPS Control, Low Detail,
Stretched Mode, Tile Indicators, Ground Markers, NPC Indicators, Key Remapping, Camera, Bank,
Menu Entry Swapper, Idle Notifier, Status Bars, Interact Highlight, Anti Drag, Virtual Levels and Logout Timer.

### From the Plugin Hub

| Plugin | Author | Source |
|---|---|---|
| Quest Helper | Zoinkwiz | [Zoinkwiz/quest-helper](https://github.com/Zoinkwiz/quest-helper) |
| Inventory Setups | dillydill123 | [dillydill123/inventory-setups](https://github.com/dillydill123/inventory-setups) |
| Shortest Path | Skretzo, FIrgolitsch, mpickering | [Skretzo/shortest-path](https://github.com/Skretzo/shortest-path) |
| Item Charges Improved | TicTac7x | [TicTac7x/runelite-plugins](https://github.com/TicTac7x/runelite-plugins) |
| Tile Packs | TrevorMDev | [TrevorMDev/tile-packs](https://github.com/TrevorMDev/tile-packs) |
| Radius Markers | Skretzo | [Skretzo/runelite-plugins](https://github.com/Skretzo/runelite-plugins) |
| 117 HD | 117 | [117HD/RLHD](https://github.com/117HD/RLHD) |

The boss and raid tile packs RuneRelay switches on are part of Tile Packs and were made by its community.

### Marker data

Radius marker packs come from the [OSRS Wiki](https://oldschool.runescape.wiki):
[Doom of Mokhaiotl/Strategies](https://oldschool.runescape.wiki/w/Doom_of_Mokhaiotl/Strategies),
[The Leviathan/Strategies](https://oldschool.runescape.wiki/w/The_Leviathan/Strategies),
[User:Daddy Bilbo/Reference](https://oldschool.runescape.wiki/w/User:Daddy_Bilbo/Reference) and
[User:Dodokipje](https://oldschool.runescape.wiki/w/User:Dodokipje), under CC BY-NC-SA 3.0.

### Design

RuneRelay's panel follows the layout of RuneLite's own plugin list and settings pages so it feels familiar.
Ideas for managing many plugin settings at once came from [Plugin Presets](https://github.com/antero111/plugin-presets)
by antero111, and reading the game's keybindings to label F-keys follows
[Tab Keybind Display](https://github.com/Jin-Jiyunsun/jins-plugins).

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
