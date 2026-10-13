package com.runerelay;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import lombok.Value;
import lombok.With;
import net.runelite.client.plugins.gpu.GpuPluginConfig;
import net.runelite.client.plugins.gpu.config.AntiAliasingMode;

/**
 * Everything RuneRelay knows how to configure or suggest. Settings are referenced by
 * owning plugin name + config key; labels, types and ranges are read from the plugin's
 * own config descriptor at runtime, so they always match the client.
 */
final class Catalog
{
	static final String GPU = "GPU";
	static final String HD = "117 HD";

	private static final String FPS = "FPS Control";
	private static final String LOW_DETAIL = "Low Detail";
	private static final String STRETCHED = "Stretched Mode";
	private static final String TILES = "Tile Indicators";
	private static final String GROUND_MARKERS = "Ground Markers";
	private static final String NPC_INDICATORS = "NPC Indicators";
	private static final String KEY_REMAPPING = "Key Remapping";
	private static final String CAMERA = "Camera";
	private static final String BANK = "Bank";
	private static final String RADIUS_MARKERS = "Radius Markers";
	private static final String TILE_PACKS = "Tile Packs";
	private static final String IMPROVED_TILES = "Improved Tile Indicators";
	private static final String QUEST_HELPER = "Quest Helper";
	private static final String INVENTORY_SETUPS = "Inventory Setups";
	private static final String SHORTEST_PATH = "Shortest Path";
	private static final String ITEM_CHARGES = "Item Charges Improved";
	private static final String KEY_REMAPPING_PLUS = "Key Remapping Plus";
	// Registers itself with a lowercase "k"
	private static final String SMART_KEY_REMAPPING = "Smart key remapping";
	private static final String LOGOUT_TIMER = "Logout Timer";

	private static final String SPAWN_RANGES_WHY = "These ranges are drawn around where a marker was placed, not around "
		+ "the NPC, so they leave stray lines on the map as you walk. Attack ranges, which follow the NPC, stay on.";

	/**
	 * Logical screen width above which the game UI gets uncomfortably small.
	 */
	private static final int STRETCH_SCREEN_WIDTH = 3200;

	/**
	 * Plain-language explanation of each plugin, for players new to RuneLite.
	 */
	private static final Map<String, String> PLUGIN_INFO = ImmutableMap.<String, String>builder()
		.put(GPU, "Draws the game with your graphics card instead of your processor. Much smoother, and needed for longer view distance, anti-aliasing and frame rates above 50.")
		.put(HD, "A much prettier renderer with lighting, shadows and new textures. It replaces the GPU plugin, so only one of them can be on, and it needs a strong graphics card.")
		.put(FPS, "Caps how many frames per second the game draws. RuneRelay uses it to slow the game down while its window is in the background, which saves power and keeps your computer cool.")
		.put(LOW_DETAIL, "Removes small ground decorations and can hide the floors below you. Makes the game lighter for slow computers.")
		.put(STRETCHED, "Scales the whole game up so it stays readable on big or high-resolution screens.")
		.put(TILES, "Highlights the tile you are walking to and, optionally, your \"true tile\": the tile the server thinks you stand on, which is often a step behind your character's animation.")
		.put(GROUND_MARKERS, "Mark tiles on the floor yourself. Hold Shift and right-click a tile, then choose Mark tile. Marked tiles can be exported and imported, which is how players share tile setups.")
		.put(NPC_INDICATORS, "Highlights NPCs you choose. Hold Shift and right-click an NPC, then choose Tag, or type names in its settings.")
		.put(KEY_REMAPPING, "Move the camera with W A S D and use number keys as F-keys. While it is on, press Enter before typing in chat, then Enter again to send.")
		.put(CAMERA, "More camera freedom: zoom further out, relax the angle limits, and drag the camera by holding right-click on empty ground.")
		.put(BANK, "Shows the value of your bank and lets you type your bank PIN with the number keys instead of clicking.")
		.put("Menu Entry Swapper", "Puts the option you usually want on left-click, for example Bank on bankers and Trade on shopkeepers.")
		.put("Idle Notifier", "Sends a notification when your character stops what it was doing, such as running out of logs to cut or a fight ending.")
		.put("Status Bars", "Shows hitpoints and prayer bars next to your inventory.")
		.put("Interact Highlight", "Outlines whatever you hover over and whatever you are currently interacting with.")
		.put("Anti Drag", "Makes it harder to drag inventory items by accident. By default it only kicks in while you hold Shift.")
		.put("Virtual Levels", "Keeps counting levels past 99 based on your experience, up to 126.")
		.put(LOGOUT_TIMER, "The game logs you out after 5 minutes without input. This plugin stretches that, up to 30 minutes.")
		.put(RADIUS_MARKERS, "Shows an NPC's attack, aggression or wander range, also inside raids and other instances. Add markers from its side panel, or add ready-made boss and raid markers with the button below. Markers are measured in game; the plugin doesn't work them out itself.")
		.put(TILE_PACKS, "Ready-made tile setups made by other players, including raids, bosses, the Inferno and the Colosseum. Use the button below to pick the fights you do, or switch packs on in its own side panel.")
		.put(QUEST_HELPER, "Step-by-step help for every quest. Open its side panel, pick a quest, and it highlights what to do next.")
		.put(INVENTORY_SETUPS, "Save your gear and inventory for each activity. In the bank it highlights exactly what to take out.")
		.put(SHORTEST_PATH, "Right-click the world map, or Shift + right-click a tile, to set a destination. It draws the fastest route, including teleports you can use.")
		.put(ITEM_CHARGES, "Shows the remaining charges on many items that the game doesn't display.")
		.build();

