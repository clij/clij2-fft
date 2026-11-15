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

import java.awt.BorderLayout;
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
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import java.awt.Component;

//Functional interface with three parameters and no return value
interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
}

public class RichardsonLucyGUI extends JFrame {
	
	StatusService statusService;
	
	RichardsonLucyModelController modelController;  
	
	public void initiateModel(OpService ops, LogService log, StatusService status) {
		this.modelController = new RichardsonLucyModelController(ops, log, this.statusService);
	}
	
    public RichardsonLucyGUI() {
    	setTitle("CLIJ 3D Deconvolution");
        // Set the layout manager for the content pane
        getContentPane().setLayout(new BorderLayout());
        
        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.5);
        getContentPane().add(splitPane, BorderLayout.CENTER);
        
        JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane2.setResizeWeight(0.5);
       
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
        gbc_lblPSF.anchor = GridBagConstraints.WEST;
        gbc_lblPSF.gridx = 0;
        gbc_lblPSF.gridy = 0;
        panelMeasured.add(lblPSF, gbc_lblPSF);
        
        ImagePlusComboBox imagePlusCombo1 = new ImagePlusComboBox();
        JComboBox comboBoxPSF = new JComboBox(imagePlusCombo1);
        comboBoxPSF.setToolTipText("Measured PSF image, with the same voxel size as the input image");
        comboBoxPSF.setRenderer(new ImagePlusListCellRenderer());
        
        ImageJPopupMenuListener popupMenuListener1 = new ImageJPopupMenuListener(imagePlusCombo1);
        comboBoxPSF.addPopupMenuListener(popupMenuListener1);
        
        comboBoxPSF.addMouseListener(null);
        GridBagConstraints gbc_comboBoxPSF = new GridBagConstraints();
        gbc_comboBoxPSF.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBoxPSF.gridx = 1;
        gbc_comboBoxPSF.gridy = 0;
        panelMeasured.add(comboBoxPSF, gbc_comboBoxPSF);
        
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
        gbc_lblWavelength.anchor = GridBagConstraints.WEST;
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
        panelGibsonLanni.add(spinnerWavelength, gbc_spinnerWavelength);
        
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
        gbc_lblSigmaXY.anchor = GridBagConstraints.WEST;
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
        panelGaussian.add(spinnerSigmaXY, gbc_spinnerSigmaXY);
        
        JLabel lblSigmaZ = new JLabel("Sigma Z (px)");
        GridBagConstraints gbc_lblSigmaZ = new GridBagConstraints();
        gbc_lblSigmaZ.anchor = GridBagConstraints.WEST;
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
        panelGaussian.add(spinnerSigmaZ, gbc_spinnerSigmaZ);
        
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
        gbc_lblBeadFile.anchor = GridBagConstraints.WEST;
        gbc_lblBeadFile.gridx = 0;
        gbc_lblBeadFile.gridy = 0;
        panelExtractPSF.add(lblBeadImage, gbc_lblBeadFile);
        
        ImagePlusComboBox imagePlusComboBeads = new ImagePlusComboBox();
        JComboBox comboBoxBeads = new JComboBox(imagePlusComboBeads);
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
        panelExtractPSF.add(comboBoxBeads, gbc_comboBoxBeads);
        
        JButton btnExtractPSF = new JButton("Extract PSF");
        btnExtractPSF.setToolTipText("Extract a PSF from the bead image");
        btnExtractPSF.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		ImagePlus beads= (ImagePlus)comboBoxBeads.getSelectedItem();
        		
