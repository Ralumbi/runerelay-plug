package com.runerelay;

import java.awt.Color;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Value;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ConfigSectionDescriptor;
import net.runelite.client.config.Keybind;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/**
 * Resolves catalog entries against the running client: what each setting is, its live value,
 * and what RuneRelay suggests for it. Must be used from the Swing EDT.
 */
@Singleton
class SettingsModel
{
	@Value
	static class Setting
	{
		Catalog.SettingDef def;
		Plugin plugin;
		/**
		 * Config group, or null for a plugin on/off setting.
		 */
		String group;
		/**
		 * Null for a plugin on/off setting.
		 */
		ConfigItemDescriptor item;
		String label;
		Type type;
		/**
		 * Validated suggestion, or null.
		 */
		Object recommended;

		String id()
		{
			return def.id();
		}

		boolean isPluginToggle()
		{
			return def.getKey() == null;
		}

		/**
		 * What the setting does, from the owning plugin's own config description.
		 */
		String getDescription()
		{
			if (item == null)
			{
				return "Switches " + plugin.getName() + " on or off.";
			}
			String description = item.getItem().description();
			return description.isEmpty() ? null : description;
		}
	}

	@Value
	static class ResolvedSection
	{
		Catalog.Section section;
		List<Setting> settings;
	}

	private static final Pattern REMAP_KEY = Pattern.compile("f([1-9]|1[0-2])|esc");

	private final ConfigManager configManager;
	private final PluginManager pluginManager;
	private final GameKeybinds gameKeybinds;

	/**
	 * Plugins whose page lists every editable setting, not just the curated ones.
	 */
	@Getter
	private final Set<String> showAllPlugins = new HashSet<>();
	/**
	 * Settings whose explanation is open.
	 */
	@Getter
	private final Set<String> openInfo = new HashSet<>();
	@Getter
	private final Set<String> collapsedSections = new HashSet<>();
	/**
	 * Suggestions the user unticked on the review page.
	 */
	@Getter
	private final Set<String> reviewExcluded = new HashSet<>();
	/**
	 * Plugins installed through RuneRelay whose suggested settings still need applying.
	 */
	@Getter
	private final Set<String> pendingSetup = new HashSet<>();

	private final Map<Plugin, ConfigDescriptor> descriptors = new HashMap<>();
	/**
	 * Settings from the last {@link #resolve}, by id.
	 */
	private final Map<String, Setting> resolvedById = new LinkedHashMap<>();

	@Inject
	SettingsModel(ConfigManager configManager, PluginManager pluginManager, GameKeybinds gameKeybinds)
	{
		this.configManager = configManager;
		this.pluginManager = pluginManager;
		this.gameKeybinds = gameKeybinds;
	}

	List<ResolvedSection> resolve(PerformanceTier tier, SystemSpecs specs)
	{
		resolvedById.clear();
		List<ResolvedSection> out = new ArrayList<>();
		for (Catalog.Section section : Catalog.sections())
		{
			List<Setting> settings = new ArrayList<>();
			List<Catalog.SettingDef> defs = section.getSettings();
			for (int i = 0; i < defs.size(); i++)
			{
				Catalog.SettingDef def = defs.get(i);
				// Never suggest anything for a plugin whose rival is on (e.g. GPU while 117 HD runs)
				boolean suppressed = isRivalEnabled(def.getPlugin());
				add(settings, resolve(def, tier, specs, suppressed));

				boolean lastOfPlugin = i + 1 == defs.size() || !defs.get(i + 1).getPlugin().equals(def.getPlugin());
				if (lastOfPlugin && showAllPlugins.contains(def.getPlugin()))
				{
					for (Catalog.SettingDef extra : uncuratedSettings(def.getPlugin(), defs))
					{
						add(settings, resolve(extra, tier, specs, false));
					}
				}
			}
			out.add(new ResolvedSection(section, settings));
		}
		return out;
	}

	private void add(List<Setting> settings, Setting setting)
	{
		if (setting != null)
		{
			settings.add(setting);
			resolvedById.put(setting.id(), setting);
		}
	}

	Setting byId(String id)
	{
		return resolvedById.get(id);
	}

	/**
	 * Resolved settings of one plugin, excluding its on/off switch.
	 */
	List<Setting> settingsOf(String pluginName)
	{
		List<Setting> out = new ArrayList<>();
		for (Setting setting : resolvedById.values())
		{
			if (setting.getDef().getPlugin().equals(pluginName) && !setting.isPluginToggle())
			{
				out.add(setting);
			}
		}
		return out;
	}

