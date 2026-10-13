package com.runerelay;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MarkerPacksTest
{
	@Test
	public void bundledRadiusPacksAreValidRadiusMarkersExports() throws Exception
	{
		List<MarkerPacks.RadiusPack> packs;
		try (InputStream in = MarkerPacks.class.getResourceAsStream("radius-packs/index.json"))
		{
			assertNotNull("radius-packs/index.json is missing", in);
			packs = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
				new TypeToken<List<MarkerPacks.RadiusPack>>()
				{
				}.getType());
		}
		assertFalse(packs.isEmpty());

		Set<Long> ids = new HashSet<>();
		boolean anyInstalledWithPlugin = false;
		for (MarkerPacks.RadiusPack pack : packs)
		{
			assertNotNull(pack.getName());
			assertNotNull(pack.getName() + " needs a credit", pack.getCredit());
			anyInstalledWithPlugin |= pack.isInstallWithPlugin();
			try (InputStream in = MarkerPacks.class.getResourceAsStream("radius-packs/" + pack.getFile()))
			{
				assertNotNull(pack.getFile() + " is missing", in);
				JsonElement json = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
				assertTrue(json.isJsonArray());
				JsonArray markers = json.getAsJsonArray();
				assertTrue(markers.size() > 0);
				for (JsonElement marker : markers)
				{
					assertTrue("Invalid marker in " + pack.getFile() + ": " + marker, MarkerPacks.isValidMarker(marker));
					long id = marker.getAsJsonObject().get("id").getAsLong();
					assertTrue("Marker id " + id + " is used twice", ids.add(id));
				}
			}
		}
		assertTrue("Some pack should come with Radius Markers on install", anyInstalledWithPlugin);
	}

	@Test
	public void rejectsTextThatIsNotAMarker()
	{
		assertFalse(MarkerPacks.isValidMarker(new JsonParser().parse("{\"id\": 5, \"name\": \"no colours\"}")));
		assertFalse(MarkerPacks.isValidMarker(new JsonParser().parse("\"hello\"")));
	}

	@Test
	public void bossTilePackIdsAreUnique()
	{
		int count = 0;
		for (MarkerPacks.EncounterGroup group : MarkerPacks.BOSS_TILE_PACKS)
		{
			for (MarkerPacks.Encounter encounter : group.getEncounters())
			{
				count += encounter.getPacks().size();
			}
		}
		assertTrue(count == MarkerPacks.bossTilePackIds().size());
	}
}
