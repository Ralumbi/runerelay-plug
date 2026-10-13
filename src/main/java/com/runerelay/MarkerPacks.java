package com.runerelay;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Ready-made boss and raid markers: Tile Packs' own community packs, switched on for the player,
 * and Radius Markers packs (exports of markers measured in game) imported into Radius Markers.
 * RuneRelay never works out positions itself; it only switches on or imports existing packs.
 */
@Slf4j
@Singleton
class MarkerPacks
{
	static final String TILE_PACKS = "Tile Packs";
	static final String RADIUS_MARKERS = "Radius Markers";

	// Where Tile Packs and Radius Markers keep their data (from their source code)
	private static final String TILE_PACKS_GROUP = "tilePacks";
	private static final String TILE_PACKS_ENABLED_KEY = "packs";
	private static final String RADIUS_GROUP = "radiusmarkers";
	private static final String RADIUS_KEY = "markers";
	/**
	 * Fields Radius Markers requires on every marker; it drops markers missing any of them.
	 */
	private static final List<String> RADIUS_REQUIRED = Arrays.asList("name", "spawnColour", "wanderColour",
		"maxColour", "aggressionColour", "retreatInteractionColour", "attackColour", "attackType", "huntColour",
		"interactionColour");
	private static final String RADIUS_PACK_INDEX = "radius-packs/index.json";

	/**
	 * One Tile Packs pack, by the id Tile Packs gives it.
	 */
	@Value
	static class TilePack
	{
		int id;
		String name;
		/**
		 * Switched on by default; packs that are one of several kill methods aren't.
		 */
		boolean preferred;
	}

	/**
	 * A boss or raid room. Several packs mean several kill methods: pick one.
	 */
	@Value
	static class Encounter
	{
		String name;
		List<TilePack> packs;
	}

	@Value
	static class EncounterGroup
	{
		String title;
		List<Encounter> encounters;
	}

	/**
	 * A bundled Radius Markers pack, listed in {@value #RADIUS_PACK_INDEX}.
	 */
	@Value
	static class RadiusPack
	{
		String name;
		String file;
		String credit;
		String description;
		/**
		 * Added automatically when Radius Markers is installed through RuneRelay.
		 */
		boolean installWithPlugin;
		/**
		 * The pack relies on Radius Markers' "Include max range", which RuneRelay suggests switching off.
		 */
		boolean needsMaxRange;
	}

	/**
	 * Boss, raid and wave content packs that ship with Tile Packs.
	 */
	static final List<EncounterGroup> BOSS_TILE_PACKS = Arrays.asList(
		new EncounterGroup("Raids", Arrays.asList(
			one("CoX: Crabs", 5, "CoX crabs"),
			methods("CoX: Great Olm", pack(6, "Standard", true), pack(33, "4:1 melee", false),
				pack(87, "3 tick mage", false), pack(88, "5 tick mage", false)),
			one("ToB: Maiden", 14, "ToB Maiden"),
			one("ToB: Sotetseg", 15, "ToB Soteseg"),
			one("ToB: Xarpus", 17, "ToB Xarpus"),
			one("ToB: Verzik", 16, "ToB Verzik"),
			methods("ToA: Akkha", pack(2, "Butterfly", false), pack(24, "Butterfly 4 tick", false),
				pack(65, "Butterfly 3 tick", false), pack(70, "Butterfly 3 tick (low invocation)", false),
				pack(38, "Double Trouble", false), pack(50, "Green tick", false)),
			one("ToA: Ba-Ba", 56, "ToA Ba-ba"),
			one("ToA: Kephri", 36, "ToA Kephri Melee Trap"),
			one("ToA: Zebak", 58, "ToA Zebak"),
			one("ToA: Zebak puzzle", 35, "ToA Zebak Puzzle"),
			one("ToA: Wardens", 57, "ToA Wardens")
		)),
		new EncounterGroup("Bosses", Arrays.asList(
			one("Abyssal Sire", 1, "Abyssal Sire"),
			methods("Alchemical Hydra", pack(3, "Ranged", false), pack(47, "Melee", false)),
			one("Barrows", 42, "Barrows"),
			one("Chaos Elemental", 46, "Chaos Elemental"),
			one("Dagannoth Kings", 61, "DK's path"),
			methods("Doom of Mokhaiotl", pack(67, "Sidewalk", false), pack(89, "Deep delve", false)),
			methods("Duke Sucellus", pack(85, "Normal", true), pack(51, "Awakened", false)),
			methods("General Graardor", pack(8, "5:0", false), pack(9, "6:0 5 tick", false), pack(31, "6:0 4 tick", false),
				pack(32, "7:0", false), pack(10, "9:0", false), pack(60, "Altar door", false)),
			one("Grotesque Guardians", 27, "Grotesque Guardians"),
			one("Hueycoatl", 63, "Huey"),
			methods("K'ril Tsutsaroth", pack(43, "5:0", false), pack(13, "6:0", false), pack(44, "8:0", false),
				pack(59, "Altar door", false)),
			methods("Commander Zilyana", pack(28, "Bow of Faerdhinen", false), pack(29, "Twisted bow", false)),
			methods("The Leviathan", pack(86, "Normal", true), pack(52, "Awakened", false)),
			one("Moons of Peril", 48, "Moons of Peril"),
			one("Phantom Muspah", 39, "Muspah safespot"),
			one("The Gauntlet: Hunllef", 7, "Gauntlet Hunllef(both)"),
			one("Vardorvis (awakened)", 53, "Awakened Vardorvis"),
			one("The Whisperer (awakened)", 54, "Awakened Whisperer"),
			one("Yama", 66, "Yama No Fly"),
			one("Zulrah", 25, "Zulrah")
		)),
		new EncounterGroup("Waves", Arrays.asList(
			one("Fortis Colosseum", 55, "Colosseum"),
			one("The Inferno", 12, "Inferno")
		))
	);

