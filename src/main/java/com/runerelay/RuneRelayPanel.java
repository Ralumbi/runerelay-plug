package com.runerelay;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * Shell of the side panel: a fixed header, one scrolling view (plugin list, plugin page or
 * suggestion review) and a fixed undo footer. Also holds the actions the views share.
 */
class RuneRelayPanel extends PluginPanel
{
	private static final int SCROLL_SPEED = 16;
	private static final String WEBSITE = "https://runerelay.com";

	enum View
	{
		HOME,
		PLUGIN,
		REVIEW,
		PACKS
	}

	private final Client client;
	@Getter
	private final ConfigManager configManager;
	@Getter
	private final RuneRelayConfig config;
	@Getter
	private final SettingsModel model;
	@Getter
	private final SettingsApplier applier;
	private final ExternalPluginManager externalPluginManager;
	@Getter
	private final ColorPickerManager colorPickerManager;
	@Getter
	private final MarkerPacks markerPacks;

	private final JPanel tierRow = new JPanel(new DynamicGridLayout(0, 1, 0, 5));
	private final JPanel viewBar = new JPanel(new BorderLayout());
	private final JPanel content = new JPanel(new DynamicGridLayout(0, 1, 0, 5));
	private final JScrollPane scrollPane;
	private final JLabel status = new JLabel();
	private final JButton undoButton = new JButton("Undo");

	private final HomeView home;
	private final PluginView pluginView;
	private final ReviewView reviewView;
	private final PacksView packsView;

	private View view = View.HOME;
	private String openPlugin;
	private boolean scrollToTop;
	private String lastStatus;
	@Getter
	private PerformanceTier tier = PerformanceTier.BALANCED;
	@Getter
	private SystemSpecs specs;

	private volatile boolean rebuildPending;
	/**
	 * Whether the panel is open; while closed, rebuilds wait until it is opened.
	 */
	private volatile boolean active;
	private volatile boolean stale = true;
	private volatile Set<String> watchedGroups = Collections.emptySet();

	@Inject
	RuneRelayPanel(Client client, ConfigManager configManager, RuneRelayConfig config, SettingsModel model,
		SettingsApplier applier, ExternalPluginManager externalPluginManager, ColorPickerManager colorPickerManager,
		MarkerPacks markerPacks)
	{
		super(false);
		this.client = client;
		this.configManager = configManager;
		this.config = config;
		this.model = model;
		this.applier = applier;
		this.externalPluginManager = externalPluginManager;
		this.colorPickerManager = colorPickerManager;
		this.markerPacks = markerPacks;
		this.home = new HomeView(this);
		this.pluginView = new PluginView(this);
		this.reviewView = new ReviewView(this);
		this.packsView = new PacksView(this);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Fixed top: never scrolls away
		JPanel north = new JPanel(new DynamicGridLayout(0, 1, 0, 6));
		north.setBackground(ColorScheme.DARK_GRAY_COLOR);
		north.setBorder(new EmptyBorder(8, 8, 6, 8));
		north.add(header());
		tierRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		tierRow.setBorder(new EmptyBorder(7, 8, 7, 8));
		north.add(tierRow);
		viewBar.setOpaque(false);
		north.add(viewBar);
		add(north, BorderLayout.NORTH);

		// Scrolling middle: wheel scrolling without a visible bar
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setBorder(new EmptyBorder(2, 8, 8, 8));
		JPanel wrapper = new WidthTrackingPanel();
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(content, BorderLayout.NORTH);
		scrollPane = new JScrollPane(wrapper);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
		scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_SPEED);
		add(scrollPane, BorderLayout.CENTER);

