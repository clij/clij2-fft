package net.clij2fft.deconvolution;

import javax.swing.SwingUtilities;

import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import net.imagej.ops.OpService;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin(type = Command.class, headless = true, menuPath = "Test>Test Deconvolution")
public class RichardsonLucyGUICommand<T extends RealType<T> & NativeType<T>> implements Command {

	@Parameter
	OpService ops;

	@Parameter
	LogService log;
	
	@Parameter
	StatusService status;

	@Parameter
	UIService ui;

	private static RichardsonLucyGUI dialog = null;

	@Override
	public void run() {
		
		SwingUtilities.invokeLater(() -> {
				if (dialog == null) {
					dialog = new RichardsonLucyGUI();
					dialog.initiateModel(ops, log, status);
				}
				dialog.setVisible(true);
			});
		}
	}

