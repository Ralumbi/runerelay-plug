package com.runerelay;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "RuneRelay",
	description = "Recommends and applies the best settings and plugins for the computer you're on",
	tags = {"settings", "performance", "gpu", "fps", "setup", "qol", "recommend"}
)
public class RuneRelayPlugin extends Plugin
{
	@Inject
	private Client client;

	@Override
	protected void startUp() throws Exception
	{
		SystemSpecs specs = SystemSpecs.detect(client.getCanvas());
		log.debug("RuneRelay started on {} ({} tier)", specs, PerformanceTier.detect(specs));
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("RuneRelay stopped!");
	}

	@Provides
	RuneRelayConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneRelayConfig.class);
	}
}