	/**
	 * Every editable, visible setting of a plugin that the catalog doesn't already list.
	 */
	private List<Catalog.SettingDef> uncuratedSettings(String pluginName, List<Catalog.SettingDef> curated)
	{
		List<Catalog.SettingDef> out = new ArrayList<>();
		Plugin plugin = findPlugin(pluginName);
		ConfigDescriptor descriptor = plugin == null ? null : descriptorFor(plugin);
		if (descriptor == null)
		{
			return out;
		}
		Set<String> covered = new HashSet<>();
		for (Catalog.SettingDef def : curated)
		{
			if (def.getPlugin().equals(pluginName) && def.getKey() != null)
			{
				covered.add(def.getKey());
			}
		}
		for (ConfigItemDescriptor item : descriptor.getItems())
		{
			if (!covered.contains(item.key()) && !item.getItem().hidden() && !item.getItem().secret() && isEditable(item.getType()))
			{
				out.add(new Catalog.SettingDef(pluginName, item.key(), (t, s) -> null, null, null));
			}
		}
		return out;
	}

	/**
	 * How many editable settings the plugin has that the catalog doesn't list.
	 */
	int extraSettingCount(String pluginName)
	{
		for (Catalog.Section section : Catalog.sections())
		{
			if (Catalog.pluginsIn(section).contains(pluginName))
			{
				return uncuratedSettings(pluginName, section.getSettings()).size();
			}
		}
		return 0;
	}

	/**
	 * Types {@link ValueEditors} can edit.
	 */
	static boolean isEditable(Type type)
	{
		if (type == boolean.class || type == int.class || type == double.class)
		{
			return true;
		}
		if (!(type instanceof Class))
		{
			return false;
		}
		Class<?> cls = (Class<?>) type;
		return cls == Boolean.class || cls == Integer.class || cls == Double.class || cls.isEnum()
			|| cls == Color.class || Keybind.class.isAssignableFrom(cls);
	}

	private Setting resolve(Catalog.SettingDef def, PerformanceTier tier, SystemSpecs specs, boolean suppressed)
	{
		Plugin plugin = findPlugin(def.getPlugin());
		if (plugin == null)
		{
			return null;
		}

		Object recommended = suppressed ? null : def.getRecommender().apply(tier, specs);
		if (def.getKey() == null)
		{
			return new Setting(def, plugin, null, null, "Enabled", boolean.class,
				recommended instanceof Boolean ? recommended : null);
		}

		ConfigDescriptor descriptor = descriptorFor(plugin);
		if (descriptor == null)
		{
			return null;
		}
		for (ConfigItemDescriptor item : descriptor.getItems())
		{
			if (item.key().equals(def.getKey()))
			{
				Object value = byName(item.getType(), recommended);
				return new Setting(def, plugin, descriptor.getGroup().value(), item,
					label(def, descriptor, item), item.getType(),
					isCompatible(item.getType(), value) ? value : null);
			}
		}
		return null;
	}

	private ConfigDescriptor descriptorFor(Plugin plugin)
	{
		return descriptors.computeIfAbsent(plugin, p ->
		{
			Config proxy = pluginManager.getPluginConfigProxy(p);
			return proxy == null ? null : configManager.getConfigDescriptor(proxy);
		});
	}

	/**
	 * Prefix the item's name with its config section, since names like "Highlight color" repeat.
	 * Key remapping's F-key rows say which tab the key opens in the game instead.
	 */
	private String label(Catalog.SettingDef def, ConfigDescriptor descriptor, ConfigItemDescriptor item)
	{
		if (Catalog.isKeyRemapper(def.getPlugin()) && REMAP_KEY.matcher(def.getKey()).matches())
		{
			String key = def.getKey().equals("esc") ? "Esc" : def.getKey().toUpperCase(Locale.ROOT);
			String does = gameKeybinds.describe(def.getKey());
			return does == null ? key + " key (log in to see its tab)" : key + " key → " + does;
		}

		String sectionKey = item.getItem().section();
		for (ConfigSectionDescriptor section : descriptor.getSections())
		{
			if (section.key().equals(sectionKey))
			{
				return section.name() + ": " + item.name();
			}
		}
		return item.name();
	}

	/**
	 * Hub plugins' enums can't be referenced at compile time, so the catalog names their constants.
	 */
	private static Object byName(Type type, Object value)
	{
		if (value instanceof String && type instanceof Class && ((Class<?>) type).isEnum())
		{
			for (Object constant : ((Class<?>) type).getEnumConstants())
			{
				if (((Enum<?>) constant).name().equals(value))
				{
					return constant;
				}
			}
		}
		return value;
	}

	private static boolean isCompatible(Type type, Object value)
	{
		if (value == null)
		{
			return false;
		}
		if (type == int.class || type == Integer.class)
		{
			return value instanceof Integer;
		}
		if (type == boolean.class || type == Boolean.class)
		{
			return value instanceof Boolean;
		}
		if (type == double.class || type == Double.class)
		{
			return value instanceof Double;
		}
		return type instanceof Class && ((Class<?>) type).isInstance(value);
	}

