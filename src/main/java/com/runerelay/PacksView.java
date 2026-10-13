package com.runerelay;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Pick which of Tile Packs' boss, raid and wave packs are switched on.
 */
class PacksView
{
	private final RuneRelayPanel panel;
	private final MarkerPacks packs;
	private final Map<JCheckBox, MarkerPacks.TilePack> boxes = new LinkedHashMap<>();
	private JButton apply;

	PacksView(RuneRelayPanel panel)
	{
		this.panel = panel;
		this.packs = panel.getMarkerPacks();
	}

	JComponent bar()
	{
		JPanel bar = new JPanel(new BorderLayout(6, 0));
		bar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bar.setBorder(new EmptyBorder(6, 6, 6, 6));
		JLabel back = Ui.link("‹ Back", "Back to Tile Packs", () -> panel.open(RuneRelayPanel.View.PLUGIN, MarkerPacks.TILE_PACKS));
		back.setFont(FontManager.getRunescapeBoldFont());
		bar.add(back, BorderLayout.WEST);
		JLabel title = new JLabel("Boss & raid tiles");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.TEXT_COLOR);
		bar.add(title, BorderLayout.CENTER);
		return bar;
	}

	void build(JPanel content)
	{
		boxes.clear();
		Set<Integer> enabled = packs.enabledTilePacks();
		boolean firstTime = enabled.stream().noneMatch(MarkerPacks.bossTilePackIds()::contains);

		content.add(Ui.note("Tile setups made by the community and shipped with the Tile Packs plugin. Tick the "
			+ "fights you do. Where a boss has several methods, tick the one you use. The tiles appear when you "
			+ "enter that fight, and you can edit or hide them in Tile Packs' own panel."));

		apply = new JButton();
		apply.setFont(FontManager.getRunescapeBoldFont());
		apply.setForeground(ColorScheme.BRAND_ORANGE);
		apply.addActionListener(e ->
		{
			Set<Integer> wanted = boxes.entrySet().stream()
				.filter(entry -> entry.getKey().isSelected())
				.map(entry -> entry.getValue().getId())
				.collect(Collectors.toSet());
			packs.setBossTilePacks(wanted);
			panel.status(wanted.isEmpty()
				? "Switched off all boss & raid tile packs."
				: "Switched on " + wanted.size() + " boss & raid tile packs in Tile Packs.");
			panel.open(RuneRelayPanel.View.PLUGIN, MarkerPacks.TILE_PACKS);
		});
		content.add(apply);

		JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		links.setOpaque(false);
		links.add(Ui.link("Tick suggested", "Tick every fight that has a single pack", () -> setAll(true)));
		links.add(Ui.small("   ", ColorScheme.LIGHT_GRAY_COLOR));
		links.add(Ui.link("Untick all", "Untick every pack", () -> setAll(false)));
		content.add(links);

		for (MarkerPacks.EncounterGroup group : MarkerPacks.BOSS_TILE_PACKS)
		{
			content.add(Ui.title(group.getTitle()));
			for (MarkerPacks.Encounter encounter : group.getEncounters())
			{
				if (encounter.getPacks().size() == 1)
				{
					MarkerPacks.TilePack pack = encounter.getPacks().get(0);
					content.add(box(encounter.getName(), pack, firstTime ? pack.isPreferred() : enabled.contains(pack.getId()), 4));
					continue;
				}
				JLabel name = Ui.small(Ui.html(encounter.getName() + Ui.colored(" · pick the method you use",
					ColorScheme.LIGHT_GRAY_COLOR), Ui.CONTENT_WIDTH), ColorScheme.TEXT_COLOR);
				name.setBorder(new EmptyBorder(4, 4, 0, 0));
				content.add(name);
				for (MarkerPacks.TilePack pack : encounter.getPacks())
				{
					content.add(box(pack.getName(), pack, firstTime ? pack.isPreferred() : enabled.contains(pack.getId()), 22));
				}
			}
		}
		refresh();
	}

	private JCheckBox box(String label, MarkerPacks.TilePack pack, boolean selected, int indent)
	{
		JCheckBox box = new JCheckBox(Ui.html(Ui.escape(label), Ui.CONTENT_WIDTH - 30 - indent), selected);
		box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		box.setBorder(new EmptyBorder(4, indent, 4, 4));
		box.setFocusPainted(false);
		box.addActionListener(e -> refresh());
		boxes.put(box, pack);
		return box;
	}

	private void setAll(boolean suggested)
	{
		boxes.forEach((box, pack) -> box.setSelected(suggested && pack.isPreferred()));
		refresh();
	}

	private void refresh()
	{
		long count = boxes.keySet().stream().filter(JCheckBox::isSelected).count();
		apply.setText(count == 0 ? "Switch all off" : "Switch on " + count + (count == 1 ? " pack" : " packs"));
	}
}
