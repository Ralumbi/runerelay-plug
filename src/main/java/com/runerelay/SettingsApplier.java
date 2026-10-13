package com.runerelay;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;

/**
 * Writes settings straight away, like RuneLite's own settings panel, and keeps an undo history.
 * All methods must be called on the Swing EDT, because plugins can only be started there.
 */
@Slf4j
@Singleton
class SettingsApplier
{
	private static final int MAX_UNDO = 30;
	/**
	 * Repeated edits of one setting within this window (spinner clicks, typing) undo as one step.
	 */
	private static final long MERGE_WINDOW_MS = 4000;

	@Value
	static class Change
	{
		SettingsModel.Setting setting;
		Object value;
	}

	@AllArgsConstructor
	private static class BackupEntry
	{
		/**
		 * Config group, or null for a plugin on/off entry.
		 */
		String group;
		/**
		 * Config key, or the plugin name for a plugin on/off entry.
		 */
		String key;
		/**
		 * Previous raw value; null means the config key was unset.
		 */
		String previous;
	}

	@AllArgsConstructor
	private static class Batch
	{
		String label;
		/**
		 * The setting id when the batch changed a single setting, for merging repeated edits.
		 */
		String singleId;
		long time;
		List<BackupEntry> entries;
	}

	private final ConfigManager configManager;
	private final PluginManager pluginManager;
	private final SettingsModel model;
	private final Deque<Batch> history = new ArrayDeque<>();

	/**
	 * True while RuneRelay itself is writing, so its own config events can be told apart.
	 */
	@Getter
	private boolean applying;

	@Inject
	SettingsApplier(ConfigManager configManager, PluginManager pluginManager, SettingsModel model)
	{
		this.configManager = configManager;
		this.pluginManager = pluginManager;
		this.model = model;
	}

	/**
	 * Write the changes now and remember the old values under {@code label} for undo.
	 */
	void apply(List<Change> changes, String label)
	{
		List<BackupEntry> backup = new ArrayList<>();
		applying = true;
		try
		{
			// Config first, so plugins switched on below start up with their new settings
			for (Change change : changes)
			{
				SettingsModel.Setting setting = change.getSetting();
				if (setting.isPluginToggle() || Objects.equals(change.getValue(), model.current(setting)))
				{
					continue;
				}
				String key = setting.getDef().getKey();
				backup.add(new BackupEntry(setting.getGroup(), key, configManager.getConfiguration(setting.getGroup(), key)));
				configManager.setConfiguration(setting.getGroup(), key, change.getValue());
			}
			// Switch plugins off before switching others on, so rivals never run at the same time
			for (boolean enable : new boolean[]{false, true})
			{
				for (Change change : changes)
				{
					SettingsModel.Setting setting = change.getSetting();
					if (!setting.isPluginToggle() || (Boolean) change.getValue() != enable)
					{
						continue;
					}
					Plugin plugin = setting.getPlugin();
					if (pluginManager.isPluginEnabled(plugin) != enable)
					{
						backup.add(new BackupEntry(null, plugin.getName(), String.valueOf(!enable)));
						setPluginEnabled(plugin, enable);
					}
				}
			}
		}
		finally
		{
			applying = false;
		}

		if (backup.isEmpty())
		{
			return;
		}
		long now = System.currentTimeMillis();
		String singleId = changes.size() == 1 ? changes.get(0).getSetting().id() : null;
		Batch last = history.peek();
		if (singleId != null && last != null && singleId.equals(last.singleId) && now - last.time < MERGE_WINDOW_MS)
		{
			// Keep the oldest values so undo goes back to before the first of these edits
			last.time = now;
			last.label = label;
			return;
		}
		history.push(new Batch(label, singleId, now, backup));
		while (history.size() > MAX_UNDO)
		{
			history.removeLast();
		}
	}

	boolean canUndo()
	{
		return !history.isEmpty();
	}

	String undoLabel()
	{
		Batch last = history.peek();
		return last == null ? null : last.label;
	}

	void undo()
	{
		Batch batch = history.poll();
		if (batch == null)
		{
			return;
		}
		List<BackupEntry> entries = new ArrayList<>(batch.entries);
		Collections.reverse(entries);
		applying = true;
		try
		{
			for (BackupEntry entry : entries)
			{
				if (entry.group == null)
				{
					Plugin plugin = model.findPlugin(entry.key);
					if (plugin != null)
					{
						setPluginEnabled(plugin, Boolean.parseBoolean(entry.previous));
					}
				}
				else if (entry.previous == null)
				{
					configManager.unsetConfiguration(entry.group, entry.key);
				}
				else
				{
					configManager.setConfiguration(entry.group, entry.key, entry.previous);
				}
			}
		}
		finally
		{
			applying = false;
		}
	}

	void setPluginEnabled(Plugin plugin, boolean enabled)
	{
		pluginManager.setPluginEnabled(plugin, enabled);
		try
		{
			if (enabled)
			{
				pluginManager.startPlugin(plugin);
			}
			else
			{
				pluginManager.stopPlugin(plugin);
			}
		}
		catch (PluginInstantiationException ex)
		{
			log.warn("Unable to {} plugin {}", enabled ? "start" : "stop", plugin.getName(), ex);
		}
	}
}
