package com.runerelay;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.IconTextField;

/**
 * The plugin list: one row per plugin, grouped by what it helps with, like RuneLite's own plugin list.
 */
class HomeView
{
	@Getter
	@RequiredArgsConstructor
	private enum Filter
	{
		ALL("All", "Every plugin"),
		SUGGESTED("Suggested", "Plugins with suggestions waiting"),
		ON("On", "Plugins that are switched on"),
		OFF("Off", "Plugins that are switched off"),
		NOT_INSTALLED("Install", "Plugin Hub plugins you can install");

		private final String label;
		private final String tooltip;
	}

	private static class Entry
	{
		String sectionId;
		JPanel row;
		String haystack;
		boolean installed;
		boolean enabled;
		int suggestions;
	}

	private static class SectionHeader
	{
		String id;
		JPanel header;
	}

	private final RuneRelayPanel panel;
	private final SettingsModel model;
	private final JPanel bar = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
	private final IconTextField search = new IconTextField();
	private final JPanel filterTabs = new JPanel(new BorderLayout());
	private Filter filter = Filter.ALL;
	private final List<Entry> entries = new ArrayList<>();
	private final List<SectionHeader> headers = new ArrayList<>();
	/**
	 * Holds only the rows currently shown. RuneLite's grid layout keeps space for hidden
	 * components, so filtering removes and re-adds rows instead of hiding them.
	 */
	private final JPanel list = new JPanel(new DynamicGridLayout(0, 1, 0, 5));
	private final JLabel empty = Ui.note("No plugins match. Try another search or filter.");

