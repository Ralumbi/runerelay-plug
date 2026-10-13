package com.runerelay;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.ModifierlessKeybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.TitleCaseListCellRenderer;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

/**
 * Builds a compact editor for one setting, picked from the setting's config type.
 */
final class ValueEditors
{
	private static final Dimension SPINNER_SIZE = new Dimension(62, 22);
	private static final Dimension COMBO_SIZE = new Dimension(104, 22);

	private ValueEditors()
	{
	}

	static JComponent create(SettingsModel.Setting setting, Object value, Consumer<Object> onChange, ColorPickerManager colorPickerManager)
	{
		Type type = setting.getType();
		if (type == boolean.class || type == Boolean.class)
		{
			return toggle(Boolean.TRUE.equals(value), onChange);
		}
		if (type == int.class || type == Integer.class)
		{
			return intSpinner(setting, value instanceof Integer ? (Integer) value : 0, onChange);
		}
		if (type == double.class || type == Double.class)
		{
			return doubleSpinner(value instanceof Double ? (Double) value : 0d, onChange);
		}
		if (type instanceof Class)
		{
			Class<?> cls = (Class<?>) type;
			if (cls.isEnum())
			{
				return enumCombo(cls, value, onChange);
			}
			if (cls == Color.class)
			{
				return new ColorButton(setting, (Color) value, onChange, colorPickerManager);
			}
			if (Keybind.class.isAssignableFrom(cls))
			{
				return new KeybindButton((Keybind) value, cls == ModifierlessKeybind.class, onChange);
			}
		}
		return Ui.small("Edit in RuneLite settings", ColorScheme.LIGHT_GRAY_COLOR);
	}

	private static JComponent toggle(boolean on, Consumer<Object> onChange)
	{
		JToggleButton button = new JToggleButton(on ? "On" : "Off", on);
		button.setPreferredSize(SPINNER_SIZE);
		button.setFocusPainted(false);
		button.addActionListener(e ->
		{
			button.setText(button.isSelected() ? "On" : "Off");
			onChange.accept(button.isSelected());
		});
		return button;
	}

	private static JComponent intSpinner(SettingsModel.Setting setting, int value, Consumer<Object> onChange)
	{
		Range range = setting.getItem() != null ? setting.getItem().getRange() : null;
		int min = range != null ? range.min() : 0;
		int max = range != null ? range.max() : Integer.MAX_VALUE;
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(Math.max(min, Math.min(max, value)), min, max, 1));
		spinner.setPreferredSize(SPINNER_SIZE);
		spinner.addChangeListener(e -> onChange.accept(spinner.getValue()));

		Units units = setting.getItem() != null ? setting.getItem().getUnits() : null;
		if (units == null)
		{
			return spinner;
		}
		JPanel withUnits = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		withUnits.setOpaque(false);
		withUnits.add(spinner);
		withUnits.add(Ui.small(units.value().trim(), ColorScheme.LIGHT_GRAY_COLOR));
		return withUnits;
	}

	private static JComponent doubleSpinner(double value, Consumer<Object> onChange)
	{
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, 0d, 50d, 0.5d));
		spinner.setPreferredSize(SPINNER_SIZE);
		spinner.addChangeListener(e -> onChange.accept(spinner.getValue()));
		return spinner;
	}

	private static JComponent enumCombo(Class<?> cls, Object value, Consumer<Object> onChange)
	{
		JComboBox<Object> combo = new JComboBox<>(cls.getEnumConstants());
		combo.setRenderer(new TitleCaseListCellRenderer());
		combo.setSelectedItem(value);
		Ui.styleCombo(combo);
		combo.setPreferredSize(COMBO_SIZE);
		combo.addActionListener(e -> onChange.accept(combo.getSelectedItem()));
		return combo;
	}

	private static class ColorButton extends JButton
	{
		private Color color;

		ColorButton(SettingsModel.Setting setting, Color initial, Consumer<Object> onChange, ColorPickerManager colorPickerManager)
		{
			color = initial != null ? initial : Color.WHITE;
			setFocusPainted(false);
			setIcon(new Swatch());
			setFont(FontManager.getRunescapeSmallFont());
			updateText();
			addActionListener(e ->
			{
				Color opened = color;
				RuneliteColorPicker picker = colorPickerManager.create(
					SwingUtilities.windowForComponent(this), color, setting.getLabel(),
					setting.getItem() == null || setting.getItem().getAlpha() == null);
				picker.setOnColorChange(c ->
				{
					color = c;
					updateText();
					repaint();
				});
				// Save once when the picker closes rather than on every drag
				picker.setOnClose(c ->
				{
					if (c != null && !c.equals(opened))
					{
						onChange.accept(c);
					}
				});
				picker.setVisible(true);
			});
		}

		private void updateText()
		{
			setText(String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue()));
		}

		private class Swatch implements Icon
		{
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y)
			{
				// Checkerboard so translucent colours are visible
				g.setColor(Color.LIGHT_GRAY);
				g.fillRect(x, y, 12, 12);
				g.setColor(Color.GRAY);
				g.fillRect(x, y, 6, 6);
				g.fillRect(x + 6, y + 6, 6, 6);
				g.setColor(color);
				g.fillRect(x, y, 12, 12);
				g.setColor(ColorScheme.BORDER_COLOR);
				g.drawRect(x, y, 11, 11);
			}

			@Override
			public int getIconWidth()
			{
				return 12;
			}

			@Override
			public int getIconHeight()
			{
				return 12;
			}
		}
	}

	/**
	 * Click, then press the key to bind. Clicking clears the binding until a key is pressed.
	 */
	private static class KeybindButton extends JButton
	{
		KeybindButton(Keybind initial, boolean modifierless, Consumer<Object> onChange)
		{
			// Allow Tab to be bound
			setFocusTraversalKeysEnabled(false);
			setFocusPainted(false);
			setPreferredSize(COMBO_SIZE);
			setValue(initial);
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseReleased(MouseEvent e)
				{
					if (e.getButton() == MouseEvent.BUTTON1)
					{
						setText("Press a key...");
					}
				}
			});
			addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyPressed(KeyEvent e)
				{
					Keybind bind = modifierless ? new ModifierlessKeybind(e) : new Keybind(e);
					setValue(bind);
					onChange.accept(bind);
				}
			});
		}

		private void setValue(Keybind value)
		{
			setText((value == null ? Keybind.NOT_SET : value).toString());
		}
	}
}
