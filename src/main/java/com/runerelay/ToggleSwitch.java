package com.runerelay;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JToggleButton;
import net.runelite.client.ui.ColorScheme;

/**
 * A small on/off switch, in the spirit of RuneLite's plugin list toggles.
 */
class ToggleSwitch extends JToggleButton
{
	private static final int TRACK_WIDTH = 28;
	private static final int TRACK_HEIGHT = 14;

	ToggleSwitch(boolean on)
	{
		super((String) null, on);
		setPreferredSize(new Dimension(TRACK_WIDTH + 4, TRACK_HEIGHT + 6));
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int x = (getWidth() - TRACK_WIDTH) / 2;
		int y = (getHeight() - TRACK_HEIGHT) / 2;

		Color track = isSelected() ? Ui.GOOD : ColorScheme.MEDIUM_GRAY_COLOR;
		if (!isEnabled())
		{
			track = track.darker();
		}
		g2.setColor(track);
		g2.fillRoundRect(x, y, TRACK_WIDTH, TRACK_HEIGHT, TRACK_HEIGHT, TRACK_HEIGHT);

		int knob = TRACK_HEIGHT - 4;
		int knobX = isSelected() ? x + TRACK_WIDTH - knob - 2 : x + 2;
		g2.setColor(isEnabled() ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
		g2.fillOval(knobX, y + 2, knob, knob);
		g2.dispose();
	}
}
