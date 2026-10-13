package com.runerelay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * One plugin's page: what it does, its switch, and its settings, like RuneLite's own plugin config page.
 */
class PluginView
{
	private final RuneRelayPanel panel;
	private final SettingsModel model;

	PluginView(RuneRelayPanel panel)
	{
		this.panel = panel;
		this.model = panel.getModel();
	}

	JComponent bar(String name)
	{
		JPanel bar = new JPanel(new BorderLayout(6, 0));
		bar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bar.setBorder(new EmptyBorder(6, 6, 6, 6));

		JLabel back = Ui.link("‹ Back", "Back to all plugins", () -> panel.open(RuneRelayPanel.View.HOME, null));
		back.setFont(FontManager.getRunescapeBoldFont());
		bar.add(back, BorderLayout.WEST);

		int titleWidth = Ui.CONTENT_WIDTH - 12 - 12 - back.getPreferredSize().width;
		if (model.findPlugin(name) != null)
		{
			boolean on = model.isPluginEnabled(name);
			ToggleSwitch toggle = new ToggleSwitch(on);
			toggle.setToolTipText(on ? "Switch " + name + " off" : "Switch " + name + " on");
			toggle.addActionListener(e -> panel.setPluginEnabled(name, toggle.isSelected()));
			bar.add(toggle, BorderLayout.EAST);
			titleWidth -= toggle.getPreferredSize().width;
		}

		JLabel title = new JLabel(Ui.html(name, titleWidth));
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		bar.add(title, BorderLayout.CENTER);
		return bar;
	}

	void build(JPanel content, String name)
	{
		Catalog.HubPlugin hub = Catalog.hub(name);
		boolean installed = model.findPlugin(name) != null;

		content.add(Ui.small(hub == null ? "Built into RuneLite" : "From the Plugin Hub", ColorScheme.LIGHT_GRAY_COLOR));
		String info = Catalog.info(name);
		if (info != null)
		{
			content.add(Ui.infoBox(info, Ui.INFO_WIDTH));
		}
		List<String> rivals = Catalog.rivals(name);
		if (!rivals.isEmpty())
		{
			content.add(Ui.small(Ui.html("⚠ Can't be on together with " + String.join(" or ", rivals)
				+ ". Switching this on switches the other off.", Ui.CONTENT_WIDTH), Ui.WARN));
		}

		if (!installed)
		{
			JPanel card = Ui.card(ColorScheme.BRAND_ORANGE);
			card.add(Ui.note("Not installed yet. RuneRelay installs it and applies its suggested settings for you.", Ui.CARD_TEXT_WIDTH));
			JButton install = new JButton("Install " + name);
			install.setForeground(ColorScheme.BRAND_ORANGE);
			install.addActionListener(e ->
			{
				install.setEnabled(false);
				install.setText("Installing...");
				panel.install(hub);
			});
			card.add(install);
			content.add(card);
			return;
		}

		if (!model.isPluginEnabled(name))
		{
			content.add(Ui.small(Ui.html("Switched off. Changing a setting below switches it on.", Ui.CONTENT_WIDTH), Ui.WARN));
		}

		List<SettingsModel.Setting> suggestions = model.suggestions(name);
		suggestions.removeIf(SettingsModel.Setting::isPluginToggle);
		if (!suggestions.isEmpty())
		{
			JPanel card = Ui.card(ColorScheme.BRAND_ORANGE);
			card.add(Ui.small(Ui.html("<b>" + suggestions.size() + (suggestions.size() == 1 ? " suggestion" : " suggestions")
				+ "</b> for this plugin, marked ★ below.", Ui.CARD_TEXT_WIDTH), ColorScheme.TEXT_COLOR));
			JButton useAll = new JButton("Use " + (suggestions.size() == 1 ? "it" : "all " + suggestions.size()));
			useAll.setForeground(ColorScheme.BRAND_ORANGE);
			useAll.addActionListener(e -> panel.applySuggestions(suggestions));
			card.add(useAll);
			content.add(card);
		}

		JPanel settingsHeader = new JPanel(new BorderLayout());
		settingsHeader.setOpaque(false);
		settingsHeader.add(Ui.title("Settings"), BorderLayout.WEST);
		int extra = model.extraSettingCount(name);
		if (extra > 0)
		{
			boolean all = model.getShowAllPlugins().contains(name);
			JLabel more = Ui.link(all ? "Show fewer" : "Show all (+" + extra + ")",
				all ? "Only show the settings RuneRelay picked" : "Show every setting of " + name, () ->
				{
					if (!model.getShowAllPlugins().remove(name))
					{
						model.getShowAllPlugins().add(name);
					}
					panel.requestRebuild();
				});
			more.setBorder(new EmptyBorder(8, 0, 2, 0));
			settingsHeader.add(more, BorderLayout.EAST);
		}
		content.add(settingsHeader);

		List<SettingsModel.Setting> settings = model.settingsOf(name);
		if (settings.isEmpty())
		{
			content.add(Ui.note(extra > 0
				? "Press \"Show all\" to see this plugin's settings."
				: "Nothing to set here; this plugin works on its own. Open its side panel to use it."));
		}
		for (SettingsModel.Setting setting : settings)
		{
			content.add(new SettingRow(setting));
		}

		if (hub != null)
		{
			JButton remove = new JButton("Remove from RuneLite");
			remove.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
			remove.addActionListener(e -> panel.remove(hub));
			JPanel wrap = new JPanel(new BorderLayout());
			wrap.setOpaque(false);
			wrap.setBorder(new EmptyBorder(12, 0, 0, 0));
			wrap.add(remove, BorderLayout.CENTER);
			content.add(wrap);
		}
	}

