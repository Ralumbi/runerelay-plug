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

	/**
	 * A raw write to another plugin's stored data, e.g. its list of enabled packs.
	 */
	@Value
	static class RawChange
	{
		String group;
		String key;
		/**
		 * New raw value, or null to unset.
		 */
		String value;
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
		/**
		 * Plugin that only reads this data when it starts, so it is restarted around writes; may be null.
		 */
		String restartPlugin;
	}

	private final ConfigManager configManager;
	private final PluginManager pluginManager;
	private final SettingsModel model;
	private final Deque<Batch> history = new ArrayDeque<>();

	/**
	 * True while RuneRelay itself is writing, so its own config events can be told apart.
	 */
	@Getter
	private volatile boolean applying;

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
		push(new Batch(label, singleId, now, backup, null));
	}

	/**
	 * Write another plugin's stored data. That plugin only reads it on start-up, so it is stopped
	 * while writing and started again afterwards. Undo does the same.
	 */
	void applyRaw(List<RawChange> changes, String label, Plugin restart)
	{
		List<BackupEntry> backup = new ArrayList<>();
		withStopped(restart, () ->
		{
			for (RawChange change : changes)
			{
				backup.add(new BackupEntry(change.getGroup(), change.getKey(),
					configManager.getConfiguration(change.getGroup(), change.getKey())));
				write(change.getGroup(), change.getKey(), change.getValue());
			}
		});
		push(new Batch(label, null, System.currentTimeMillis(), backup, restart == null ? null : restart.getName()));
	}

	private void push(Batch batch)
	{
		history.push(batch);
		while (history.size() > MAX_UNDO)
		{
			history.removeLast();
		}
	}

	private void write(String group, String key, String value)
	{
		if (value == null)
		{
			configManager.unsetConfiguration(group, key);
		}
		else
		{
			configManager.setConfiguration(group, key, value);
		}
	}

	/**
	 * Run {@code action} with the plugin stopped (if it was running), then start it again.
	 */
	private void withStopped(Plugin plugin, Runnable action)
	{
		boolean wasActive = plugin != null && pluginManager.isPluginActive(plugin);
		applying = true;
		try
		{
			if (wasActive)
			{
				pluginManager.stopPlugin(plugin);
			}
			action.run();
			if (wasActive)
			{
				pluginManager.startPlugin(plugin);
			}
		}
		catch (PluginInstantiationException ex)
		{
			log.warn("Unable to restart plugin {}", plugin.getName(), ex);
		}
		finally
		{
			applying = false;
		}
	}

	/**
	 * Forget undo history, e.g. after switching RuneLite profile, where it would undo the wrong settings.
	 */
	void clearHistory()
	{
		history.clear();
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
		Plugin restart = batch.restartPlugin == null ? null : model.findPlugin(batch.restartPlugin);
		withStopped(restart, () ->
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
				else
				{
					write(entry.group, entry.key, entry.previous);
				}
			}
		});
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
