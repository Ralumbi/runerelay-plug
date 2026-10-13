package com.runerelay;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CatalogTest
{
	private static final SystemSpecs SPECS = new SystemSpecs("Linux", 8, 16384, 1024, 3840, 2160, 3840, 144);

	@Test
	public void settingIdsAreUnique()
	{
		Set<String> ids = new HashSet<>();
		for (Catalog.Section section : Catalog.sections())
		{
			for (Catalog.SettingDef def : section.getSettings())
			{
				assertTrue("Duplicate setting " + def.id(), ids.add(def.id()));
			}
		}
	}

	@Test
	public void everyHubPluginHasARowAndInfo()
	{
		Set<String> listed = new HashSet<>();
		for (Catalog.Section section : Catalog.sections())
		{
			listed.addAll(Catalog.pluginsIn(section));
		}
		for (Catalog.HubCategory category : Catalog.hubCategories())
		{
			for (Catalog.HubPlugin hub : category.getPlugins())
			{
				assertTrue(hub.getPluginName() + " has no row in any section", listed.contains(hub.getPluginName()));
				assertNotNull(hub.getPluginName() + " has no info text", Catalog.info(hub.getPluginName()));
			}
		}
	}

	@Test
	public void everyListedPluginHasInfo()
	{
		for (Catalog.Section section : Catalog.sections())
		{
			for (String plugin : Catalog.pluginsIn(section))
			{
				assertNotNull(plugin + " has no info text", Catalog.info(plugin));
			}
		}
	}

	@Test
	public void rivalsAreMutual()
	{
		for (List<String> group : Catalog.exclusiveGroups())
		{
			for (String plugin : group)
			{
				for (String rival : Catalog.rivals(plugin))
				{
					assertTrue(rival + " should list " + plugin + " as a rival", Catalog.rivals(rival).contains(plugin));
				}
			}
		}
	}

	@Test
	public void recommendersWorkForEveryTier()
	{
		for (PerformanceTier tier : PerformanceTier.values())
		{
			if (tier == PerformanceTier.AUTO)
			{
				continue;
			}
			for (Catalog.Section section : Catalog.sections())
			{
				for (Catalog.SettingDef def : section.getSettings())
				{
					Object value = def.getRecommender().apply(tier, SPECS);
					if (def.getKey() == null && value != null)
					{
						assertEquals("Plugin switch " + def.id() + " must suggest a boolean", Boolean.class, value.getClass());
					}
				}
			}
		}
	}

	@Test
	public void logoutTimerIsSuggestedAtThirtyMinutes()
	{
		Catalog.SettingDef timeout = null;
		for (Catalog.Section section : Catalog.sections())
		{
			for (Catalog.SettingDef def : section.getSettings())
			{
				if (def.id().equals("Logout Timer.idleTimeout"))
				{
					timeout = def;
				}
			}
		}
		assertNotNull(timeout);
		assertEquals(30, timeout.getRecommender().apply(PerformanceTier.BALANCED, SPECS));
	}

	@Test
	public void radiusMarkersSpawnRangesAreSuggestedOff()
	{
		for (String key : new String[]{"includeWanderRange", "includeMaxRange", "includeAggressionRange"})
		{
			Catalog.SettingDef found = null;
			for (Catalog.Section section : Catalog.sections())
			{
				for (Catalog.SettingDef def : section.getSettings())
				{
					if (def.id().equals("Radius Markers." + key))
					{
						found = def;
					}
				}
			}
			assertNotNull(key, found);
			assertEquals(key, false, found.getRecommender().apply(PerformanceTier.BALANCED, SPECS));
		}
	}

	@Test
	public void tierDetection()
	{
		assertEquals(PerformanceTier.LOW, PerformanceTier.detect(new SystemSpecs("x", 4, 16384, 0, 0, 0, 0, 0)));
		assertEquals(PerformanceTier.LOW, PerformanceTier.detect(new SystemSpecs("x", 8, 4096, 0, 0, 0, 0, 0)));
		assertEquals(PerformanceTier.BALANCED, PerformanceTier.detect(new SystemSpecs("x", 8, 16384, 0, 0, 0, 0, 0)));
		assertEquals(PerformanceTier.HIGH, PerformanceTier.detect(new SystemSpecs("x", 16, 32768, 0, 0, 0, 0, 0)));
	}
}