	private final ConfigManager configManager;
	private final SettingsModel model;
	private final SettingsApplier applier;
	private final Gson gson;

	@Inject
	MarkerPacks(ConfigManager configManager, SettingsModel model, SettingsApplier applier, Gson gson)
	{
		this.configManager = configManager;
		this.model = model;
		this.applier = applier;
		this.gson = gson;
	}

	private static TilePack pack(int id, String name, boolean preferred)
	{
		return new TilePack(id, name, preferred);
	}

	private static Encounter one(String encounter, int id, String packName)
	{
		return new Encounter(encounter, Collections.singletonList(new TilePack(id, packName, true)));
	}

	private static Encounter methods(String encounter, TilePack... packs)
	{
		return new Encounter(encounter, Arrays.asList(packs));
	}

	// region Tile Packs

	static Set<Integer> bossTilePackIds()
	{
		Set<Integer> ids = new HashSet<>();
		for (EncounterGroup group : BOSS_TILE_PACKS)
		{
			for (Encounter encounter : group.getEncounters())
			{
				for (TilePack pack : encounter.getPacks())
				{
					ids.add(pack.getId());
				}
			}
		}
		return ids;
	}

	/**
	 * Pack ids currently switched on in Tile Packs.
	 */
	Set<Integer> enabledTilePacks()
	{
		String json = configManager.getConfiguration(TILE_PACKS_GROUP, TILE_PACKS_ENABLED_KEY);
		if (json == null || json.isEmpty())
		{
			return new LinkedHashSet<>();
		}
		try
		{
			List<Integer> ids = gson.fromJson(json, new TypeToken<List<Integer>>()
			{
			}.getType());
			return ids == null ? new LinkedHashSet<>() : new LinkedHashSet<>(ids);
		}
		catch (JsonParseException ex)
		{
			log.warn("Unreadable Tile Packs pack list", ex);
			return new LinkedHashSet<>();
		}
	}

	/**
	 * Switch exactly these boss and raid packs on in Tile Packs, leaving its other packs alone.
	 */
	void setBossTilePacks(Set<Integer> wanted)
	{
		Set<Integer> enabled = enabledTilePacks();
		enabled.removeAll(bossTilePackIds());
		enabled.addAll(wanted);
		String json = enabled.isEmpty() ? null : gson.toJson(new ArrayList<>(enabled));
		applier.applyRaw(Collections.singletonList(new SettingsApplier.RawChange(TILE_PACKS_GROUP, TILE_PACKS_ENABLED_KEY, json)),
			"Boss & raid tile packs", model.findPlugin(TILE_PACKS));
	}

	// endregion

	// region Radius Markers

