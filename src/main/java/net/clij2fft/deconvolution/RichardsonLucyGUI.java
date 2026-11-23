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

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import ij.plugin.Duplicator;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxUI;

import org.apache.commons.lang3.tuple.Triple;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.app.event.StatusEvent;
import org.scijava.log.LogService;
import org.scijava.plugin.PluginInfo;
import org.scijava.ui.UIService;

import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.Calibration;
import net.clij2fft.deconvolution.RichardsonLucyModelController.PSFTypeEnum;
import net.imagej.ops.OpService;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;

import javax.swing.JSplitPane;
import javax.swing.BorderFactory;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;

import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import java.awt.Insets;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import java.awt.Component;
import net.haesleinhuepf.clijx.imglib2cache.CellPlanner;
import java.util.Locale;
import net.haesleinhuepf.clij2.CLIJ2;

//Functional interface with three parameters and no return value
interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
}

public class RichardsonLucyGUI extends JFrame {
	
	StatusService statusService;
	
	RichardsonLucyModelController modelController;

	// GPU limits for the cell planner, read in the background when the GUI opens
	private volatile String gpuName;
	private volatile long gpuMemoryBytes, gpuMaxAllocationBytes;
	// re-estimates the automatic cells; replaced once the controls exist
	private Runnable replanCells = () -> {};
	private Runnable replanCellsAndLog = () -> {};

	// one field per input channel on each PSF tab (see setPsfChannels)
	private ChannelFields<JComboBox<ImagePlus>> psfFields, beadFields;
	private ChannelFields<JSpinner> wavelengthFields, sigmaXYFields, sigmaZFields;

	public void initiateModel(OpService ops, LogService log, StatusService status) {
		this.modelController = new RichardsonLucyModelController(ops, log, this.statusService);
	}
	
    public RichardsonLucyGUI() {
    	setTitle("CLIJ 3D Deconvolution");
        // Set the layout manager for the content pane
        getContentPane().setLayout(new BorderLayout());
        
        // settings on top (inputs left, PSF tabs right), log full width below.
        // Resize weight 0: the top keeps the height of its tallest tab, the log gets the rest.
        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.5);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplit.setResizeWeight(0.0);
        mainSplit.setTopComponent(splitPane);
        getContentPane().add(mainSplit, BorderLayout.CENTER);
       
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        
        JPanel panelMeasured = new JPanel();
        tabbedPane.addTab("Measured", null, panelMeasured, "Use a measured PSF image");
        GridBagLayout gbl_panelMeasured = new GridBagLayout();
        gbl_panelMeasured.columnWidths = new int[]{0, 0, 0};
        gbl_panelMeasured.rowHeights = new int[]{0, 0};
        gbl_panelMeasured.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gbl_panelMeasured.rowWeights = new double[]{0.0, Double.MIN_VALUE};
        panelMeasured.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelMeasured.setLayout(gbl_panelMeasured);
        
        JLabel lblPSF = new JLabel("PSF image");
        GridBagConstraints gbc_lblPSF = new GridBagConstraints();
        gbc_lblPSF.insets = new Insets(0, 0, 0, 5);
        gbc_lblPSF.anchor = GridBagConstraints.NORTHWEST;
        gbc_lblPSF.gridx = 0;
        gbc_lblPSF.gridy = 0;
        panelMeasured.add(lblPSF, gbc_lblPSF);
        
        ImagePlusComboBox imagePlusCombo1 = new ImagePlusComboBox();
        JComboBox<ImagePlus> comboBoxPSF = new JComboBox<>(imagePlusCombo1);
        comboBoxPSF.setToolTipText("Measured PSF image, with the same voxel size as the input image");
        comboBoxPSF.setRenderer(new ImagePlusListCellRenderer());
        
        ImageJPopupMenuListener popupMenuListener1 = new ImageJPopupMenuListener(imagePlusCombo1);
        comboBoxPSF.addPopupMenuListener(popupMenuListener1);
        
        comboBoxPSF.addMouseListener(null);
        GridBagConstraints gbc_comboBoxPSF = new GridBagConstraints();
        gbc_comboBoxPSF.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBoxPSF.gridx = 1;
        gbc_comboBoxPSF.gridy = 0;
        psfFields = new ChannelFields<>(comboBoxPSF, this::copyImageCombo);
        panelMeasured.add(psfFields, gbc_comboBoxPSF);
        
        // Create a panel for the "Gibson Lanni" tab
        JPanel panelGibsonLanni = new JPanel();
        GridBagLayout gblPanelGibsonLanni = new GridBagLayout();
        gblPanelGibsonLanni.columnWidths = new int[]{0, 0, 0};
        gblPanelGibsonLanni.rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        gblPanelGibsonLanni.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gblPanelGibsonLanni.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        panelGibsonLanni.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelGibsonLanni.setLayout(gblPanelGibsonLanni);

        // Create a label for the x spacing field
        JLabel labelXYSpacing = new JLabel("XY spacing (μm)");

