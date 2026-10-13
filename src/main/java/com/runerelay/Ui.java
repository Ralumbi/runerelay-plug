package com.runerelay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Shared look for RuneRelay's panel.
 */
final class Ui
{
	static final Color GOOD = new Color(110, 200, 110);
	static final Color WARN = ColorScheme.PROGRESS_INPROGRESS_COLOR;
	/**
	 * Width of the scrolling content: the side panel minus RuneRelay's 8px margins.
	 */
	static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH - 16;
	/**
	 * Text width inside a {@link #card}: 3px stripe plus 8px padding on both sides.
	 */
	static final int CARD_TEXT_WIDTH = CONTENT_WIDTH - 19;
	/**
	 * Inner width of a list row: 8px left and 6px right padding.
	 */
	static final int ROW_WIDTH = CONTENT_WIDTH - 14;
	/**
	 * Text width of an {@link #infoBox} placed directly in the content.
	 */
	static final int INFO_WIDTH = CONTENT_WIDTH - 14;
	/**
	 * Swing's HTML renderer scales CSS pixels by 1.3, so widths are converted to keep them exact.
	 */
	private static final float CSS_PX_SCALE = 1.3f;

	private Ui()
	{
	}

	static JLabel title(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0));
		return label;
	}

	static JLabel note(String text)
	{
		return note(text, CONTENT_WIDTH);
	}

	static JLabel note(String text, int width)
	{
		JLabel label = new JLabel(html(text, width));
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	static JLabel small(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		return label;
	}

	static JLabel link(String text, String tooltip, Runnable onClick)
	{
		JLabel link = small(text, ColorScheme.BRAND_ORANGE);
		link.setToolTipText(tooltip);
		link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		link.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				onClick.run();
			}
		});
		return link;
	}

	/**
	 * A dark panel with a coloured stripe on the left.
	 */
	static JPanel card(Color accent)
	{
		JPanel card = new JPanel(new DynamicGridLayout(0, 1, 0, 5));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
			new EmptyBorder(8, 8, 8, 8)));
		return card;
	}

	static JComponent infoBox(String html, int width)
	{
		JLabel text = new JLabel(html(html, width));
		text.setFont(FontManager.getRunescapeSmallFont());
		text.setForeground(ColorScheme.TEXT_COLOR);
		JPanel box = new JPanel(new BorderLayout());
		box.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 2, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(5, 6, 5, 6)));
		box.add(text, BorderLayout.CENTER);
		return box;
	}

	/**
	 * A row of text tabs, like a segmented control: grey labels, the selected one orange and underlined.
	 * Used instead of dropdowns where every option fits on one line.
	 *
	 * @param tooltips one per label, may be null
	 */
	static JPanel tabs(String[] labels, String[] tooltips, int selected, IntConsumer onSelect)
	{
		return tabs(labels, tooltips, selected, onSelect, ColorScheme.DARKER_GRAY_COLOR);
	}

	static JPanel tabs(String[] labels, String[] tooltips, int selected, IntConsumer onSelect, Color background)
	{
		// Each tab grows with its label, so long ones like "Suggested" never get clipped
		JPanel row = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.insets = new Insets(0, 1, 0, 1);
		row.setBackground(background);
		row.setBorder(new EmptyBorder(2, 2, 2, 2));
		for (int i = 0; i < labels.length; i++)
		{
			int index = i;
			boolean active = i == selected;
			JLabel tab = new JLabel(labels[i], JLabel.CENTER);
			tab.setFont(FontManager.getRunescapeSmallFont());
			tab.setForeground(active ? ColorScheme.BRAND_ORANGE : ColorScheme.LIGHT_GRAY_COLOR);
			tab.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 2, 0, active ? ColorScheme.BRAND_ORANGE : background),
				new EmptyBorder(4, 0, 3, 0)));
			if (tooltips != null)
			{
				tab.setToolTipText(tooltips[i]);
			}
			tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			tab.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					onSelect.accept(index);
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					if (!active)
					{
						tab.setForeground(ColorScheme.TEXT_COLOR);
					}
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					if (!active)
					{
						tab.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
					}
				}
			});
			c.weightx = tab.getPreferredSize().width;
			row.add(tab, c);
		}
		return row;
	}

	/**
	 * A small outlined button for the header, e.g. "Website".
	 */
	static JLabel pillButton(String text, String tooltip, Runnable onClick)
	{
		JLabel button = new JLabel(text, JLabel.CENTER);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		button.setToolTipText(tooltip);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		Border normal = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), new EmptyBorder(2, 6, 2, 6));
		Border hover = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE), new EmptyBorder(2, 6, 2, 6));
		button.setBorder(normal);
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				onClick.run();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				button.setBorder(hover);
				button.setForeground(ColorScheme.TEXT_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				button.setBorder(normal);
				button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
		});
		return button;
	}

	/**
	 * Dark dropdown matching RuneLite's panels. Set any custom renderer before calling this,
	 * since the preferred size is measured with it.
	 */
	static <T> JComboBox<T> styleCombo(JComboBox<T> combo)
	{
		combo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		combo.setForeground(ColorScheme.TEXT_COLOR);
		combo.setPreferredSize(new Dimension(combo.getPreferredSize().width, 24));
		return combo;
	}

	static String hex(Color color)
	{
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	static String colored(String text, Color color)
	{
		return "<span style='color:" + hex(color) + "'>" + text + "</span>";
	}

	/**
	 * Escape text from other plugins before putting it in an HTML label.
	 */
	static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 * Wrap text in HTML so it word-wraps at {@code width} real pixels.
	 */
	static String html(String text, int width)
	{
		int cssWidth = Math.max(20, Math.round((width - 4) / CSS_PX_SCALE));
		return "<html><body style='width:" + cssWidth + "px'>" + text + "</body></html>";
	}
}