	/**
	 * Plugins that clash when on together: two renderers, two key remappers fighting over the same keys,
	 * or two tile indicator plugins drawing the same tiles twice. Only one per group should be on.
	 * RuneRelay only offers the first of each group, but players may have the others installed.
	 */
	private static final List<String> KEY_REMAPPERS = Arrays.asList(KEY_REMAPPING, KEY_REMAPPING_PLUS, SMART_KEY_REMAPPING);

	private static final List<List<String>> EXCLUSIVE_GROUPS = Arrays.asList(
		Arrays.asList(GPU, HD),
		KEY_REMAPPERS,
		Arrays.asList(TILES, IMPROVED_TILES)
	);

	/**
	 * Produces the recommended value for a tier and machine, or null for "no opinion".
	 */
	interface Recommender extends BiFunction<PerformanceTier, SystemSpecs, Object>
	{
	}

	@Value
	static class SettingDef
	{
		String plugin;
		/**
		 * Config key, or null for the plugin's on/off state.
		 */
		String key;
		Recommender recommender;
		/**
		 * Why RuneRelay recommends this, shown in the row's info. May be null.
		 */
		String why;
		/**
		 * Key of an on/off setting in the same plugin that must be on for this one to do anything.
		 * Editing this setting switches it on too. May be null.
		 */
		@With
		String requires;

		String id()
		{
			return plugin + (key == null ? "#enabled" : "." + key);
		}
	}

	@Value
	static class Section
	{
		String id;
		String title;
		String description;
		List<SettingDef> settings;
	}

	@Value
	static class HubPlugin
	{
		String internalName;
		/**
		 * The plugin's own descriptor name, used to find it once installed.
		 */
		String pluginName;
		String description;
		BiPredicate<PerformanceTier, SystemSpecs> recommended;
	}

	@Value
	static class HubCategory
	{
		String title;
		List<HubPlugin> plugins;
	}

	private Catalog()
	{
	}

	static String info(String pluginName)
	{
		return PLUGIN_INFO.get(pluginName);
	}

	/**
	 * Plugins that must not be on at the same time as this one.
	 */
	static List<String> rivals(String pluginName)
	{
		for (List<String> group : EXCLUSIVE_GROUPS)
		{
			if (group.contains(pluginName))
			{
				List<String> out = new ArrayList<>(group);
				out.remove(pluginName);
				return out;
			}
		}
		return new ArrayList<>();
	}

