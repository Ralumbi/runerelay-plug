package com.runerelay;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Every open suggestion with a tick box, so players can pick before applying.
 */
class ReviewView
{
	private final RuneRelayPanel panel;
	private final SettingsModel model;
	private final Map<JCheckBox, SettingsModel.Setting> boxes = new LinkedHashMap<>();
	private JButton apply;

	ReviewView(RuneRelayPanel panel)
	{
		this.panel = panel;
		this.model = panel.getModel();
	}

	JComponent bar()
	{
		JPanel bar = new JPanel(new BorderLayout(6, 0));
		bar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bar.setBorder(new EmptyBorder(6, 6, 6, 6));
		JLabel back = Ui.link("‹ Back", "Back to all plugins", () -> panel.open(RuneRelayPanel.View.HOME, null));
		back.setFont(FontManager.getRunescapeBoldFont());
		bar.add(back, BorderLayout.WEST);
		JLabel title = new JLabel("Suggested changes");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.TEXT_COLOR);
		bar.add(title, BorderLayout.CENTER);
		return bar;
	}

	void build(JPanel content)
	{
		boxes.clear();
		List<SettingsModel.Setting> suggestions = model.suggestions(null);
		if (suggestions.isEmpty())
		{
			content.add(Ui.note("Nothing left to suggest. Everything follows RuneRelay's suggestions."));
			return;
		}

		content.add(Ui.note("Untick anything you want to keep as it is, then apply. Undo puts it all back."));

		apply = new JButton();
		apply.setFont(FontManager.getRunescapeBoldFont());
		apply.setForeground(ColorScheme.BRAND_ORANGE);
		apply.addActionListener(e ->
		{
			List<SettingsModel.Setting> chosen = new ArrayList<>();
			boxes.forEach((box, setting) ->
			{
				if (box.isSelected())
				{
					chosen.add(setting);
				}
			});
			panel.applySuggestions(chosen);
			panel.open(RuneRelayPanel.View.HOME, null);
		});
		content.add(apply);

		JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		links.setOpaque(false);
		links.add(Ui.link("Tick all", "Tick every suggestion", () -> setAll(true)));
		links.add(Ui.small("   ", ColorScheme.LIGHT_GRAY_COLOR));
		links.add(Ui.link("Untick all", "Untick every suggestion", () -> setAll(false)));
		content.add(links);

		String lastPlugin = null;
		for (SettingsModel.Setting setting : suggestions)
		{
			String plugin = setting.getPlugin().getName();
			if (!plugin.equals(lastPlugin))
			{
				lastPlugin = plugin;
				JLabel title = Ui.title(plugin);
				title.setForeground(ColorScheme.TEXT_COLOR);
				content.add(title);
			}

			String what = setting.isPluginToggle() ? "Switch " + Ui.escape(plugin) + " " : Ui.escape(setting.getLabel()) + ": ";
			String change = setting.isPluginToggle()
				? Ui.colored(SettingsModel.format(setting.getRecommended()).toLowerCase(), ColorScheme.BRAND_ORANGE)
				: Ui.colored(Ui.escape(SettingsModel.format(model.current(setting))), ColorScheme.LIGHT_GRAY_COLOR)
					+ " → " + Ui.colored(Ui.escape(SettingsModel.format(setting.getRecommended())), ColorScheme.BRAND_ORANGE);
			JCheckBox box = new JCheckBox(Ui.html(what + change, Ui.CONTENT_WIDTH - 32),
				!model.getReviewExcluded().contains(setting.id()));
			box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			box.setBorder(new EmptyBorder(5, 4, 5, 4));
			box.setFocusPainted(false);
			if (setting.getDef().getWhy() != null)
			{
				box.setToolTipText(setting.getDef().getWhy());
			}
			box.addActionListener(e ->
			{
				if (box.isSelected())
				{
					model.getReviewExcluded().remove(setting.id());
				}
				else
				{
					model.getReviewExcluded().add(setting.id());
				}
				refresh();
			});
			boxes.put(box, setting);
			content.add(box);
		}
		refresh();
	}

	private void setAll(boolean selected)
	{
		boxes.forEach((box, setting) ->
		{
			box.setSelected(selected);
			if (selected)
			{
				model.getReviewExcluded().remove(setting.id());
			}
			else
			{
				model.getReviewExcluded().add(setting.id());
			}
		});
		refresh();
	}

	private void refresh()
	{
		long count = boxes.keySet().stream().filter(JCheckBox::isSelected).count();
		apply.setText(count == 0 ? "Nothing ticked" : "Apply " + count + (count == 1 ? " change" : " changes"));
		apply.setEnabled(count > 0);
	}
}