        		new Thread("Extract PSF Thread") {
        			@Override
        			public void run() {
        				modelController.computePSF(PSFTypeEnum.EXTRACTED_FROM_MULTIPLE_BEADS, ImageJFunctions.wrap(beads));
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
        splitPane.setLeftComponent(splitPane2);
        splitPane2.setTopComponent(panel);
        splitPane2.setBottomComponent(panel2);
        
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
        
        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setEditable(false);

        // Wrap the text area in a scroll pane
        JScrollPane scrollPane = new JScrollPane(textArea);
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
                
        JCheckBox chckbxNewCheckBox = new JCheckBox("Divide into cells");
        chckbxNewCheckBox.setToolTipText("Process the image in cells (tiles) so large images fit in GPU memory");
        GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
        gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
        gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 0);
        gbc_chckbxNewCheckBox.gridx = 0;
        gbc_chckbxNewCheckBox.gridwidth = 2;
        gbc_chckbxNewCheckBox.gridy = 4;
        panel.add(chckbxNewCheckBox, gbc_chckbxNewCheckBox);
        
        JLabel lblNumCells = new JLabel("Cell XY size (px)");
        GridBagConstraints gbc_lblNumCells = new GridBagConstraints();
        gbc_lblNumCells.anchor = GridBagConstraints.WEST;
        gbc_lblNumCells.insets = new Insets(0, 0, 5, 5);
        gbc_lblNumCells.gridx = 0;
        gbc_lblNumCells.gridy = 5;
        panel.add(lblNumCells, gbc_lblNumCells);
        
        JSpinner spinner_1 = new JSpinner();
        spinner_1.setToolTipText("Cell size in X and Y (pixels)");
        spinner_1.setModel(new SpinnerNumberModel(new Integer(256), null, null, new Integer(1)));
        GridBagConstraints gbc_spinner_1 = new GridBagConstraints();
        gbc_spinner_1.fill = GridBagConstraints.BOTH;
        gbc_spinner_1.insets = new Insets(0, 0, 5, 0);
        gbc_spinner_1.gridx = 1;
        gbc_spinner_1.gridy = 5;
        panel.add(spinner_1, gbc_spinner_1);
        
        JLabel lblNewLabel = new JLabel("Cell Z size (px)");
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
        gbc_lblNewLabel.gridx = 0;
        gbc_lblNewLabel.gridy = 6;
        panel.add(lblNewLabel, gbc_lblNewLabel);
        
        JSpinner spinner_2 = new JSpinner();
        spinner_2.setToolTipText("Cell size in Z (pixels)");
        spinner_2.setModel(new SpinnerNumberModel(new Integer(128), null, null, new Integer(1)));
        GridBagConstraints gbc_spinner_2 = new GridBagConstraints();
        gbc_spinner_2.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinner_2.insets = new Insets(0, 0, 5, 0);
        gbc_spinner_2.gridx = 1;
        gbc_spinner_2.gridy = 6;
        panel.add(spinner_2, gbc_spinner_2);
        
        // the cell sizes only matter when the image is divided into cells
        Component[] cellControls = {lblNumCells, spinner_1, lblNewLabel, spinner_2};
        chckbxNewCheckBox.addItemListener(e -> {
        	for (Component c : cellControls) c.setEnabled(chckbxNewCheckBox.isSelected());
        });
        for (Component c : cellControls) c.setEnabled(false);
        GridBagConstraints gbc_btnRun = new GridBagConstraints();
        gbc_btnRun.gridx = 1;
        gbc_btnRun.gridy = 7;
        panel.add(btnRun, gbc_btnRun);
        
        btnRun.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		ImagePlus imp = (ImagePlus)comboBoxInput.getSelectedItem();
        		ImagePlus psf = (ImagePlus)comboBoxPSF.getSelectedItem();
        		ImagePlus beads= (ImagePlus)comboBoxBeads.getSelectedItem();
        		
        		   // Get values from GUI components
                int iterations = (int) spinnerIterations.getValue();
                float regularizationFactor = ((Number) spinnerRegularizationFactor.getValue()).floatValue();
                boolean useCells = chckbxNewCheckBox.isSelected();
                int xyCellSize = (int) spinner_1.getValue();
                int zCellSize = (int) spinner_2.getValue();
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
                modelController.setXyCellSize(xyCellSize);
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
        		
        		System.out.println("run decon on "+imp.getTitle()+" with PSF "+psf.getTitle());
        		System.out.println("x spacing "+modelController.getXYSpacing());
        		System.out.println("PSF Tab "+tabbedPane.getSelectedIndex());
        		
        		PSFTypeEnum psfType;
        		final Img psfImg;
        		
        		if (tabbedPane.getSelectedIndex()==0) {
        			statusService.showStatus(1, 2, "Using Measured PSF");
        			psfType = PSFTypeEnum.MEASURED_SINGLE_BEAD;
        			psfImg = ImageJFunctions.convertFloat(psf);
        		}
        		else if (tabbedPane.getSelectedIndex()==1) {
        			psfType = PSFTypeEnum.GIBSON_LANNI;
        			psfImg = modelController.computePSF(psfType, null);
        		}
        		else if (tabbedPane.getSelectedIndex()==2) {
        			psfType = PSFTypeEnum.GAUSSIAN;
        			psfImg = modelController.computePSF(psfType, null);
        		}
        		else {
        			psfType = PSFTypeEnum.EXTRACTED_FROM_MULTIPLE_BEADS;
        			psfImg = modelController.computePSF(psfType, ImageJFunctions.wrap(beads));
        		}
        		
        		modelController.setPSFType(psfType);
        		
        		new Thread("Deconvolution Thread") {
        			@Override
        			public void run() {
        				modelController.runDeconvolution(imp, psfImg, 100);
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
            	if (SwingUtilities.isEventDispatchThread()) {
	            	textArea.append(message);
	            	textArea.append("\n");
	            	progressBar.setMaximum(maximum);
	            	progressBar.setValue(progress);
            	}
            	else {
	            	SwingUtilities.invokeLater(() -> {
		            	textArea.append(message);
		            	textArea.append("\n");
		            	progressBar.setMaximum(maximum);
		            	progressBar.setValue(progress);
	            	});
            	}
            }

            @Override
            public void showStatus(int progress, int maximum, String message, boolean warn) {
                System.out.println("Progress: " + progress + " out of " + maximum + ", Status message: " + message + ", Warn: " + warn);
            }

            @Override
            public void warn(String message) {
                System.out.println("Warning: " + message);
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
       
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RichardsonLucyGUI().setVisible(true));
    }
}
