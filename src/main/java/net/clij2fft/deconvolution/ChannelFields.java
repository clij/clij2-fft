package net.clij2fft.deconvolution;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * One field per input channel, stacked in the place of a single field. With one
 * channel it shows just that field; with more, each row is labelled C1, C2, ...
 * New rows are copies of the first field, so they start with its value.
 */
public class ChannelFields<T extends JComponent> extends JPanel {

	private final UnaryOperator<T> copy;
	private final List<T> fields = new ArrayList<>();
	private final List<JLabel> labels = new ArrayList<>();

	/**
	 * @param first the field for channel 1, used as is
	 * @param copy makes the field for another channel from the first one,
	 *          including any listeners it needs
	 */
	public ChannelFields(T first, UnaryOperator<T> copy) {
		super(new GridBagLayout());
		setOpaque(false);
		this.copy = copy;
		addRow(first);
		layoutRows();
	}

	public void setChannels(int n) {
		n = Math.max(1, n);
		if (n == fields.size()) return;
		while (fields.size() > n) {
			int last = fields.size() - 1;
			remove(fields.remove(last));
			remove(labels.remove(last));
		}
		while (fields.size() < n) addRow(copy.apply(fields.get(0)));
		layoutRows();
		revalidate();
		repaint();
	}

	public int channels() {
		return fields.size();
	}

	/** The field for channel {@code c} (0-based). */
	public T get(int c) {
		return fields.get(c);
	}

	public List<T> all() {
		return fields;
	}

	private void addRow(T field) {
		fields.add(field);
		labels.add(new JLabel());
	}

	private void layoutRows() {
		removeAll();
		boolean several = fields.size() > 1;
		for (int c = 0; c < fields.size(); c++) {
			Insets insets = new Insets(c == 0 ? 0 : 3, 0, 0, 0);
			if (several) {
				JLabel label = labels.get(c);
				label.setText("C" + (c + 1));
				GridBagConstraints gl = new GridBagConstraints();
				gl.gridx = 0;
				gl.gridy = c;
				gl.anchor = GridBagConstraints.WEST;
				gl.insets = new Insets(insets.top, 0, 0, 5);
				add(label, gl);
			}
			GridBagConstraints gf = new GridBagConstraints();
			gf.gridx = 1;
			gf.gridy = c;
			gf.weightx = 1.0;
			gf.fill = GridBagConstraints.HORIZONTAL;
			gf.insets = insets;
			add(fields.get(c), gf);
		}
	}
}