	/**
	 * One setting: name and control on a line, then whether it matches RuneRelay's suggestion.
	 */
	private class SettingRow extends JPanel
	{
		private final SettingsModel.Setting setting;
		private final JLabel status = new JLabel();
		private final JLabel use;

		SettingRow(SettingsModel.Setting setting)
		{
			this.setting = setting;
			setLayout(new DynamicGridLayout(0, 1, 0, 3));
			setBackground(ColorScheme.DARKER_GRAY_COLOR);
			setBorder(new EmptyBorder(6, 8, 6, 6));

			JPanel top = new JPanel(new BorderLayout(6, 0));
			top.setOpaque(false);
			JComponent editor = ValueEditors.create(setting, model.current(setting),
				value -> panel.change(setting, value, this::refresh), panel.getColorPickerManager());
			top.add(editor, BorderLayout.EAST);
			// Size the name around the control, so the control is never pushed out of view
			JLabel label = new JLabel(Ui.html(Ui.escape(setting.getLabel()), Ui.ROW_WIDTH - 6 - editor.getPreferredSize().width));
			label.setForeground(Color.WHITE);
			top.add(label, BorderLayout.CENTER);
			add(top);

			String infoId = setting.id();
			boolean infoOpen = model.getOpenInfo().contains(infoId);
			JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			meta.setOpaque(false);
			meta.add(Ui.link(infoOpen ? "ⓘ less   " : "ⓘ what's this?   ", "Explain this setting", () ->
			{
				if (!model.getOpenInfo().remove(infoId))
				{
					model.getOpenInfo().add(infoId);
				}
				panel.requestRebuild();
			}));
			status.setFont(FontManager.getRunescapeSmallFont());
			meta.add(status);
			use = Ui.link("  Use", "Use the suggested value", () ->
				panel.change(setting, setting.getRecommended(), panel::requestRebuild));
			meta.add(use);
			add(meta);

			if (infoOpen)
			{
				add(Ui.infoBox(explain(setting), Ui.ROW_WIDTH - 14));
			}
			refresh();
		}

		void refresh()
		{
			Object recommended = setting.getRecommended();
			if (recommended == null)
			{
				status.setText("");
				use.setVisible(false);
			}
			else if (model.isSuggested(setting))
			{
				status.setText("★ Suggested: " + SettingsModel.format(recommended));
				status.setForeground(ColorScheme.BRAND_ORANGE);
				use.setVisible(true);
			}
			else
			{
				status.setText("✓ Suggested");
				status.setForeground(Ui.GOOD);
				use.setVisible(false);
			}
		}
	}

	private String explain(SettingsModel.Setting setting)
	{
		StringBuilder sb = new StringBuilder();
		String description = setting.getDescription();
		sb.append(description != null ? Ui.escape(description) : "No description from the plugin.");
		if (setting.getRecommended() != null)
		{
			if (setting.getDef().getWhy() != null)
			{
				sb.append("<br><br><b>Why RuneRelay suggests it:</b> ").append(setting.getDef().getWhy());
			}
			sb.append("<br><b>Suggested for a ").append(panel.getTier().getDisplayName().toLowerCase())
				.append(" computer:</b> ").append(Ui.escape(SettingsModel.format(setting.getRecommended())));
		}
		return sb.toString();
	}
}
