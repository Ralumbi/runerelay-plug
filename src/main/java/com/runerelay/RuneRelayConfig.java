package com.runerelay;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(RuneRelayConfig.GROUP)
public interface RuneRelayConfig extends Config
{
	String GROUP = "runerelay";

	@ConfigItem(
		keyName = "tier",
		name = "Performance tier",
		description = "Which hardware tier to recommend settings for. Auto-detect guesses from your CPU and memory."
	)
	default PerformanceTier tier()
	{
		return PerformanceTier.AUTO;
	}
}
