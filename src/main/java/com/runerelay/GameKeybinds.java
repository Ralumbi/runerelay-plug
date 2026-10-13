package com.runerelay;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarbitID;

/**
 * Which side-panel tab each key opens, as set in the game's own Keybinding settings, so key
 * remapping rows can say "F6 → Prayer" instead of just "F6".
 */
@Singleton
class GameKeybinds
{
	/**
	 * Game enum mapping a keybinding index to the key's name ("F1".."F12", "ESC").
	 */
	private static final int KEY_LABEL_ENUM = 1159;

	private static final Map<Integer, String> TABS = ImmutableMap.<Integer, String>builder()
		.put(VarbitID.STONE_COMBAT_KEY, "Combat options")
		.put(VarbitID.STONE_STATS_KEY, "Skills")
		.put(VarbitID.STONE_JOURNAL_KEY, "Quest list")
		.put(VarbitID.STONE_INV_KEY, "Inventory")
		.put(VarbitID.STONE_WORN_KEY, "Worn equipment")
		.put(VarbitID.STONE_PRAYER_KEY, "Prayer")
		.put(VarbitID.STONE_MAGIC_KEY, "Magic")
		.put(VarbitID.STONE_CLANCHAT_KEY, "Chat-channel")
		.put(VarbitID.STONE_FRIENDS_KEY, "Friends list")
		.put(VarbitID.STONE_ACCOUNT_KEY, "Account management")
		.put(VarbitID.STONE_LOGOUT_KEY, "Logout")
		.put(VarbitID.STONE_OPTIONS1_KEY, "Settings")
		.put(VarbitID.STONE_OPTIONS2_KEY, "Emotes")
		.put(VarbitID.STONE_MUSIC_KEY, "Music")
		.build();

	static final Set<Integer> VARBITS = ImmutableSet.<Integer>builder()
		.addAll(TABS.keySet())
		.add(VarbitID.STONE_LOGOUT_KEY_DESKTOP)
		.add(VarbitID.KEYBINDING_ESC_TO_CLOSE)
		.build();

	private final Client client;

	/**
	 * Key name to the tabs it opens; null while logged out, when the game hasn't told us.
	 */
	private volatile Map<String, List<String>> tabsByKey;
	private volatile boolean escCloses;

	@Inject
	GameKeybinds(Client client)
	{
		this.client = client;
	}

	/**
	 * Re-read the game's keybindings. Must be called on the client thread.
	 *
	 * @return true if anything changed
	 */
	boolean refresh()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			boolean changed = tabsByKey != null;
			tabsByKey = null;
			return changed;
		}

		EnumComposition keyNames = client.getEnum(KEY_LABEL_ENUM);
		Map<String, List<String>> out = new LinkedHashMap<>();
		for (Map.Entry<Integer, String> tab : TABS.entrySet())
		{
			int index = client.getVarbitValue(tab.getKey());
			if (index <= 0 && tab.getKey() == VarbitID.STONE_LOGOUT_KEY)
			{
				index = client.getVarbitValue(VarbitID.STONE_LOGOUT_KEY_DESKTOP);
			}
			String key = index > 0 && keyNames != null ? keyNames.getStringValue(index) : null;
			if (key != null && !key.trim().isEmpty())
			{
				out.computeIfAbsent(key.trim().toUpperCase(Locale.ROOT), k -> new ArrayList<>()).add(tab.getValue());
			}
		}
		boolean esc = client.getVarbitValue(VarbitID.KEYBINDING_ESC_TO_CLOSE) == 1;

		boolean changed = !out.equals(tabsByKey) || esc != escCloses;
		tabsByKey = out;
		escCloses = esc;
		return changed;
	}

	/**
	 * What a key does in the game, e.g. "Prayer", or null while that's unknown.
	 *
	 * @param key a key remapping config key: "f1".."f12" or "esc"
	 */
	String describe(String key)
	{
		Map<String, List<String>> tabs = tabsByKey;
		if (tabs == null)
		{
			return null;
		}
		String name = key.toUpperCase(Locale.ROOT);
		List<String> opens = tabs.get(name);
		if (opens != null)
		{
			return String.join(" & ", opens);
		}
		if (name.equals("ESC") && escCloses)
		{
			return "closes windows";
		}
		return "not used in game";
	}
}