		// Fixed bottom: what just happened, and undo
		JPanel footer = new JPanel(new BorderLayout(6, 0));
		footer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		footer.setBorder(new EmptyBorder(6, 8, 6, 8));
		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		footer.add(status, BorderLayout.CENTER);
		undoButton.setFocusPainted(false);
		undoButton.addActionListener(e ->
		{
			String label = applier.undoLabel();
			applier.undo();
			status("Undone: " + label);
			requestRebuild();
		});
		footer.add(undoButton, BorderLayout.EAST);
		add(footer, BorderLayout.SOUTH);
	}

	/**
	 * Scroll content that always matches the panel's width, so rows can never stick out sideways.
	 */
	private static class WidthTrackingPanel extends JPanel implements Scrollable
	{
		WidthTrackingPanel()
		{
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return SCROLL_SPEED;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return visibleRect.height - SCROLL_SPEED;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}

	private JPanel header()
	{
		JPanel header = new JPanel(new BorderLayout(8, 0));
		header.setOpaque(false);
		header.add(new JLabel(new ImageIcon(ImageUtil.loadImageResource(RuneRelayPanel.class, "logo_32.png"))), BorderLayout.WEST);

		JPanel text = new JPanel(new DynamicGridLayout(0, 1, 0, 1));
		text.setOpaque(false);
		text.setBorder(new EmptyBorder(1, 0, 0, 0));
		JLabel name = new JLabel("RuneRelay");
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(ColorScheme.BRAND_ORANGE);
		text.add(name);
		text.add(Ui.small("RuneLite, set up right", ColorScheme.LIGHT_GRAY_COLOR));
		header.add(text, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new DynamicGridLayout(0, 1, 0, 3));
		buttons.setOpaque(false);
		buttons.add(Ui.pillButton("Website", "Open " + WEBSITE + " in your browser", () -> LinkBrowser.browse(WEBSITE)));
		buttons.add(Ui.pillButton("Guide", "Show how RuneRelay works", () ->
		{
			configManager.setConfiguration(RuneRelayConfig.GROUP, RuneRelayConfig.GUIDE_DISMISSED, false);
			open(View.HOME, null);
		}));
		header.add(buttons, BorderLayout.EAST);
		return header;
	}

	// region rebuilding

	/**
	 * Plugin on/off changes arrive as PluginChanged, so RuneLite's own (busy) config group isn't watched.
	 */
	boolean isWatched(String group)
	{
		return RuneRelayConfig.GROUP.equals(group) || watchedGroups.contains(group);
	}

	@Override
	public void onActivate()
	{
		active = true;
		if (stale)
		{
			requestRebuild();
		}
	}

	@Override
	public void onDeactivate()
	{
		active = false;
	}

	/**
	 * Settings now belong to another profile: start over, and drop undo steps for the old one.
	 */
	void onProfileChanged()
	{
		applier.clearHistory();
		lastStatus = null;
		SwingUtilities.invokeLater(() -> open(View.HOME, null));
	}

	void onConfigChanged(ConfigChanged event)
	{
		// RuneRelay's own writes refresh the affected rows directly, so editors keep focus
		if (!applier.isApplying() && isWatched(event.getGroup()))
		{
			requestRebuild();
		}
	}

	/**
	 * Schedule a rebuild on the EDT; bursts of events collapse into one rebuild.
	 */
	void requestRebuild()
	{
		if (!active)
		{
			stale = true;
			return;
		}
		if (rebuildPending)
		{
			return;
		}
		rebuildPending = true;
		SwingUtilities.invokeLater(() ->
		{
			rebuildPending = false;
			rebuild();
		});
	}

	void open(View view, String plugin)
	{
		this.view = view;
		this.openPlugin = plugin;
		scrollToTop = true;
		rebuild();
	}

	private void rebuild()
	{
		stale = false;
		specs = SystemSpecs.detect(client.getCanvas());
		PerformanceTier detected = PerformanceTier.detect(specs);
		tier = config.tier() == PerformanceTier.AUTO ? detected : config.tier();

		model.resolve(tier, specs);
		if (model.getPendingSetup().contains(MarkerPacks.RADIUS_MARKERS) && model.findPlugin(MarkerPacks.RADIUS_MARKERS) != null)
		{
			// Radius Markers starts empty; give it the bundled boss markers it was installed for
			try
			{
				int added = markerPacks.installDefaultRadiusPacks();
				lastStatus = "Radius Markers installed with " + added + " boss markers from the OSRS Wiki.";
			}
			catch (IllegalArgumentException ex)
			{
				lastStatus = "Radius Markers installed, but its boss markers couldn't be added: " + ex.getMessage();
			}
		}
		List<SettingsApplier.Change> setup = model.takeSetupChanges();
		if (!setup.isEmpty())
		{
			// A plugin installed from RuneRelay has loaded: give it the suggested settings straight away
			applier.apply(setup, "Set up new plugin");
			lastStatus = "Applied RuneRelay's settings to the new plugin.";
			model.resolve(tier, specs);
		}
		watchedGroups = model.watchedGroups();

		rebuildTierRow(detected);
		if (view == View.PLUGIN && model.findPlugin(openPlugin) == null && Catalog.hub(openPlugin) == null)
		{
			view = View.HOME;
		}

		int scroll = scrollToTop ? 0 : scrollPane.getVerticalScrollBar().getValue();
		scrollToTop = false;
		viewBar.removeAll();
		content.removeAll();
		switch (view)
		{
			case PLUGIN:
				viewBar.add(pluginView.bar(openPlugin), BorderLayout.CENTER);
				pluginView.build(content, openPlugin);
				break;
			case REVIEW:
				viewBar.add(reviewView.bar(), BorderLayout.CENTER);
				reviewView.build(content);
				break;
			case PACKS:
				viewBar.add(packsView.bar(), BorderLayout.CENTER);
				packsView.build(content);
				break;
			default:
				viewBar.add(home.bar(), BorderLayout.CENTER);
				home.build(content);
		}
		refreshFooter();
		revalidate();
		repaint();
		SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(scroll));
	}

	void relayout()
	{
		content.revalidate();
		content.repaint();
	}

	private void rebuildTierRow(PerformanceTier detected)
	{
		tierRow.removeAll();

		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		JLabel label = Ui.small("YOUR PC", ColorScheme.LIGHT_GRAY_COLOR);
		label.setToolTipText("Which settings RuneRelay suggests depends on how strong your computer is");
		top.add(label, BorderLayout.WEST);
		String chosen = config.tier() == PerformanceTier.AUTO ? "Auto picked " + detected.getDisplayName() : "Set by you";
		top.add(Ui.small(chosen, ColorScheme.TEXT_COLOR), BorderLayout.EAST);
		tierRow.add(top);

		PerformanceTier[] tiers = PerformanceTier.values();
		String[] labels = new String[tiers.length];
		String[] tooltips = new String[tiers.length];
		for (int i = 0; i < tiers.length; i++)
		{
			labels[i] = tiers[i] == PerformanceTier.AUTO ? "Auto" : tiers[i].getDisplayName();
			tooltips[i] = tiers[i] == PerformanceTier.AUTO
				? "Let RuneRelay pick: " + detected.getDisplayName() + " for this computer"
				: "Suggest settings for a " + tiers[i].getDisplayName().toLowerCase() + " computer. "
					+ "Graphics cards can't be detected, so pick a lower tier if the game feels slow.";
		}
		tierRow.add(Ui.tabs(labels, tooltips, config.tier().ordinal(), index ->
			configManager.setConfiguration(RuneRelayConfig.GROUP, "tier", tiers[index]), ColorScheme.DARK_GRAY_COLOR));

		JLabel specsLine = Ui.note(describe(specs), Ui.CONTENT_WIDTH - 16);
		specsLine.setToolTipText(specs.getOs());
		tierRow.add(specsLine);
	}

	private static String describe(SystemSpecs specs)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(specs.getCpuThreads()).append(" threads");
		if (specs.getTotalRamMb() > 0)
		{
			sb.append(" · ").append(Math.round(specs.getTotalRamMb() / 1024.0)).append(" GB");
		}
		if (specs.getScreenWidth() > 0)
		{
			sb.append(" · ").append(specs.getScreenWidth()).append('×').append(specs.getScreenHeight());
			if (specs.isRefreshRateKnown())
			{
				sb.append(" · ").append(specs.getRefreshRate()).append(" Hz");
			}
		}
		return sb.toString();
	}

	// endregion

	// region shared actions

	void status(String text)
	{
		lastStatus = text;
		refreshFooter();
	}

	void refreshFooter()
	{
		undoButton.setEnabled(applier.canUndo());
		undoButton.setToolTipText(applier.canUndo() ? "Undo: " + applier.undoLabel() : "Nothing to undo");
		String text = lastStatus != null ? lastStatus : "Changes save instantly. Undo is always here.";
		status.setText(Ui.html(Ui.escape(text), Ui.CONTENT_WIDTH - 6 - undoButton.getPreferredSize().width));
	}

	/**
	 * Change one setting now, plus whatever it needs to take effect.
	 *
	 * @param refreshRow refreshes the edited row when nothing else changed
	 */
	void change(SettingsModel.Setting setting, Object value, Runnable refreshRow)
	{
		List<SettingsApplier.Change> changes = model.plan(setting, value);
		String what = setting.isPluginToggle() ? setting.getPlugin().getName() : setting.getLabel();
		applier.apply(changes, what + " → " + SettingsModel.format(value));
		if (changes.size() > 1)
		{
			List<String> also = new ArrayList<>();
			for (SettingsApplier.Change extra : changes.subList(1, changes.size()))
			{
				SettingsModel.Setting s = extra.getSetting();
				also.add((s.isPluginToggle() ? s.getPlugin().getName() : s.getLabel()) + " " + SettingsModel.format(extra.getValue()).toLowerCase());
			}
			status("Saved. Also: " + String.join(", ", also) + ".");
			requestRebuild();
		}
		else
		{
			status("Saved: " + what + " → " + SettingsModel.format(value));
			refreshRow.run();
		}
	}

	void applySuggestions(List<SettingsModel.Setting> suggestions)
	{
		if (suggestions.isEmpty())
		{
			return;
		}
		applier.apply(model.planSuggestions(suggestions), suggestions.size() + (suggestions.size() == 1 ? " suggestion" : " suggestions"));
		status("Applied " + suggestions.size() + (suggestions.size() == 1 ? " suggestion." : " suggestions."));
		requestRebuild();
	}

	void setPluginEnabled(String name, boolean on)
	{
		if (on && !confirmRivals(name))
		{
			requestRebuild();
			return;
		}
		SettingsModel.Setting toggle = model.byId(name + "#enabled");
		if (toggle != null)
		{
			change(toggle, on, () -> { });
		}
		else
		{
			applier.setPluginEnabled(model.findPlugin(name), on);
		}
		requestRebuild();
	}

	void install(Catalog.HubPlugin hub)
	{
		String name = hub.getPluginName();
		List<String> rivals = model.enabledRivals(name);
		if (!confirmRivals(name))
		{
			return;
		}
		for (String rival : rivals)
		{
			applier.setPluginEnabled(model.findPlugin(rival), false);
		}
		model.getPendingSetup().add(name);
		externalPluginManager.install(hub.getInternalName());
		status("Installing " + name + "...");
	}

	void remove(Catalog.HubPlugin hub)
	{
		String name = hub.getPluginName();
		int choice = JOptionPane.showConfirmDialog(this,
			"Remove " + name + " from RuneLite? Its settings are kept if you reinstall it later.",
			"Remove plugin", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (choice == JOptionPane.OK_OPTION)
		{
			externalPluginManager.remove(hub.getInternalName());
			status("Removed " + name + ".");
			open(View.HOME, null);
		}
	}

	boolean isInstalled(Catalog.HubPlugin hub)
	{
		return externalPluginManager.getInstalledExternalPlugins().contains(hub.getInternalName());
	}

	/**
	 * Ask before a plugin goes on while a rival is on. Switching the rival off is the caller's job.
	 */
	private boolean confirmRivals(String name)
	{
		List<String> on = model.enabledRivals(name);
		if (on.isEmpty())
		{
			return true;
		}
		String others = String.join(" and ", on);
		int choice = JOptionPane.showConfirmDialog(this,
			name + " can't run together with " + others + ". RuneRelay will switch " + others + " off.",
			"Avoid a clash", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
		return choice == JOptionPane.OK_OPTION;
	}

	// endregion
}