        // Create a spinner for the x spacing field
        SpinnerNumberModel model = new SpinnerNumberModel(0.1, 0.0, 20.0, 0.01);
        JSpinner spinner = new JSpinner(model);
        spinner.setToolTipText("Pixel size of the input image in X and Y (μm)");
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)spinner.getEditor();
        editor.getFormat().setMinimumFractionDigits(2);
        
        // Add an ActionListener to the spinner to update the xSpacing variable
        spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                modelController.setXYSpacing( ((Double)spinner.getValue()).floatValue());
            }
        });

        // Add the label and spinner to the panel
        GridBagConstraints gbc_labelXYSpacing = new GridBagConstraints();
        gbc_labelXYSpacing.anchor = GridBagConstraints.WEST;
        gbc_labelXYSpacing.insets = new Insets(0, 0, 5, 5);
        gbc_labelXYSpacing.gridx = 0;
        gbc_labelXYSpacing.gridy = 0;
        panelGibsonLanni.add(labelXYSpacing, gbc_labelXYSpacing);
        GridBagConstraints gbc_spinner = new GridBagConstraints();
        gbc_spinner.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinner.insets = new Insets(0, 0, 5, 0);
        gbc_spinner.gridx = 1;
        gbc_spinner.gridy = 0;
        panelGibsonLanni.add(spinner, gbc_spinner);

        // Add the panel to the tabbed pane
        tabbedPane.addTab("Gibson-Lanni", null, panelGibsonLanni, "Theoretical PSF from the Gibson-Lanni model");
        
        JLabel lblZSpacing = new JLabel("Z spacing (μm)");
        GridBagConstraints gbc_lblZSpacing = new GridBagConstraints();
        gbc_lblZSpacing.anchor = GridBagConstraints.WEST;
        gbc_lblZSpacing.insets = new Insets(0, 0, 5, 5);
        gbc_lblZSpacing.gridx = 0;
        gbc_lblZSpacing.gridy = 1;
        panelGibsonLanni.add(lblZSpacing, gbc_lblZSpacing);
        
        JSpinner spinnerZSpacing = new JSpinner();
        spinnerZSpacing.setToolTipText("Z step of the input image (μm)");
        spinnerZSpacing.setModel(new SpinnerNumberModel(0.3, 0.0, 20, 0.1));
        GridBagConstraints gbc_spinnerZSpacing = new GridBagConstraints();
        gbc_spinnerZSpacing.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerZSpacing.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerZSpacing.gridx = 1;
        gbc_spinnerZSpacing.gridy = 1;
        panelGibsonLanni.add(spinnerZSpacing, gbc_spinnerZSpacing);
        
        JLabel lblNA = new JLabel("Numerical aperture");
        GridBagConstraints gbc_lblNA = new GridBagConstraints();
        gbc_lblNA.anchor = GridBagConstraints.WEST;
        gbc_lblNA.insets = new Insets(0, 0, 5, 5);
        gbc_lblNA.gridx = 0;
        gbc_lblNA.gridy = 2;
        panelGibsonLanni.add(lblNA, gbc_lblNA);
        
        JSpinner spinnerNA = new JSpinner();
        spinnerNA.setToolTipText("Numerical aperture of the objective");
        spinnerNA.setModel(new SpinnerNumberModel(1.3, 0.0, 2, 0.1));
        GridBagConstraints gbc_spinnerNA = new GridBagConstraints();
        gbc_spinnerNA.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerNA.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerNA.gridx = 1;
        gbc_spinnerNA.gridy = 2;
        panelGibsonLanni.add(spinnerNA, gbc_spinnerNA);
        
        JLabel lblWavelength = new JLabel("Wavelength (μm)");
        GridBagConstraints gbc_lblWavelength = new GridBagConstraints();
        gbc_lblWavelength.anchor = GridBagConstraints.NORTHWEST;
        gbc_lblWavelength.insets = new Insets(0, 0, 5, 5);
        gbc_lblWavelength.gridx = 0;
        gbc_lblWavelength.gridy = 3;
        panelGibsonLanni.add(lblWavelength, gbc_lblWavelength);
        
        JSpinner spinnerWavelength = new JSpinner();
        spinnerWavelength.setToolTipText("Emission wavelength (μm)");
        spinnerWavelength.setModel(new SpinnerNumberModel(0.5, 0, 100, 0.1));
        GridBagConstraints gbc_spinnerWavelength = new GridBagConstraints();
        gbc_spinnerWavelength.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerWavelength.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerWavelength.gridx = 1;
        gbc_spinnerWavelength.gridy = 3;
        wavelengthFields = new ChannelFields<>(spinnerWavelength, this::copySpinner);
        panelGibsonLanni.add(wavelengthFields, gbc_spinnerWavelength);
        
        JLabel lblRiImersion = new JLabel("RI immersion");
        GridBagConstraints gbc_lblRiImersion = new GridBagConstraints();
        gbc_lblRiImersion.anchor = GridBagConstraints.WEST;
        gbc_lblRiImersion.insets = new Insets(0, 0, 5, 5);
        gbc_lblRiImersion.gridx = 0;
        gbc_lblRiImersion.gridy = 4;
        panelGibsonLanni.add(lblRiImersion, gbc_lblRiImersion);
        
        JSpinner spinnerRiImmersion = new JSpinner();
        spinnerRiImmersion.setToolTipText("Refractive index of the immersion medium (oil about 1.515, water 1.33)");
        spinnerRiImmersion.setModel(new SpinnerNumberModel(1.5, 0, 2, 0.1));
        GridBagConstraints gbc_spinnerRiImmersion = new GridBagConstraints();
        gbc_spinnerRiImmersion.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerRiImmersion.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerRiImmersion.gridx = 1;
        gbc_spinnerRiImmersion.gridy = 4;
        panelGibsonLanni.add(spinnerRiImmersion, gbc_spinnerRiImmersion);
        
        JLabel lblRiSample = new JLabel("RI sample");
        GridBagConstraints gbc_lblRiSample = new GridBagConstraints();
        gbc_lblRiSample.anchor = GridBagConstraints.WEST;
        gbc_lblRiSample.insets = new Insets(0, 0, 5, 5);
        gbc_lblRiSample.gridx = 0;
        gbc_lblRiSample.gridy = 5;
        panelGibsonLanni.add(lblRiSample, gbc_lblRiSample);
        
        JSpinner spinnerRiSample = new JSpinner();
        spinnerRiSample.setToolTipText("Refractive index of the sample");
        spinnerRiSample.setModel(new SpinnerNumberModel(1.4, 0, 2, 0.1));
        GridBagConstraints gbc_spinnerRiSample = new GridBagConstraints();
        gbc_spinnerRiSample.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerRiSample.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerRiSample.gridx = 1;
        gbc_spinnerRiSample.gridy = 5;
        panelGibsonLanni.add(spinnerRiSample, gbc_spinnerRiSample);
        
        // Add label for PSF Depth
        JLabel lblPsfDepth = new JLabel("PSF depth (μm)");
        GridBagConstraints gbc_lblPsfDepth = new GridBagConstraints();
        gbc_lblPsfDepth.anchor = GridBagConstraints.WEST;
        gbc_lblPsfDepth.insets = new Insets(0, 0, 5, 5);
        gbc_lblPsfDepth.gridx = 0;
        gbc_lblPsfDepth.gridy = 6; // Increment the grid y index
        panelGibsonLanni.add(lblPsfDepth, gbc_lblPsfDepth);

        // Add spinner for PSF Depth
        JSpinner spinnerPSFDepth = new JSpinner();
        spinnerPSFDepth.setToolTipText("Depth of the point source in the sample (μm); controls depth-induced spherical aberration");
        spinnerPSFDepth.setModel(new SpinnerNumberModel(0.1, 0.1, 10.0, 0.1)); // Example range and step size
        GridBagConstraints gbc_spinnerPSFDepth = new GridBagConstraints();
        gbc_spinnerPSFDepth.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerPSFDepth.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerPSFDepth.gridx = 1;
        gbc_spinnerPSFDepth.gridy = 6; // Increment the grid y index
        panelGibsonLanni.add(spinnerPSFDepth, gbc_spinnerPSFDepth);
        
        JLabel lblXYSize = new JLabel("PSF XY size (px)");
        GridBagConstraints gbc_lblXYSize = new GridBagConstraints();
        gbc_lblXYSize.anchor = GridBagConstraints.WEST;
        gbc_lblXYSize.insets = new Insets(0, 0, 5, 5);
        gbc_lblXYSize.gridx = 0;
        gbc_lblXYSize.gridy = 7;
        panelGibsonLanni.add(lblXYSize, gbc_lblXYSize);
        
        JSpinner spinnerXYSize = new JSpinner();
        spinnerXYSize.setToolTipText("Size of the generated PSF in X and Y (pixels)");
        spinnerXYSize.setModel(new SpinnerNumberModel(new Integer(128), null, null, new Integer(1)));
        GridBagConstraints gbc_spinnerXYSize = new GridBagConstraints();
        gbc_spinnerXYSize.fill = GridBagConstraints.BOTH;
        gbc_spinnerXYSize.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerXYSize.gridx = 1;
        gbc_spinnerXYSize.gridy = 7;
        panelGibsonLanni.add(spinnerXYSize, gbc_spinnerXYSize);
        
        JLabel lblZSize = new JLabel("PSF Z size (px)");
        GridBagConstraints gbc_lblZSize = new GridBagConstraints();
        gbc_lblZSize.anchor = GridBagConstraints.WEST;
        gbc_lblZSize.insets = new Insets(0, 0, 5, 5);
        gbc_lblZSize.gridx = 0;
        gbc_lblZSize.gridy = 8;
        panelGibsonLanni.add(lblZSize, gbc_lblZSize);
        
        JSpinner spinnerZSize = new JSpinner();
        spinnerZSize.setToolTipText("Size of the generated PSF in Z (pixels)");
        spinnerZSize.setModel(new SpinnerNumberModel(new Integer(128), null, null, new Integer(1)));
        GridBagConstraints gbc_spinnerZSize = new GridBagConstraints();
        gbc_spinnerZSize.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerZSize.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerZSize.gridx = 1;
        gbc_spinnerZSize.gridy = 8;
        panelGibsonLanni.add(spinnerZSize, gbc_spinnerZSize);
        
        JLabel lblConfocalFactor = new JLabel("Confocal factor");
        GridBagConstraints gbc_lblConfocalFactor = new GridBagConstraints();
        gbc_lblConfocalFactor.anchor = GridBagConstraints.WEST;
        gbc_lblConfocalFactor.insets = new Insets(0, 0, 0, 5);
        gbc_lblConfocalFactor.gridx = 0;
        gbc_lblConfocalFactor.gridy = 9;
        panelGibsonLanni.add(lblConfocalFactor, gbc_lblConfocalFactor);
        
        JSpinner spinnerConfocalFactor = new JSpinner();
        spinnerConfocalFactor.setToolTipText("1 = widefield, 2 = confocal (PSF squared); values in between approximate spinning disk");
        spinnerConfocalFactor.setModel(new SpinnerNumberModel(1.0, 1.0, 2.0, 0.1)); // Example range and step size
        GridBagConstraints gbc_spinnerConfocalFactor = new GridBagConstraints();
        gbc_spinnerConfocalFactor.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerConfocalFactor.gridx = 1;
        gbc_spinnerConfocalFactor.gridy = 9;
        panelGibsonLanni.add(spinnerConfocalFactor, gbc_spinnerConfocalFactor);
        
        JPanel panelGaussian = new JPanel();
        tabbedPane.addTab("Gaussian", null, panelGaussian, "Gaussian approximation of the PSF");
        GridBagLayout gbl_panelGaussian = new GridBagLayout();
        gbl_panelGaussian.columnWidths = new int[] {0, 0, 0};
        gbl_panelGaussian.rowHeights = new int[] {0, 0, 0};
        gbl_panelGaussian.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gbl_panelGaussian.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
        panelGaussian.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelGaussian.setLayout(gbl_panelGaussian);
        
        JLabel lblSigmaXY = new JLabel("Sigma XY (px)");
        GridBagConstraints gbc_lblSigmaXY = new GridBagConstraints();
        gbc_lblSigmaXY.anchor = GridBagConstraints.NORTHWEST;
        gbc_lblSigmaXY.insets = new Insets(0, 0, 5, 5);
        gbc_lblSigmaXY.gridx = 0;
        gbc_lblSigmaXY.gridy = 0;
        panelGaussian.add(lblSigmaXY, gbc_lblSigmaXY);
        
        JSpinner spinnerSigmaXY = new JSpinner();
        spinnerSigmaXY.setToolTipText("Gaussian sigma in X and Y (pixels)");
        spinnerSigmaXY.setModel(new SpinnerNumberModel(1.0, 0.001, 10.0, 0.1)); // Example range and step size
        GridBagConstraints gbc_spinnerSigmaXY = new GridBagConstraints();
        gbc_spinnerSigmaXY.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerSigmaXY.insets = new Insets(0, 0, 5, 5);
        gbc_spinnerSigmaXY.gridx = 1;
        gbc_spinnerSigmaXY.gridy = 0;
        sigmaXYFields = new ChannelFields<>(spinnerSigmaXY, this::copySpinner);
        panelGaussian.add(sigmaXYFields, gbc_spinnerSigmaXY);
        
        JLabel lblSigmaZ = new JLabel("Sigma Z (px)");
        GridBagConstraints gbc_lblSigmaZ = new GridBagConstraints();
        gbc_lblSigmaZ.anchor = GridBagConstraints.NORTHWEST;
        gbc_lblSigmaZ.insets = new Insets(0, 0, 0, 5);
        gbc_lblSigmaZ.gridx = 0;
        gbc_lblSigmaZ.gridy = 1;
        panelGaussian.add(lblSigmaZ, gbc_lblSigmaZ);
        
        JSpinner spinnerSigmaZ = new JSpinner();
        spinnerSigmaZ.setToolTipText("Gaussian sigma in Z (pixels)");
        spinnerSigmaZ.setModel(new SpinnerNumberModel(3.0, 0.001, 10.0, 0.1)); // Example range and step size
        GridBagConstraints gbc_spinnerSigmaZ = new GridBagConstraints();
        gbc_spinnerSigmaZ.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerSigmaZ.insets = new Insets(0, 0, 0, 5);
        gbc_spinnerSigmaZ.gridx = 1;
        gbc_spinnerSigmaZ.gridy = 1;
        sigmaZFields = new ChannelFields<>(spinnerSigmaZ, this::copySpinner);
        panelGaussian.add(sigmaZFields, gbc_spinnerSigmaZ);
        
        splitPane.setRightComponent(tabbedPane);
        
        JPanel panelExtractPSF = new JPanel();
        tabbedPane.addTab("From beads", null, panelExtractPSF, "Extract a PSF from an image of beads");
        GridBagLayout gbl_panelExtractPSF = new GridBagLayout();
        gbl_panelExtractPSF.columnWidths = new int[]{0, 0, 0};
        gbl_panelExtractPSF.rowHeights = new int[]{0, 0, 0};
        gbl_panelExtractPSF.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gbl_panelExtractPSF.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
        panelExtractPSF.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelExtractPSF.setLayout(gbl_panelExtractPSF);
        
        JLabel lblBeadImage = new JLabel("Bead image");
        GridBagConstraints gbc_lblBeadFile = new GridBagConstraints();
        gbc_lblBeadFile.insets = new Insets(0, 0, 5, 5);
        gbc_lblBeadFile.anchor = GridBagConstraints.NORTHWEST;
        gbc_lblBeadFile.gridx = 0;
        gbc_lblBeadFile.gridy = 0;
        panelExtractPSF.add(lblBeadImage, gbc_lblBeadFile);
        
        ImagePlusComboBox imagePlusComboBeads = new ImagePlusComboBox();
        JComboBox<ImagePlus> comboBoxBeads = new JComboBox<>(imagePlusComboBeads);
        comboBoxBeads.setToolTipText("Image of sub-resolution beads to extract the PSF from");
        comboBoxBeads.setRenderer(new ImagePlusListCellRenderer());
        
        ImageJPopupMenuListener popupMenuListenerBeads = new ImageJPopupMenuListener(imagePlusComboBeads);
        comboBoxBeads.addPopupMenuListener(popupMenuListenerBeads);
        
        comboBoxBeads.addMouseListener(null);
        GridBagConstraints gbc_comboBoxBeads = new GridBagConstraints();
        gbc_comboBoxBeads.insets = new Insets(0, 0, 5, 0);
        gbc_comboBoxBeads.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBoxBeads.gridx = 1;
        gbc_comboBoxBeads.gridy = 0;
        beadFields = new ChannelFields<>(comboBoxBeads, this::copyImageCombo);
        panelExtractPSF.add(beadFields, gbc_comboBoxBeads);
        
        JButton btnExtractPSF = new JButton("Extract PSF");
        btnExtractPSF.setToolTipText("Extract a PSF from the bead image");
        btnExtractPSF.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		List<ImagePlus> beads = new ArrayList<>();
        		for (JComboBox<ImagePlus> combo : beadFields.all()) beads.add((ImagePlus) combo.getSelectedItem());

        		new Thread("Extract PSF Thread") {
        			@Override
        			public void run() {
        				for (ImagePlus b : beads) {
        					if (b != null) modelController.computePSF(PSFTypeEnum.EXTRACTED_FROM_MULTIPLE_BEADS, ImageJFunctions.wrap(b));
        				}
        			}
        		}.start();
        	}
        });
        GridBagConstraints gbc_btnExtractPSF = new GridBagConstraints();
        gbc_btnExtractPSF.gridx = 1;
        gbc_btnExtractPSF.gridy = 1;
        panelExtractPSF.add(btnExtractPSF, gbc_btnExtractPSF);
         
        
        JPanel panel = new JPanel();
        JPanel panel2 = new JPanel();
        splitPane.setLeftComponent(panel);
        mainSplit.setBottomComponent(panel2);
        
        GridBagLayout gbl_panel2 = new GridBagLayout();
        gbl_panel2.columnWidths = new int[] {0, 0};
        gbl_panel2.rowHeights = new int[] {27, 0};
        gbl_panel2.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gbl_panel2.rowWeights = new double[]{0.0, 1.0};
        panel2.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        panel2.setLayout(gbl_panel2);
        
        JProgressBar progressBar = new JProgressBar();
        GridBagConstraints gbc_progressBar = new GridBagConstraints();
        gbc_progressBar.fill = GridBagConstraints.BOTH;
        gbc_progressBar.insets = new Insets(0, 0, 5, 0);
        gbc_progressBar.gridx = 0;
        gbc_progressBar.gridy = 0;
        panel2.add(progressBar, gbc_progressBar);
        
        LogPane textArea = new LogPane();

        // Wrap the log in a scroll pane
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        GridBagConstraints gbc_textArea = new GridBagConstraints();
        gbc_textArea.fill = GridBagConstraints.BOTH;
        gbc_textArea.gridx = 0;
        gbc_textArea.gridy = 1;
        panel2.add(scrollPane, gbc_textArea);
        
        GridBagLayout gbl_panel = new GridBagLayout();
        gbl_panel.columnWidths = new int[]{0, 0, 0};
        gbl_panel.rowHeights = new int[] {30, 0, 0, 0, 0, 0, 0, 0, 0};
        gbl_panel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(gbl_panel);
        
        JLabel lblInput = new JLabel("Input image");
        GridBagConstraints gbc_lblInput = new GridBagConstraints();
        gbc_lblInput.insets = new Insets(0, 0, 5, 5);
        gbc_lblInput.anchor = GridBagConstraints.WEST;
        gbc_lblInput.gridx = 0;
        gbc_lblInput.gridy = 1;
        panel.add(lblInput, gbc_lblInput);
       
        ImagePlusComboBox imagePlusCombo2 = new ImagePlusComboBox();
        JComboBox comboBoxInput = new JComboBox(imagePlusCombo2);
        comboBoxInput.setToolTipText("3D image to deconvolve");
        comboBoxInput.setRenderer(new ImagePlusListCellRenderer());
        
        ImageJPopupMenuListener popupMenuListener2 = new ImageJPopupMenuListener(imagePlusCombo2);
        comboBoxInput.addPopupMenuListener(popupMenuListener2);

        // defaults: the active image as input (unless it looks like a PSF and something
        // else is open); as PSF and bead image, an image named like one, never the input.
        // With no such image the dropdown starts empty, so a wrong PSF is never used silently.
        ImagePlus activeImage = WindowManager.getCurrentImage();
        if (activeImage != null) comboBoxInput.setSelectedItem(activeImage);
        ImagePlus input = (ImagePlus) comboBoxInput.getSelectedItem();
        if (input != null && looksLike(input, "psf")) {
            for (int i = 0; i < imagePlusCombo2.getSize(); i++) {
                ImagePlus candidate = imagePlusCombo2.getElementAt(i);
                if (candidate != null && !looksLike(candidate, "psf")) {
                    comboBoxInput.setSelectedItem(candidate);
                    input = candidate;
                    break;
                }
            }
        }
        selectByTitle(comboBoxPSF, "psf", input);
        selectByTitle(comboBoxBeads, "bead", input);

        // report the dimensionality of the selected input, so the user can check it
        final ImagePlus[] lastDescribed = { null };
        comboBoxInput.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                ImagePlus newInput = (ImagePlus) e.getItem();
                describeInput(newInput, textArea, lastDescribed);
                // a PSF or bead field showing the new input image gets the default instead
                for (JComboBox<ImagePlus> combo : psfFields.all()) {
                    if (combo.getSelectedItem() == newInput) selectByTitle(combo, "psf", newInput);
                }
                for (JComboBox<ImagePlus> combo : beadFields.all()) {
                    if (combo.getSelectedItem() == newInput) selectByTitle(combo, "bead", newInput);
                }
                setPsfChannels(newInput.getNChannels());
                replanCells.run();
            }
        });
        SwingUtilities.invokeLater(() -> {
            ImagePlus selected = (ImagePlus) comboBoxInput.getSelectedItem();
            describeInput(selected, textArea, lastDescribed);
            setPsfChannels(selected == null ? 1 : selected.getNChannels());
            replanCells.run();
        });

        GridBagConstraints gbc_comboBoxInput = new GridBagConstraints();
        gbc_comboBoxInput.insets = new Insets(0, 0, 5, 0);
        gbc_comboBoxInput.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBoxInput.gridx = 1;
        gbc_comboBoxInput.gridy = 1;
        panel.add(comboBoxInput, gbc_comboBoxInput);
        
        JLabel lblIterations = new JLabel("Iterations");
        GridBagConstraints gbc_lblIterations = new GridBagConstraints();
        gbc_lblIterations.anchor = GridBagConstraints.WEST;
        gbc_lblIterations.insets = new Insets(0, 0, 5, 5);
        gbc_lblIterations.gridx = 0;
        gbc_lblIterations.gridy = 2;
        panel.add(lblIterations, gbc_lblIterations);
        
        JSpinner spinnerIterations = new JSpinner();
        spinnerIterations.setToolTipText("Number of Richardson-Lucy iterations. More iterations sharpen further but can amplify noise.");
        GridBagConstraints gbc_spinnerIterations = new GridBagConstraints();
        gbc_spinnerIterations.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerIterations.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerIterations.gridx = 1;
        gbc_spinnerIterations.gridy = 2;
        panel.add(spinnerIterations, gbc_spinnerIterations);
        
        JLabel lblRegularizationFactor = new JLabel("Regularization factor");
        GridBagConstraints gbc_lblRegularizationFactor = new GridBagConstraints();
        gbc_lblRegularizationFactor.anchor = GridBagConstraints.WEST;
        gbc_lblRegularizationFactor.insets = new Insets(0, 0, 5, 5);
        gbc_lblRegularizationFactor.gridx = 0;
        gbc_lblRegularizationFactor.gridy = 3;
        panel.add(lblRegularizationFactor, gbc_lblRegularizationFactor);
        
        JSpinner spinnerRegularizationFactor = new JSpinner();
        spinnerRegularizationFactor.setToolTipText("Total variation regularization strength. 0 = off; 0.0002 is a typical start. Higher values suppress noise but smooth fine detail.");
        GridBagConstraints gbc_spinnerRegularizationFactor = new GridBagConstraints();
        gbc_spinnerRegularizationFactor.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerRegularizationFactor.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerRegularizationFactor.gridx = 1;
        gbc_spinnerRegularizationFactor.gridy = 3;
        panel.add(spinnerRegularizationFactor, gbc_spinnerRegularizationFactor);
        
        SpinnerNumberModel modelIterations = new SpinnerNumberModel(10, 0, 100, 1);
        spinnerIterations.setModel(new SpinnerNumberModel(100, 1, 10000, 1));
        
        SpinnerNumberModel modelRegularizationFactor = new SpinnerNumberModel(0.0002f, 0.0f, 100.0f, 0.0001f);
        spinnerRegularizationFactor.setModel(modelRegularizationFactor);
        JSpinner.NumberEditor editorFloat = new JSpinner.NumberEditor(spinnerRegularizationFactor, "#0.0000");
        spinnerRegularizationFactor.setEditor(editorFloat);
        
        JButton btnRun = new JButton("Run");
        btnRun.setToolTipText("Deconvolve the input image with the PSF from the selected tab");
                
        // cells (tiles) are planned automatically from the image, PSF and GPU memory;
        // tick this to type the cell size yourself
        JCheckBox chckbxManualCells = new JCheckBox("Set cell size manually");
        chckbxManualCells.setToolTipText("Off: cells are chosen automatically to fit GPU memory. On: use the cell size below.");
        GridBagConstraints gbc_chckbxManualCells = new GridBagConstraints();
        gbc_chckbxManualCells.anchor = GridBagConstraints.WEST;
        gbc_chckbxManualCells.insets = new Insets(0, 0, 5, 0);
        gbc_chckbxManualCells.gridx = 0;
        gbc_chckbxManualCells.gridwidth = 2;
        gbc_chckbxManualCells.gridy = 4;
        panel.add(chckbxManualCells, gbc_chckbxManualCells);

        String[] cellAxes = { "X", "Y", "Z" };
        int[] cellDefaults = { 256, 256, 128 };
        JLabel[] cellLabels = new JLabel[3];
        JSpinner[] cellSpinners = new JSpinner[3];
        for (int d = 0; d < 3; d++) {
            cellLabels[d] = new JLabel("Cell " + cellAxes[d] + " size (px)");
            GridBagConstraints gbcLabel = new GridBagConstraints();
            gbcLabel.anchor = GridBagConstraints.WEST;
            gbcLabel.insets = new Insets(0, 0, 5, 5);
            gbcLabel.gridx = 0;
            gbcLabel.gridy = 5 + d;
            panel.add(cellLabels[d], gbcLabel);

            cellSpinners[d] = new JSpinner(new SpinnerNumberModel(cellDefaults[d], 1, Integer.MAX_VALUE, 1));
            cellSpinners[d].setToolTipText("Cell size in " + cellAxes[d] + " (pixels)");
            GridBagConstraints gbcSpinner = new GridBagConstraints();
            gbcSpinner.fill = GridBagConstraints.HORIZONTAL;
            gbcSpinner.insets = new Insets(0, 0, 5, 0);
            gbcSpinner.gridx = 1;
            gbcSpinner.gridy = 5 + d;
            panel.add(cellSpinners[d], gbcSpinner);
        }

        // the size fields are editable only in manual mode; in auto mode they show the plan
        Runnable updateCellControls = () -> {
            for (int d = 0; d < 3; d++) cellSpinners[d].setEnabled(chckbxManualCells.isSelected());
        };
        updateCellControls.run();

        // GPU memory the automatic cell estimate may plan for. Lower it to leave room
        // for other programs, or to force cells on a small test image.
        JLabel lblGpuMemory = new JLabel("GPU memory to use (GB)");
        GridBagConstraints gbc_lblGpuMemory = new GridBagConstraints();
        gbc_lblGpuMemory.anchor = GridBagConstraints.WEST;
        gbc_lblGpuMemory.insets = new Insets(0, 0, 5, 5);
        gbc_lblGpuMemory.gridx = 0;
        gbc_lblGpuMemory.gridy = 9;
        panel.add(lblGpuMemory, gbc_lblGpuMemory);

        JSpinner spinnerGpuMemory = new JSpinner(new SpinnerNumberModel(4.0, 0.5, 1024.0, 0.5));
        spinnerGpuMemory.setToolTipText("GPU memory the automatic cell estimate plans for; defaults to 80% of the GPU");
        GridBagConstraints gbc_spinnerGpuMemory = new GridBagConstraints();
        gbc_spinnerGpuMemory.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerGpuMemory.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerGpuMemory.gridx = 1;
        gbc_spinnerGpuMemory.gridy = 9;
        panel.add(spinnerGpuMemory, gbc_spinnerGpuMemory);

        GridBagConstraints gbc_btnRun = new GridBagConstraints();
        gbc_btnRun.gridx = 1;
        gbc_btnRun.gridy = 10;
        panel.add(btnRun, gbc_btnRun);

        // plan the cells; in auto mode the plan fills the cell size fields and decides
        // whether Run divides the image into cells at all
        final String[] lastPlanKey = { null };
        final CellPlanner.Plan[] currentPlan = { null };

        // how many cells Run will use, always visible under the size fields
        JLabel lblCellCount = new JLabel(" ");
        lblCellCount.setFont(lblCellCount.getFont().deriveFont(Font.BOLD));
        GridBagConstraints gbc_lblCellCount = new GridBagConstraints();
        gbc_lblCellCount.anchor = GridBagConstraints.WEST;
        gbc_lblCellCount.insets = new Insets(0, 0, 5, 0);
        gbc_lblCellCount.gridx = 0;
        gbc_lblCellCount.gridwidth = 2;
        gbc_lblCellCount.gridy = 8;
        panel.add(lblCellCount, gbc_lblCellCount);

        // mirrors the decision Run makes (see the Run button's listener)
        Runnable updateCellCount = () -> {
            ImagePlus imp = (ImagePlus) comboBoxInput.getSelectedItem();
            if (imp == null) { lblCellCount.setText("Number of cells: -"); return; }
            CellPlanner.Plan plan = currentPlan[0];
            boolean manual = chckbxManualCells.isSelected();
            if (!manual && plan == null) {
                lblCellCount.setText("Number of cells: 1 (whole volume, no estimate yet)");
                return;
            }
            if (!manual && plan.feasible && !plan.needsCells()) {
                lblCellCount.setText("Number of cells: 1 (whole volume)");
                return;
            }
            long[] volume = { imp.getWidth(), imp.getHeight(), imp.getNSlices() };
            long[] div = new long[3];
            long n = 1;
            for (int d = 0; d < 3; d++) {
                long cell = ((Number) cellSpinners[d].getValue()).longValue();
                div[d] = (volume[d] + cell - 1) / cell;
                n *= div[d];
            }
            lblCellCount.setText("Number of cells: " + n + "  (" + div[0] + " x " + div[1] + " x " + div[2] + ")");
        };
        for (JSpinner s : cellSpinners) s.addChangeListener(e -> updateCellCount.run());

        // several controls change together (input switch, GPU read, new channel rows);
        // wait for them to settle so the log gets one plan, not one per change
        javax.swing.Timer replanTimer = new javax.swing.Timer(250, e -> {
            replanCellsAndLog.run();
            updateCellCount.run();
        });
        replanTimer.setRepeats(false);
        replanCells = replanTimer::restart;
        replanCellsAndLog = () -> {
            if (gpuMemoryBytes == 0) return; // GPU limits not read yet
            ImagePlus imp = (ImagePlus) comboBoxInput.getSelectedItem();
            if (imp == null) return;
            boolean manual = chckbxManualCells.isSelected();
            long[] psfSize;
            String psfSource;
            int tab = tabbedPane.getSelectedIndex();
            if (tab == 0) {
                // plan for the largest PSF, so the cells fit for every channel
                psfSize = new long[3];
                Set<String> titles = new LinkedHashSet<>();
                for (JComboBox<ImagePlus> combo : psfFields.all()) {
                    ImagePlus psfImp = (ImagePlus) combo.getSelectedItem();
                    if (psfImp == null) return;
                    psfSize[0] = Math.max(psfSize[0], psfImp.getWidth());
                    psfSize[1] = Math.max(psfSize[1], psfImp.getHeight());
                    psfSize[2] = Math.max(psfSize[2], psfImp.getNSlices());
                    titles.add(psfImp.getTitle());
                }
                psfSource = (titles.size() == 1 ? "measured PSF " : "largest of measured PSFs ")
                    + String.join(", ", titles);
            }
            else if (tab == 1) {
                long xy = ((Number) spinnerXYSize.getValue()).longValue();
                psfSize = new long[] { xy, xy, ((Number) spinnerZSize.getValue()).longValue() };
                psfSource = "Gibson-Lanni PSF";
            }
            else if (tab == 2) {
                // approximate size of the Gaussian kernel ops creates: about 3 sigma each side;
                // the largest sigma over the channels
                double sxy = 0, sz = 0;
                for (JSpinner s : sigmaXYFields.all()) sxy = Math.max(sxy, ((Number) s.getValue()).doubleValue());
                for (JSpinner s : sigmaZFields.all()) sz = Math.max(sz, ((Number) s.getValue()).doubleValue());
                long xy = 2 * (long) Math.ceil(3 * sxy) + 1;
                long z = 2 * (long) Math.ceil(3 * sz) + 1;
                psfSize = new long[] { xy, xy, z };
                psfSource = "Gaussian PSF (approximate size)";
            }
            else {
                // the bead PSF's size is only known once it is extracted
                currentPlan[0] = null;
                lastPlanKey[0] = null;
                return;
            }
            long[] volume = { imp.getWidth(), imp.getHeight(), imp.getNSlices() };
            double budgetGB = ((Number) spinnerGpuMemory.getValue()).doubleValue();
            String key = imp.getID() + " " + psfSource + " " + Arrays.toString(volume) + " " + Arrays.toString(psfSize) + " "
                + budgetGB + " " + manual;
            if (key.equals(lastPlanKey[0])) return;
            lastPlanKey[0] = key;

            CellPlanner.Plan plan = CellPlanner.plan(volume, psfSize, new long[] { 10, 10, 10 },
                (long) (budgetGB * (1L << 30)), gpuMaxAllocationBytes,
                CellPlanner.DEFAULT_MEMORY_FACTOR, 1.0, CellPlanner.DEFAULT_Z_SPLIT_PENALTY);
            currentPlan[0] = plan;
            textArea.section(manual ? "Cells (manual size)" : "Cells (automatic)");
            textArea.info(String.format(Locale.ROOT, "%d x %d x %d volume, %s %d x %d x %d, GPU memory to use %.1f GB",
                volume[0], volume[1], volume[2], psfSource, psfSize[0], psfSize[1], psfSize[2], budgetGB));
            for (String line : plan.describe().split("\n")) {
                if (!plan.feasible || line.startsWith("Splits z")) textArea.warn(line);
                else textArea.info(line);
            }
            if (manual) {
                long[] cell = new long[3];
                long manualCells = 1;
                for (int d = 0; d < 3; d++) {
                    cell[d] = ((Number) cellSpinners[d].getValue()).longValue();
                    manualCells *= (volume[d] + cell[d] - 1) / cell[d];
                }
                textArea.info("Run will use the manual size " + cell[0] + " x " + cell[1] + " x " + cell[2]
                    + ": " + manualCells + " cell" + (manualCells == 1 ? "" : "s")
                    + " (automatic would use " + plan.numberOfCells() + ").");
            }
            else if (plan.feasible) {
                long[] cell = plan.needsCells() ? plan.cellSize : volume;
                for (int d = 0; d < 3; d++) cellSpinners[d].setValue((int) cell[d]);
                textArea.info(plan.needsCells() ? "Run will use these cells." : "Run will process the volume in one piece.");
            }
            else {
                textArea.warn("Run will try the cell size in the fields, which may not fit; "
                    + "lower the cell size manually or free GPU memory.");
            }
        };
        chckbxManualCells.addItemListener(e -> {
            updateCellControls.run();
            replanCells.run();
        });
        comboBoxPSF.addItemListener(e -> { if (e.getStateChange() == ItemEvent.SELECTED) replanCells.run(); });
        tabbedPane.addChangeListener(e -> replanCells.run());
        for (JSpinner s : new JSpinner[] { spinnerGpuMemory, spinnerXYSize, spinnerZSize, spinnerSigmaXY,
            spinnerSigmaZ }) {
            s.addChangeListener(e -> replanCells.run());
        }

        // read the GPU's limits once, off the event thread (creating the OpenCL context takes a moment)
        new Thread(() -> {
            try {
                CLIJ2 clij2 = CLIJ2.getInstance("RTX"); // same device the deconvolution uses
                gpuName = clij2.getGPUName();
                gpuMaxAllocationBytes = clij2.getCLIJ().getClearCLContext().getDevice()
                    .getMaxMemoryAllocationSizeInBytes();
                long total = clij2.getCLIJ().getGPUMemoryInBytes();
                SwingUtilities.invokeLater(() -> {
                    textArea.section("GPU: " + gpuName);
                    textArea.info(String.format(Locale.ROOT, "%.1f GB, largest single buffer %.1f GB",
                        total / (double) (1L << 30), gpuMaxAllocationBytes / (double) (1L << 30)));
                    gpuMemoryBytes = total;
                    // default to 80% of the GPU, or to the clij2fft.gpuMemoryGB system property
                    // (dev scripts lower it to force cells on small test images);
                    // setting it re-runs the estimate
                    double defaultGB = Math.floor(0.8 * total / (double) (1L << 30) * 2) / 2;
                    String override = System.getProperty("clij2fft.gpuMemoryGB");
                    if (override != null) {
                        try {
                            defaultGB = Double.parseDouble(override);
                            textArea.warn("GPU memory to use set to " + defaultGB + " GB by clij2fft.gpuMemoryGB");
                        }
                        catch (NumberFormatException nfe) {
                            textArea.warn("Ignoring clij2fft.gpuMemoryGB=" + override + " (not a number)");
                        }
                    }
                    if (((Number) spinnerGpuMemory.getValue()).doubleValue() == defaultGB) replanCells.run();
                    else spinnerGpuMemory.setValue(defaultGB);
                });
            }
            catch (Exception ex) {
                SwingUtilities.invokeLater(() -> textArea.warn("Could not read GPU memory: " + ex));
            }
        }, "GPU info").start();
        
        btnRun.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		ImagePlus imp = (ImagePlus)comboBoxInput.getSelectedItem();
        		if (imp == null) {
        			textArea.warn("Choose an input image.");
        			return;
        		}
        		// a pending re-plan decides whether Run uses cells: do it now
        		if (replanTimer.isRunning()) {
        			replanTimer.stop();
        			replanCellsAndLog.run();
        			updateCellCount.run();
        		}
        		
        		   // Get values from GUI components
                int iterations = (int) spinnerIterations.getValue();
                float regularizationFactor = ((Number) spinnerRegularizationFactor.getValue()).floatValue();
                // manual: always cells of the typed size. Auto: cells only if the plan needs them;
                // with no plan (GPU not read yet, or a bead PSF) run in one piece and say so
                boolean useCells;
                if (chckbxManualCells.isSelected()) useCells = true;
                else if (currentPlan[0] == null) {
                    useCells = false;
                    textArea.warn("No automatic cell plan for this PSF yet; running the volume in one piece.");
                }
                else useCells = currentPlan[0].needsCells() || !currentPlan[0].feasible;
                int xCellSize = (int) cellSpinners[0].getValue();
                int yCellSize = (int) cellSpinners[1].getValue();
                int zCellSize = (int) cellSpinners[2].getValue();
                float xySpacing = ((Number) spinner.getValue()).floatValue();
                float zSpacing = ((Number) spinnerZSpacing.getValue()).floatValue();
                float wavelength = ((Number) spinnerWavelength.getValue()).floatValue();
                float NA = ((Number) spinnerNA.getValue()).floatValue();
                float riImmersion = ((Number) spinnerRiImmersion.getValue()).floatValue();
                float riSample = ((Number) spinnerRiSample.getValue()).floatValue();
                float PSFDepth = ((Number) spinnerPSFDepth.getValue()).floatValue();
                int XYSize = (int) spinnerXYSize.getValue();
                int ZSize = (int) spinnerZSize.getValue();
                float confocalFactor = ((Number) spinnerConfocalFactor.getValue()).floatValue();
                float sigmaXY = ((Number) spinnerSigmaXY.getValue()).floatValue();
                float sigmaZ = ((Number) spinnerSigmaZ.getValue()).floatValue();

                // Update model using setters
                modelController.setIterations(iterations);
                modelController.setRegularizaitonFactor(regularizationFactor);
                modelController.setUseCells(useCells);
                modelController.setXCellSize(xCellSize);
                modelController.setYCellSize(yCellSize);
                modelController.setzCellSize(zCellSize);
                modelController.setXYSpacing(xySpacing);
                modelController.setZSpacing(zSpacing);
                modelController.setWavelength(wavelength);
                modelController.setNA(NA);
                modelController.setRiImmersion(riImmersion);
                modelController.setRiSample(riSample);
                modelController.setPSFDepth(PSFDepth);
                modelController.setPsfXYSize(XYSize);
                modelController.setPsfZSize(ZSize);
                modelController.setSigmaXY(sigmaXY);
                modelController.setSigmaZ(sigmaZ);
                modelController.setConfocalFactor(confocalFactor);
        		
        		System.out.println("run decon on "+imp.getTitle());
        		System.out.println("x spacing "+modelController.getXYSpacing());
        		System.out.println("PSF Tab "+tabbedPane.getSelectedIndex());
        		
        		PSFTypeEnum psfType;
        		final int nChannels = imp.getNChannels();
        		final Img[] psfImgs = new Img[nChannels];
        		setPsfChannels(nChannels);
        		String ch = nChannels > 1 ? "Channel %d " : "";
        		textArea.section("Run");

        		if (tabbedPane.getSelectedIndex()==0) {
        			psfType = PSFTypeEnum.MEASURED_SINGLE_BEAD;
        			Set<ImagePlus> distinct = new HashSet<>();
        			for (int c = 0; c < nChannels; c++) {
        				ImagePlus p = (ImagePlus) psfFields.get(c).getSelectedItem();
        				if (p == null) {
        					textArea.warn("Choose a PSF for channel " + (c + 1) + ".");
        					return;
        				}
        				distinct.add(p);
        				String which = p.getTitle();
        				if (p.getNChannels() > 1) {
        					// a multichannel PSF image: use its matching channel (or its last one)
        					int pc = Math.min(c + 1, p.getNChannels());
        					which += " (its channel " + pc + ")";
        					p = new Duplicator().run(p, pc, pc, 1, p.getNSlices(), 1, 1);
        				}
        				textArea.info((nChannels > 1 ? "Channel " + (c + 1) + " " : "") + "measured PSF " + which);
        				psfImgs[c] = ImageJFunctions.convertFloat(p);
        			}
        			if (nChannels > 1 && distinct.size() == 1 && distinct.iterator().next().getNChannels() == 1) {
        				textArea.warn("All " + nChannels + " channels use the same PSF image.");
        			}
        		}
        		else if (tabbedPane.getSelectedIndex()==1) {
        			psfType = PSFTypeEnum.GIBSON_LANNI;
        			Set<Float> distinct = new HashSet<>();
        			for (int c = 0; c < nChannels; c++) {
        				float wl = ((Number) wavelengthFields.get(c).getValue()).floatValue();
        				distinct.add(wl);
        				textArea.info(String.format(Locale.ROOT, ch + "Gibson-Lanni PSF, wavelength %.3g μm",
        					nChannels > 1 ? new Object[] { c + 1, wl } : new Object[] { wl }));
        				modelController.setWavelength(wl);
        				psfImgs[c] = modelController.computePSF(psfType, null);
        			}
        			if (nChannels > 1 && distinct.size() == 1) {
        				textArea.warn("All " + nChannels + " channels use the same wavelength.");
        			}
        		}
        		else if (tabbedPane.getSelectedIndex()==2) {
        			psfType = PSFTypeEnum.GAUSSIAN;
        			Set<String> distinct = new HashSet<>();
        			for (int c = 0; c < nChannels; c++) {
        				float sxy = ((Number) sigmaXYFields.get(c).getValue()).floatValue();
        				float sz = ((Number) sigmaZFields.get(c).getValue()).floatValue();
        				distinct.add(sxy + " " + sz);
        				textArea.info(String.format(Locale.ROOT, ch + "Gaussian PSF, sigma XY %.3g px, Z %.3g px",
        					nChannels > 1 ? new Object[] { c + 1, sxy, sz } : new Object[] { sxy, sz }));
        				modelController.setSigmaXY(sxy);
        				modelController.setSigmaZ(sz);
        				psfImgs[c] = modelController.computePSF(psfType, null);
        			}
        			if (nChannels > 1 && distinct.size() == 1) {
        				textArea.warn("All " + nChannels + " channels use the same sigmas.");
        			}
        		}
        		else {
        			psfType = PSFTypeEnum.EXTRACTED_FROM_MULTIPLE_BEADS;
        			Set<ImagePlus> distinct = new HashSet<>();
        			for (int c = 0; c < nChannels; c++) {
        				ImagePlus b = (ImagePlus) beadFields.get(c).getSelectedItem();
        				if (b == null) {
        					textArea.warn("Choose a bead image for channel " + (c + 1) + ".");
        					return;
        				}
        				distinct.add(b);
        				textArea.info(String.format(ch + "PSF extracted from beads %s",
        					nChannels > 1 ? new Object[] { c + 1, b.getTitle() } : new Object[] { b.getTitle() }));
        				psfImgs[c] = modelController.computePSF(psfType, ImageJFunctions.wrap(b));
        				if (psfImgs[c] == null) {
        					textArea.warn("Could not extract a PSF from " + b.getTitle() + ".");
        					return;
        				}
        			}
        			if (nChannels > 1 && distinct.size() == 1) {
        				textArea.warn("All " + nChannels + " channels use the same bead image.");
        			}
        		}

        		modelController.setPSFType(psfType);

        		new Thread("Deconvolution Thread") {
        			@Override
        			public void run() {
        				modelController.runDeconvolution(imp, psfImgs);
        			}
        		}.start();
        		
        	}
        });
        
        // Create an instance of the StatusService interface
        this.statusService = new StatusService() {
            @Override
            public void showProgress(int value, int maximum) {
                System.out.println("Progress: " + value + " out of " + maximum);
            }

            @Override
            public void showStatus(String message) {
                System.out.println("Status message: " + message);
            }

            @Override
            public void showStatus(int progress, int maximum, String message) {
            	if (message != null && !message.isEmpty()) textArea.info(message);
            	Runnable progressUpdate = () -> {
	            	progressBar.setMaximum(maximum);
	            	progressBar.setValue(progress);
            	};
            	if (SwingUtilities.isEventDispatchThread()) progressUpdate.run();
            	else SwingUtilities.invokeLater(progressUpdate);
            }

            @Override
            public void showStatus(int progress, int maximum, String message, boolean warn) {
                if (warn) textArea.warn(message);
                else showStatus(progress, maximum, message);
            }

            @Override
            public void warn(String message) {
                textArea.warn(message);
            }

            @Override
            public void clearStatus() {
                System.out.println("Clearing status");
            }

         	@Override
			public Context context() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Context getContext() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public double getPriority() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public void setPriority(double priority) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public PluginInfo<?> getInfo() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setInfo(PluginInfo<?> info) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public String getStatusMessage(String appName, StatusEvent statusEvent) {
				// TODO Auto-generated method stub
				return null;
			}
        };
        

    }

    /**
     * Shows one PSF field per input channel on every PSF tab. New fields start
     * with channel 1's value; existing ones keep theirs.
     */
    private void setPsfChannels(int nChannels) {
        for (ChannelFields<?> f : new ChannelFields<?>[] { psfFields, beadFields, wavelengthFields,
            sigmaXYFields, sigmaZFields }) {
            f.setChannels(nChannels);
        }
    }

    private static boolean looksLike(ImagePlus imp, String hint) {
        return imp.getTitle().toLowerCase(Locale.ROOT).contains(hint);
    }

    /**
     * Selects the first image whose title contains {@code hint} and that is not
     * {@code exclude}; with none, leaves the dropdown empty.
     */
    private static void selectByTitle(JComboBox<ImagePlus> combo, String hint, ImagePlus exclude) {
        for (int i = 0; i < combo.getModel().getSize(); i++) {
            ImagePlus candidate = combo.getModel().getElementAt(i);
            if (candidate != null && candidate != exclude && looksLike(candidate, hint)) {
                combo.setSelectedItem(candidate);
                return;
            }
        }
        combo.setSelectedItem(null);
    }

    /** Another image dropdown like {@code first}, starting on the same image. */
    private JComboBox<ImagePlus> copyImageCombo(JComboBox<ImagePlus> first) {
        ImagePlusComboBox model = new ImagePlusComboBox();
        JComboBox<ImagePlus> combo = new JComboBox<>(model);
        combo.setRenderer(new ImagePlusListCellRenderer());
        combo.addPopupMenuListener(new ImageJPopupMenuListener(model));
        combo.setSelectedItem(first.getSelectedItem());
        combo.setToolTipText(first.getToolTipText());
        combo.addItemListener(e -> { if (e.getStateChange() == ItemEvent.SELECTED) replanCells.run(); });
        return combo;
    }

    /** Another number spinner like {@code first}, starting on the same value. */
    private JSpinner copySpinner(JSpinner first) {
        SpinnerNumberModel m = (SpinnerNumberModel) first.getModel();
        JSpinner s = new JSpinner(new SpinnerNumberModel(m.getNumber(), m.getMinimum(), m.getMaximum(), m.getStepSize()));
        s.setToolTipText(first.getToolTipText());
        s.addChangeListener(e -> replanCells.run());
        return s;
    }

    /**
     * Writes one line describing the input image's dimensions to the status text.
     * Skips the image it described last, since refreshing the image list re-selects.
     */
    private static void describeInput(ImagePlus imp, LogPane log, ImagePlus[] lastDescribed) {
        if (imp == null || imp == lastDescribed[0]) return;
        lastDescribed[0] = imp;

        Calibration cal = imp.getCalibration();
        int c = imp.getNChannels(), z = imp.getNSlices(), t = imp.getNFrames();
        log.section("Input image selected: " + imp.getTitle());
        log.info(String.format(
            "%d x %d x %d (XYZ), %d channel%s, %d timepoint%s, %d-bit, voxel %.4g x %.4g x %.4g %s",
            imp.getWidth(), imp.getHeight(), z,
            c, c == 1 ? "" : "s", t, t == 1 ? "" : "s", imp.getBitDepth(),
            cal.pixelWidth, cal.pixelHeight, cal.pixelDepth, cal.getUnit()));
        if (c > 1) {
            log.warn("Each of the " + c + " channels is deconvolved separately. "
                + "The Measured tab has a PSF per channel; the other PSF types use one PSF for all channels.");
        }
        if (t > 1) {
            log.warn("Only the first of " + t + " time points is deconvolved.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RichardsonLucyGUI().setVisible(true));
    }
}
