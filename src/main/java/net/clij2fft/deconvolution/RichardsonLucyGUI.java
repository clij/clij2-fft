package net.clij2fft.deconvolution;

import java.awt.BorderLayout;
import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ij.ImagePlus;
import ij.WindowManager;

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

public class RichardsonLucyGUI extends JFrame {
	
	RichardsonLucyModelController modelController = new RichardsonLucyModelController();
	
    public RichardsonLucyGUI() {
        // Set the layout manager for the content pane
        getContentPane().setLayout(new BorderLayout());
        
        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.5);
        getContentPane().add(splitPane, BorderLayout.CENTER);
        
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
        gblPanelGibsonLanni.rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
        gblPanelGibsonLanni.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gblPanelGibsonLanni.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
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
                modelController.setxSpacing( ((Double)spinner.getValue()).floatValue());
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
        gbc_lblRiSample.insets = new Insets(0, 0, 0, 5);
        gbc_lblRiSample.gridx = 0;
        gbc_lblRiSample.gridy = 5;
        panelGibsonLanni.add(lblRiSample, gbc_lblRiSample);
        
        JSpinner spinnerRiSample = new JSpinner();
        spinnerRiSample.setModel(new SpinnerNumberModel(1.4, 0, 2, 0.1));
        GridBagConstraints gbc_spinnerRiSample = new GridBagConstraints();
        gbc_spinnerRiSample.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerRiSample.gridx = 1;
        gbc_spinnerRiSample.gridy = 5;
        panelGibsonLanni.add(spinnerRiSample, gbc_spinnerRiSample);
        
        // Add label for PSF Depth
        JLabel lblPsfDepth = new JLabel("PSF Depth");
        GridBagConstraints gbc_lblPsfDepth = new GridBagConstraints();
        gbc_lblPsfDepth.insets = new Insets(0, 0, 0, 5);
        gbc_lblPsfDepth.gridx = 0;
        gbc_lblPsfDepth.gridy = 6; // Increment the grid y index
        panelGibsonLanni.add(lblPsfDepth, gbc_lblPsfDepth);

        // Add spinner for PSF Depth
        JSpinner spinnerPSFDepth = new JSpinner();
        spinnerPSFDepth.setModel(new SpinnerNumberModel(0.1, 0.1, 10.0, 0.1)); // Example range and step size
        GridBagConstraints gbc_spinnerPSFDepth = new GridBagConstraints();
        gbc_spinnerPSFDepth.fill = GridBagConstraints.HORIZONTAL;
        gbc_spinnerPSFDepth.gridx = 1;
        gbc_spinnerPSFDepth.gridy = 6; // Increment the grid y index
        panelGibsonLanni.add(spinnerPSFDepth, gbc_spinnerPSFDepth);
        
        tabbedPane.addTab("Gaussian", new JPanel());
        
        splitPane.setRightComponent(tabbedPane);
        
        JPanel panel = new JPanel();
        splitPane.setLeftComponent(panel);
        GridBagLayout gbl_panel = new GridBagLayout();
        gbl_panel.columnWidths = new int[]{0, 0, 0};
        gbl_panel.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
        gbl_panel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
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
        
        JButton btnRun = new JButton("Run");
        btnRun.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		ImagePlus imp = (ImagePlus)comboBoxInput.getSelectedItem();
        		ImagePlus psf = (ImagePlus)comboBoxPSF.getSelectedItem();
        		
        		System.out.println("run decon on "+imp.getTitle()+" with PSF "+psf.getTitle());
        		
        		System.out.println("x spacing "+modelController.getxSpacing());
        		
        		modelController.runDeconvolution(imp, psf, 100);
        	}
        });
        GridBagConstraints gbc_btnRun = new GridBagConstraints();
        gbc_btnRun.gridx = 1;
        gbc_btnRun.gridy = 4;
        panel.add(btnRun, gbc_btnRun);
        
        SpinnerNumberModel modelIterations = new SpinnerNumberModel(10, 0, 100, 1);
        spinnerIterations.setModel(modelIterations);
        
        SpinnerNumberModel modelRegularizationFactor = new SpinnerNumberModel(0.0002f, 0.0f, 100.0f, 0.0001f);
        spinnerRegularizationFactor.setModel(modelRegularizationFactor);
        JSpinner.NumberEditor editorFloat = new JSpinner.NumberEditor(spinnerRegularizationFactor, "#0.0000");
        spinnerRegularizationFactor.setEditor(editorFloat);

    }
    
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RichardsonLucyGUI().setVisible(true));
    }
}