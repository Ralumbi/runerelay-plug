package com.runerelay;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ExternalPluginsChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "RuneRelay",
	description = "Recommends and applies the best settings and plugins for the computer you're on",
	tags = {"settings", "performance", "gpu", "fps", "setup", "qol", "recommend"}
)
public class RuneRelayPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private RuneRelayPanel panel;

	@Inject
	private GameKeybinds gameKeybinds;

	@Inject
	private ClientThread clientThread;

	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		navButton = NavigationButton.builder()
			.tooltip("RuneRelay")
			.icon(ImageUtil.loadImageResource(getClass(), "nav_icon.png"))
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		panel.requestRebuild();
		clientThread.invokeLater(this::refreshKeybinds);
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		panel.onConfigChanged(event);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (GameKeybinds.VARBITS.contains(event.getVarbitId()))
		{
			refreshKeybinds();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN || event.getGameState() == GameState.LOGIN_SCREEN)
		{
			refreshKeybinds();
		}
	}

	/**
	 * Runs on the client thread, where the game's keybinding settings can be read.
	 */
	private void refreshKeybinds()
	{
		if (gameKeybinds.refresh())
		{
			panel.requestRebuild();
		}
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		panel.onProfileChanged();
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		panel.requestRebuild();
	}

	@Subscribe
	public void onExternalPluginsChanged(ExternalPluginsChanged event)
	{
		panel.requestRebuild();
	}

	@Provides
	RuneRelayConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneRelayConfig.class);
	}
}