	Plugin findPlugin(String name)
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (plugin.getName().equals(name))
			{
				return plugin;
			}
		}
		return null;
	}

	boolean isPluginEnabled(String name)
	{
		Plugin plugin = findPlugin(name);
		return plugin != null && pluginManager.isPluginEnabled(plugin);
	}

	boolean isRivalEnabled(String pluginName)
	{
		return !enabledRivals(pluginName).isEmpty();
	}

	List<String> enabledRivals(String pluginName)
	{
		List<String> out = new ArrayList<>();
		for (String rival : Catalog.rivals(pluginName))
		{
			if (isPluginEnabled(rival))
			{
				out.add(rival);
			}
		}
		return out;
	}

	/**
	 * Groups of plugins that are on together although they clash.
	 */
	List<List<String>> activeConflicts()
	{
		List<List<String>> out = new ArrayList<>();
		for (List<String> group : Catalog.exclusiveGroups())
		{
			List<String> on = new ArrayList<>();
			for (String name : group)
			{
				if (isPluginEnabled(name))
				{
					on.add(name);
				}
			}
			if (on.size() > 1)
			{
				out.add(on);
			}
		}
		return out;
	}

	Object current(Setting setting)
	{
		if (setting.isPluginToggle())
		{
			return pluginManager.isPluginEnabled(setting.getPlugin());
		}
		return configManager.getConfiguration(setting.getGroup(), setting.getDef().getKey(), setting.getType());
	}

	/**
	 * Whether RuneRelay suggests a different value from the live one.
	 */
	boolean isSuggested(Setting setting)
	{
		return setting.getRecommended() != null && !Objects.equals(setting.getRecommended(), current(setting));
	}

	/**
	 * Open suggestions, for one plugin or (with null) all of them.
	 */
	List<Setting> suggestions(String pluginName)
	{
		List<Setting> out = new ArrayList<>();
		for (Setting setting : resolvedById.values())
		{
			if ((pluginName == null || setting.getDef().getPlugin().equals(pluginName)) && isSuggested(setting))
			{
				out.add(setting);
			}
		}
		return out;
	}

	/**
	 * Changes that take the given suggestions, plus whatever they depend on.
	 */
	List<SettingsApplier.Change> planSuggestions(List<Setting> settings)
	{
		Map<String, SettingsApplier.Change> out = new LinkedHashMap<>();
		for (Setting setting : settings)
		{
			for (SettingsApplier.Change change : plan(setting, setting.getRecommended()))
			{
				out.putIfAbsent(change.getSetting().id(), change);
			}
		}
		return new ArrayList<>(out.values());
	}

	/**
	 * A change plus what it needs to take effect: the setting's master switch, the plugin itself,
	 * and switching off any rival when a plugin goes on.
	 */
	List<SettingsApplier.Change> plan(Setting setting, Object value)
	{
		List<SettingsApplier.Change> out = new ArrayList<>();
		out.add(new SettingsApplier.Change(setting, value));
		String plugin = setting.getDef().getPlugin();

		boolean turningOn;
		if (setting.isPluginToggle())
		{
			turningOn = Boolean.TRUE.equals(value);
		}
		else
		{
			String requires = setting.getDef().getRequires();
			if (requires != null)
			{
				switchOn(out, plugin + "." + requires);
			}
			turningOn = switchOn(out, plugin + "#enabled");
		}

		if (turningOn)
		{
			for (String rival : Catalog.rivals(plugin))
			{
				Setting toggle = resolvedById.get(rival + "#enabled");
				if (toggle != null && Boolean.TRUE.equals(current(toggle)))
				{
					out.add(new SettingsApplier.Change(toggle, false));
				}
			}
		}
		return out;
	}

	private boolean switchOn(List<SettingsApplier.Change> out, String id)
	{
		Setting dependency = resolvedById.get(id);
		if (dependency == null || Boolean.TRUE.equals(current(dependency)))
		{
			return false;
		}
		out.add(new SettingsApplier.Change(dependency, true));
		return true;
	}

	/**
	 * Suggested settings for plugins that were just installed through RuneRelay and have loaded.
	 * Plugins are cleared from {@link #pendingSetup} once found.
	 */
	List<SettingsApplier.Change> takeSetupChanges()
	{
		List<SettingsApplier.Change> changes = new ArrayList<>();
		Set<String> found = new HashSet<>();
		for (String name : pendingSetup)
		{
			if (findPlugin(name) == null)
			{
				continue;
			}
			found.add(name);
			for (Setting setting : suggestions(name))
			{
				if (!setting.isPluginToggle())
				{
					changes.add(new SettingsApplier.Change(setting, setting.getRecommended()));
				}
			}
		}
		pendingSetup.removeAll(found);
		return changes;
	}

	Set<String> watchedGroups()
	{
		Set<String> groups = new HashSet<>();
		for (Setting setting : resolvedById.values())
		{
			if (setting.getGroup() != null)
			{
				groups.add(setting.getGroup());
			}
		}
		return groups;
	}

	static String format(Object value)
	{
		if (value == null)
		{
			return "unset";
		}
		if (value instanceof Boolean)
		{
			return (Boolean) value ? "On" : "Off";
		}
		if (value instanceof Color)
		{
			Color c = (Color) value;
			String hex = String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
			return c.getAlpha() == 255 ? hex : hex + String.format(" (%d%%)", Math.round(c.getAlpha() / 2.55));
		}
		if (value instanceof Double)
		{
			double d = (Double) value;
			return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
		}
		return value.toString();
	}
}
