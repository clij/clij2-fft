/*-
 * #%L
 * clij2 fft
 * %%
 * Copyright (C) 2020 - 2025 Robert Haase, MPI CBG
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the MPI CBG nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.clij2fft.deconvolution;

import java.awt.Color;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Read-only log for the deconvolution GUI. Sections start with a blank line and
 * a bold, colored heading; warnings are orange. Safe to call from any thread.
 */
public class LogPane extends JTextPane {

	private static final Color HEADING = new Color(0x2A6FDB);
	private static final Color WARNING = new Color(0xC0620C);

	private final SimpleAttributeSet heading = new SimpleAttributeSet();
	private final SimpleAttributeSet plain = new SimpleAttributeSet();
	private final SimpleAttributeSet warning = new SimpleAttributeSet();

	public LogPane() {
		setEditable(false);
		StyleConstants.setBold(heading, true);
		StyleConstants.setForeground(heading, HEADING);
		StyleConstants.setForeground(warning, WARNING);
	}

	/** Starts a new section: a blank line (unless the log is empty), then a heading. */
	public void section(String title) {
		onEdt(() -> {
			if (getDocument().getLength() > 0) append("\n", plain);
			append(title + "\n", heading);
		});
	}

	public void info(String message) {
		onEdt(() -> append(message + "\n", plain));
	}

	public void warn(String message) {
		onEdt(() -> append(message + "\n", warning));
	}

	private void append(String text, SimpleAttributeSet style) {
		StyledDocument doc = getStyledDocument();
		try {
			doc.insertString(doc.getLength(), text, style);
		}
		catch (BadLocationException e) {
			// cannot happen when inserting at the end
		}
		setCaretPosition(doc.getLength());
	}

	private static void onEdt(Runnable r) {
		if (SwingUtilities.isEventDispatchThread()) r.run();
		else SwingUtilities.invokeLater(r);
	}
}