	/**
	 * Packs bundled with RuneRelay. Each is an export from Radius Markers' copy button.
	 */
	List<RadiusPack> bundledRadiusPacks()
	{
		try (InputStream in = MarkerPacks.class.getResourceAsStream(RADIUS_PACK_INDEX))
		{
			if (in == null)
			{
				return Collections.emptyList();
			}
			List<RadiusPack> packs = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), new TypeToken<List<RadiusPack>>()
			{
			}.getType());
			return packs == null ? Collections.emptyList() : packs;
		}
		catch (IOException | JsonParseException ex)
		{
			log.warn("Unreadable radius pack index", ex);
			return Collections.emptyList();
		}
	}

	/**
	 * Import a bundled pack. @return how many markers were added
	 */
	int importBundledRadiusPack(RadiusPack pack)
	{
		try (InputStream in = MarkerPacks.class.getResourceAsStream("radius-packs/" + pack.getFile()))
		{
			if (in == null)
			{
				throw new IllegalArgumentException("Pack file " + pack.getFile() + " is missing.");
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return importRadiusMarkers(new JsonParser().parse(reader), pack.getName());
			}
		}
		catch (IOException | JsonParseException ex)
		{
			throw new IllegalArgumentException("Pack " + pack.getName() + " couldn't be read.", ex);
		}
	}

	/**
	 * Import markers exported with Radius Markers' copy button. @return how many markers were added
	 *
	 * @throws IllegalArgumentException if the text isn't a Radius Markers export
	 */
	int importRadiusMarkers(String exported)
	{
		try
		{
			return importRadiusMarkers(new JsonParser().parse(exported), "Radius markers from clipboard");
		}
		catch (JsonParseException ex)
		{
			throw new IllegalArgumentException("That text isn't a Radius Markers export.", ex);
		}
	}

	private int importRadiusMarkers(JsonElement exported, String label)
	{
		if (!exported.isJsonArray())
		{
			throw new IllegalArgumentException("That text isn't a Radius Markers export.");
		}

		JsonArray existing = currentRadiusMarkers();
		Set<Long> ids = new HashSet<>();
		for (JsonElement marker : existing)
		{
			ids.add(marker.getAsJsonObject().get("id").getAsLong());
		}

		int added = 0;
		for (JsonElement element : exported.getAsJsonArray())
		{
			if (!isValidMarker(element))
			{
				throw new IllegalArgumentException("That text isn't a Radius Markers export.");
			}
			if (ids.add(element.getAsJsonObject().get("id").getAsLong()))
			{
				existing.add(element);
				added++;
			}
		}
		if (added > 0)
		{
			applier.applyRaw(Collections.singletonList(new SettingsApplier.RawChange(RADIUS_GROUP, RADIUS_KEY, gson.toJson(existing))),
				label, model.findPlugin(RADIUS_MARKERS));
		}
		return added;
	}

	private JsonArray currentRadiusMarkers()
	{
		String json = configManager.getConfiguration(RADIUS_GROUP, RADIUS_KEY);
		if (json == null || json.isEmpty())
		{
			return new JsonArray();
		}
		JsonElement parsed = new JsonParser().parse(json);
		JsonArray valid = new JsonArray();
		if (parsed.isJsonArray())
		{
			for (JsonElement marker : parsed.getAsJsonArray())
			{
				if (isValidMarker(marker))
				{
					valid.add(marker);
				}
			}
		}
		return valid;
	}

	/**
	 * Same rules Radius Markers itself applies when loading markers.
	 */
	static boolean isValidMarker(JsonElement element)
	{
		if (!element.isJsonObject())
		{
			return false;
		}
		JsonObject marker = element.getAsJsonObject();
		JsonElement id = marker.get("id");
		if (id == null || !id.isJsonPrimitive() || !id.getAsJsonPrimitive().isNumber() || id.getAsLong() <= 0)
		{
			return false;
		}
		for (String field : RADIUS_REQUIRED)
		{
			if (marker.get(field) == null || marker.get(field).isJsonNull())
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Add the packs meant to come with Radius Markers when it was installed through RuneRelay.
	 *
	 * @return how many markers were added
	 */
	int installDefaultRadiusPacks()
	{
		int added = 0;
		for (RadiusPack pack : bundledRadiusPacks())
		{
			if (pack.isInstallWithPlugin())
			{
				added += importBundledRadiusPack(pack);
			}
		}
		return added;
	}

	/**
	 * Whether Radius Markers currently draws max ranges.
	 */
	boolean isMaxRangeShown()
	{
		return !"false".equals(configManager.getConfiguration(RADIUS_GROUP, "includeMaxRange"));
	}

	// endregion
}
