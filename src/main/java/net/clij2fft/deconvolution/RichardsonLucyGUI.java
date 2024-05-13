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

import org.apache.commons.lang3.tuple.Triple;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.app.event.StatusEvent;
import org.scijava.log.LogService;
import org.scijava.plugin.PluginInfo;
import org.scijava.ui.UIService;

import ij.ImagePlus;
import ij.WindowManager;
import net.imagej.ops.OpService;

import javax.swing.JSplitPane;
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
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JProgressBar;
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
        // Set the layout manager for the content pane
        getContentPane().setLayout(new BorderLayout());
        
        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.5);
        getContentPane().add(splitPane, BorderLayout.CENTER);
        
        JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane2.setResizeWeight(0.5);
       
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        
        JPanel panel_1 = new JPanel();
        tabbedPane.addTab("Measured Input", panel_1);
        GridBagLayout gbl_panel_1 = new GridBagLayout();
        gbl_panel_1.columnWidths = new int[]{0, 0, 0};
        gbl_panel_1.rowHeights = new int[]{0, 0};
        gbl_panel_1.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gbl_panel_1.rowWeights = new double[]{0.0, Double.MIN_VALUE};
        panel_1.setLayout(gbl_panel_1);
        
        JLabel lblPSF = new JLabel("Input PSF");
        GridBagConstraints gbc_lblPSF = new GridBagConstraints();
        gbc_lblPSF.insets = new Insets(0, 0, 0, 5);
        gbc_lblPSF.anchor = GridBagConstraints.EAST;
        gbc_lblPSF.gridx = 0;
        gbc_lblPSF.gridy = 0;
        panel_1.add(lblPSF, gbc_lblPSF);
        
        JComboBox comboBoxPSF = new JComboBox(new ImagePlusComboBox());
        GridBagConstraints gbc_comboBoxPSF = new GridBagConstraints();
        gbc_comboBoxPSF.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBoxPSF.gridx = 1;
        gbc_comboBoxPSF.gridy = 0;
        panel_1.add(comboBoxPSF, gbc_comboBoxPSF);
        
        // Create a panel for the "Gibson Lanni" tab
        JPanel panelGibsonLanni = new JPanel();
        GridBagLayout gblPanelGibsonLanni = new GridBagLayout();
        gblPanelGibsonLanni.columnWidths = new int[]{0, 0, 0};
        gblPanelGibsonLanni.rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        gblPanelGibsonLanni.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gblPanelGibsonLanni.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        panelGibsonLanni.setLayout(gblPanelGibsonLanni);

        // Create a label for the x spacing field
        JLabel labelXYSpacing = new JLabel("xy spacing (μm):");

        // Create a spinner for the x spacing field
        SpinnerNumberModel model = new SpinnerNumberModel(0.1, 0.0, 20.0, 0.01);
        JSpinner spinner = new JSpinner(model);
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
        tabbedPane.addTab("Gibson Lanni", panelGibsonLanni);
        
        JLabel lblZSpacing = new JLabel("z spacing (μm):");
        GridBagConstraints gbc_lblZSpacing = new GridBagConstraints();
        gbc_lblZSpacing.insets = new Insets(0, 0, 5, 5);
        gbc_lblZSpacing.gridx = 0;
        gbc_lblZSpacing.gridy = 1;
        panelGibsonLanni.add(lblZSpacing, gbc_lblZSpacing);
        
        JSpinner spinnerZSpacing = new JSpinner();
        spinnerZSpacing.setModel(new SpinnerNumberModel(0.3, 0.0, 20, 0.1));
        GridBagConstraints gbc_spinnerZSpacing = new GridBagConstraints();
        gbc_spinnerZSpacing.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerZSpacing.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerZSpacing.gridx = 1;
        gbc_spinnerZSpacing.gridy = 1;
        panelGibsonLanni.add(spinnerZSpacing, gbc_spinnerZSpacing);
        
        JLabel lblNA = new JLabel("NA");
        GridBagConstraints gbc_lblNA = new GridBagConstraints();
        gbc_lblNA.insets = new Insets(0, 0, 5, 5);
        gbc_lblNA.gridx = 0;
        gbc_lblNA.gridy = 2;
        panelGibsonLanni.add(lblNA, gbc_lblNA);
        
        JSpinner spinnerNA = new JSpinner();
        spinnerNA.setModel(new SpinnerNumberModel(1.3, 0.0, 2, 0.1));
        GridBagConstraints gbc_spinnerNA = new GridBagConstraints();
        gbc_spinnerNA.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerNA.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerNA.gridx = 1;
        gbc_spinnerNA.gridy = 2;
        panelGibsonLanni.add(spinnerNA, gbc_spinnerNA);
        
        JLabel lblWavelength = new JLabel("Wavelength");
        GridBagConstraints gbc_lblWavelength = new GridBagConstraints();
        gbc_lblWavelength.insets = new Insets(0, 0, 5, 5);
        gbc_lblWavelength.gridx = 0;
        gbc_lblWavelength.gridy = 3;
        panelGibsonLanni.add(lblWavelength, gbc_lblWavelength);
        
        JSpinner spinnerWavelength = new JSpinner();
        spinnerWavelength.setModel(new SpinnerNumberModel(0.5, 0, 100, 0.1));
        GridBagConstraints gbc_spinnerWavelength = new GridBagConstraints();
        gbc_spinnerWavelength.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerWavelength.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerWavelength.gridx = 1;
        gbc_spinnerWavelength.gridy = 3;
        panelGibsonLanni.add(spinnerWavelength, gbc_spinnerWavelength);
        
        JLabel lblRiImersion = new JLabel("RI Immersion");
        GridBagConstraints gbc_lblRiImersion = new GridBagConstraints();
        gbc_lblRiImersion.insets = new Insets(0, 0, 5, 5);
        gbc_lblRiImersion.gridx = 0;
        gbc_lblRiImersion.gridy = 4;
        panelGibsonLanni.add(lblRiImersion, gbc_lblRiImersion);
        
        JSpinner spinnerRiImmersion = new JSpinner();
        spinnerRiImmersion.setModel(new SpinnerNumberModel(1.5, 0, 2, 0.1));
        GridBagConstraints gbc_spinnerRiImmersion = new GridBagConstraints();
        gbc_spinnerRiImmersion.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerRiImmersion.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerRiImmersion.gridx = 1;
        gbc_spinnerRiImmersion.gridy = 4;
        panelGibsonLanni.add(spinnerRiImmersion, gbc_spinnerRiImmersion);
        
        JLabel lblRiSample = new JLabel("RI Sample");
        GridBagConstraints gbc_lblRiSample = new GridBagConstraints();
        gbc_lblRiSample.insets = new Insets(0, 0, 5, 5);
        gbc_lblRiSample.gridx = 0;
        gbc_lblRiSample.gridy = 5;
        panelGibsonLanni.add(lblRiSample, gbc_lblRiSample);
        
        JSpinner spinnerRiSample = new JSpinner();
        spinnerRiSample.setModel(new SpinnerNumberModel(1.4, 0, 2, 0.1));
        GridBagConstraints gbc_spinnerRiSample = new GridBagConstraints();
        gbc_spinnerRiSample.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerRiSample.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerRiSample.gridx = 1;
        gbc_spinnerRiSample.gridy = 5;
        panelGibsonLanni.add(spinnerRiSample, gbc_spinnerRiSample);
        
        // Add label for PSF Depth
        JLabel lblPsfDepth = new JLabel("PSF Depth");
        GridBagConstraints gbc_lblPsfDepth = new GridBagConstraints();
        gbc_lblPsfDepth.insets = new Insets(0, 0, 5, 5);
        gbc_lblPsfDepth.gridx = 0;
        gbc_lblPsfDepth.gridy = 6; // Increment the grid y index
        panelGibsonLanni.add(lblPsfDepth, gbc_lblPsfDepth);

        // Add spinner for PSF Depth
        JSpinner spinnerPSFDepth = new JSpinner();
        spinnerPSFDepth.setModel(new SpinnerNumberModel(0.1, 0.1, 10.0, 0.1)); // Example range and step size
        GridBagConstraints gbc_spinnerPSFDepth = new GridBagConstraints();
        gbc_spinnerPSFDepth.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerPSFDepth.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerPSFDepth.gridx = 1;
        gbc_spinnerPSFDepth.gridy = 6; // Increment the grid y index
        panelGibsonLanni.add(spinnerPSFDepth, gbc_spinnerPSFDepth);
        
        JLabel lblXYSize = new JLabel("xy size");
        GridBagConstraints gbc_lblXYSize = new GridBagConstraints();
        gbc_lblXYSize.insets = new Insets(0, 0, 5, 5);
        gbc_lblXYSize.gridx = 0;
        gbc_lblXYSize.gridy = 7;
        panelGibsonLanni.add(lblXYSize, gbc_lblXYSize);
        
        JSpinner spinnerXYSize = new JSpinner();
        spinnerXYSize.setModel(new SpinnerNumberModel(new Integer(128), null, null, new Integer(1)));
        GridBagConstraints gbc_spinnerXYSize = new GridBagConstraints();
        gbc_spinnerXYSize.fill = GridBagConstraints.BOTH;
        gbc_spinnerXYSize.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerXYSize.gridx = 1;
        gbc_spinnerXYSize.gridy = 7;
        panelGibsonLanni.add(spinnerXYSize, gbc_spinnerXYSize);
        
        JLabel lblZSize = new JLabel("Z Size");
        GridBagConstraints gbc_lblZSize = new GridBagConstraints();
        gbc_lblZSize.insets = new Insets(0, 0, 0, 5);
        gbc_lblZSize.gridx = 0;
        gbc_lblZSize.gridy = 8;
        panelGibsonLanni.add(lblZSize, gbc_lblZSize);
        
        JSpinner spinnerZSize = new JSpinner();
        spinnerZSize.setModel(new SpinnerNumberModel(new Integer(128), null, null, new Integer(1)));
        GridBagConstraints gbc_spinnerZSize = new GridBagConstraints();
        gbc_spinnerZSize.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerZSize.gridx = 1;
        gbc_spinnerZSize.gridy = 8;
        panelGibsonLanni.add(spinnerZSize, gbc_spinnerZSize);
        
        tabbedPane.addTab("Gaussian", new JPanel());
        
        splitPane.setRightComponent(tabbedPane);
        
        JPanel panel = new JPanel();
        JPanel panel2 = new JPanel();
        splitPane.setLeftComponent(splitPane2);
        splitPane2.setTopComponent(panel);
        splitPane2.setBottomComponent(panel2);
        
        GridBagLayout gbl_panel2 = new GridBagLayout();
        gbl_panel2.columnWidths = new int[] {0, 0};
        gbl_panel2.rowHeights = new int[]{62, 0};
        gbl_panel2.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gbl_panel2.rowWeights = new double[]{0.0, 1.0};
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
        GridBagConstraints gbc_textArea = new GridBagConstraints();
        gbc_textArea.fill = GridBagConstraints.BOTH;
        gbc_textArea.gridx = 0;
        gbc_textArea.gridy = 1;
        panel2.add(textArea, gbc_textArea);
        
        GridBagLayout gbl_panel = new GridBagLayout();
        gbl_panel.columnWidths = new int[]{0, 0, 0};
        gbl_panel.rowHeights = new int[] {30, 0, 0, 0, 0, 0, 0, 0, 0};
        gbl_panel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        panel.setLayout(gbl_panel);
        
        JLabel lblInput = new JLabel("Input Image");
        GridBagConstraints gbc_lblInput = new GridBagConstraints();
        gbc_lblInput.insets = new Insets(0, 0, 5, 5);
        gbc_lblInput.anchor = GridBagConstraints.WEST;
        gbc_lblInput.gridx = 0;
        gbc_lblInput.gridy = 1;
        panel.add(lblInput, gbc_lblInput);
        
        JComboBox comboBoxInput = new JComboBox(new ImagePlusComboBox());
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
        GridBagConstraints gbc_spinnerIterations = new GridBagConstraints();
        gbc_spinnerIterations.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerIterations.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerIterations.gridx = 1;
        gbc_spinnerIterations.gridy = 2;
        panel.add(spinnerIterations, gbc_spinnerIterations);
        
        JLabel lblRegularizationFactor = new JLabel("Regularization Factor");
        GridBagConstraints gbc_lblRegularizationFactor = new GridBagConstraints();
        gbc_lblRegularizationFactor.anchor = GridBagConstraints.WEST;
        gbc_lblRegularizationFactor.insets = new Insets(0, 0, 5, 5);
        gbc_lblRegularizationFactor.gridx = 0;
        gbc_lblRegularizationFactor.gridy = 3;
        panel.add(lblRegularizationFactor, gbc_lblRegularizationFactor);
        
        JSpinner spinnerRegularizationFactor = new JSpinner();
        GridBagConstraints gbc_spinnerRegularizationFactor = new GridBagConstraints();
        gbc_spinnerRegularizationFactor.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerRegularizationFactor.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerRegularizationFactor.gridx = 1;
        gbc_spinnerRegularizationFactor.gridy = 3;
        panel.add(spinnerRegularizationFactor, gbc_spinnerRegularizationFactor);
        
        SpinnerNumberModel modelIterations = new SpinnerNumberModel(10, 0, 100, 1);
        spinnerIterations.setModel(modelIterations);
        
        SpinnerNumberModel modelRegularizationFactor = new SpinnerNumberModel(0.0002f, 0.0f, 100.0f, 0.0001f);
        spinnerRegularizationFactor.setModel(modelRegularizationFactor);
        JSpinner.NumberEditor editorFloat = new JSpinner.NumberEditor(spinnerRegularizationFactor, "#0.0000");
        spinnerRegularizationFactor.setEditor(editorFloat);
        
        JButton btnRun = new JButton("Run");
                
        JLabel lblUseCells = new JLabel("Use Cells");
        GridBagConstraints gbc_lblUseCells = new GridBagConstraints();
        gbc_lblUseCells.insets = new Insets(0, 0, 5, 5);
        gbc_lblUseCells.gridx = 0;
        gbc_lblUseCells.gridy = 4;
        panel.add(lblUseCells, gbc_lblUseCells);
        
        JCheckBox chckbxNewCheckBox = new JCheckBox("");
        GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
        gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
        gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 0);
        gbc_chckbxNewCheckBox.gridx = 1;
        gbc_chckbxNewCheckBox.gridy = 4;
        panel.add(chckbxNewCheckBox, gbc_chckbxNewCheckBox);
        
        JLabel lblNumCells = new JLabel("Cell XY");
        GridBagConstraints gbc_lblNumCells = new GridBagConstraints();
        gbc_lblNumCells.insets = new Insets(0, 0, 5, 5);
        gbc_lblNumCells.gridx = 0;
        gbc_lblNumCells.gridy = 5;
        panel.add(lblNumCells, gbc_lblNumCells);
        
        JSpinner spinner_1 = new JSpinner();
        spinner_1.setModel(new SpinnerNumberModel(new Integer(256), null, null, new Integer(1)));
        GridBagConstraints gbc_spinner_1 = new GridBagConstraints();
        gbc_spinner_1.fill = GridBagConstraints.BOTH;
        gbc_spinner_1.insets = new Insets(0, 0, 5, 0);
        gbc_spinner_1.gridx = 1;
        gbc_spinner_1.gridy = 5;
        panel.add(spinner_1, gbc_spinner_1);
        
        JLabel lblNewLabel = new JLabel("Cell Z");
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
        gbc_lblNewLabel.gridx = 0;
        gbc_lblNewLabel.gridy = 6;
        panel.add(lblNewLabel, gbc_lblNewLabel);
        
        JSpinner spinner_2 = new JSpinner();
        spinner_2.setModel(new SpinnerNumberModel(new Integer(128), null, null, new Integer(1)));
        GridBagConstraints gbc_spinner_2 = new GridBagConstraints();
        gbc_spinner_2.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinner_2.insets = new Insets(0, 0, 5, 0);
        gbc_spinner_2.gridx = 1;
        gbc_spinner_2.gridy = 6;
        panel.add(spinner_2, gbc_spinner_2);
        GridBagConstraints gbc_btnRun = new GridBagConstraints();
        gbc_btnRun.gridx = 1;
        gbc_btnRun.gridy = 7;
        panel.add(btnRun, gbc_btnRun);
        
        btnRun.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		ImagePlus imp = (ImagePlus)comboBoxInput.getSelectedItem();
        		ImagePlus psf = (ImagePlus)comboBoxPSF.getSelectedItem();
        		
        		   // Get values from GUI components
                int psfType = comboBoxPSF.getSelectedIndex();
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

                // Update model using setters
                modelController.setPsfType(psfType);
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
        		
        		System.out.println("run decon on "+imp.getTitle()+" with PSF "+psf.getTitle());
        		System.out.println("x spacing "+modelController.getXYSpacing());
        		System.out.println("PSF Tab "+tabbedPane.getSelectedIndex());
        		
        		new Thread("Deconvolution Thread") {
        			@Override
        			public void run() {
        				modelController.runDeconvolution(imp, psf, 100);
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
	            	progressBar.setMaximum(maximum);
	            	progressBar.setValue(progress);
            	}
            	SwingUtilities.invokeLater(() -> {
	            	textArea.append(message);
	            	progressBar.setMaximum(maximum);
	            	progressBar.setValue(progress);
            	});
            	//progress.
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