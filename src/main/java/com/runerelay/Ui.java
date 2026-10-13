package com.runerelay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