	HomeView(RuneRelayPanel panel)
	{
		this.panel = panel;
		this.model = panel.getModel();

		bar.setOpaque(false);
		search.setIcon(IconTextField.Icon.SEARCH);
		search.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		search.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		search.setPreferredSize(new Dimension(0, 30));
		search.setToolTipText("Search plugins by name or what they do");
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				applyFilter();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				applyFilter();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				applyFilter();
			}
		});
		bar.add(search);

		filterTabs.setOpaque(false);
		renderFilterTabs();
		bar.add(filterTabs);
		list.setOpaque(false);
	}

	JComponent bar()
	{
		return bar;
	}

	void build(JPanel content)
	{
		entries.clear();
		headers.clear();

		if (!panel.getConfig().guideDismissed())
		{
			content.add(guide());
		}
		JPanel conflicts = conflictCard();
		if (conflicts != null)
		{
			content.add(conflicts);
		}
		content.add(suggestionCard());

		for (Catalog.Section section : Catalog.sections())
		{
			List<Entry> rows = new ArrayList<>();
			for (String name : Catalog.pluginsIn(section))
			{
				Entry entry = entry(section, name);
				if (entry != null)
				{
					rows.add(entry);
				}
			}
			if (rows.isEmpty())
			{
				continue;
			}
			SectionHeader header = new SectionHeader();
			header.id = section.getId();
			header.header = sectionHeader(section, rows.size());
			headers.add(header);
			entries.addAll(rows);
		}

		content.add(list);
		applyFilter();
	}

	private void applyFilter()
	{
		String query = search.getText().trim().toLowerCase(Locale.ROOT);
		Filter selected = filter;
		list.removeAll();
		int shown = 0;
		for (SectionHeader header : headers)
		{
			boolean collapsed = query.isEmpty() && model.getCollapsedSections().contains(header.id);
			List<JPanel> matching = new ArrayList<>();
			for (Entry entry : entries)
			{
				if (entry.sectionId.equals(header.id)
					&& (query.isEmpty() || entry.haystack.contains(query)) && matches(entry, selected))
				{
					matching.add(entry.row);
				}
			}
			if (matching.isEmpty())
			{
				continue;
			}
			shown += matching.size();
			list.add(header.header);
			if (!collapsed)
			{
				matching.forEach(list::add);
			}
		}
		if (shown == 0)
		{
			list.add(empty);
		}
		list.revalidate();
		list.repaint();
		panel.relayout();
	}

	private void renderFilterTabs()
	{
		Filter[] filters = Filter.values();
		String[] labels = new String[filters.length];
		String[] tooltips = new String[filters.length];
		for (int i = 0; i < filters.length; i++)
		{
			labels[i] = filters[i].getLabel();
			tooltips[i] = filters[i].getTooltip();
		}
		filterTabs.removeAll();
		filterTabs.add(Ui.tabs(labels, tooltips, filter.ordinal(), index ->
		{
			filter = filters[index];
			renderFilterTabs();
			applyFilter();
		}), BorderLayout.CENTER);
		filterTabs.revalidate();
		filterTabs.repaint();
	}

	private static boolean matches(Entry entry, Filter filter)
	{
		switch (filter)
		{
			case SUGGESTED:
				return entry.suggestions > 0;
			case ON:
				return entry.installed && entry.enabled;
			case OFF:
				return entry.installed && !entry.enabled;
			case NOT_INSTALLED:
				return !entry.installed;
			default:
				return true;
		}
	}

	private Entry entry(Catalog.Section section, String name)
	{
		Catalog.HubPlugin hub = Catalog.hub(name);
		boolean installed = model.findPlugin(name) != null;
		if (!installed && hub == null)
		{
			return null;
		}
		Entry entry = new Entry();
		entry.sectionId = section.getId();
		entry.installed = installed;
		entry.enabled = installed && model.isPluginEnabled(name);
		entry.suggestions = installed ? model.suggestions(name).size() : 0;
		String info = Catalog.info(name);
		entry.haystack = (name + " " + (info == null ? "" : info) + " " + (hub == null ? "" : hub.getDescription())
			+ " " + section.getTitle()).toLowerCase(Locale.ROOT);
		entry.row = row(name, hub, entry);
		return entry;
	}

	private JPanel row(String name, Catalog.HubPlugin hub, Entry entry)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(6, 8, 6, 6));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText("Open " + name);

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		right.setOpaque(false);
		if (entry.installed)
		{
			ToggleSwitch toggle = new ToggleSwitch(entry.enabled);
			toggle.setToolTipText(entry.enabled ? "Switch " + name + " off" : "Switch " + name + " on");
			toggle.addActionListener(e -> panel.setPluginEnabled(name, toggle.isSelected()));
			right.add(toggle);
		}
		else
		{
			JButton install = new JButton("Install");
			install.setFont(FontManager.getRunescapeSmallFont());
			install.setFocusPainted(false);
			install.setToolTipText("Install " + name + " from the Plugin Hub, already set up by RuneRelay");
			install.addActionListener(e ->
			{
				install.setEnabled(false);
				install.setText("...");
				panel.install(hub);
			});
			right.add(install);
		}
		right.add(Ui.small("›", ColorScheme.LIGHT_GRAY_COLOR));
		// Size the text around whatever sits on the right, so nothing is pushed out of view
		int nameWidth = Ui.ROW_WIDTH - 6 - right.getPreferredSize().width;

		JPanel text = new JPanel(new DynamicGridLayout(0, 1, 0, 1));
		text.setOpaque(false);
		JLabel title = new JLabel(Ui.html(name, nameWidth));
		title.setForeground(entry.installed ? ColorScheme.TEXT_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
		text.add(title);

		StringBuilder meta = new StringBuilder(hub == null ? "Built-in" : "Plugin Hub");
		if (!entry.installed)
		{
			meta.append(" · not installed");
			if (hub.getRecommended().test(panel.getTier(), panel.getSpecs()))
			{
				meta.append(" · ").append(Ui.colored("★ recommended", ColorScheme.BRAND_ORANGE));
			}
		}
		if (entry.suggestions > 0)
		{
			meta.append(" · ").append(Ui.colored(entry.suggestions + (entry.suggestions == 1 ? " suggestion" : " suggestions"),
				ColorScheme.BRAND_ORANGE));
		}
		JLabel metaLabel = new JLabel(Ui.html(meta.toString(), nameWidth));
		metaLabel.setFont(FontManager.getRunescapeSmallFont());
		metaLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		text.add(metaLabel);
		row.add(text, BorderLayout.CENTER);

		row.add(right, BorderLayout.EAST);

		MouseAdapter open = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				panel.open(RuneRelayPanel.View.PLUGIN, name);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		};
		row.addMouseListener(open);
		text.addMouseListener(open);
		return row;
	}

	private JPanel sectionHeader(Catalog.Section section, int count)
	{
		boolean collapsed = model.getCollapsedSections().contains(section.getId());
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(8, 0, 1, 0));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		header.setToolTipText(section.getDescription());

		JLabel title = new JLabel((collapsed ? "▸ " : "▾ ") + section.getTitle());
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		header.add(title, BorderLayout.WEST);
		header.add(Ui.small(String.valueOf(count), ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.EAST);
		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (!model.getCollapsedSections().remove(section.getId()))
				{
					model.getCollapsedSections().add(section.getId());
				}
				boolean nowCollapsed = model.getCollapsedSections().contains(section.getId());
				title.setText((nowCollapsed ? "▸ " : "▾ ") + section.getTitle());
				applyFilter();
			}
		});
		return header;
	}

	private JPanel suggestionCard()
	{
		List<SettingsModel.Setting> suggestions = model.suggestions(null);
		if (suggestions.isEmpty())
		{
			JPanel card = Ui.card(Ui.GOOD);
			JLabel done = Ui.small(Ui.html("✓ Everything follows RuneRelay's suggestions for a "
				+ panel.getTier().getDisplayName().toLowerCase(Locale.ROOT) + " computer.", Ui.CARD_TEXT_WIDTH), Ui.GOOD);
			card.add(done);
			return card;
		}

		JPanel card = Ui.card(ColorScheme.BRAND_ORANGE);
		JLabel text = new JLabel(Ui.html("<b>" + suggestions.size() + (suggestions.size() == 1 ? " suggestion" : " suggestions")
			+ "</b> for your " + panel.getTier().getDisplayName().toLowerCase(Locale.ROOT) + " computer.", Ui.CARD_TEXT_WIDTH));
		text.setForeground(ColorScheme.TEXT_COLOR);
		card.add(text);

		JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
		buttons.setOpaque(false);
		JButton apply = new JButton("Apply all");
		apply.setForeground(ColorScheme.BRAND_ORANGE);
		apply.setToolTipText("Use every suggestion now; Undo puts it back");
		apply.addActionListener(e -> panel.applySuggestions(suggestions));
		JButton review = new JButton("Review");
		review.setToolTipText("See each suggestion and pick which to use");
		review.addActionListener(e -> panel.open(RuneRelayPanel.View.REVIEW, null));
		buttons.add(apply);
		buttons.add(review);
		card.add(buttons);
		return card;
	}

	private JPanel conflictCard()
	{
		List<List<String>> conflicts = model.activeConflicts();
		if (conflicts.isEmpty())
		{
			return null;
		}
		JPanel card = Ui.card(ColorScheme.PROGRESS_ERROR_COLOR);
		for (List<String> group : conflicts)
		{
			card.add(Ui.small(Ui.html("<b>⚠ " + String.join(" and ", group) + " are both on.</b> They clash; keep one:",
				Ui.CARD_TEXT_WIDTH), ColorScheme.TEXT_COLOR));
			for (String keep : group)
			{
				JButton button = new JButton("Keep " + keep);
				button.addActionListener(e ->
				{
					for (String other : group)
					{
						if (!other.equals(keep))
						{
							panel.setPluginEnabled(other, false);
						}
					}
				});
				card.add(button);
			}
		}
		return card;
	}

	private JPanel guide()
	{
		JPanel card = Ui.card(ColorScheme.BRAND_ORANGE);
		JLabel title = new JLabel("Getting started");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		card.add(title);
		card.add(Ui.note(
			"<b>1.</b> Check that <b>Your PC</b> above matches your computer.<br>"
				+ "<b>2.</b> Press <b>Apply all</b> to use RuneRelay's suggestions, or <b>Review</b> to pick.<br>"
				+ "<b>3.</b> Click a plugin to see what it does and change its settings. Changes save instantly.<br>"
				+ "<b>4.</b> Made a mistake? <b>Undo</b> at the bottom puts it back.<br>"
				+ "<b>5.</b> Plugins marked <b>not installed</b> can be installed with one click, already set up.",
			Ui.CARD_TEXT_WIDTH));
		JButton gotIt = new JButton("Got it");
		gotIt.addActionListener(e -> panel.getConfigManager().setConfiguration(RuneRelayConfig.GROUP, RuneRelayConfig.GUIDE_DISMISSED, true));
		card.add(gotIt);
		return card;
	}
}