	/**
	 * Whether the plugin remaps keys (core Key Remapping or one of its Plugin Hub alternatives).
	 */
	static boolean isKeyRemapper(String pluginName)
	{
		return KEY_REMAPPERS.contains(pluginName);
	}

	static List<List<String>> exclusiveGroups()
	{
		return EXCLUSIVE_GROUPS;
	}

	/**
	 * Plugin Hub details for a plugin, or null for built-in plugins.
	 */
	static HubPlugin hub(String pluginName)
	{
		for (HubCategory category : hubCategories())
		{
			for (HubPlugin hub : category.getPlugins())
			{
				if (hub.getPluginName().equals(pluginName))
				{
					return hub;
				}
			}
		}
		return null;
	}

	/**
	 * The plugins a section covers, in catalog order.
	 */
	static List<String> pluginsIn(Section section)
	{
		List<String> out = new ArrayList<>();
		for (SettingDef def : section.getSettings())
		{
			if (!out.contains(def.getPlugin()))
			{
				out.add(def.getPlugin());
			}
		}
		return out;
	}

	static List<Section> sections()
	{
		return Arrays.asList(
			new Section("graphics", "Graphics", "How good the game looks versus how smoothly it runs.", Arrays.asList(
				on(GPU, "Almost every computer runs the game more smoothly with it."),
				tier(GPU, "drawDistance", 25, 50, 90, 184),
				tier(GPU, "expandedMapLoadingChunks", 0, 1, 3, 5),
				tier(GPU, "antiAliasingMode", AntiAliasingMode.DISABLED, AntiAliasingMode.MSAA_2, AntiAliasingMode.MSAA_4, AntiAliasingMode.MSAA_8),
				tier(GPU, "anisotropicFilteringLevel", 0, 4, 8, 16),
				tier(GPU, "unlockFps", false, true, true, true),
				tier(GPU, "vsyncMode", null, GpuPluginConfig.SyncMode.ON, GpuPluginConfig.SyncMode.ON, GpuPluginConfig.SyncMode.ON),
				free(GPU, "fpsTarget"),
				free(GPU, "fogDepth"),
				free(GPU, "smoothBanding"),
				free(GPU, "colorBlindMode"),
				free(HD, null)
			)),
			new Section("framerate", "Frame rate & power", "Keep the game light on your computer, especially in the background.", Arrays.asList(
				on(FPS, "Needed for the background frame rate limit below."),
				fixed(FPS, "limitFpsUnfocused", true, "There's no point drawing at full speed while you're in another window."),
				tier(FPS, "maxFpsUnfocused", 10, 20, 20, 30),
				free(FPS, "limitFps"),
				free(FPS, "maxFps"),
				free(FPS, "drawFps"),
				def(LOW_DETAIL, null, (t, s) -> t == PerformanceTier.LOW ? true : null, "Takes load off slow computers.")
			)),
			new Section("display", "Display & scaling", "Make the game readable on large or high-resolution screens.", Arrays.asList(
				def(STRETCHED, null, (t, s) -> s.getLogicalScreenWidth() >= STRETCH_SCREEN_WIDTH ? true : null,
					"Your screen is very wide, so the game would otherwise look tiny."),
				free(STRETCHED, "scalingFactor"),
				free(STRETCHED, "keepAspectRatio"),
				free(STRETCHED, "integerScaling"),
				def(STRETCHED, "increasedPerformance", (t, s) -> t == PerformanceTier.LOW ? true : null, "Faster scaling for slow computers.")
			)),
			new Section("tiles", "Tiles & markers", "Highlight tiles and NPCs, and mark tiles yourself.", Arrays.asList(
				on(TILES, "Seeing where you walk makes movement much easier."),
				fixed(TILES, "highlightDestinationTile", true, "Shows where your click will take you."),
				fixed(TILES, "highlightCurrentTile", true, "Shows where you really are, which matters for precise movement."),
				free(TILES, "highlightCurrentColor"),
				free(TILES, "currentTileBorderWidth"),
				free(TILES, "highlightDestinationColor"),
				free(TILES, "highlightHoveredTile"),
				free(TILES, "highlightHoveredColor"),
				on(GROUND_MARKERS, "Lets you mark tiles with Shift + right-click and import shared tiles."),
				fixed(GROUND_MARKERS, "showImportExport", true, "Needed to paste in tile setups other players share."),
				free(GROUND_MARKERS, "markerColor"),
				free(GROUND_MARKERS, "fillOpacity"),
				free(GROUND_MARKERS, "borderWidth"),
				free(GROUND_MARKERS, "drawOnMinimap"),
				free(NPC_INDICATORS, null),
				free(NPC_INDICATORS, "highlightTrueTile"),
				free(NPC_INDICATORS, "highlightTile"),
				free(NPC_INDICATORS, "npcColor"),
				free(NPC_INDICATORS, "drawNames")
			)),
			// Hub plugins below only appear once installed; their defaults can be noisy
			// Plugin Hub plugins below only appear once installed
			new Section("markeraddons", "Marker add-ons", "Ready-made tile and radius markers from the Plugin Hub.", Arrays.asList(
				free(TILE_PACKS, null),
				free(TILE_PACKS, "showLabels"),
				free(TILE_PACKS, "fillOpacity"),
				free(TILE_PACKS, "drawTilesOnMinimmap"),
				free(RADIUS_MARKERS, null),
				// Wander, max and aggression ranges are drawn around a marker's spawn tile, not the NPC,
				// which leaves stray lines on the map; attack ranges follow the NPC and stay on
				fixed(RADIUS_MARKERS, "includeWanderRange", false, SPAWN_RANGES_WHY),
				fixed(RADIUS_MARKERS, "includeMaxRange", false, SPAWN_RANGES_WHY),
				fixed(RADIUS_MARKERS, "includeAggressionRange", false, SPAWN_RANGES_WHY),
				free(RADIUS_MARKERS, "includeAttackRange"),
				free(RADIUS_MARKERS, "borderWidth"),
				free(RADIUS_MARKERS, "showMinimap"),
				free(RADIUS_MARKERS, "showWorldMap")
			)),
			new Section("keybinds", "Keybinds & camera", "W A S D camera, F-keys on number keys, and right-click camera control.", Arrays.asList(
				on(KEY_REMAPPING, "Camera on W A S D is far more comfortable than the arrow keys. Press Enter to chat."),
				fixed(KEY_REMAPPING, "cameraRemap", true, "This is what moves the camera with W A S D."),
				free(KEY_REMAPPING, "up").withRequires("cameraRemap"),
				free(KEY_REMAPPING, "down").withRequires("cameraRemap"),
				free(KEY_REMAPPING, "left").withRequires("cameraRemap"),
				free(KEY_REMAPPING, "right").withRequires("cameraRemap"),
				// Number keys acting as F-keys surprises people; only on when the user asks for it
				fixed(KEY_REMAPPING, "fkeyRemap", false,
					"Number keys keep working as numbers. Change an F-key below and this switches on by itself."),
				free(KEY_REMAPPING, "f1").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "f2").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "f3").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "f4").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "f5").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "f6").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "f7").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "f8").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "f9").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "f10").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "f11").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "f12").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "esc").withRequires("fkeyRemap"),
				free(KEY_REMAPPING, "space"),
				free(KEY_REMAPPING, "control"),
				on(CAMERA, "Needed for right-click camera control."),
				fixed(CAMERA, "rightClickMovesCamera", true, "Hold right-click on empty ground to turn the camera, like most modern games."),
				free(CAMERA, "rightClickMenuBlocksCamera"),
				free(CAMERA, "middleClickMenu"),
				free(CAMERA, "outerLimit"),
				free(CAMERA, "inner"),
				free(CAMERA, "relaxCameraPitch"),
				free(CAMERA, "cameraSpeed"),
				free(CAMERA, "zoomIncrement"),
				free(CAMERA, "invertYaw"),
				free(CAMERA, "invertPitch"),
				free(CAMERA, "disableCameraShake")
			)),
			new Section("bank", "Bank", "Type your bank PIN with the keyboard and tune the bank screen.", Arrays.asList(
				on(BANK, "Needed for keyboard bank PIN and bank value."),
				fixed(BANK, "bankPinKeyboard", true, "Type your PIN with the number keys instead of clicking the buttons."),
				free(BANK, "searchKeybind"),
				free(BANK, "showHA"),
				free(BANK, "showExact"),
				free(BANK, "rightClickBankInventory"),
				free(BANK, "rightClickBankEquip"),
				free(BANK, "blockJagexAccountAd")
			)),
			new Section("qol", "Quality of life", "Built-in plugins that start switched off but most players turn on.", qolPlugins()),
			new Section("essentials", "Plugin Hub essentials", "Switch installed essentials on or off and tune them.", Arrays.asList(
				free(QUEST_HELPER, null),
				free(INVENTORY_SETUPS, null),
				free(SHORTEST_PATH, null),
				free(ITEM_CHARGES, null)
			))
		);
	}

	private static List<SettingDef> qolPlugins()
	{
		List<SettingDef> out = new ArrayList<>();
		for (String name : Arrays.asList(
			"Menu Entry Swapper",
			"Idle Notifier",
			"Status Bars",
			"Interact Highlight",
			"Anti Drag",
			"Virtual Levels"))
		{
			out.add(on(name, "Most players keep this on."));
		}
		out.add(on(LOGOUT_TIMER, "Stops the game logging you out after only 5 minutes of not touching it."));
		out.add(fixed(LOGOUT_TIMER, "idleTimeout", 30, "The longest the game allows, so a short break doesn't log you out."));
		return out;
	}

	static List<HubCategory> hubCategories()
	{
		BiPredicate<PerformanceTier, SystemSpecs> always = (t, s) -> true;
		BiPredicate<PerformanceTier, SystemSpecs> strongPc = (t, s) -> t == PerformanceTier.HIGH || t == PerformanceTier.ULTRA;

		return Arrays.asList(
			new HubCategory("Essentials", Arrays.asList(
				new HubPlugin("quest-helper", QUEST_HELPER, "Step-by-step guidance for every quest", always),
				new HubPlugin("inventory-setups", INVENTORY_SETUPS, "Save and load gear setups per activity", always),
				new HubPlugin("shortest-path", SHORTEST_PATH, "Draws a route to any spot on the world map", always),
				new HubPlugin("tictac7x-charges", ITEM_CHARGES, "Shows charges on many more items", always)
			)),
			new HubCategory("Tiles & markers", Arrays.asList(
				new HubPlugin("tile-packs", TILE_PACKS, "Ready-made tile setups, including raids and bosses", always),
				new HubPlugin("radius-markers", RADIUS_MARKERS, "NPC attack and wander ranges, with boss markers", always)
			)),
			new HubCategory("Graphics", Arrays.asList(
				new HubPlugin("117hd", HD, "Big graphics upgrade; replaces the GPU plugin and needs a strong graphics card", strongPc)
			))
		);
	}

	private static SettingDef def(String plugin, String key, Recommender recommender, String why)
	{
		return new SettingDef(plugin, key, recommender, why, null);
	}

	/**
	 * Recommend turning the plugin on.
	 */
	private static SettingDef on(String plugin, String why)
	{
		return def(plugin, null, (t, s) -> true, why);
	}

	private static SettingDef fixed(String plugin, String key, Object value, String why)
	{
		return def(plugin, key, (t, s) -> value, why);
	}

	/**
	 * Shown for manual control, with no recommendation.
	 */
	private static SettingDef free(String plugin, String key)
	{
		return def(plugin, key, (t, s) -> null, null);
	}

	private static SettingDef tier(String plugin, String key, Object low, Object balanced, Object high, Object ultra)
	{
		return def(plugin, key, (t, s) ->
		{
			switch (t)
			{
				case LOW:
					return low;
				case HIGH:
					return high;
				case ULTRA:
					return ultra;
				default:
					return balanced;
			}
		}, "Picked for your performance tier. Lower it if the game stutters, raise it if it runs smoothly.");
	}
}
