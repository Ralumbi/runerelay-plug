package com.runerelay;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(RuneRelayConfig.GROUP)
public interface RuneRelayConfig extends Config
{
	String GROUP = "runerelay";
	String GUIDE_DISMISSED = "guideDismissed";

	@ConfigItem(
		keyName = "tier",
		name = "Performance tier",
		description = "Which hardware tier to recommend settings for. Auto-detect guesses from your CPU and memory."
	)
	default PerformanceTier tier()
	{
		return PerformanceTier.AUTO;
	}

	@ConfigItem(
		keyName = GUIDE_DISMISSED,
		name = "Hide getting started",
		description = "Hide the getting started guide at the top of the panel.",
		hidden = true
	)
	default boolean guideDismissed()
	{
		return false;
	}
}
